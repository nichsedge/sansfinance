#!/usr/bin/env python3
"""
Downloads the latest Sans Finance SQLite snapshot from Google Cloud Storage.
Usage:
    python3 scripts/pull_cloud_db.py [output_path]
"""

import sys
import os
from pathlib import Path
from google.cloud import storage

BUCKET_NAME = "ichsanul-portfolio-snapshots"
BLOB_NAME = "db/sans_finance_latest.sqlite"

def get_storage_client() -> storage.Client:
    creds_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not creds_path:
        base_dir = Path(__file__).resolve().parents[2]
        candidate = base_dir / "creds" / "gcp" / "SA_cred_general.json"
        if candidate.exists():
            creds_path = str(candidate)
    
    if creds_path and os.path.exists(creds_path):
        return storage.Client.from_service_account_json(creds_path)
    return storage.Client()

def pull_cloud_db(output_path: Path):
    print(f"☁️ Connecting to GCS bucket: {BUCKET_NAME}...")
    client = get_storage_client()
    bucket = client.bucket(BUCKET_NAME)
    blob = bucket.blob(BLOB_NAME)

    if not blob.exists():
        print(f"❌ Error: {BLOB_NAME} does not exist in bucket {BUCKET_NAME} yet.")
        print("💡 Hint: Open Sans Finance on your phone -> Settings -> 'Backup DB to Cloud (GCS)' to upload.")
        sys.exit(1)

    blob.reload()
    size_kb = blob.size / 1024.0 if blob.size else 0
    updated = blob.updated.strftime('%Y-%m-%d %H:%M:%S UTC') if blob.updated else "Unknown"

    print(f"📥 Found {BLOB_NAME} ({size_kb:.1f} KB, updated {updated})")
    print(f"💾 Downloading to {output_path}...")
    blob.download_to_filename(str(output_path))
    print(f"✅ Successfully downloaded to {output_path}")

if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("sans_finance_latest.sqlite")
    pull_cloud_db(out)
