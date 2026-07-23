package com.example.spannerbank.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@IdClass(AccountId.class) // BEST PRACTICE 1: Composite key for INTERLEAVE IN PARENT customers
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Id
    @Column(name = "account_id", length = 36, nullable = false)
    private String accountId;

    @Column(name = "account_type", length = 50, nullable = false)
    private String accountType;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void ensureIdAndTimestamp() {
        if (this.accountId == null) {
            this.accountId = UUID.randomUUID().toString(); // Anti-Hotspot UUID
        }
        this.updatedAt = OffsetDateTime.now();
    }
}
