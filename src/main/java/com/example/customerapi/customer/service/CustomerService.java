package com.example.customerapi.customer.service;

import com.example.customerapi.customer.dto.CustomerRequest;
import com.example.customerapi.customer.model.Customer;
import com.example.customerapi.customer.repository.CustomerRepository;
import com.example.customerapi.security.CurrentUser;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

  private final CustomerRepository repository;

  public CustomerService(CustomerRepository repository) {
    this.repository = repository;
  }

  public List<Customer> findAll(CurrentUser user) {
    if (user.isAdmin()) {
      return repository.findAll();
    }
    if (user.department() == null) {
      return List.of();
    }
    return repository.findByDepartmentIgnoreCase(user.department());
  }

  public Customer findById(Long id, CurrentUser user) {
    Customer customer = getOrThrow(id);
    if (!user.canAccessDepartment(customer.getDepartment())) {
      throw new AccessDeniedException("Customer " + id + " belongs to a different department");
    }
    return customer;
  }

  public Customer create(CustomerRequest request) {
    if (repository.existsByEmailIgnoreCase(request.email())) {
      throw new EmailAlreadyExistsException(request.email());
    }
    Customer customer = new Customer();
    request.applyTo(customer);
    return save(customer);
  }

  public Customer update(Long id, CustomerRequest request) {
    Customer customer = getOrThrow(id);
    if (repository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
      throw new EmailAlreadyExistsException(request.email());
    }
    request.applyTo(customer);
    return save(customer);
  }

  public void delete(Long id) {
    repository.delete(getOrThrow(id));
  }

  private Customer save(Customer customer) {
    try {
      return repository.save(customer);
    } catch (DataIntegrityViolationException e) {
      if (isEmailUniqueViolation(e)) {
        throw new EmailAlreadyExistsException(customer.getEmail());
      }
      throw e;
    }
  }

  private static boolean isEmailUniqueViolation(DataIntegrityViolationException e) {
    return e.getCause() instanceof ConstraintViolationException violation
        && violation.getConstraintName() != null
        && violation
            .getConstraintName()
            .toLowerCase()
            .contains(Customer.EMAIL_UNIQUE_CONSTRAINT);
  }

  private Customer getOrThrow(Long id) {
    return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
  }
}
