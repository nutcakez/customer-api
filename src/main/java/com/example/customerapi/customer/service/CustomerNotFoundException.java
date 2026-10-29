package com.example.customerapi.customer.service;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long id) {
        super("Customer " + id + " not found");
    }
}
