#!/usr/bin/env python3
"""
Diagnostic utility to inspect a Sans Finance SQLite database snapshot.
Usage:
    python3 scripts/inspect_db.py [path_to_db.sqlite]
"""

import sys
import sqlite3
from pathlib import Path
from datetime import datetime

def format_idr(amount):
    return f"Rp {amount:,.0f}".replace(",", ".")

def inspect_database(db_path: Path):
    if not db_path.exists():
        print(f"❌ Error: Database file {db_path} not found.")
        sys.exit(1)

    print(f"📊 Analyzing Sans Finance Database: {db_path}")
    print(f"📁 Size: {db_path.stat().st_size / 1024.0:.1f} KB\n" + "=" * 60)

    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    # 1. Accounts & Balances
    print("🏦 1. LIVE ACCOUNTS (from `accounts` table):")
    try:
        cursor.execute("SELECT id, name, type, currency, balance FROM accounts ORDER BY balance DESC")
        accounts = cursor.fetchall()
        total_cash_idr = 0.0
        for acc in accounts:
            bal = acc["balance"] / 100.0  # balances are stored in cents
            curr = acc["currency"]
            bal_str = format_idr(bal) if curr == "IDR" else f"${bal:,.2f}"
            print(f"  • [{acc['type']:12s}] {acc['name']:20s}: {bal_str:>16s} ({curr})")
            if curr == "IDR" and acc["type"] != "Investment":
                total_cash_idr += bal
        print(f"  👉 Total Live Liquid Accounts (IDR): {format_idr(total_cash_idr)}")
    except Exception as e:
        print(f"  ⚠️ Error reading accounts: {e}")

    print("\n" + "=" * 60)

    # 2. Portfolio Snapshot Holdings
    print("📈 2. LATEST PORTFOLIO HOLDINGS (from `portfolio_holdings` table):")
    try:
        cursor.execute("SELECT DISTINCT snapshot_date FROM portfolio_holdings ORDER BY snapshot_date DESC LIMIT 5")
        snapshot_dates = [r[0] for r in cursor.fetchall()]
        if not snapshot_dates:
            print("  ℹ️ No portfolio holdings found in database.")
        else:
            latest_date_ms = snapshot_dates[0]
            dt_str = datetime.fromtimestamp(latest_date_ms / 1000.0).strftime('%Y-%m-%d')
            print(f"  📅 Latest Snapshot Date: {dt_str} (timestamp: {latest_date_ms})")
            print(f"  📜 Available Snapshot Dates: {[datetime.fromtimestamp(d/1000.0).strftime('%Y-%m-%d') for d in snapshot_dates]}")

            cursor.execute(
                "SELECT source, category, asset, asset_class, value_idr FROM portfolio_holdings WHERE snapshot_date = ? ORDER BY value_idr DESC",
                (latest_date_ms,)
            )
            holdings = cursor.fetchall()
            total_snap_idr = sum(h["value_idr"] for h in holdings)
            print(f"\n  Top Holdings on {dt_str}:")
            for h in holdings[:10]:
                print(f"    • [{h['asset_class']:18s}] {h['asset']:30s} ({h['source']}): {format_idr(h['value_idr']):>18s}")
            
            if len(holdings) > 10:
                print(f"    ... and {len(holdings) - 10} more holdings")

            print(f"\n  👉 Total Snapshot Value: {format_idr(total_snap_idr)}")
            
            # Consolidated calculation
            consolidated = total_snap_idr + total_cash_idr
            print(f"\n✨ Consolidated Net Worth (Snapshot + Live Cash): {format_idr(consolidated)}")
    except Exception as e:
        print(f"  ⚠️ Error reading portfolio_holdings: {e}")

    print("\n" + "=" * 60)

    # 3. Recent Transactions
    print("💳 3. RECENT TRANSACTIONS (from `expenses` table):")
    try:
        cursor.execute("SELECT date, title, amount, currency, type FROM expenses ORDER BY date DESC LIMIT 5")
        txs = cursor.fetchall()
        for tx in txs:
            dt = datetime.fromtimestamp(tx["date"] / 1000.0).strftime('%Y-%m-%d')
            amt = tx["amount"] / 100.0
            amt_str = format_idr(amt) if tx["currency"] == "IDR" else f"${amt:,.2f}"
            sign = "+" if tx["type"] == "INCOME" else "-"
            print(f"  • {dt} | {sign}{amt_str:>14s} | [{tx['type']:7s}] {tx['title']}")
    except Exception as e:
        print(f"  ⚠️ Error reading expenses: {e}")

    conn.close()

if __name__ == "__main__":
    db_file = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("sans_finance_latest.sqlite")
    if not db_file.exists() and Path("sans_finance_db_snapshot.sqlite").exists():
        db_file = Path("sans_finance_db_snapshot.sqlite")
    inspect_database(db_file)
