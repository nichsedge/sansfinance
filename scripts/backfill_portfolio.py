import json
import sqlite3
import os
import re
import sys
import shutil
from datetime import datetime
import time

# Configuration
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SANS_FINANCE_DIR = os.path.dirname(SCRIPT_DIR)
PROJECTS_DIR = os.path.dirname(SANS_FINANCE_DIR)

DB_PATH = os.path.join(SANS_FINANCE_DIR, "sans_finance_db")
DATA_DIR = os.getenv("PORTFOLIO_DATA_DIR")
if not DATA_DIR:
    standard_path = os.path.join(PROJECTS_DIR, "portfolio-integration", "data")
    if os.path.exists(standard_path):
        DATA_DIR = standard_path
    else:
        DATA_DIR = "./portfolio_data"
SNAPSHOT_PATTERN = re.compile(r".*_snapshot\.json$")

def load_r2_credentials():
    """Load R2 credentials from environment or creds directory."""
    account_id = os.getenv("R2_ACCOUNT_ID") or os.getenv("CLOUDFLARE_ACCOUNT_ID")
    access_key = os.getenv("R2_ACCESS_KEY_ID") or os.getenv("AWS_ACCESS_KEY_ID")
    secret_key = os.getenv("R2_SECRET_ACCESS_KEY") or os.getenv("AWS_SECRET_ACCESS_KEY")
    bucket_name = os.getenv("R2_BUCKET_NAME") or os.getenv("PORTFOLIO_R2_BUCKET")

    if not (account_id and access_key and secret_key):
        candidates = [
            os.path.join(PROJECTS_DIR, "creds", "cloudflare", "r2_cred.json"),
            os.path.join(SANS_FINANCE_DIR, "app", "src", "main", "assets", "r2_cred.json")
        ]
        for candidate in candidates:
            if os.path.exists(candidate):
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

    return account_id, access_key, secret_key, bucket_name or "ichsanul-dev"

def download_snapshots_from_r2(bucket_name, temp_dir, existing_dates=None):
    """Downloads snapshot files from Cloudflare R2 using AWS SigV4."""
    import hashlib
    import hmac
    import urllib.request
    import xml.etree.ElementTree as ET
    from datetime import datetime, timezone

    account_id, access_key, secret_key, default_bucket = load_r2_credentials()
    bucket = bucket_name or default_bucket
    if not (account_id and access_key and secret_key):
        print("ℹ️ Direct S3 API keys not found in creds/cloudflare/r2_cred.json. Checking for latest snapshot via Wrangler...")
        import shutil
        import subprocess
        wrangler_cmd = ["bunx", "wrangler"] if not shutil.which("wrangler") else ["wrangler"]
        os.makedirs(temp_dir, exist_ok=True)
        dest_path = os.path.join(temp_dir, "latest_snapshot.json")
        cmd = wrangler_cmd + ["r2", "object", "get", f"{bucket}/snapshots/latest.json", f"--file={dest_path}", "--remote"]
        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode == 0 and os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
            print(f"✅ Downloaded snapshots/latest.json from Cloudflare R2.")
            return True
        else:
            print("💡 Note: To download all historical snapshots from Cloudflare R2, add access_key_id & secret_access_key to creds/cloudflare/r2_cred.json")
            return False

    if existing_dates is None:
        existing_dates = set()

    host = f"{account_id}.r2.cloudflarestorage.com"

    def execute_s3_request(method, canonical_uri, query_params=""):
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
        canonical_request = f"{method}\n{canonical_uri}\n{query_params}\n{canonical_headers}\n{signed_headers}\n{payload_hash}"
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

        url = f"https://{host}{canonical_uri}"
        if query_params:
            url += f"?{query_params}"

        req = urllib.request.Request(
            url,
            headers={
                "Authorization": auth_header,
                "Host": host,
                "x-amz-date": amz_date,
                "x-amz-content-sha256": payload_hash
            },
            method=method
        )
        with urllib.request.urlopen(req, timeout=20) as resp:
            return resp.read()

    try:
        print(f"☁️ Listing snapshots from Cloudflare R2 bucket: {bucket} (prefix: snapshots/)...")
        list_xml = execute_s3_request("GET", f"/{bucket}", "list-type=2&prefix=snapshots%2F")
        root = ET.fromstring(list_xml)
        
        # S3 namespace
        ns = {'s3': 'http://s3.amazonaws.com/doc/2006-03-01/'}
        contents = root.findall('s3:Contents', ns) or root.findall('Contents')

        os.makedirs(temp_dir, exist_ok=True)
        download_count = 0
        skipped_count = 0

        for item in contents:
            key_elem = item.find('s3:Key', ns) if item.find('s3:Key', ns) is not None else item.find('Key')
            if key_elem is None or not key_elem.text:
                continue
            key = key_elem.text
            if key.endswith("_snapshot.json"):
                filename = os.path.basename(key)
                date_match = re.match(r"(\d{4}-\d{2}-\d{2})_snapshot\.json", filename)
                if date_match:
                    date_str = date_match.group(1)
                    try:
                        date_ms = parse_date_to_millis(date_str)
                        if date_ms in existing_dates:
                            skipped_count += 1
                            continue
                    except Exception:
                        pass

                dest_path = os.path.join(temp_dir, filename)
                print(f"📥 Downloading r2://{bucket}/{key} to {dest_path}...")
                data = execute_s3_request("GET", f"/{bucket}/{key}")
                with open(dest_path, "wb") as f:
                    f.write(data)
                download_count += 1

        if skipped_count > 0:
            print(f"ℹ️ Skipped {skipped_count} snapshot(s) already present in the database.")
        print(f"✅ Downloaded {download_count} new snapshot file(s) from Cloudflare R2.")
        return True
    except Exception as e:
        print(f"❌ Failed to download snapshots from Cloudflare R2: {e}")
        return False

