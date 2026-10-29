package com.example.customerapi.config;

import com.example.customerapi.customer.dto.CustomerRequest;
import com.example.customerapi.customer.model.Customer;
import com.example.customerapi.customer.repository.CustomerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "customer-api.seed-file")
public class CustomerDataLoader implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CustomerDataLoader.class);

  private final CustomerRepository repository;
  private final ObjectMapper objectMapper;
  private final Resource seedFile;

  public CustomerDataLoader(
      CustomerRepository repository,
      ObjectMapper objectMapper,
      @Value("${customer-api.seed-file}") Resource seedFile) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.seedFile = seedFile;
  }

  @Override
  public void run(ApplicationArguments args) throws IOException {
    if (repository.count() > 0 || !seedFile.exists()) {
      return;
    }
    try (InputStream in = seedFile.getInputStream()) {
      List<CustomerRequest> seed = objectMapper.readValue(in, new TypeReference<>() {});
      List<Customer> customers =
          seed.stream()
              .map(
                  request -> {
                    Customer customer = new Customer();
                    request.applyTo(customer);
                    return customer;
                  })
              .toList();
      repository.saveAll(customers);
      log.info("Loaded {} customers from {}", customers.size(), seedFile);
    }
  }
}
