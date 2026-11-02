import sqlite3
import json
import os
from datetime import datetime

DB_PATH = "sans_finance_db_snapshot.sqlite"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECTS_DIR = os.path.dirname(os.path.dirname(SCRIPT_DIR))
OUTPUT_DIR = os.getenv("PORTFOLIO_DATA_DIR") or os.path.join(PROJECTS_DIR, "ierp", "data", "portfolio")

def get_asset_class(category: str) -> str:
    mapping = {
        "Bank Account": "Cash & Equivalents",
        "Digital Bank": "Cash & Equivalents",
        "Stablecoin": "Cash & Equivalents",
        "Money Market Fund": "Cash & Equivalents",
        "SBN": "Fixed Income",
        "Corporate Bond": "Fixed Income",
        "P2P Lending": "Fixed Income",
        "US Stocks": "Equities",
        "Indo Stocks": "Equities",
        "Equity Fund": "Equities",
        "Spot": "Crypto",
        "Staked": "Crypto",
        "Yield / LP": "Crypto",
        "Gold": "Commodities",
        "Silver": "Commodities",
    }
    return mapping.get(category, "Other")

def main():
    if not os.path.exists(DB_PATH):
        print(f"❌ Error: Database snapshot not found at {DB_PATH}")
        return

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM portfolio_snapshot_headers ORDER BY snapshotDate ASC")
    headers = cursor.fetchall()
    
    print(f"Loaded {len(headers)} snapshot headers from database.")

    for header in headers:
        snapshot_date_ms = header["snapshotDate"]
        exchange_rate = header["exchangeRateUsd"]
        total_val_idr = header["totalValueIdr"]
        total_val_usd = header["totalValueUsd"]
        created_at_ms = header["createdAt"]
        
        # Convert timestamp to date string (UTC+7 approximation)
        # Using datetime.fromtimestamp should correctly map it to local system time.
        dt = datetime.fromtimestamp(snapshot_date_ms / 1000.0)
        date_str = dt.strftime("%Y-%m-%d")
        
        generated_at = datetime.fromtimestamp(created_at_ms / 1000.0).isoformat() + "+07:00"
        
        cursor.execute("SELECT * FROM portfolio_holdings WHERE snapshot_date = ?", (snapshot_date_ms,))
        rows = cursor.fetchall()
        
        holdings = []
        total_assets_idr = 0.0
        total_liabilities_idr = 0.0
        categories = {}
        asset_classes = {}

        for row in rows:
            val_idr = row["value_idr"] or 0.0
            cat = row["category"]
            asset = row["asset"]
            
            if any(term in cat for term in ["Debt", "Borrow", "Liabilities"]):
                total_liabilities_idr += val_idr
            else:
                total_assets_idr += val_idr
                
            if cat not in categories:
                categories[cat] = {"value_idr": 0.0, "count": 0}
            categories[cat]["value_idr"] += val_idr
            categories[cat]["count"] += 1
            
            aclass = row["asset_class"] or get_asset_class(cat)
            if aclass not in asset_classes:
                asset_classes[aclass] = {"value_idr": 0.0, "count": 0}
            asset_classes[aclass]["value_idr"] += val_idr
            asset_classes[aclass]["count"] += 1
            
            holding = {
                "source": row["source"],
                "category": cat,
                "asset": asset,
                "currency": row["currency"],
                "quantity": row["quantity"],
                "price": row["price"],
                "value_idr": val_idr,
                "value_usd": round(val_idr / exchange_rate, 2) if exchange_rate else 0.0,
                "account": row["account"],
                "details": row["details"] or "",
                "asset_class": aclass,
                "account_name": row["account_name"],
                "account_key": row["account_key"]
            }
            holdings.append(holding)
            
        net_worth_idr = total_assets_idr - total_liabilities_idr
        
        snapshot = {
            "metadata": {
                "date": date_str,
                "exchange_rate": exchange_rate,
                "total_items": len(holdings),
                "generated_at": generated_at
            },
            "totals": {
                "net_worth_idr": round(net_worth_idr, 2),
                "net_worth_usd": round(net_worth_idr / exchange_rate, 2),
                "total_assets_idr": round(total_assets_idr, 2),
                "total_liabilities_idr": round(total_liabilities_idr, 2)
            },
            "allocation": {
                "by_category": [
                    {
                        "category": k, 
                        "value_idr": round(v["value_idr"], 2), 
                        "percentage": round(v["value_idr"] / total_assets_idr * 100, 2) if total_assets_idr > 0 else 0,
                        "count": v["count"]
                    }
                    for k, v in sorted(categories.items(), key=lambda x: x[1]["value_idr"], reverse=True)
                ],
                "by_asset_class": [
                    {
                        "asset_class": k, 
                        "value_idr": round(v["value_idr"], 2), 
                        "percentage": round(v["value_idr"] / total_assets_idr * 100, 2) if total_assets_idr > 0 else 0,
                        "count": v["count"]
                    }
                    for k, v in sorted(asset_classes.items(), key=lambda x: x[1]["value_idr"], reverse=True)
                ]
            },
            "holdings": holdings
        }
        
        output_file_name = f"{date_str}_snapshot.json"
        output_path = os.path.join(OUTPUT_DIR, output_file_name)
        
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(snapshot, f, indent=2)
            
        print(f"✅ Successfully reconstructed and wrote {output_path} with {len(holdings)} holdings.")

    conn.close()

if __name__ == "__main__":
    main()
