"""Offline playback checks. Run with python -m unittest discover -s tools/performance."""
from concurrent.futures import ThreadPoolExecutor
from functools import partial
from http.server import ThreadingHTTPServer
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import threading
import time
import unittest
from urllib.request import urlopen

from local_iptv_fixture import SegmentHandler, create


@unittest.skipUnless(shutil.which("ffmpeg"), "ffmpeg is required for playback checks")
class PlaybackFixtureTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.media = Path(temp.name) / "media"

    def test_silent_audio_while_video_continues(self):
        create(self.media, "http://127.0.0.1", 3, audio_gap=True, duration_seconds=16)
        result = subprocess.run([
            "ffmpeg", "-hide_banner", "-nostats", "-nostdin", "-i", str(self.media / "stream.m3u8"),
            "-vf", "showinfo", "-af", "silencedetect=n=-55dB:d=1.5", "-f", "null", "-",
        ], check=True, capture_output=True, text=True, timeout=30)
        starts = re.findall(r"silence_start: ([\d.]+)", result.stderr)
        ends = re.findall(r"silence_end: ([\d.]+)", result.stderr)
        self.assertEqual((len(starts), len(ends)), (1, 1))
        start, end = float(starts[0]), float(ends[0])
        self.assertAlmostEqual(start, 8, delta=0.1)
        self.assertAlmostEqual(end, 10, delta=0.1)
        frames = [float(t) for t in re.findall(r"pts_time:\s*([\d.]+)", result.stderr)]
        during = [t for t in frames if start <= t <= end]
        self.assertGreaterEqual(len(during), 48)
        self.assertLess(max(b - a for a, b in zip(during, during[1:])), 0.05)
        self.assertGreater(frames[-1], 15)
        print(f"AAC silence {start:.3f}-{end:.3f} s; {len(during)} decoded video frames")

    def test_delayed_hls_segment_stalls_reader_then_resumes(self):
        create(self.media, "http://127.0.0.1", 3, duration_seconds=16)
        requested = threading.Event()

        class Handler(SegmentHandler):
            def do_GET(self):
                if self.path == "/stream4.ts":
                    requested.set()
                super().do_GET()

        handler = partial(Handler, directory=str(self.media), delay_ms=4000)
        with ThreadingHTTPServer(("127.0.0.1", 0), handler) as server:
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                base = f"http://127.0.0.1:{server.server_port}"
                def check_manifest_during_delay():
                    self.assertTrue(requested.wait(timeout=15))
                    with urlopen(base + "/stream.m3u8", timeout=2) as response:
                        self.assertTrue(response.read().startswith(b"#EXTM3U"))
                    return time.monotonic()

                # One HTTP request at a time so the delayed segment cannot be prefetched.
                with subprocess.Popen([
                    "ffmpeg", "-hide_banner", "-nostdin", "-nostats", "-http_multiple", "0",
                    "-analyzeduration", "200000", "-probesize", "100000", "-i", base + "/stream.m3u8",
                    "-an", "-threads", "1", "-vf", "showinfo", "-f", "null", "-",
                ], stderr=subprocess.PIPE, text=True) as proc, ThreadPoolExecutor(max_workers=1) as checks:
                    watchdog = threading.Timer(30, proc.kill)
                    watchdog.start()
                    frames = []
                    manifest_check = checks.submit(check_manifest_during_delay)
                    try:
                        for line in proc.stderr:
                            arrival = time.monotonic()
                            match = re.search(r"pts_time:\s*([\d.]+)", line)
                            if match:
                                frames.append((float(match[1]), arrival))
                    finally:
                        if proc.poll() is None and not frames:
                            proc.kill()
                        proc.wait(timeout=30)
                        watchdog.cancel()
                    self.assertEqual(proc.returncode, 0)
                    manifest_received = manifest_check.result()
            finally:
                server.shutdown()
                thread.join()
        self.assertGreaterEqual(frames[-1][0], 15)
        left, right = max(zip(frames, frames[1:]), key=lambda pair: pair[1][1] - pair[0][1])
        gap, before, after = right[1] - left[1], left[0], right[0]
        self.assertLess(manifest_received, right[1])
        self.assertGreaterEqual(gap, 3.6)
        self.assertTrue(6 <= before <= 8.1 and after - before < 0.05, (before, after))
        print(f"HLS reader waited {gap:.2f} s at {before:.2f} s; resumed through {frames[-1][0]:.2f} s")


if __name__ == "__main__":
    unittest.main()
