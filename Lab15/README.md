# Lab 15: Production-Grade Cloud Spanner Spring Boot Application with Advanced Best Practices

This directory contains the completed production codebase for **Lab 15**, incorporating all 6 Cloud Spanner & PGAdapter engineering recommendations.

---

## 🌟 Implemented Production Best Practices

1. **Aborted Transaction Retries (`@Retryable`) & `SELECT FOR UPDATE`**:
   - `SELECT FOR UPDATE` (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) acquires exclusive locks immediately during initial read phase, preventing lock upgrade aborts under concurrency.
   - Annotated service methods with `@Retryable(retryFor = {Exception.class}, maxAttempts = 5)` to handle any remaining lock contention `AbortedException`.

2. **Spanner Mutations vs SQL DML**:
   - For high-throughput bulk write pipelines, Spanner Mutations bypass SQL query parsing and plan compilation, writing key-value tuples directly to storage nodes for 2x–3x higher ingestion performance.
   - PGAdapter leverages batch Mutation protocol under `preferQueryMode=extended`.

2. **Server-Side TrueTime Commit Timestamps**:
   - Replaced client-side timestamps (`OffsetDateTime.now()`) with Spanner's atomic TrueTime `spanner.commit_timestamp()`.
   - Annotated JPA entities with `@Column(columnDefinition = "TIMESTAMPTZ DEFAULT spanner.commit_timestamp()", insertable = false, updatable = false)`.

3. **Extended Query Protocol & Query Optimizer Settings**:
   - Connection URL: `jdbc:postgresql://localhost:5432/spanner-bank-db?preferQueryMode=extended&options=-c%20spanner.optimizer_version=LATEST`

4. **Foreign Key Referential Integrity Constraints**:
   - DDL schema defines `CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id)` for database-enforced integrity.

5. **Partitioned DML for Massive Maintenance Operations**:
   - Implemented `bulkUpdateAccountTypesPartitioned` for large batch DML updates without exceeding Spanner's 80,000 mutation limit per transaction.

6. **Anti-Hotspotting Primary Keys & Snapshot Reads**:
   - Client-side random `UUID` primary keys (`UUID.randomUUID().toString()`) prevent monotonic write hotspots.
   - `@Transactional(readOnly = true)` snapshot reads execute lock-free from local read-replicas.

---

## 🚀 Execution Steps

### 1. Launch PGAdapter Proxy
```powershell
.\start-pgadapter.ps1 -ProjectId instructor-04 -InstanceId test-instance1 -DatabaseId spanner-bank-db -Port 5432
```

### 2. Deploy DDL Schema
```bash
gcloud spanner databases ddl update spanner-bank-db \
  --instance=test-instance1 \
  --ddl-file=ddl/schema.sql
```

### 3. Launch Spring Boot App
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot"
mvn spring-boot:run
```
