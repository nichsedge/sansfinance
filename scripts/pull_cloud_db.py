#!/usr/bin/env python3
"""
Downloads the latest Sans Finance SQLite snapshot from Google Cloud Storage or Cloudflare R2.
Usage:
    python3 scripts/pull_cloud_db.py [output_path] [--provider gcs|r2] [--bucket BUCKET]
"""

import sys
import os
import json
import argparse
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import hmac
import urllib.request
import urllib.error

GCS_BUCKET_NAME = "ichsanul-portfolio-snapshots"
R2_BUCKET_NAME = "ichsanul-dev"
BLOB_NAME = "db/sans_finance_latest.sqlite"

def load_r2_credentials():
    """Load R2 credentials from environment or creds directory."""
    account_id = os.getenv("R2_ACCOUNT_ID") or os.getenv("CLOUDFLARE_ACCOUNT_ID")
    access_key = os.getenv("R2_ACCESS_KEY_ID") or os.getenv("AWS_ACCESS_KEY_ID")
    secret_key = os.getenv("R2_SECRET_ACCESS_KEY") or os.getenv("AWS_SECRET_ACCESS_KEY")
    bucket_name = os.getenv("R2_BUCKET_NAME")

    if not (account_id and access_key and secret_key):
        base_dir = Path(__file__).resolve().parents[2]
        candidates = [
            base_dir / "creds" / "cloudflare" / "r2_cred.json",
            base_dir / "sansfinance" / "app" / "src" / "main" / "assets" / "r2_cred.json"
        ]
        for candidate in candidates:
            if candidate.exists():
                try:
                    with open(candidate, "r") as f:
                        data = json.load(f)
                    account_id = account_id or data.get("account_id")
                    access_key = access_key or data.get("access_key_id")
                    secret_key = secret_key or data.get("secret_access_key")
                    bucket_name = bucket_name or data.get("bucket_name")
                    if account_id and access_key and secret_key:
                        break
                except Exception:
                    pass

    return account_id, access_key, secret_key, bucket_name or R2_BUCKET_NAME

def get_gcs_storage_client():
    try:
        from google.cloud import storage
    except ImportError:
        print("❌ Error: google-cloud-storage package is not installed.")
        print("💡 Install it via: pip install google-cloud-storage")
        sys.exit(1)

    creds_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not creds_path:
        base_dir = Path(__file__).resolve().parents[2]
        candidate = base_dir / "creds" / "gcp" / "SA_cred_general.json"
        if candidate.exists():
            creds_path = str(candidate)
    
    if creds_path and os.path.exists(creds_path):
        return storage.Client.from_service_account_json(creds_path)
    return storage.Client()

def pull_from_gcs(output_path: Path, bucket_name: str):
    print(f"☁️ Connecting to GCS bucket: {bucket_name}...")
    client = get_gcs_storage_client()
    bucket = client.bucket(bucket_name)
    blob = bucket.blob(BLOB_NAME)

    if not blob.exists():
        print(f"❌ Error: {BLOB_NAME} does not exist in bucket {bucket_name} yet.")
        print("💡 Hint: Open Sans Finance on your phone -> Settings -> 'Back Up' to upload.")
        sys.exit(1)

    blob.reload()
    size_kb = blob.size / 1024.0 if blob.size else 0
    updated = blob.updated.strftime('%Y-%m-%d %H:%M:%S UTC') if blob.updated else "Unknown"

    print(f"📥 Found {BLOB_NAME} ({size_kb:.1f} KB, updated {updated})")
    print(f"💾 Downloading to {output_path}...")
    blob.download_to_filename(str(output_path))
    print(f"✅ Successfully downloaded to {output_path}")

def pull_from_r2(output_path: Path, bucket_name: str):
    account_id, access_key, secret_key, detected_bucket = load_r2_credentials()
    bucket = bucket_name or detected_bucket
    if not (account_id and access_key and secret_key):
        # Fallback to wrangler CLI (OAuth authenticated)
        import shutil
        import subprocess
        wrangler_cmd = ["bunx", "wrangler"] if not shutil.which("wrangler") else ["wrangler"]
        cmd = wrangler_cmd + ["r2", "object", "get", f"{bucket}/{BLOB_NAME}", f"--file={output_path}", "--remote"]
        print(f"☁️ Connecting to Cloudflare R2 bucket '{bucket}' via Wrangler...")
        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode == 0:
            print(f"✅ Successfully downloaded to {output_path}")
            return
        else:
            print(f"❌ Could not download from Cloudflare R2: {res.stderr.strip() or res.stdout.strip()}")
            print("💡 Hint: Place API keys in creds/cloudflare/r2_cred.json or export R2_ACCESS_KEY_ID & R2_SECRET_ACCESS_KEY")
            sys.exit(1)

    print(f"☁️ Connecting to Cloudflare R2 bucket: {bucket}...")
    host = f"{account_id}.r2.cloudflarestorage.com"
    canonical_uri = f"/{bucket}/{BLOB_NAME}"
    endpoint_url = f"https://{host}{canonical_uri}"

    now = datetime.now(timezone.utc)
    amz_date = now.strftime('%Y%m%dT%H%M%SZ')
    date_stamp = now.strftime('%Y%m%d')

    payload_hash = hashlib.sha256(b"").hexdigest()
    headers = {
        "host": host,
        "x-amz-content-sha256": payload_hash,
        "x-amz-date": amz_date
    }

    canonical_headers = "".join([f"{k}:{v}\n" for k, v in sorted(headers.items())])
    signed_headers = ";".join(sorted(headers.keys()))
    canonical_request = f"GET\n{canonical_uri}\n\n{canonical_headers}\n{signed_headers}\n{payload_hash}"
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

    req = urllib.request.Request(
        endpoint_url,
        headers={
            "Authorization": auth_header,
            "Host": host,
            "x-amz-date": amz_date,
            "x-amz-content-sha256": payload_hash
        },
        method="GET"
    )

    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            content = resp.read()
            with open(output_path, "wb") as f:
                f.write(content)
            size_kb = len(content) / 1024.0
            print(f"📥 Found {BLOB_NAME} ({size_kb:.1f} KB)")
            print(f"✅ Successfully downloaded to {output_path}")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            print(f"❌ Error: {BLOB_NAME} does not exist in Cloudflare R2 bucket {bucket} yet.")
            print("💡 Hint: Open Sans Finance on your phone -> Settings -> 'Back Up' to upload.")
        else:
            print(f"❌ Error downloading from Cloudflare R2: HTTP {e.code} - {e.read().decode('utf-8', errors='ignore')}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Connection error: {e}")
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="Download latest SQLite snapshot from Cloud Storage (GCS or Cloudflare R2)")
    parser.add_argument("output", nargs="?", default="sans_finance_latest.sqlite", help="Destination output file")
    parser.add_argument("--provider", choices=["gcs", "r2"], default=os.getenv("STORAGE_PROVIDER", "r2").lower(), help="Storage provider (gcs or r2)")
    parser.add_argument("--bucket", default=None, help="Bucket name override")
    args = parser.parse_args()

    out_path = Path(args.output)
    if args.provider == "r2":
        pull_from_r2(out_path, args.bucket or R2_BUCKET_NAME)
    else:
        pull_from_gcs(out_path, args.bucket or GCS_BUCKET_NAME)

if __name__ == "__main__":
    main()

