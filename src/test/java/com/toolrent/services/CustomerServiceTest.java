package com.toolrent.services;

import com.toolrent.entities.CustomerEntity;
import com.toolrent.entities.CustomerStatus;
import com.toolrent.repositories.CustomerRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.toolrent.entities.CustomerStatus.ACTIVE;
import static com.toolrent.entities.CustomerStatus.RESTRICTED;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    /* ---------------------------------------------------------- */
    /* --------------------- ALTAS / REGISTRO ------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Registrar cliente: datos válidos → cliente en estado ACTIVE")
    void whenRegisterCustomer_withValidData_thenSuccess() {
        String name = "Ana", rut = "12.345.678-9", phone = "+56987654321", email = "ana@test.com";
        CustomerEntity saved = buildCustomer(1L, name, rut, phone, email, ACTIVE);
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(saved);

        CustomerEntity result = customerService.registerCustomer(name, rut, phone, email);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getRut()).isEqualTo(rut);
        assertThat(result.getEmail()).isEqualTo(email);
        verify(customerRepository).save(any(CustomerEntity.class));
    }

    @ParameterizedTest(name = "name=''{0}'' → debe fallar por vacío")
    @NullSource
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("Registrar cliente: nombre nulo o blanco → excepción")
    void whenRegisterCustomer_withNullOrBlankName_thenThrows(String name) {
        assertThatThrownBy(() -> customerService.registerCustomer(name, "rut", "phone", "mail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
        verifyNoInteractions(customerRepository);
    }

    @ParameterizedTest(name = "rut=''{0}'' → debe fallar por vacío")
    @NullSource
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("Registrar cliente: RUT nulo o blanco → excepción")
    void whenRegisterCustomer_withNullOrBlankRut_thenThrows(String rut) {
        assertThatThrownBy(() -> customerService.registerCustomer("name", rut, "phone", "mail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @ParameterizedTest(name = "phone=''{0}'' → debe fallar por vacío")
    @NullSource
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("Registrar cliente: teléfono nulo o blanco → excepción")
    void whenRegisterCustomer_withNullOrBlankPhone_thenThrows(String phone) {
        assertThatThrownBy(() -> customerService.registerCustomer("name", "rut", phone, "mail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @ParameterizedTest(name = "email=''{0}'' → debe fallar por vacío")
    @NullSource
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("Registrar cliente: email nulo o blanco → excepción")
    void whenRegisterCustomer_withNullOrBlankEmail_thenThrows(String email) {
        assertThatThrownBy(() -> customerService.registerCustomer("name", "rut", "phone", email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    @Test
    @DisplayName("Registrar cliente: campos extremadamente largos → guarda sin truncar")
    void whenRegisterCustomer_withMaxLongValues_thenOk() {
        int target = 265;
        String domain = "@gmail.com";
        int domainLen = domain.length();
        int localPartLen = target - domainLen;

        String bigName = "X".repeat(255);
        String bigRut = "99.999.999-9";
        String bigPhone = "+".repeat(50);
        String bigEmail = "a".repeat(localPartLen) + domain;

        CustomerEntity saved = buildCustomer(1L, bigName, bigRut, bigPhone, bigEmail, ACTIVE);
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(saved);

        CustomerEntity result = customerService.registerCustomer(bigName, bigRut, bigPhone, bigEmail);

        assertThat(result.getEmail()).hasSize(265);
    }

    @Test
    @DisplayName("Registrar cliente: email mal formado → se acepta (no hay validación de formato)")
    void whenRegisterCustomer_withInvalidEmailFormat_thenStillSaves() {
        CustomerEntity saved = buildCustomer(1L, "n", "r", "p", "invalid-email", ACTIVE);
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(saved);

        CustomerEntity result = customerService.registerCustomer("n", "r", "p", "invalid-email");

        assertThat(result.getEmail()).isEqualTo("invalid-email");
    }

    /* ---------------------------------------------------------- */
    /* --------------------- CAMBIO DE ESTADO ------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Cambiar estado: RESTRICTED → ACTIVE")
    void whenChangeStatus_toActive_thenSuccess() {
        Long id = 1L;
        CustomerEntity customer = buildCustomer(id, "Luis", "22.222.222-2", "phone", "luis@test.com", RESTRICTED);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        customerService.changeStatus(id, ACTIVE);

        assertThat(customer.getStatus()).isEqualTo(ACTIVE);
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("Cambiar estado: ACTIVE → RESTRICTED")
    void whenChangeStatus_toRestricted_thenSuccess() {
        Long id = 1L;
        CustomerEntity customer = buildCustomer(id, "Ana", "11.111.111-1", "phone", "ana@test.com", ACTIVE);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        customerService.changeStatus(id, RESTRICTED);

        assertThat(customer.getStatus()).isEqualTo(RESTRICTED);
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("Cambiar estado: mismo estado → aún así persiste")
    void whenChangeStatus_sameStatus_thenStillSaves() {
        Long id = 1L;
        CustomerEntity customer = buildCustomer(id, "Lucas", "33.333.333-3", "phone", "lucas@test.com", ACTIVE);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        customerService.changeStatus(id, ACTIVE);

        assertThat(customer.getStatus()).isEqualTo(ACTIVE);
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("Cambiar estado: estado null → se acepta y persiste")
    void whenChangeStatus_withNullStatus_thenStillSaves() {
        Long id = 1L;
        CustomerEntity customer = buildCustomer(id, "Marta", "44.444.444-4", "phone", "marta@test.com", RESTRICTED);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        customerService.changeStatus(id, null);

        assertThat(customer.getStatus()).isNull();
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("Cambiar estado: ID negativo → lanza excepción por no encontrar cliente")
    void whenChangeStatus_withNegativeId_thenThrows() {
        Long id = -99L;
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.changeStatus(id, ACTIVE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    @DisplayName("Cambiar estado: ID inexistente → lanza excepción")
    void whenChangeStatus_withNotFoundId_thenThrows() {
        Long id = 9999L;
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.changeStatus(id, RESTRICTED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");
    }

    /* ---------------------------------------------------------- */
    /* --------------------- LISTADOS --------------------------- */
    /* ---------------------------------------------------------- */

    @Test
    @DisplayName("Listar todos: repositorio con datos → devuelve lista completa")
    void whenGetAllCustomers_thenReturnsList() {
        List<CustomerEntity> data = List.of(
                buildCustomer(1L, "A", "1", "p", "a@test.com", ACTIVE),
                buildCustomer(2L, "B", "2", "p", "b@test.com", RESTRICTED)
        );
        when(customerRepository.findAll()).thenReturn(data);

        Iterable<CustomerEntity> result = customerService.getAllCustomers();

        assertThat(result).hasSize(2);
        verify(customerRepository).findAll();
    }

    @Test
    @DisplayName("Listar todos: repositorio vacío → iterable vacío")
    void whenGetAllCustomers_emptyRepo_thenReturnsEmpty() {
        when(customerRepository.findAll()).thenReturn(List.of());

        Iterable<CustomerEntity> result = customerService.getAllCustomers();

        assertThat(result).isEmpty();
    }

    /* ---------------------------------------------------------- */
    /* --------------------- AUXILIARES ------------------------- */
    /* ---------------------------------------------------------- */

    private CustomerEntity buildCustomer(Long id, String name, String rut, String phone, String email,
                                         CustomerStatus status) {
        CustomerEntity c = new CustomerEntity();
        c.setId(id);
        c.setName(name);
        c.setRut(rut);
        c.setPhone(phone);
        c.setEmail(email);
        c.setStatus(status);
        return c;
    }
}