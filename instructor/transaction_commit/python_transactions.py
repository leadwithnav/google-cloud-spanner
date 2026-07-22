import json
import os
import uuid
from decimal import Decimal

from google.api_core.exceptions import (
    GoogleAPICallError,
    InvalidArgument,
    NotFound,
    PermissionDenied,
)
from google.cloud import spanner
from google.cloud.spanner_v1 import param_types


# ============================================================
# Cloud Spanner configuration
# ============================================================

PROJECT_ID = "instructor-04"
INSTANCE_ID = "test-instance1"
DATABASE_ID = "test-database1"


# ============================================================
# Prevent accidental connection to the Spanner emulator
# ============================================================

emulator_host = os.environ.get("SPANNER_EMULATOR_HOST")

if emulator_host:
    raise RuntimeError(
        f"SPANNER_EMULATOR_HOST is set to {emulator_host!r}.\n"
        "Remove this environment variable before connecting to the "
        "real Cloud Spanner instance."
    )


# ============================================================
# Create Cloud Spanner client
#
# disable_builtin_metrics=True prevents the earlier client-side
# Monitoring error about a missing instance_id resource label.
# ============================================================

spanner_client = spanner.Client(
    project=PROJECT_ID,
    disable_builtin_metrics=True,
)

instance = spanner_client.instance(INSTANCE_ID)
database = instance.database(DATABASE_ID)


# ============================================================
# Generate IDs
#
# The database columns are STRING(36), so use plain UUID values.
# Do not add ORDER- or AUDIT- prefixes because that would exceed
# the 36-character column limit.
# ============================================================

order_id = str(uuid.uuid4())
audit_id = str(uuid.uuid4())


print("Connection target:")
print(f"  Project : {PROJECT_ID}")
print(f"  Instance: {INSTANCE_ID}")
print(f"  Database: {DATABASE_ID}")
print(f"  Order ID: {order_id}")
print(f"  Audit ID: {audit_id}")
print()


# ============================================================
# Transaction callback
#
# Both INSERT statements run inside one read-write transaction.
# Both PENDING_COMMIT_TIMESTAMP() values therefore resolve to
# the same transaction commit timestamp.
# ============================================================

def insert_order_and_audit(transaction):
    order_row_count = transaction.execute_update(
        """
        INSERT INTO TradeOrders (
            OrderId,
            CustomerId,
            Amount,
            Status,
            CommittedAt
        )
        VALUES (
            @order_id,
            @customer_id,
            @amount,
            @status,
            PENDING_COMMIT_TIMESTAMP()
        )
        """,
        params={
            "order_id": order_id,
            "customer_id": "CUSTOMER-101",
            "amount": Decimal("25000.00"),
            "status": "ACCEPTED",
        },
        param_types={
            "order_id": param_types.STRING,
            "customer_id": param_types.STRING,
            "amount": param_types.NUMERIC,
            "status": param_types.STRING,
        },
    )

    audit_event_data = json.dumps(
        {
            "source": "python-client",
            "message": "Order accepted successfully",
        }
    )

    audit_row_count = transaction.execute_update(
        """
        INSERT INTO AuditEvents (
            AuditId,
            EntityId,
            EventType,
            EventData,
            CommittedAt
        )
        VALUES (
            @audit_id,
            @order_id,
            @event_type,
            @event_data,
            PENDING_COMMIT_TIMESTAMP()
        )
        """,
        params={
            "audit_id": audit_id,
            "order_id": order_id,
            "event_type": "ORDER_ACCEPTED",
            "event_data": audit_event_data,
        },
        param_types={
            "audit_id": param_types.STRING,
            "order_id": param_types.STRING,
            "event_type": param_types.STRING,
            "event_data": param_types.JSON,
        },
    )

    return order_row_count, audit_row_count


# ============================================================
# Main program
# ============================================================

try:
    # Test connectivity before starting the write transaction.
    with database.snapshot() as snapshot:
        list(snapshot.execute_sql("SELECT 1"))

    print("Database connection successful.")

    # Execute both INSERT statements in one transaction.
    order_count, audit_count = database.run_in_transaction(
        insert_order_and_audit
    )

    print("Transaction committed successfully.")
    print(f"TradeOrders rows inserted: {order_count}")
    print(f"AuditEvents rows inserted: {audit_count}")
    print()

    # ========================================================
    # Verify that both rows received the same commit timestamp
    # ========================================================

    verification_sql = """
        SELECT
            o.OrderId,
            o.CommittedAt AS OrderCommitTimestamp,
            a.AuditId,
            a.CommittedAt AS AuditCommitTimestamp,
            o.CommittedAt = a.CommittedAt AS SameCommitTimestamp
        FROM TradeOrders AS o
        JOIN AuditEvents AS a
            ON a.EntityId = o.OrderId
        WHERE o.OrderId = @order_id
          AND a.AuditId = @audit_id
    """

    with database.snapshot() as snapshot:
        result_set = snapshot.execute_sql(
            verification_sql,
            params={
                "order_id": order_id,
                "audit_id": audit_id,
            },
            param_types={
                "order_id": param_types.STRING,
                "audit_id": param_types.STRING,
            },
        )

        rows = list(result_set)

    if not rows:
        print("Verification failed: no matching rows were found.")

    else:
        for row in rows:
            (
                returned_order_id,
                order_commit_timestamp,
                returned_audit_id,
                audit_commit_timestamp,
                same_commit_timestamp,
            ) = row

            print("Verification result")
            print("-------------------")
            print(f"Order ID               : {returned_order_id}")
            print(f"Order commit timestamp : {order_commit_timestamp}")
            print(f"Audit ID               : {returned_audit_id}")
            print(f"Audit commit timestamp : {audit_commit_timestamp}")
            print(f"Same commit timestamp  : {same_commit_timestamp}")
            print()

            if same_commit_timestamp:
                print(
                    "SUCCESS: Both rows received exactly the same "
                    "commit timestamp."
                )
                print(
                    "Both INSERT statements were committed atomically "
                    "inside one read-write transaction."
                )
            else:
                print(
                    "UNEXPECTED RESULT: The commit timestamps are different."
                )


except NotFound as exc:
    print("RESOURCE NOT FOUND")
    print(
        "Check PROJECT_ID, INSTANCE_ID, DATABASE_ID, and table names."
    )
    print(exc)

except PermissionDenied as exc:
    print("PERMISSION DENIED")
    print(
        "Your Application Default Credentials do not have permission "
        "to access this Cloud Spanner database."
    )
    print()
    print("Run:")
    print("gcloud auth application-default login")
    print(
        f"gcloud auth application-default "
        f"set-quota-project {PROJECT_ID}"
    )
    print()
    print(exc)

except InvalidArgument as exc:
    print("INVALID SQL OR SCHEMA")
    print(
        "Check the table definitions, column names, column lengths, "
        "and data types."
    )
    print(exc)

except GoogleAPICallError as exc:
    print("CLOUD SPANNER API ERROR")
    print(exc)

except Exception as exc:
    print("UNEXPECTED ERROR")
    print(f"{type(exc).__name__}: {exc}")