#!/usr/bin/env python3
"""Portable archive inspection tests; synthetic APKs, not signed/installable packages."""
import importlib.util
import tempfile
import zipfile
from pathlib import Path

root=Path(__file__).resolve().parent.parent
spec=importlib.util.spec_from_file_location('apk_audit', root/'scripts/audit_apk.py')
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

with tempfile.TemporaryDirectory() as folder:
    folder=Path(folder)
    def apk(name, libs=(), manifest=True):
        path=folder/name
        with zipfile.ZipFile(path, 'w') as z:
            if manifest:
                z.writestr('AndroidManifest.xml', b'synthetic')
            z.writestr('classes.dex', b'synthetic')
            for lib in libs:
                z.writestr(f'lib/{lib}/libtest.so', b'synthetic')
        return path
    basic=mod.inspect_archive(apk('base.apk'))
    assert basic['pure_java'] and basic['abis']==[]
    both=mod.inspect_archive(apk('both.apk',('armeabi-v7a','arm64-v8a')))
    assert both['abis']==['arm64-v8a','armeabi-v7a']
    for path in [apk('old.apk',('armeabi-v7a',)), apk('invalid.apk',manifest=False)]:
        try: mod.inspect_archive(path)
        except ValueError: pass
        else: raise AssertionError(f'Expected rejection: {path.name}')
    try: mod.inspect_archive(apk('wrong.xapk'))
    except ValueError: pass
    else: raise AssertionError('Expected single APK format rejection')
    assert len(basic['sha256'])==64
print('test_apk_audit.py: PASS (Java-only, multi-ABI, 32-bit-only rejection, invalid APK, bundle confusion)')
