package com.example.customerapi.customer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.customerapi.customer.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByDepartmentIgnoreCase(String department);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
