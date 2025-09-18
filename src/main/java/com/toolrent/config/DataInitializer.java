package com.toolrent.config;

import com.toolrent.entities.CustomerEntity;
import com.toolrent.repositories.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    public DataInitializer(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        createSystemCustomer();
    }

    private void createSystemCustomer() {
        // Verificar si ya existe el cliente sistema
        if (!customerRepository.findByEmail("system@toolrent.com").isPresent()) {
            CustomerEntity systemCustomer = new CustomerEntity();
            systemCustomer.setName("Sistema ToolRent");
            systemCustomer.setEmail("system@toolrent.com");
            systemCustomer.setRut("99999999-9"); // RUT ficticio
            systemCustomer.setPhone("123456789");

            customerRepository.save(systemCustomer);
            System.out.println("✅ Cliente sistema creado automáticamente");
        }
    }
}