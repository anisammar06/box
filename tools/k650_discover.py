#!/usr/bin/env python3
"""
k650_discover.py — probe which WAM/UIC/CPM commands the HW-K650 firmware honours.

Standard library only. Follows the two hard rules from the field notes:

  1. ENCODING: spaces are percent-encoded as %20. urllib's default quoting via
     urlencode() would turn them into "+", which the bar rejects with errcode 53.
     We build the query manually with quote(safe="").

  2. NOTIFICATION QUIRK: the bar queues notifications and may return a PREVIOUS
     request's reply. So we never trust an immediate ack — for the "does this do
     anything" probes we snapshot state, fire the command, wait, and re-read.

Usage:
    python3 k650_discover.py [--host 192.168.0.107] [--port 55001]
    python3 k650_discover.py --prev      # only probe the unknown "previous track"
    python3 k650_discover.py --advanced  # only probe advanced settings
"""

import argparse
import sys
import time
import urllib.request
from urllib.parse import quote
from xml.etree import ElementTree as ET

SETTLE = 0.6  # seconds — matches the app's read-back delay


def build_url(host, port, module, cmd_xml):
    # Percent-encode the whole XML; space -> %20 (safe="" quotes everything).
    return f"http://{host}:{port}/{module}?cmd={quote(cmd_xml, safe='')}"


def call(host, port, module, cmd_xml, timeout=3.0):
    """Return (ok, raw_text_or_None, error_or_None). Timeout is not an error."""
    url = build_url(host, port, module, cmd_xml)
    try:
        with urllib.request.urlopen(url, timeout=timeout) as resp:
            return True, resp.read().decode("utf-8", "replace"), None
    except urllib.error.URLError as e:
        reason = getattr(e, "reason", e)
        # A socket timeout here is expected for some commands (e.g. SetFunc to an
        # already-active source) — report it distinctly, not as a hard failure.
        return False, None, f"{type(reason).__name__}: {reason}"
    except Exception as e:  # noqa: BLE001
        return False, None, f"{type(e).__name__}: {e}"


def flatten(xml_text):
    """Best-effort {tag: text} of all leaf elements, plus response @result."""
    values = {}
    result = None
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return result, values
    for el in root.iter():
        if el.tag == "response":
            result = el.attrib.get("result")
        text = (el.text or "").strip()
        if text:
            values[el.tag.lower()] = text
    return result, values


def read_state(host, port):
    """Snapshot the fields relevant to track/playback probes."""
    snap = {}
    for module, cmd, keys in (
        ("CPM", "<name>GetRadioInfo</name>", ("title", "artist", "playstatus")),
        ("UIC", "<name>GetVolume</name>", ("volume",)),
        ("UIC", "<name>GetFunc</name>", ("function", "submode")),
    ):
        ok, raw, _ = call(host, port, module, cmd)
        if ok and raw:
            _, vals = flatten(raw)
            for k in keys:
                snap[k] = vals.get(k)
    return snap


def probe_reads(host, port):
    print("== Reads (baseline state) ==")
    reads = [
        ("UIC", "GetSpkName"),
        ("UIC", "GetVolume"),
        ("UIC", "GetMute"),
        ("UIC", "GetFunc"),
        ("CPM", "GetRadioInfo"),
    ]
    for module, name in reads:
        ok, raw, err = call(host, port, module, f"<name>{name}</name>")
        if ok:
            result, vals = flatten(raw)
            print(f"  [{module}] {name:16} result={result} {vals}")
        else:
            print(f"  [{module}] {name:16} ERROR {err}")


def probe_previous_track(host, port):
    """Fire each candidate and judge by whether title/artist actually changed."""
    print("\n== Probing 'previous track' candidates (state-diff judged) ==")
    candidates = [
        ("CPM", "<name>SetPreviousTrack</name>"),
        ("CPM", "<name>SetSkipPreviousTrack</name>"),
        ("CPM", "<name>SetPrevTrack</name>"),
        ("CPM", "<name>SetSkipBackwardTrack</name>"),
        # For reference / contrast, the known-good "next" (no param):
        ("CPM", "<name>SetSkipCurrentTrack</name>"),
    ]
    for module, cmd in candidates:
        before = read_state(host, port)
        ok, _raw, err = call(host, port, module, cmd)
        time.sleep(SETTLE)
        after = read_state(host, port)
        changed = (before.get("title"), before.get("artist")) != (
            after.get("title"), after.get("artist"))
        status = "sent" if ok else f"no-ack ({err})"
        verdict = "TRACK CHANGED ✔" if changed else "no change"
        name = cmd.split(">")[1].split("<")[0]
        print(f"  {name:22} {status:24} -> {verdict}")
        print(f"       before={before.get('title')!r}/{before.get('artist')!r}"
              f"  after={after.get('title')!r}/{after.get('artist')!r}")


def probe_advanced(host, port):
    """Read-only sonde of advanced settings before integrating any of them."""
    print("\n== Probing advanced settings (read-only) ==")
    probes = [
        ("UIC", "<name>GetWooferLevel</name>"),
        ("UIC", "<name>Get7bandEQMode</name>"),
        ("UIC", "<name>Get7BandEQList</name>"),
        ("UIC", "<name>GetCurrentEQMode</name>"),
        ("UIC", "<name>GetSoftwareVersion</name>"),
        ("UIC", "<name>GetLedStatus</name>"),
        ("UIC", "<name>GetAutoUpdate</name>"),
        ("UIC", "<name>GetSleepTimer</name>"),
        ("UIC", "<name>GetAlarmInfo</name>"),
        ("CPM", "<name>GetCurrentPlayTime</name>"),
    ]
    for module, cmd in probes:
        ok, raw, err = call(host, port, module, cmd)
        name = cmd.split(">")[1].split("<")[0]
        if ok:
            result, vals = flatten(raw)
            supported = "SUPPORTED" if result == "ok" else "rejected"
            print(f"  [{module}] {name:22} {supported:10} result={result} {vals}")
        else:
            print(f"  [{module}] {name:22} no-ack/timeout ({err})")


def main():
    ap = argparse.ArgumentParser(description="Probe HW-K650 firmware commands.")
    ap.add_argument("--host", default="192.168.0.107")
    ap.add_argument("--port", type=int, default=55001)
    ap.add_argument("--prev", action="store_true", help="only probe previous-track")
    ap.add_argument("--advanced", action="store_true", help="only probe advanced settings")
    args = ap.parse_args()

    only = args.prev or args.advanced
    if not only:
        probe_reads(args.host, args.port)
    if args.prev or not only:
        probe_previous_track(args.host, args.port)
    if args.advanced or not only:
        probe_advanced(args.host, args.port)

    print("\nDone. 'SUPPORTED'/'TRACK CHANGED' lines are the only ones to trust —")
    print("acks are unreliable on this firmware (notification stacking).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
