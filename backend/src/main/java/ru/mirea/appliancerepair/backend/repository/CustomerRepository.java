package ru.mirea.appliancerepair.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.appliancerepair.backend.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> { }
