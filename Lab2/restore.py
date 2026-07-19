import datetime
from google.cloud import spanner

# Configuration (using live multi-region instance details)
INSTANCE_ID = "multi-region-spanner"
DATABASE_ID = "test-database"
TIMESTAMP_STR = "2026-07-19T17:02:09.247704144Z" # Replace with your noted pre-corruption timestamp

# Initialize client
client = spanner.Client()
instance = client.instance(INSTANCE_ID)
database = instance.database(DATABASE_ID)

# Parse recovery timestamp (ISO format)
recovery_time = datetime.datetime.fromisoformat(TIMESTAMP_STR.replace("Z", "+00:00"))

print(f"Reading historical state as of {recovery_time}...")

# 1. Read historical rows in a stale snapshot
with database.snapshot(read_timestamp=recovery_time) as snapshot:
    results = snapshot.execute_sql("SELECT AccountId, AccountName, FirmName FROM TradingAccounts")
    rows_to_restore = list(results)

print(f"Found {len(rows_to_restore)} rows to restore.")

# 2. Write them back in a read-write transaction
def restore_tx(transaction, rows):
    transaction.insert_or_update(
        table="TradingAccounts",
        columns=["AccountId", "AccountName", "FirmName"],
        values=rows
    )

if rows_to_restore:
    database.run_in_transaction(restore_tx, rows_to_restore)
    print("In-place restoration completed successfully!")
else:
    print("No rows found at the specified timestamp.")