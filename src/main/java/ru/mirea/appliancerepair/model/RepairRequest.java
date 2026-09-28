package ru.mirea.appliancerepair.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RepairRequest {
    private Long id;
    private Long customerId;
    private String customerName;
    private String applianceType;
    private String applianceBrand;
    private String issueTitle;
    private String issueDescription;
    private RequestStatus status;
    private Priority priority;
    private BigDecimal estimatedCost;
    private LocalDate plannedDate;
    private boolean urgent;
    private LocalDateTime createdAt;

    public RepairRequest() {
    }

    public RepairRequest(Long id, Long customerId, String customerName, String applianceType, String applianceBrand,
                         String issueTitle, String issueDescription, RequestStatus status, Priority priority,
                         BigDecimal estimatedCost, LocalDate plannedDate, boolean urgent, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.applianceType = applianceType;
        this.applianceBrand = applianceBrand;
        this.issueTitle = issueTitle;
        this.issueDescription = issueDescription;
        this.status = status;
        this.priority = priority;
        this.estimatedCost = estimatedCost;
        this.plannedDate = plannedDate;
        this.urgent = urgent;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getApplianceType() {
        return applianceType;
    }

    public String getApplianceBrand() {
        return applianceBrand;
    }

    public String getIssueTitle() {
        return issueTitle;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setApplianceType(String applianceType) {
        this.applianceType = applianceType;
    }

    public void setApplianceBrand(String applianceBrand) {
        this.applianceBrand = applianceBrand;
    }

    public void setIssueTitle(String issueTitle) {
        this.issueTitle = issueTitle;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
