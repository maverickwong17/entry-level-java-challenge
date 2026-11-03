package com.challenge.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.challenge.api.exception.EmployeeNotFoundException;
import com.challenge.api.exception.InvalidEmployeeException;
import com.challenge.api.model.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployeeServiceTest {

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService();
    }

    @Test
    void getAllReturnsSeededEmployees() {
        assertThat(service.getAll()).isNotEmpty();
    }

    @Test
    void seededEmployeeHasContractTerminationDate() {
        Employee employee = service.getAll().stream()
                .filter(candidate -> candidate.getFullName().equals("Grace Hopper"))
                .findFirst()
                .orElseThrow();

        assertThat(employee.getContractTerminationDate()).isEqualTo(Instant.parse("2024-12-31T00:00:00Z"));
    }

    @Test
    void createAssignsUuidAndDerivesFullName() {
        Employee created = service.create(validRequest());

        assertThat(created.getUuid()).isNotNull();
        assertThat(created.getFullName()).isEqualTo("Katherine Johnson");
        assertThat(created.getContractTerminationDate()).isNull();
        assertThat(service.getByUuid(created.getUuid())).isEqualTo(created);
    }

    @Test
    void createRejectsMissingRequiredFields() {
        assertThatThrownBy(() -> service.create(new CreateEmployeeRequest()))
                .isInstanceOfSatisfying(InvalidEmployeeException.class, e -> assertThat(e.getErrors())
                        .hasSize(7));
    }

    @Test
    void createRejectsNullRequest() {
        assertThatThrownBy(() -> service.create(null)).isInstanceOf(InvalidEmployeeException.class);
    }

    @Test
    void createRejectsTerminationBeforeHire() {
        CreateEmployeeRequest request = validRequest();
        request.setContractTerminationDate(request.getContractHireDate().minusSeconds(60));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidEmployeeException.class);
    }

    @Test
    void createRejectsInvalidEmailAndAge() {
        CreateEmployeeRequest request = validRequest();
        request.setEmail("not-an-email");
        request.setAge(5);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(InvalidEmployeeException.class, e -> assertThat(e.getErrors())
                        .hasSize(2));
    }

    @Test
    void getByUuidThrowsWhenUnknown() {
        assertThatThrownBy(() -> service.getByUuid(UUID.randomUUID())).isInstanceOf(EmployeeNotFoundException.class);
    }

    private CreateEmployeeRequest validRequest() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName("Katherine");
        request.setLastName("Johnson");
        request.setSalary(95000);
        request.setAge(38);
        request.setJobTitle("Mathematician");
        request.setEmail("katherine.johnson@example.com");
        request.setContractHireDate(Instant.parse("2024-05-01T00:00:00Z"));
        return request;
    }
}
