package ru.mirea.appliancerepair.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.appliancerepair.backend.domain.Customer;
import ru.mirea.appliancerepair.backend.repository.CustomerRepository;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {
    private final CustomerRepository customers;
    public CustomerController(CustomerRepository customers) { this.customers = customers; }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','REPAIRER','ADMIN')")
    public List<CustomerResponse> list() {
        return customers.findAll(org.springframework.data.domain.Sort.by("fullName")).stream()
                .map(c -> new CustomerResponse(c.getId(), c.getFullName(), c.getPhone(), c.getEmail(), c.getAddress()))
                .toList();
    }

    public record CustomerResponse(Long id, String fullName, String phone, String email, String address) { }
}
