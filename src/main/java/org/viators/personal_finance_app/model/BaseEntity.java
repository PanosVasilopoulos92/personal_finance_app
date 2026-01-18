package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NaturalId
    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private String uuid;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "status", nullable = false, length = 1)
    private String status;

    @PrePersist
    public void onCreate() {
        if (uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }

        this.createdBy = getCurrentUser();
        this.updatedBy = getCurrentUser();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "1";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedBy = getCurrentUser();
        this.updatedAt = LocalDateTime.now();
    }

    private String getCurrentUser() {
        // Todo: implement authentication
        return "panosV";
    }
}
