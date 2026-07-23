import os
import time

from google.cloud import spanner


PROJECT_ID = "instructor-04"
INSTANCE_ID = "test-instance1"
DATABASE_ID = "test-database1"


ACCOUNT_ID = "A-00000001"


def conflicting_update(transaction):
    print("Session 2 submitting conflicting UPDATE...", flush=True)

    row_count = transaction.execute_update(
        """
        UPDATE Account_new
        SET
            AvailableBalance = AvailableBalance - 20,
            VersionNumber = VersionNumber + 1,
            LastModifiedAt = PENDING_COMMIT_TIMESTAMP()
        WHERE AccountId = @account_id
        """,
        params={"account_id": ACCOUNT_ID},
        param_types={
            "account_id": spanner.param_types.STRING
        },
    )

    print(f"Session 2 updated {row_count} row.", flush=True)


def main():
    client = spanner.Client(
        project=PROJECT_ID,
        disable_builtin_metrics=True,
    )

    database = (
        client.instance(INSTANCE_ID)
        .database(DATABASE_ID)
    )

    start = time.perf_counter()

    try:
        database.run_in_transaction(conflicting_update)

        elapsed = time.perf_counter() - start
        print(
            f"Session 2 committed after {elapsed:.2f} seconds."
        )

    finally:
        client.close()


if __name__ == "__main__":
    main()