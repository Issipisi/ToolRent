package com.toolrent.services;

import com.toolrent.entities.CustomerEntity;
import com.toolrent.entities.CustomerStatus;
import com.toolrent.repositories.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /* Registrar cliente */
    public CustomerEntity registerCustomer(String name, String rut, String phone, String email) {
        /* En CustomerService.registerCustomer(...) */
        if (name == null || name.isBlank() ||
                rut == null || rut.isBlank() ||
                phone == null || phone.isBlank() ||
                email == null || email.isBlank()) {
            throw new RuntimeException("Nombre, RUT, teléfono y correo son obligatorios");
        }
        CustomerEntity customer = new CustomerEntity();
        customer.setName(name);
        customer.setRut(rut);
        customer.setPhone(phone);
        customer.setEmail(email);
        return customerRepository.save(customer);
    }

    /* Cambiar estado del cliente */
    public void changeStatus(Long id, CustomerStatus newStatus) {
        CustomerEntity customer = customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer " +
                "not found"));
        customer.setStatus(newStatus);
        customerRepository.save(customer);
    }

    public Iterable<CustomerEntity> getAllCustomers() {
        return customerRepository.findAll();
    }

    public List<CustomerEntity> getCustomersByStatus(CustomerStatus status) {
        return customerRepository.findByStatus(status);
    }

    public CustomerEntity getSystemCustomer() {

        return customerRepository.findByEmail("system@toolrent.com")
                .orElseGet(() -> {
                    CustomerEntity sys = new CustomerEntity();
                    sys.setName("Sistema");
                    sys.setRut("0-0");
                    sys.setEmail("system@toolrent.com");
                    sys.setPhone("000");
                    sys.setStatus(CustomerStatus.ACTIVE);
                    return customerRepository.save(sys);
                });
    }
}