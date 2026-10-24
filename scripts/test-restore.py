#!/usr/bin/env python3
"""Exercise recovery against a fake adb; never connects to a device."""
import json
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parent.parent
cases = [
    ("flar2.homebutton/a.i:com.beryu.tvhome/.HomeRedirectService", "flar2.homebutton/a.i"),
    ("com.beryu.tvhome/com.beryu.tvhome.HomeRedirectService:other.pkg/.Service", "other.pkg/.Service"),
    ("com.beryu.tvhome/.HomeRedirectService", ""),
    ("flar2.homebutton/a.i", None),
    ("null", ""),
    ("flar2.homebutton/a.i:com.beryu.tvhome/.HomeKeyService", "flar2.homebutton/a.i"),
    ("com.beryu.tvhome/com.beryu.tvhome.HomeKeyService:other.pkg/.Service", "other.pkg/.Service"),
    ("com.beryu.tvhome/.HomeKeyService:com.beryu.tvhome/.HomeRedirectService", ""),
]
with tempfile.TemporaryDirectory() as folder:
    folder = Path(folder)
    fake = folder / "adb"
    fake.write_text("""#!/usr/bin/env python3
import json, os, sys
with open(os.environ['QUIET_TV_TEST_LOG'], 'a') as output:
    output.write(json.dumps(sys.argv[1:]) + '\\n')
if 'get' in sys.argv and 'enabled_accessibility_services' in sys.argv:
    print(os.environ['QUIET_TV_TEST_SERVICES'])
""")
    fake.chmod(0o755)
    for current, expected in cases:
        log = folder / "calls.jsonl"
        log.write_text("")
        env = dict(os.environ, ADB=str(fake), QUIET_TV_TEST_LOG=str(log), QUIET_TV_TEST_SERVICES=current)
        subprocess.run(["sh", str(root / "scripts/restore-google-tv.sh"), "test-device"], env=env, check=True)
        calls = [json.loads(line)[3:] for line in log.read_text().splitlines()]
        writes = [args for args in calls if args[:2] in (["settings", "put"], ["settings", "delete"])]
        wanted = [] if expected is None else [
            ["settings", "put", "secure", "enabled_accessibility_services", expected]
            if expected else ["settings", "delete", "secure", "enabled_accessibility_services"]
        ]
        assert writes == wanted, (current, writes, wanted)
        assert ["cmd", "package", "set-home-activity", "--user", "0",
                "com.google.android.apps.tv.launcherx/.home.HomeActivity"] in calls
print("8 recovery checks passed; existing accessibility services preserved.")
