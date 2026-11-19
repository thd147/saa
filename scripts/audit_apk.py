#!/usr/bin/env python3
"""Inspect standard Android APKs, signatures, ABIs and permission separation.

This tool deliberately DOES NOT install APKs or bypass Play Protect.
Run --macro + --preview on CI after building; optional SDK tools verify identity.
"""
import argparse
import hashlib
import re
import subprocess
import zipfile
from pathlib import Path

CPU64 = {'arm64-v8a', 'x86_64', 'mips64'}
CPU32 = {'armeabi-v7a', 'armeabi', 'x86', 'mips'}


def inspect_archive(path):
    path = Path(path)
    if path.suffix.lower() != '.apk':
        raise ValueError('Expected a single .apk file, not ZIP/.apkm/.xapk')
    if not path.is_file() or not zipfile.is_zipfile(path):
        raise ValueError('Not a readable APK ZIP archive')
    with zipfile.ZipFile(path) as f:
        bad = f.testzip()
        if bad is not None:
            raise ValueError(f'Corrupted ZIP member: {bad}')
        names = set(f.namelist())
        if 'AndroidManifest.xml' not in names or 'classes.dex' not in names:
            raise ValueError('Missing AndroidManifest.xml or classes.dex')
        abis = sorted(set(m.group(1) for name in names
                          if (m := re.fullmatch(r'lib/([^/]+)/[^/]+\.so', name))))
        if abis and not set(abis).intersection(CPU64):
            raise ValueError(f'Native libraries found without a 64-bit variant: {abis}')
    sha = hashlib.sha256(path.read_bytes()).hexdigest()
    return {'file': path.name, 'sha256': sha, 'bytes': path.stat().st_size,
            'abis': abis, 'pure_java': not abis}


def sdk_run(binary, args):
    result = subprocess.run([binary, *args], stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, check=False)
    if result.returncode != 0:
        raise ValueError(f'{binary} error ({result.returncode}): {result.stdout[-1000:]}')
    return result.stdout


def audit(path, app_id, aapt=None, apksigner=None, preview=False):
    info = inspect_archive(path)
    info['package'] = app_id
    info['signature'] = 'SDK tool not provided; NOT VERIFIED'
    if aapt:
        dump = sdk_run(aapt, ['dump', 'badging', str(path)])
        if not re.search(r"package: name='" + re.escape(app_id) + r"'", dump):
            raise ValueError(f'Incorrect package ID, expected {app_id}')
        if "versionName='1.6.0'" not in dump:
            raise ValueError('APK versionName is not 1.6.0')
        perms = sdk_run(aapt, ['dump', 'permissions', str(path)])
        if 'android.permission.REQUEST_INSTALL_PACKAGES' in perms:
            raise ValueError('APK unnecessarily requests package installer permission')
        if preview:
            if re.search(r'^uses-permission:', perms, re.M):
                raise ValueError('Preview APK unexpectedly requests permissions')
        else:
            for needed in ('android.permission.FOREGROUND_SERVICE',
                           'android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION'):
                if needed not in perms:
                    raise ValueError(f'Expected permission absent: {needed}')
        info['manifest_permissions'] = 'AAPT validation passed'
    if apksigner:
        signature = sdk_run(apksigner, ['verify', '--print-certs', str(path)])
        match = re.search(r'certificate SHA-256 digest:\s*([0-9a-f:]+)', signature, re.I)
        if not match:
            raise ValueError('APK signed, but SHA-256 certificate digest not found')
        info['signature'] = match.group(1)
    return info


def report(info):
    return (f"APK: {info['file']}\nPackage: {info['package']}\n"
            f"Size: {info['bytes']} bytes\nSHA-256: {info['sha256']}\n"
            f"Native ABIs: {', '.join(info['abis']) if info['abis'] else 'none (Java/Dex only)'}\n"
            f"Signer certificate SHA-256: {info['signature']}\n"
            f"Permission validation: {info.get('manifest_permissions', 'SDK tool not provided')}\n")


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--macro', required=True)
    p.add_argument('--preview', required=True)
    p.add_argument('--aapt', default=None)
    p.add_argument('--apksigner', default=None)
    p.add_argument('--output', required=True)
    args = p.parse_args()
    a = audit(args.macro, 'io.stairpilot.mobile', args.aapt, args.apksigner)
    b = audit(args.preview, 'io.mugye.preview', args.aapt, args.apksigner, preview=True)
    text = ('무계매크로 1.6.0 APK 정적 진단 — Play 프로텍트 통과를 보장하지 않음\n'
            '두 앱 모두 단일 APK이며 APK 설치/우회는 수행하지 않음\n\n'
            + report(a) + '\n' + report(b)
            + '\n검증 결과: APK 형식 및 헤더 확인, 32비트 네이티브 전용 앱 거부, '
            '서명·권한 검사(SDK 도구가 제공된 경우) 통과.\n'
            '실제 기기 Play 프로텍트/게임 실행 테스트: 미실시.\n')
    output = Path(args.output); output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(text, encoding='utf-8')
    print(text)


if __name__ == '__main__':
    main()