def download_snapshots_from_gcs(bucket_name, temp_dir, existing_dates=None):
    """Downloads snapshot files from GCS to a temporary directory, skipping already imported ones."""
    try:
        from google.cloud import storage
    except ImportError:
        print("❌ Error: google-cloud-storage package is not installed.")
        print("💡 Please install it by running: pip install google-cloud-storage")
        sys.exit(1)

    creds_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not creds_path:
        # Check relative to sansfinance project directory
        local_creds = "../creds/gcp/SA_cred_general.json"
        if os.path.exists(local_creds):
            creds_path = local_creds
        else:
            # Check absolute path dynamically from projects directory
            abs_creds = os.path.join(PROJECTS_DIR, "creds", "gcp", "SA_cred_general.json")
            if os.path.exists(abs_creds):
                creds_path = abs_creds

    if creds_path:
        print(f"🔑 Using service account credentials from: {creds_path}")
        client = storage.Client.from_service_account_json(creds_path)
    else:
        print("ℹ️ Using Google Application Default Credentials (ADC)")
        client = storage.Client()

    if existing_dates is None:
        existing_dates = set()

    try:
        bucket = client.bucket(bucket_name)
        blobs = bucket.list_blobs(prefix="snapshots/")
        
        os.makedirs(temp_dir, exist_ok=True)
        download_count = 0
        skipped_count = 0
        
        for blob in blobs:
            # We only want json snapshot files in the snapshots/ directory
            if blob.name.endswith("_snapshot.json"):
                filename = os.path.basename(blob.name)
                
                # Incremental Check: parse date from filename YYYY-MM-DD_snapshot.json
                date_match = re.match(r"(\d{4}-\d{2}-\d{2})_snapshot\.json", filename)
                if date_match:
                    date_str = date_match.group(1)
                    try:
                        date_ms = parse_date_to_millis(date_str)
                        if date_ms in existing_dates:
                            skipped_count += 1
                            continue
                    except Exception:
                        pass

                dest_path = os.path.join(temp_dir, filename)
                print(f"📥 Downloading gs://{bucket_name}/{blob.name} to {dest_path}...")
                blob.download_to_filename(dest_path)
                download_count += 1
                
        if skipped_count > 0:
            print(f"ℹ️ Skipped {skipped_count} snapshot(s) already present in the database.")
        print(f"✅ Downloaded {download_count} new snapshot file(s) from GCS.")
        return True
    except Exception as e:
        print(f"❌ Failed to download snapshots from GCS: {e}")
        return False


