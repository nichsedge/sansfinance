#!/usr/bin/env python3
"""
Downloads the latest or archived Sans Finance SQLite snapshot from Cloudflare R2.
Usage:
    python3 scripts/pull_cloud_db.py [output_path] [--key OBJECT_KEY] [--list]
"""

import sys
import os
import argparse
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import hmac
import urllib.request
import urllib.error
import xml.etree.ElementTree as ET

R2_BUCKET_NAME = "ichsanul-dev"
DEFAULT_BLOB_NAME = "db/sans_finance_latest.sqlite"

def load_r2_credentials():
    """Load R2 credentials directly from environment variables."""
    account_id = os.getenv("R2_ACCOUNT_ID") or os.getenv("CLOUDFLARE_ACCOUNT_ID")
    access_key = os.getenv("R2_ACCESS_KEY_ID") or os.getenv("AWS_ACCESS_KEY_ID")
    secret_key = os.getenv("R2_SECRET_ACCESS_KEY") or os.getenv("AWS_SECRET_ACCESS_KEY")
    bucket_name = os.getenv("R2_BUCKET_NAME")

    return account_id, access_key, secret_key, bucket_name or R2_BUCKET_NAME

def build_sigv4_headers(method: str, canonical_uri: str, canonical_query: str, payload_bytes: bytes, account_id: str, access_key: str, secret_key: str):
    host = f"{account_id}.r2.cloudflarestorage.com"
    now = datetime.now(timezone.utc)
    amz_date = now.strftime('%Y%m%dT%H%M%SZ')
    date_stamp = now.strftime('%Y%m%d')
    payload_hash = hashlib.sha256(payload_bytes).hexdigest()

    headers = {
        "host": host,
        "x-amz-content-sha256": payload_hash,
        "x-amz-date": amz_date
    }

    canonical_headers = "".join([f"{k}:{v}\n" for k, v in sorted(headers.items())])
    signed_headers = ";".join(sorted(headers.keys()))
    canonical_request = f"{method}\n{canonical_uri}\n{canonical_query}\n{canonical_headers}\n{signed_headers}\n{payload_hash}"
    credential_scope = f"{date_stamp}/auto/s3/aws4_request"
    string_to_sign = f"AWS4-HMAC-SHA256\n{amz_date}\n{credential_scope}\n{hashlib.sha256(canonical_request.encode('utf-8')).hexdigest()}"

    def sign(key, msg):
        return hmac.new(key, msg.encode('utf-8'), hashlib.sha256).digest()

    k_date = sign(("AWS4" + secret_key).encode('utf-8'), date_stamp)
    k_region = sign(k_date, "auto")
    k_service = sign(k_region, "s3")
    k_signing = sign(k_service, "aws4_request")
    signature = hmac.new(k_signing, string_to_sign.encode('utf-8'), hashlib.sha256).hexdigest()

    auth_header = f"AWS4-HMAC-SHA256 Credential={access_key}/{credential_scope}, SignedHeaders={signed_headers}, Signature={signature}"

    return {
        "Authorization": auth_header,
        "Host": host,
        "x-amz-date": amz_date,
        "x-amz-content-sha256": payload_hash
    }

