package com.example.spannerbank.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @Column(name = "transaction_id", length = 36, nullable = false)
    private String transactionId;

    @Column(name = "source_account_id", length = 36, nullable = false)
    private String sourceAccountId;

    @Column(name = "target_account_id", length = 36, nullable = false)
    private String targetAccountId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private String status;

    // BEST PRACTICE: Server-side Spanner TrueTime Commit Timestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ DEFAULT spanner.commit_timestamp()", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void ensureId() {
        if (this.transactionId == null) {
            this.transactionId = UUID.randomUUID().toString(); // Anti-Hotspot UUID
        }
    }
}
