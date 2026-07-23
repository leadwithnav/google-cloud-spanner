package com.example.spannerbank.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "status", nullable = false)
    private String status;

    // BEST PRACTICE: Server-side Spanner TrueTime Commit Timestamp
    // Column uses DEFAULT spanner.commit_timestamp() in DDL
    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ DEFAULT spanner.commit_timestamp()", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void ensureId() {
        if (this.customerId == null) {
            this.customerId = UUID.randomUUID().toString(); // Anti-Hotspot UUID
        }
    }
}
