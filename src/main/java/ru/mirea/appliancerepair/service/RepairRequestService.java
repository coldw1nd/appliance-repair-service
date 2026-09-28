package ru.mirea.appliancerepair.service;

import ru.mirea.appliancerepair.exception.BusinessException;
import ru.mirea.appliancerepair.exception.EntityNotFoundException;
import ru.mirea.appliancerepair.model.Priority;
import ru.mirea.appliancerepair.model.RepairRequest;
import ru.mirea.appliancerepair.model.RequestStatus;
import ru.mirea.appliancerepair.repository.CustomerRepository;
import ru.mirea.appliancerepair.repository.RepairRequestRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RepairRequestService {
    private final RepairRequestRepository repository;
    private final CustomerRepository customerRepository;

    public RepairRequestService(RepairRequestRepository repository, CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    public RepairRequest create(RepairRequest request) throws SQLException {
        validateCommon(request);
        validateCustomer(request.getCustomerId());
        request.setStatus(RequestStatus.NEW);
        request.setCreatedAt(LocalDateTime.now());
        return repository.create(request);
    }

    public List<RepairRequest> findAll() throws SQLException {
        return repository.findAll();
    }

    public RepairRequest requireById(Long id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Заявка с ID " + id + " не найдена"));
    }

    public void update(RepairRequest request) throws SQLException {
        RepairRequest current = requireById(request.getId());
        validateCommon(request);
        validateCustomer(request.getCustomerId());
        validateStatusTransition(current.getStatus(), request.getStatus());
        repository.update(request);
    }

    public void delete(Long id) throws SQLException {
        requireById(id);
        repository.delete(id);
    }

    public List<RepairRequest> searchByTitle(String text) throws SQLException {
        return repository.searchByTitle(text);
    }

    public List<RepairRequest> searchByBrand(String text) throws SQLException {
        return repository.searchByBrand(text);
    }

    public List<RepairRequest> searchByCustomer(String text) throws SQLException {
        return repository.searchByCustomer(text);
    }

    public List<RepairRequest> filterByStatus(RequestStatus status) throws SQLException {
        return repository.filterByStatus(status);
    }

    public List<RepairRequest> filterByPriority(Priority priority) throws SQLException {
        return repository.filterByPriority(priority);
    }

    public List<RepairRequest> sortByDate(List<RepairRequest> requests) {
        return requests.stream().sorted(Comparator.comparing(RepairRequest::getCreatedAt)).toList();
    }

    public List<RepairRequest> sortByCostDescending(List<RepairRequest> requests) {
        return requests.stream().sorted(Comparator.comparing(RepairRequest::getEstimatedCost).reversed()).toList();
    }

    private void validateCommon(RepairRequest request) {
        if (request.getCustomerId() == null) {
            throw new BusinessException("Необходимо выбрать клиента");
        }
        if (request.getApplianceType() == null || request.getApplianceType().isBlank()) {
            throw new BusinessException("Тип бытовой техники не может быть пустым");
        }
        if (request.getApplianceBrand() == null || request.getApplianceBrand().isBlank()) {
            throw new BusinessException("Бренд бытовой техники не может быть пустым");
        }
        if (request.getIssueTitle() == null || request.getIssueTitle().isBlank()) {
            throw new BusinessException("Название неисправности не может быть пустым");
        }
        if (request.getIssueDescription() == null || request.getIssueDescription().isBlank()) {
            throw new BusinessException("Описание неисправности не может быть пустым");
        }
        if (request.getStatus() == null) {
            throw new BusinessException("Статус заявки не указан");
        }
        if (request.getPriority() == null) {
            throw new BusinessException("Приоритет заявки не указан");
        }
        if (request.getEstimatedCost() == null || request.getEstimatedCost().compareTo(BigDecimal.ZERO) < 0 || request.getEstimatedCost().compareTo(new BigDecimal("1000000")) > 0) {
            throw new BusinessException("Ориентировочная стоимость должна быть от 0 до 1000000");
        }
        if (request.getPlannedDate() != null && request.getPlannedDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Плановая дата не может быть в прошлом");
        }
        if (request.isUrgent() && request.getPriority() != Priority.URGENT && request.getPriority() != Priority.HIGH) {
            throw new BusinessException("Для срочной заявки приоритет должен быть HIGH или URGENT");
        }
    }

    private void validateCustomer(Long customerId) throws SQLException {
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new EntityNotFoundException("Клиент с ID " + customerId + " не найден");
        }
    }

    private void validateStatusTransition(RequestStatus oldStatus, RequestStatus newStatus) {
        if (Objects.equals(oldStatus, newStatus)) {
            return;
        }
        boolean allowed = switch (oldStatus) {
            case NEW -> newStatus == RequestStatus.DIAGNOSING || newStatus == RequestStatus.CANCELLED;
            case DIAGNOSING -> newStatus == RequestStatus.REPAIRING || newStatus == RequestStatus.CANCELLED;
            case REPAIRING -> newStatus == RequestStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BusinessException("Переход статуса " + oldStatus + " -> " + newStatus + " запрещен");
        }
    }
}
