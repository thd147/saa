#!/usr/bin/env python3
"""Static checks for honest app labels and sensitive-permission separation."""
from pathlib import Path
from xml.etree import ElementTree as ET

root = Path(__file__).resolve().parent.parent
android = '{http://schemas.android.com/apk/res/android}'
main = root / 'app/src/main/AndroidManifest.xml'
preview = root / 'preview/src/main/AndroidManifest.xml'
for m in [main, preview, root/'app/src/main/res/values/strings.xml', root/'preview/src/main/res/values/strings.xml']:
    ET.parse(m)
    assert m.exists(), str(m)
main_xml, preview_xml = ET.parse(main).getroot(), ET.parse(preview).getroot()
main_raw, preview_raw = main.read_text(), preview.read_text()
assert 'StairAccessibilityService' in main_raw
assert 'BIND_ACCESSIBILITY_SERVICE' in main_raw
assert 'FOREGROUND_SERVICE_MEDIA_PROJECTION' in main_raw
assert 'android.permission.INTERNET' not in main_raw
assert not preview_xml.findall('uses-permission'), 'preview must not request permissions'
assert preview_xml.find('application') is not None
assert len(preview_xml.find('application').findall('service')) == 0, 'preview must not register services'
assert 'BIND_ACCESSIBILITY_SERVICE' not in preview_raw
assert 'FOREGROUND_SERVICE' not in preview_raw
assert 'usesCleartextTraffic="false"' in preview_raw
assert '<string name="app_name">무계매크로</string>' in (root/'app/src/main/res/values/strings.xml').read_text()
assert '<string name="app_name">무계매크로 미리보기</string>' in (root/'preview/src/main/res/values/strings.xml').read_text()
assert "versionName '1.5.0'" in (root/'app/build.gradle').read_text()
assert "include ':app', ':preview'" in (root/'settings.gradle').read_text()
assert 'showPermissionNotice(true)' in (root/'app/src/main/java/io/stairpilot/MainActivity.java').read_text()
assert (root/'assets/StairPilot-icon-preview.png').stat().st_size > 1000
print('check_project.py: PASS (2 manifests, labels, disclosure, preview isolation, icon)')
