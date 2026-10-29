package com.example.customerapi.customer.dto;

import com.example.customerapi.customer.model.Customer;
import com.example.customerapi.customer.model.CustomerStatus;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        CustomerStatus status,
        String department) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getStatus(),
                customer.getDepartment());
    }
}
