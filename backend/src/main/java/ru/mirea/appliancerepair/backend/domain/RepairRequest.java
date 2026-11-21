package ru.mirea.appliancerepair.backend.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "repair_requests")
public class RepairRequest {
    @Id
    @Column(name = "request_uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "request_number", nullable = false, unique = true, length = 30, updatable = false)
    private String requestNumber;

    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "appliance_type", nullable = false, length = 80)
    private String applianceTypeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appliance_type_id")
    private ApplianceType applianceType;

    @Column(name = "appliance_brand", nullable = false, length = 80)
    private String applianceBrand;

    @Column(name = "issue_title", nullable = false, length = 200)
    private String issueTitle;

    @Column(name = "issue_description", nullable = false, length = 1000)
    private String issueDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestPriority priority = RequestPriority.MEDIUM;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(nullable = false)
    private boolean urgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RepairRisk riskLevel = RepairRisk.LOW;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by")
    private UserAccount archivedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserAccount updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "repair_request_tag", joinColumns = @JoinColumn(name = "repair_request_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected RepairRequest() { }

    public RepairRequest(UUID id, String requestNumber, Customer customer, String applianceTypeName,
                         ApplianceType applianceType, String applianceBrand, String issueTitle,
                         String issueDescription, UserAccount creator) {
        this.id = id;
        this.requestNumber = requestNumber;
        this.customer = customer;
        this.applianceTypeName = applianceTypeName;
        this.applianceType = applianceType;
        this.applianceBrand = applianceBrand;
        this.issueTitle = issueTitle;
        this.issueDescription = issueDescription;
        this.createdBy = creator;
        this.updatedBy = creator;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (id == null) id = UUID.randomUUID();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getRequestNumber() { return requestNumber; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getApplianceTypeName() { return applianceTypeName; }
    public void setApplianceTypeName(String applianceTypeName) { this.applianceTypeName = applianceTypeName; }
    public ApplianceType getApplianceType() { return applianceType; }
    public void setApplianceType(ApplianceType applianceType) { this.applianceType = applianceType; }
    public String getApplianceBrand() { return applianceBrand; }
    public void setApplianceBrand(String applianceBrand) { this.applianceBrand = applianceBrand; }
    public String getIssueTitle() { return issueTitle; }
    public void setIssueTitle(String issueTitle) { this.issueTitle = issueTitle; }
    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public RequestPriority getPriority() { return priority; }
    public void setPriority(RequestPriority priority) { this.priority = priority; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
    public RepairRisk getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RepairRisk riskLevel) { this.riskLevel = riskLevel; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
    public UserAccount getArchivedBy() { return archivedBy; }
    public void setArchivedBy(UserAccount archivedBy) { this.archivedBy = archivedBy; }
    public UserAccount getCreatedBy() { return createdBy; }
    public UserAccount getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UserAccount updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags); }
}
