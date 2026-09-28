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
            if (e.getMessage() != null && e.getMessage().contains("customers_email_key")) {
                throw new BusinessException("Клиент с таким email уже существует");
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
        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new BusinessException("Телефон клиента не может быть пустым");
        }
        if (customer.getEmail() == null || customer.getEmail().isBlank() || !customer.getEmail().contains("@")) {
            throw new BusinessException("Укажите корректный email клиента");
        }
        if (customer.getAddress() == null || customer.getAddress().isBlank()) {
            throw new BusinessException("Адрес клиента не может быть пустым");
        }
    }
}
