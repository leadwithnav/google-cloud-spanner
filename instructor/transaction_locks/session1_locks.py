import os
import time

from google.cloud import spanner
from google.api_core.exceptions import Aborted


PROJECT_ID = "instructor-04"
INSTANCE_ID = "test-instance1"
DATABASE_ID = "test-database1"


ACCOUNT_ID = "A-00000001"
HOLD_SECONDS = 10


def hold_lock(transaction):
    rows = list(
        transaction.execute_sql(
            """
            SELECT
                AccountId,
                AvailableBalance
            FROM Account_new
            WHERE AccountId = @account_id
            FOR UPDATE
            """,
            params={"account_id": ACCOUNT_ID},
            param_types={
                "account_id": spanner.param_types.STRING
            },
        )
    )

    print(f"Exclusive lock acquired: {rows}", flush=True)
    print(
        f"Holding the lock for {HOLD_SECONDS} seconds...",
        flush=True,
    )

    # Keep this below Spanner's idle transaction limit.
    time.sleep(HOLD_SECONDS)

    row_count = transaction.execute_update(
        """
        UPDATE Account_new
        SET
            AvailableBalance = AvailableBalance - 10,
            VersionNumber = VersionNumber + 1,
            LastModifiedAt = PENDING_COMMIT_TIMESTAMP()
        WHERE AccountId = @account_id
        """,
        params={"account_id": ACCOUNT_ID},
        param_types={
            "account_id": spanner.param_types.STRING
        },
    )

    print(f"Session 1 updated {row_count} row.", flush=True)


def main():
    client = spanner.Client(
        project=PROJECT_ID,
        disable_builtin_metrics=True,
    )

    database = (
        client.instance(INSTANCE_ID)
        .database(DATABASE_ID)
    )

    try:
        database.run_in_transaction(hold_lock)
        print("Session 1 committed successfully.")

    except Aborted as exc:
        print(f"Session 1 was aborted: {exc}")
        raise

    finally:
        client.close()


if __name__ == "__main__":
    main()