def list_r2_backups(bucket: str):
    account_id, access_key, secret_key, _ = load_r2_credentials()
    if not (account_id and access_key and secret_key):
        print("❌ Cloudflare R2 credentials not found.")
        sys.exit(1)

    canonical_uri = f"/{bucket}"
    canonical_query = "list-type=2&prefix=db%2F"
    headers = build_sigv4_headers("GET", canonical_uri, canonical_query, b"", account_id, access_key, secret_key)
    endpoint_url = f"https://{headers['Host']}{canonical_uri}?{canonical_query}"

    req = urllib.request.Request(endpoint_url, headers=headers, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            root = ET.fromstring(resp.read())
            print(f"📦 Objects in bucket '{bucket}' under 'db/':")
            for content in root.findall('{http://s3.amazonaws.com/doc/2006-03-01/}Contents'):
                key = content.find('{http://s3.amazonaws.com/doc/2006-03-01/}Key').text
                size = int(content.find('{http://s3.amazonaws.com/doc/2006-03-01/}Size').text)
                mtime = content.find('{http://s3.amazonaws.com/doc/2006-03-01/}LastModified').text
                print(f"  • {key} ({size / 1024:.1f} KB) - {mtime}")
    except Exception as e:
        print(f"❌ Failed to list R2 objects: {e}")
        sys.exit(1)

def pull_from_r2(output_path: Path, bucket: str, object_key: str):
    account_id, access_key, secret_key, _ = load_r2_credentials()
    if not (account_id and access_key and secret_key):
        print("❌ Cloudflare R2 credentials not found in environment (R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY).")
        sys.exit(1)

    print(f"☁️ Connecting to Cloudflare R2 bucket: {bucket}...")
    canonical_uri = f"/{bucket}/{object_key}"
    headers = build_sigv4_headers("GET", canonical_uri, "", b"", account_id, access_key, secret_key)
    endpoint_url = f"https://{headers['Host']}{canonical_uri}"

    req = urllib.request.Request(endpoint_url, headers=headers, method="GET")

    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            content = resp.read()
            if len(content) == 0:
                print(f"❌ Error: Downloaded file from {object_key} is 0 bytes.")
                sys.exit(1)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            with open(output_path, "wb") as f:
                f.write(content)
            size_kb = len(content) / 1024.0
            print(f"📥 Found {object_key} ({size_kb:.1f} KB)")
            print(f"✅ Successfully downloaded to {output_path}")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            print(f"❌ Error: {object_key} does not exist in Cloudflare R2 bucket '{bucket}'.")
        else:
            print(f"❌ Error downloading from Cloudflare R2: HTTP {e.code} - {e.read().decode('utf-8', errors='ignore')}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Connection error: {e}")
        sys.exit(1)

def push_to_r2(input_path: Path, bucket: str, object_key: str):
    account_id, access_key, secret_key, _ = load_r2_credentials()
    if not (account_id and access_key and secret_key):
        print("❌ Cloudflare R2 credentials not found in environment (R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY).")
        sys.exit(1)

    if not input_path.exists():
        print(f"❌ Error: Input file {input_path} does not exist.")
        sys.exit(1)

    payload_bytes = input_path.read_bytes()
    if len(payload_bytes) == 0:
        print(f"❌ Error: Input file {input_path} is empty (0 bytes).")
        sys.exit(1)

    print(f"☁️ Uploading {input_path} ({len(payload_bytes) / 1024:.1f} KB) to R2 bucket: {bucket} -> {object_key}...")
    canonical_uri = f"/{bucket}/{object_key}"
    headers = build_sigv4_headers("PUT", canonical_uri, "", payload_bytes, account_id, access_key, secret_key)
    headers["Content-Type"] = "application/x-sqlite3"
    endpoint_url = f"https://{headers['Host']}{canonical_uri}"

    req = urllib.request.Request(endpoint_url, data=payload_bytes, headers=headers, method="PUT")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            if resp.status in (200, 201, 204):
                print(f"✅ Successfully uploaded to {object_key}")
            else:
                print(f"⚠️ Upload response status: {resp.status}")
    except Exception as e:
        print(f"❌ Error uploading to Cloudflare R2: {e}")
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="Download or upload SQLite snapshot from/to Cloudflare R2")
    parser.add_argument("target", nargs="?", default="sans_finance_latest.sqlite", help="File path (destination for pull, source for push)")
    parser.add_argument("--push", action="store_true", help="Push target file to R2 instead of downloading")
    parser.add_argument("--key", default=DEFAULT_BLOB_NAME, help="R2 object key")
    parser.add_argument("--bucket", default=R2_BUCKET_NAME, help="Bucket name override")
    parser.add_argument("--list", action="store_true", help="List database backups in R2 bucket")
    args = parser.parse_args()

    if args.list:
        list_r2_backups(args.bucket)
        return

    target_path = Path(args.target)
    if args.push:
        push_to_r2(target_path, args.bucket, args.key)
    else:
        pull_from_r2(target_path, args.bucket, args.key)

if __name__ == "__main__":
    main()