def extract_price(details):
    if not details:
        return None
    # Match "Price: $1,234.56" or "Price: 1234.56"
    match = re.search(r"Price:\s*\$?([\d,]+\.?\d*)", details)
    if match:
        try:
            return float(match.group(1).replace(",", ""))
        except ValueError:
            return None
    return None

def parse_date_to_millis(date_str):
    # Parse yyyy-MM-dd to epoch millis (Jakarta time approx by just using local if same)
    # PortfolioJsonImporter uses SimpleDateFormat with Asia/Jakarta
    dt = datetime.strptime(date_str, "%Y-%m-%d")
    return int(dt.timestamp() * 1000)

def main():
    if not os.path.exists(DB_PATH):
        print(f"❌ Error: Database file {DB_PATH} not found!")
        return

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Query existing snapshot dates in the database for incremental sync
    existing_dates = set()
    try:
        cursor.execute("SELECT snapshotDate FROM portfolio_snapshot_headers")
        existing_dates = {row[0] for row in cursor.fetchall()}
    except sqlite3.OperationalError:
        pass

    data_dir = DATA_DIR
    gcs_bucket = os.getenv("PORTFOLIO_GCS_BUCKET")
    r2_bucket = os.getenv("PORTFOLIO_R2_BUCKET") or os.getenv("R2_BUCKET_NAME") or "ichsanul-dev"
    provider = os.getenv("PORTFOLIO_STORAGE_PROVIDER", "r2").lower()
    temp_dir = "./build/cloud_snapshots"
    is_temp_dir = False

    if provider == "r2":
        target_bucket = r2_bucket or "ichsanul-dev"
        print(f"☁️ Cloudflare R2 is configured (bucket: '{target_bucket}'). Syncing snapshots from R2...")
        if download_snapshots_from_r2(target_bucket, temp_dir, existing_dates):
            data_dir = temp_dir
            is_temp_dir = True
        else:
            print("⚠️ Failed to download snapshots from Cloudflare R2. Falling back to local data directory.")
    elif gcs_bucket:
        print(f"☁️ PORTFOLIO_GCS_BUCKET is set to '{gcs_bucket}'. Syncing snapshots from GCS...")
        if download_snapshots_from_gcs(gcs_bucket, temp_dir, existing_dates):
            data_dir = temp_dir
            is_temp_dir = True
        else:
            print("⚠️ Failed to download snapshots from GCS. Falling back to local data directory.")

    if not os.path.exists(data_dir):
        print(f"⚠️ Warning: Portfolio data directory '{data_dir}' does not exist!")
        conn.close()
        return

    files = [f for f in os.listdir(data_dir) if SNAPSHOT_PATTERN.match(f)]
    files.sort()

    if not files:
        if is_temp_dir:
            print("✅ Database is already up to date with GCS (no new snapshots).")
        else:
            print("⚠️ No snapshot files found. Aborting to prevent erasing existing database data.")
        if is_temp_dir and os.path.exists(temp_dir):
            print("🧹 Cleaning up GCS temporary snapshots directory...")
            shutil.rmtree(temp_dir)
        conn.close()
        return

    print(f"📂 Found {len(files)} snapshot files.")

    # Determine which dates we are about to import to only clear those specific dates
    dates_to_import = []
    for filename in files:
        filepath = os.path.join(data_dir, filename)
        try:
            with open(filepath, 'r') as f:
                data = json.load(f)
            date_str = data.get("metadata", {}).get("date")
            if date_str:
                dates_to_import.append(parse_date_to_millis(date_str))
        except Exception as e:
            print(f"⚠️ Error reading {filename}: {e}")

    if not dates_to_import:
        print("⚠️ No valid dates found in snapshot files. Aborting.")
        if is_temp_dir and os.path.exists(temp_dir):
            print("🧹 Cleaning up GCS temporary snapshots directory...")
            shutil.rmtree(temp_dir)
        conn.close()
        return

    print("🧹 Clearing existing portfolio data only for the target dates to be imported...")
    for s_date in dates_to_import:
        cursor.execute("DELETE FROM portfolio_snapshot_headers WHERE snapshotDate = ?", (s_date,))
        cursor.execute("DELETE FROM portfolio_holdings WHERE snapshot_date = ?", (s_date,))

    for filename in files:
        filepath = os.path.join(data_dir, filename)
        print(f"📄 Processing {filename}...")
        
        with open(filepath, 'r') as f:
            data = json.load(f)
        
        metadata = data.get("metadata", {})
        date_str = metadata.get("date")
        if not date_str:
            print(f"⚠️ Warning: No date in {filename}, skipping.")
            continue
            
        snapshot_date = parse_date_to_millis(date_str)
        exchange_rate = metadata.get("exchange_rate") or 16000.0
        
        holdings = data.get("holdings", [])
        total_idr = sum((h.get("value_idr") or 0.0) for h in holdings)
        total_usd = total_idr / exchange_rate
        
        # Insert Header
        cursor.execute("""
            INSERT INTO portfolio_snapshot_headers 
            (snapshotDate, exchangeRateUsd, totalValueIdr, totalValueUsd, createdAt)
            VALUES (?, ?, ?, ?, ?)
        """, (snapshot_date, exchange_rate, total_idr, total_usd, int(time.time() * 1000)))
        
        # Insert Holdings
        for h in holdings:
            details = h.get("details")
            account_key = (h.get("account_key") or "").strip() or None
            account_name = (h.get("account_name") or "").strip() or None
            legacy_account = (h.get("account") or "").strip() or None

            # Link resolution order: account_key -> account_name -> legacy account
            account_id = None
            if account_key:
                cursor.execute(
                    """
                    SELECT account_id
                    FROM portfolio_holdings
                    WHERE account_key = ? AND account_id IS NOT NULL
                    ORDER BY snapshot_date DESC
                    LIMIT 1
                    """,
                    (account_key,)
                )
                row = cursor.fetchone()
                if row and row[0] is not None:
                    cursor.execute("SELECT id FROM accounts WHERE id = ? LIMIT 1", (row[0],))
                    verified = cursor.fetchone()
                    if verified:
                        account_id = verified[0]

            lookup_name = account_name or legacy_account
            if account_id is None and lookup_name:
                cursor.execute("SELECT id FROM accounts WHERE name = ? LIMIT 1", (lookup_name,))
                row = cursor.fetchone()
                if row:
                    account_id = row[0]

            if account_id is None:
                now_ms = int(time.time() * 1000)
                new_name = lookup_name or account_key or "Imported Investment Account"
                cursor.execute(
                    """
                    INSERT INTO accounts
                    (name, type, balance, currency, interest_rate, min_payment, created_at, updated_at)
                    VALUES (?, 'Investment', 0, 'IDR', 0.0, 0, ?, ?)
                    """,
                    (new_name, now_ms, now_ms)
                )
                account_id = cursor.lastrowid

            # Prioritize price field from JSON, fallback to extraction from details
            price = h.get("price")
            if price is None:
                price = extract_price(details)
            # Use quantity from JSON, fallback to amount, then value_idr for IDR assets
            quantity = h.get("quantity")
            if quantity is None:
                quantity = h.get("amount")
                
            if (quantity is None or quantity == 0) and h.get("currency") == "IDR":
                quantity = h.get("value_idr") or 0.0
            elif quantity is None:
                quantity = 0.0
 
            value_idr = h.get("value_idr")
            if value_idr is None:
                value_idr = 0.0
 
            cursor.execute("""
                INSERT INTO portfolio_holdings 
                (snapshot_date, source, category, asset, currency, quantity, price, value_idr, account_id, account_key, account_name, account, details, asset_class)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                snapshot_date,
                h.get("source") or "",
                h.get("category") or "",
                h.get("asset") or "",
                h.get("currency") or "IDR",
                quantity,
                price,
                value_idr,
                account_id,
                account_key,
                account_name,
                legacy_account or account_name or account_key or "",
                details,
                h.get("asset_class") or "Other"
            ))

    conn.commit()
    conn.close()

    if is_temp_dir and os.path.exists(temp_dir):
        print("🧹 Cleaning up GCS temporary snapshots directory...")
        shutil.rmtree(temp_dir)

    print("✅ Backfill complete!")

if __name__ == "__main__":
    main()
