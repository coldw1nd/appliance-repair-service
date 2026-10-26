package ru.mirea.appliancerepair.service;

import ru.mirea.appliancerepair.exception.BusinessException;
import ru.mirea.appliancerepair.exception.EntityNotFoundException;
import ru.mirea.appliancerepair.model.Customer;
import ru.mirea.appliancerepair.repository.CustomerRepository;

import java.sql.SQLException;
import java.util.List;

public class CustomerService {
    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public Customer create(Customer customer) throws SQLException {
        validate(customer);
        try {
            return repository.create(customer);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                String message = e.getMessage() == null ? "" : e.getMessage();
                if (message.contains("customers_email_key")) {
                    throw new BusinessException("Клиент с таким email уже существует");
                }
                if (message.contains("customers_phone_key")) {
                    throw new BusinessException("Клиент с таким телефоном уже существует");
                }
                throw new BusinessException("Клиент с такими уникальными данными уже существует");
            }
            throw e;
        }
    }

    public List<Customer> findAll() throws SQLException {
        return repository.findAll();
    }

    public Customer requireById(Long id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден"));
    }

    private void validate(Customer customer) {
        if (customer.getFullName() == null || customer.getFullName().isBlank()) {
            throw new BusinessException("ФИО клиента не может быть пустым");
        }
        if (customer.getFullName().length() > 120) {
            throw new BusinessException("ФИО клиента не должно превышать 120 символов");
        }
        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new BusinessException("Телефон клиента не может быть пустым");
        }
        if (customer.getPhone().length() > 30) {
            throw new BusinessException("Телефон клиента не должен превышать 30 символов");
        }
        if (customer.getEmail() == null || customer.getEmail().isBlank()
                || !customer.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new BusinessException("Укажите корректный email клиента (например, name@example.com)");
        }
        if (customer.getEmail().length() > 150) {
            throw new BusinessException("Email клиента не должен превышать 150 символов");
        }
        if (customer.getAddress() == null || customer.getAddress().isBlank()) {
            throw new BusinessException("Адрес клиента не может быть пустым");
        }
        if (customer.getAddress().length() > 250) {
            throw new BusinessException("Адрес клиента не должен превышать 250 символов");
        }
    }
}
