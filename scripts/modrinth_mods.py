"""Upload the jars to Modrinth as versions of the project, each marked where it runs: Paper's on the server, the mods' on either side.

Standard library only: MODRINTH_TOKEN=… python3 -I scripts/modrinth_mods.py --version X.Y.Z --notes release-notes.md
The release workflow runs it after `./gradlew build`. Minotaur can't set a version's `environment`, which Modrinth
asks of every version (and refuses a version without while the project is in review, as 2.19.0's Paper jar found), so
every jar goes up through Modrinth's v3 API here; Minotaur only syncs the description. A version already uploaded is skipped.
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROJECT = "mobosses"
API = "https://api.modrinth.com"
AGENT = "MairwunNx/mobosses (github.com/MairwunNx)"
PLATFORMS = {
    # platform: its name on the version, its loaders, the projects it can't run without, the ones it works with, and where
    # it runs. The mods run on either side: on a dedicated server and under a singleplayer or LAN world. Players joining a
    # server with them need nothing on their side, and Kotlin is inside the jar.
    "paper": ("Paper", ["paper", "purpur"], [], [], "server_only"),
    "fabric": ("Fabric", ["fabric"], ["fabric-api"], ["modmenu", "jei", "rei", "placeholder-api", "flan", "open-parties-and-claims", "luckperms"], "client_or_server"),
    "neoforge": ("NeoForge", ["neoforge"], [], ["jei", "rei", "flan", "open-parties-and-claims", "luckperms"], "client_or_server"),
}


def game_version(platform):
    """The game version a platform's build names: Modrinth's `gameVersions` for Paper, `minecraft` for a mod."""
    build = (ROOT / platform / "build.gradle.kts").read_text()
    pattern = r'gameVersions\.addAll\("([^"]+)"' if platform == "paper" else r'val minecraft = "([^"]+)"'
    return re.search(pattern, build).group(1)


def request(method, path, token, body=None, headers=None):
    req = urllib.request.Request(f"{API}{path}", data=body, method=method,
                                 headers={"User-Agent": AGENT, "Authorization": token, **(headers or {})})
    try:
        with urllib.request.urlopen(req, timeout=120) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        sys.exit(f"{method} {path}: {error.code} {error.read().decode(errors='replace')}")


def project_id(slug, token):
    return request("GET", f"/v2/project/{slug}", token)["id"]


def multipart(data, file_name, file_bytes):
    boundary = uuid.uuid4().hex
    parts = [
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"data\"\r\nContent-Type: application/json\r\n\r\n".encode(),
        json.dumps(data).encode(), b"\r\n",
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{file_name}\"\r\n"
        f"Content-Type: application/java-archive\r\n\r\n".encode(),
        file_bytes, f"\r\n--{boundary}--\r\n".encode(),
    ]
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--version", required=True)
    parser.add_argument("--notes", required=True, help="a Markdown file with the release notes")
    args = parser.parse_args()
    token = os.environ.get("MODRINTH_TOKEN") or sys.exit("MODRINTH_TOKEN isn't set")
    notes = Path(args.notes).read_text(encoding="utf-8")
    project = project_id(PROJECT, token)
    uploaded = {v["version_number"] for v in request("GET", f"/v2/project/{project}/version", token)}

    # Without the mods, as before 2.11.0, Paper's jar alone goes up, numbered X.Y.Z, as `paper/build.gradle.kts` names it.
    mods = re.search(r"^mobosses\.mods=true\s*$", (ROOT / "gradle.properties").read_text(), re.M) is not None
    for platform, (name, loaders, needs, works_with, environment) in PLATFORMS.items():
        if not mods and platform != "paper":
            continue
        number = f"{args.version}+{platform}" if mods else args.version
        if number in uploaded:
            print(f"{number}: already on Modrinth")
            continue
        jar = ROOT / platform / "build/libs" / f"mobosses-{platform}-{args.version}.jar"
        data = {
            "name": f"Mo’Bosses {args.version} for {name}" if mods else f"Mo’Bosses {args.version}", "version_number": number, "changelog": notes,
            "game_versions": [game_version(platform)], "loaders": loaders, "version_type": "release", "featured": True,
            "project_id": project, "status": "listed", "environment": environment,
            "dependencies": [{"project_id": project_id(slug, token), "dependency_type": "required"} for slug in needs]
            + [{"project_id": project_id(slug, token), "dependency_type": "optional"} for slug in works_with],
            "file_parts": ["file"], "primary_file": "file",
        }
        body, content_type = multipart(data, jar.name, jar.read_bytes())
        version = request("POST", "/v3/version", token, body, {"Content-Type": content_type})
        print(f"{number}: {version['id']}, {version.get('environment')}")


if __name__ == "__main__":
    main()
