package com.example.customerapi.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.example.customerapi.customer.model.Customer;
import com.example.customerapi.customer.model.CustomerStatus;

public record CustomerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotNull CustomerStatus status,
        @NotBlank String department) {

    public void applyTo(Customer customer) {
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setStatus(status);
        customer.setDepartment(department);
    }
}
