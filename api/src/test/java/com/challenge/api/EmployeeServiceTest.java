package com.challenge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.challenge.api.exception.EmployeeNotFoundException;
import com.challenge.api.exception.InvalidEmployeeException;
import com.challenge.api.model.Employee;
import com.challenge.api.model.EmployeeRequestBody;
import com.challenge.api.service.EmployeeService;
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
        assertThatThrownBy(() -> service.create(new EmployeeRequestBody()))
                .isInstanceOfSatisfying(InvalidEmployeeException.class, e -> assertThat(e.getErrors())
                        .hasSize(7));
    }

    @Test
    void createRejectsNullRequest() {
        assertThatThrownBy(() -> service.create(null)).isInstanceOf(InvalidEmployeeException.class);
    }

    @Test
    void createRejectsTerminationBeforeHire() {
        EmployeeRequestBody request = validRequest();
        request.setContractTerminationDate(request.getContractHireDate().minusSeconds(60));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidEmployeeException.class);
    }

    @Test
    void createRejectsInvalidEmailAndAge() {
        EmployeeRequestBody request = validRequest();
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

    @Test
    void updateChangesOnlyProvidedFields() {
        Employee created = service.create(validRequest());
        EmployeeRequestBody update = new EmployeeRequestBody();
        update.setJobTitle("Senior Mathematician");

        Employee updated = service.update(created.getUuid(), update);

        assertThat(updated.getUuid()).isEqualTo(created.getUuid());
        assertThat(updated.getJobTitle()).isEqualTo("Senior Mathematician");
        assertThat(updated.getEmail()).isEqualTo("katherine.johnson@example.com");
        assertThat(service.getByUuid(created.getUuid()).getJobTitle()).isEqualTo("Senior Mathematician");
    }

    @Test
    void updateSetsTerminationDate() {
        Employee created = service.create(validRequest());
        EmployeeRequestBody update = new EmployeeRequestBody();
        update.setContractTerminationDate(Instant.parse("2025-06-30T00:00:00Z"));

        Employee updated = service.update(created.getUuid(), update);

        assertThat(updated.getContractTerminationDate()).isEqualTo(Instant.parse("2025-06-30T00:00:00Z"));
    }

    @Test
    void updateRederivesFullNameWhenNameChanges() {
        Employee created = service.create(validRequest());
        EmployeeRequestBody update = new EmployeeRequestBody();
        update.setLastName("Goble");

        assertThat(service.update(created.getUuid(), update).getFullName()).isEqualTo("Katherine Goble");
    }

    @Test
    void updateRejectsEmptyRequest() {
        Employee created = service.create(validRequest());

        assertThatThrownBy(() -> service.update(created.getUuid(), new EmployeeRequestBody()))
                .isInstanceOf(InvalidEmployeeException.class);
    }

    @Test
    void updateRejectsTerminationBeforeStoredHireDate() {
        Employee created = service.create(validRequest());
        EmployeeRequestBody update = new EmployeeRequestBody();
        update.setContractTerminationDate(Instant.parse("2024-01-01T00:00:00Z"));

        assertThatThrownBy(() -> service.update(created.getUuid(), update))
                .isInstanceOf(InvalidEmployeeException.class);
    }

    @Test
    void updateRejectsInvalidValuesAndLeavesEmployeeUnchanged() {
        Employee created = service.create(validRequest());
        EmployeeRequestBody update = new EmployeeRequestBody();
        update.setAge(5);

        assertThatThrownBy(() -> service.update(created.getUuid(), update))
                .isInstanceOf(InvalidEmployeeException.class);
        assertThat(service.getByUuid(created.getUuid()).getAge()).isEqualTo(38);
    }

    @Test
    void updateThrowsWhenUnknown() {
        EmployeeRequestBody update = new EmployeeRequestBody();
        update.setJobTitle("Anything");

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), update))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    private EmployeeRequestBody validRequest() {
        EmployeeRequestBody request = new EmployeeRequestBody();
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
