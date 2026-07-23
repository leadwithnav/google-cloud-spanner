# Lab 14: Cloud Spanner with Java, Spring Boot & Spring Data JPA (via PGAdapter)

A production-ready Spring Boot 3 + Spring Data JPA application demonstrating best practices for integrating Java applications with Google Cloud Spanner using **PGAdapter** (PostgreSQL Dialect).

---

## 🚀 Key Architectural Best Practices Covered

1. **Anti-Hotspotting Primary Key Design**: Uses `UUID` primary keys to evenly distribute row writes across Spanner splits. Avoids `IDENTITY` / `AUTO_INCREMENT` sequence anti-patterns.
2. **Snapshot Read Optimizations**: Uses `@Transactional(readOnly = true)` for read methods to perform non-locking Snapshot Reads in Spanner.
3. **High-Performance JDBC Batching**: Configured with `hibernate.jdbc.batch_size: 50`, `order_inserts`, and `order_updates` to group multiple JPA operations into single gRPC network RPC calls.
4. **HikariCP Connection Pool Tuning**: Connection pool settings optimized for PGAdapter proxy performance.
5. **Serializable Atomic Transactions**: Short-lived Read-Write transactions for multi-row money transfers.

---

## 🛠️ Step-by-Step Execution Instructions

### Step 1: Deploy Database Schema (PostgreSQL Dialect)
Create a Cloud Spanner database using the **PostgreSQL dialect** and deploy `ddl/schema.sql`:

```bash
gcloud spanner databases create spanner-bank-db \
  --instance=test-instance \
  --database-dialect=POSTGRESQL

gcloud spanner databases ddl update spanner-bank-db \
  --instance=test-instance \
  --ddl-file=ddl/schema.sql
```

### Step 2: Start PGAdapter Proxy
Start the PGAdapter proxy using Docker:

```bash
# On Linux/macOS:
./start-pgadapter.sh my-gcp-project test-instance spanner-bank-db 5432

# On Windows PowerShell:
.\start-pgadapter.ps1 -ProjectId my-gcp-project -InstanceId test-instance -DatabaseId spanner-bank-db -Port 5432
```

### Step 3: Run Spring Boot Application
Build and launch the Spring Boot service:

```bash
mvn clean spring-boot:run
```

### Step 4: Test REST API Endpoints

1. **Seed Bulk Customers (Batch Insert Test)**:
   ```bash
   curl -X POST "http://localhost:8080/api/v1/banking/seed?count=50"
   ```

2. **Fetch All Customers (Snapshot Read)**:
   ```bash
   curl "http://localhost:8080/api/v1/banking/customers"
   ```

3. **Create Customer & Account**:
   ```bash
   curl -X POST "http://localhost:8080/api/v1/banking/customers" \
     -H "Content-Type: application/json" \
     -d '{"fullName": "Alice Smith", "email": "alice@spannerbank.com", "accountType": "CHECKING", "initialBalance": 2500.00}'
   ```

4. **Transfer Money (Atomic Read-Write Transaction)**:
   ```bash
   curl -X POST "http://localhost:8080/api/v1/banking/transfer" \
     -H "Content-Type: application/json" \
     -d '{"sourceAccountId": "<SOURCE_ACCOUNT_ID>", "targetAccountId": "<TARGET_ACCOUNT_ID>", "amount": 250.00}'
   ```
