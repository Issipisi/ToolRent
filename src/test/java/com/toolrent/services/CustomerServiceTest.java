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

import static com.toolrent.entities.CustomerStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @InjectMocks private CustomerService customerService;

    /* ======================================================================
            1. REGISTER  (null / blank / ok)
       ====================================================================== */

    @Test @DisplayName("ok – rama feliz")
    void register_ok() {
        CustomerEntity expected = buildCustomer(1L,"A","1","2","a@a.com", ACTIVE);
        when(customerRepository.save(any())).thenReturn(expected);

        CustomerEntity got = customerService.registerCustomer("A","1","2","a@a.com");

        assertThat(got).isEqualTo(expected);
        verify(customerRepository).save(any());
    }

    /* ---- cada campo null ---- */
    @ParameterizedTest @NullSource @DisplayName("null name")
    void register_nullName(String name){
        assertThatThrownBy(() -> customerService.registerCustomer(name,"1","2","mail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
        verifyNoInteractions(customerRepository);
    }
    @ParameterizedTest @NullSource @DisplayName("null rut")
    void register_nullRut(String rut){
        assertThatThrownBy(() -> customerService.registerCustomer("A",rut,"2","mail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }
    @ParameterizedTest @NullSource @DisplayName("null phone")
    void register_nullPhone(String phone){
        assertThatThrownBy(() -> customerService.registerCustomer("A","1",phone,"mail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }
    @ParameterizedTest @NullSource @DisplayName("null email")
    void register_nullEmail(String email){
        assertThatThrownBy(() -> customerService.registerCustomer("A","1","2",email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    /* ---- cada campo blank (empty / spaces) ---- */
    @ParameterizedTest @ValueSource(strings = {""," ","  "}) @DisplayName("blank name")
    void register_blankName(String name){
        assertThatThrownBy(() -> customerService.registerCustomer(name,"1","2","mail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }
    @ParameterizedTest @ValueSource(strings = {""," ","  "}) @DisplayName("blank rut")
    void register_blankRut(String rut){
        assertThatThrownBy(() -> customerService.registerCustomer("A",rut,"2","mail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }
    @ParameterizedTest @ValueSource(strings = {""," ","  "}) @DisplayName("blank phone")
    void register_blankPhone(String phone){
        assertThatThrownBy(() -> customerService.registerCustomer("A","1",phone,"mail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }
    @ParameterizedTest @ValueSource(strings = {""," ","  "}) @DisplayName("blank email")
    void register_blankEmail(String email){
        assertThatThrownBy(() -> customerService.registerCustomer("A","1","2",email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("obligatorios");
    }

    /* ======================================================================
            2. changeStatus  (found / not-found)
       ====================================================================== */

    @Test @DisplayName("changeStatus – cliente existe")
    void changeStatus_found(){
        CustomerEntity c = buildCustomer(5L,"X","1","2","x@x.com", ACTIVE);
        when(customerRepository.findById(5L)).thenReturn(Optional.of(c));
        when(customerRepository.save(any())).thenReturn(c);

        customerService.changeStatus(5L, RESTRICTED);

        assertThat(c.getStatus()).isEqualTo(RESTRICTED);
        verify(customerRepository).save(c);
    }

    @Test @DisplayName("changeStatus – cliente NO existe → excepción")
    void changeStatus_notFound(){
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.changeStatus(99L, ACTIVE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
        verify(customerRepository, never()).save(any());
    }

    /* ======================================================================
            3. getSystemCustomer    (presente / ausente)
       ====================================================================== */

    @Test @DisplayName("getSystemCustomer – ya existe → no guarda")
    void getSystem_exists(){
        CustomerEntity existing = buildCustomer(10L,"Sistema","0-0","000","system@toolrent.com", ACTIVE);
        when(customerRepository.findByEmail("system@toolrent.com")).thenReturn(Optional.of(existing));

        CustomerEntity got = customerService.getSystemCustomer();

        assertThat(got).isSameAs(existing);
        verify(customerRepository, never()).save(any());
    }

    @Test @DisplayName("getSystemCustomer – no existe → crea y guarda")
    void getSystem_notExists(){
        when(customerRepository.findByEmail("system@toolrent.com")).thenReturn(Optional.empty());
        CustomerEntity toSave = buildCustomer(20L,"Sistema","0-0","000","system@toolrent.com", ACTIVE);
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(toSave);

        CustomerEntity got = customerService.getSystemCustomer();

        assertThat(got.getName()).isEqualTo("Sistema");
        verify(customerRepository).save(any());
    }

    /* ======================================================================
            4. Queries – listas vacías / con datos
       ====================================================================== */

    @Test @DisplayName("getAllCustomers – con datos")
    void getAll_withData(){
        List<CustomerEntity> list = List.of(
                buildCustomer(1L,"A","1","2","a@a.com", ACTIVE),
                buildCustomer(2L,"B","2","3","b@b.com", RESTRICTED));
        when(customerRepository.findAll()).thenReturn(list);

        Iterable<CustomerEntity> res = customerService.getAllCustomers();

        assertThat(res).hasSize(2);
    }

    @Test @DisplayName("getAllCustomers – vacía")
    void getAll_empty(){
        when(customerRepository.findAll()).thenReturn(List.of());

        Iterable<CustomerEntity> res = customerService.getAllCustomers();

        assertThat(res).isEmpty();
    }

    @Test @DisplayName("getCustomersByStatus – con datos")
    void byStatus_withData(){
        List<CustomerEntity> list = List.of(
                buildCustomer(3L,"C","3","4","c@c.com", ACTIVE),
                buildCustomer(4L,"D","4","5","d@d.com", ACTIVE));
        when(customerRepository.findByStatus(ACTIVE)).thenReturn(list);

        List<CustomerEntity> res = customerService.getCustomersByStatus(ACTIVE);

        assertThat(res).hasSize(2);
        assertThat(res).allMatch(c -> c.getStatus() == ACTIVE);
    }

    @Test @DisplayName("getCustomersByStatus – vacía")
    void byStatus_empty(){
        when(customerRepository.findByStatus(RESTRICTED)).thenReturn(List.of());

        List<CustomerEntity> res = customerService.getCustomersByStatus(RESTRICTED);

        assertThat(res).isEmpty();
    }

    /* ======================================================================
                                    Helper
       ====================================================================== */
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