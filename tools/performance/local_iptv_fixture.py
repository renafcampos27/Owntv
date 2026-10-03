"""Create/serve synthetic IPTV content for TV/phone benchmarks, without provider credentials.

Requires ffmpeg in PATH only when creating the fixture. Serve uses Python's standard library.
"""
from argparse import ArgumentParser
from datetime import datetime, timedelta, timezone
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import shutil
import subprocess
from xml.sax.saxutils import escape


def create(destination: Path, base_url: str, channel_count: int) -> None:
    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg is None:
        raise SystemExit("ffmpeg is required to create synthetic HLS. Existing content is not changed.")
    # Dedicated destination only; never overwrite an existing fixture/catalog.
    destination.mkdir(parents=True, exist_ok=False)
    subprocess.run([
        ffmpeg, "-nostdin", "-f", "lavfi", "-i", "testsrc2=size=640x360:rate=25",
        "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=48000",
        "-t", "180", "-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p",
        "-g", "50", "-c:a", "aac", "-b:a", "96k", "-f", "hls", "-hls_time", "2",
        "-hls_list_size", "0", "-hls_playlist_type", "vod", str(destination / "stream.m3u8"),
    ], check=True)
    base_url = base_url.rstrip("/")
    m3u = [f'#EXTM3U url-tvg="{base_url}/epg.xml"']
    xml = ['<?xml version="1.0" encoding="UTF-8"?>', '<tv>']
    now = datetime.now(timezone.utc).replace(minute=0, second=0, microsecond=0)
    for index in range(1, channel_count + 1):
        identifier = f"fixture.{index}"
        title = f"Fixture Channel {index:03}"
        m3u.extend([f'#EXTINF:-1 tvg-id="{identifier}" group-title="Fixture",{title}',
                    f"{base_url}/stream.m3u8?channel={index}"])
        xml.append(f'<channel id="{identifier}"><display-name>{escape(title)}</display-name></channel>')
        for hour in range(-24, 25):
            start = now + timedelta(hours=hour)
            stop = start + timedelta(hours=1)
            xml.append(f'<programme channel="{identifier}" start="{start:%Y%m%d%H%M%S} +0000" '
                       f'stop="{stop:%Y%m%d%H%M%S} +0000"><title>Fixture Programme {hour + 24:02}</title></programme>')
    xml.append('</tv>')
    (destination / "channels.m3u").write_text("\n".join(m3u) + "\n", encoding="utf-8")
    (destination / "epg.xml").write_text("\n".join(xml) + "\n", encoding="utf-8")


def main() -> None:
    parser = ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=["create", "serve"])
    parser.add_argument("directory", type=Path)
    parser.add_argument("--base-url", default="http://10.0.2.2:8765")
    parser.add_argument("--channels", type=int, default=240)
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    if args.mode == "create":
        if not 3 <= args.channels <= 1000:
            parser.error("--channels must be between 3 and 1000")
        create(args.directory, args.base_url, args.channels)
    else:
        directory = args.directory.resolve(strict=True)
        # Loopback only. A physical device can use adb reverse and a fixture base URL of 127.0.0.1.
        server = ThreadingHTTPServer(("127.0.0.1", args.port), partial(SimpleHTTPRequestHandler, directory=str(directory)))
        print(f"Fixture: http://127.0.0.1:{args.port}/channels.m3u", flush=True)
        try:
            server.serve_forever()
        finally:
            server.server_close()


if __name__ == "__main__":
    main()
