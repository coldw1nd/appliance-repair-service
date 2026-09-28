package ru.mirea.appliancerepair.repository;

import ru.mirea.appliancerepair.model.Customer;
import ru.mirea.appliancerepair.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerRepository {
    public Customer create(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (full_name, phone, email, address) VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getPhone());
            statement.setString(3, customer.getEmail());
            statement.setString(4, customer.getAddress());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                customer.setId(resultSet.getLong("id"));
                return customer;
            }
        }
    }

    public List<Customer> findAll() throws SQLException {
        String sql = "SELECT id, full_name, phone, email, address FROM customers ORDER BY id";
        List<Customer> customers = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                customers.add(map(resultSet));
            }
        }
        return customers;
    }

    public Optional<Customer> findById(Long id) throws SQLException {
        String sql = "SELECT id, full_name, phone, email, address FROM customers WHERE id = ?";
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

    private Customer map(ResultSet rs) throws SQLException {
        return new Customer(rs.getLong("id"), rs.getString("full_name"), rs.getString("phone"),
                rs.getString("email"), rs.getString("address"));
    }
}
