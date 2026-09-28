package ru.mirea.appliancerepair.repository;

import ru.mirea.appliancerepair.model.Priority;
import ru.mirea.appliancerepair.model.RepairRequest;
import ru.mirea.appliancerepair.model.RequestStatus;
import ru.mirea.appliancerepair.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepairRequestRepository {
    private static final String BASE_SELECT = "SELECT r.id, r.customer_id, c.full_name AS customer_name, r.appliance_type, r.appliance_brand, r.issue_title, r.issue_description, r.status, r.priority, r.estimated_cost, r.planned_date, r.urgent, r.created_at FROM repair_requests r JOIN customers c ON c.id = r.customer_id ";

    public RepairRequest create(RepairRequest request) throws SQLException {
        String sql = "INSERT INTO repair_requests (customer_id, appliance_type, appliance_brand, issue_title, issue_description, status, priority, estimated_cost, planned_date, urgent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, request);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                request.setId(resultSet.getLong("id"));
                request.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                return request;
            }
        }
    }

    public List<RepairRequest> findAll() throws SQLException {
        return find(BASE_SELECT + "ORDER BY r.id");
    }

    public Optional<RepairRequest> findById(Long id) throws SQLException {
        String sql = BASE_SELECT + "WHERE r.id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public void update(RepairRequest request) throws SQLException {
        String sql = "UPDATE repair_requests SET customer_id = ?, appliance_type = ?, appliance_brand = ?, issue_title = ?, issue_description = ?, status = ?, priority = ?, estimated_cost = ?, planned_date = ?, urgent = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, request);
            statement.setLong(11, request.getId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Запись не была изменена");
            }
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM repair_requests WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Запись не была удалена");
            }
        }
    }

    public List<RepairRequest> searchByTitle(String text) throws SQLException {
        String sql = BASE_SELECT + "WHERE LOWER(r.issue_title) LIKE LOWER(?) ORDER BY r.id";
        return findWithSingleParameter(sql, "%" + text + "%");
    }

    public List<RepairRequest> searchByBrand(String text) throws SQLException {
        String sql = BASE_SELECT + "WHERE LOWER(r.appliance_brand) LIKE LOWER(?) ORDER BY r.id";
        return findWithSingleParameter(sql, "%" + text + "%");
    }

    public List<RepairRequest> searchByCustomer(String text) throws SQLException {
        String sql = BASE_SELECT + "WHERE LOWER(c.full_name) LIKE LOWER(?) ORDER BY r.id";
        return findWithSingleParameter(sql, "%" + text + "%");
    }

    public List<RepairRequest> filterByStatus(RequestStatus status) throws SQLException {
        String sql = BASE_SELECT + "WHERE r.status = ? ORDER BY r.id";
        return findWithSingleParameter(sql, status.name());
    }

    public List<RepairRequest> filterByPriority(Priority priority) throws SQLException {
        String sql = BASE_SELECT + "WHERE r.priority = ? ORDER BY r.id";
        return findWithSingleParameter(sql, priority.name());
    }

    private List<RepairRequest> find(String sql) throws SQLException {
        List<RepairRequest> requests = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                requests.add(map(resultSet));
            }
        }
        return requests;
    }

    private List<RepairRequest> findWithSingleParameter(String sql, String parameter) throws SQLException {
        List<RepairRequest> requests = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(map(resultSet));
                }
            }
        }
        return requests;
    }

    private void fillStatement(PreparedStatement statement, RepairRequest request) throws SQLException {
        statement.setLong(1, request.getCustomerId());
        statement.setString(2, request.getApplianceType());
        statement.setString(3, request.getApplianceBrand());
        statement.setString(4, request.getIssueTitle());
        statement.setString(5, request.getIssueDescription());
        statement.setString(6, request.getStatus().name());
        statement.setString(7, request.getPriority().name());
        statement.setBigDecimal(8, request.getEstimatedCost());
        if (request.getPlannedDate() == null) {
            statement.setNull(9, Types.DATE);
        } else {
            statement.setDate(9, Date.valueOf(request.getPlannedDate()));
        }
        statement.setBoolean(10, request.isUrgent());
    }

    private RepairRequest map(ResultSet rs) throws SQLException {
        return new RepairRequest(
                rs.getLong("id"),
                rs.getLong("customer_id"),
                rs.getString("customer_name"),
                rs.getString("appliance_type"),
                rs.getString("appliance_brand"),
                rs.getString("issue_title"),
                rs.getString("issue_description"),
                RequestStatus.valueOf(rs.getString("status")),
                Priority.valueOf(rs.getString("priority")),
                rs.getBigDecimal("estimated_cost"),
                rs.getDate("planned_date") == null ? null : rs.getDate("planned_date").toLocalDate(),
                rs.getBoolean("urgent"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
