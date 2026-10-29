package com.example.customerapi.customer.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.customerapi.customer.dto.CustomerRequest;
import com.example.customerapi.customer.dto.CustomerResponse;
import com.example.customerapi.customer.model.Customer;
import com.example.customerapi.customer.service.CustomerService;
import com.example.customerapi.security.CurrentUser;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerResponse> findAll(JwtAuthenticationToken authentication) {
        return service.findAll(CurrentUser.from(authentication)).stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable Long id, JwtAuthenticationToken authentication) {
        return CustomerResponse.from(service.findById(id, CurrentUser.from(authentication)));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        Customer created = service.create(request);
        return ResponseEntity.created(URI.create("/api/customers/" + created.getId()))
                .body(CustomerResponse.from(created));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
