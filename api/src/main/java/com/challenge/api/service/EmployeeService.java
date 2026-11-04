package com.challenge.api.service;

import com.challenge.api.exception.EmployeeNotFoundException;
import com.challenge.api.exception.InvalidEmployeeException;
import com.challenge.api.model.Employee;
import com.challenge.api.model.EmployeeImpl;
import com.challenge.api.model.EmployeeRequestBody;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmployeeService {

    private static final int MIN_AGE = 16;
    private static final int MAX_AGE = 100;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Map<UUID, Employee> employees = new ConcurrentHashMap<>();

    public EmployeeService() {
        seedMockEmployees();
    }

    public List<Employee> getAll() {
        List<Employee> all = new ArrayList<>(employees.values());
        all.sort(Comparator.comparing(Employee::getFullName));
        log.debug("Retrieved {} employees", all.size());
        return all;
    }

    public Employee getByUuid(UUID uuid) {
        Employee employee = employees.get(uuid);
        if (employee == null) {
            throw new EmployeeNotFoundException(uuid);
        }
        log.debug("Retrieved employee {}", uuid);
        return employee;
    }

    public Employee create(EmployeeRequestBody request) {
        List<String> errors = validate(request);
        if (!errors.isEmpty()) {
            log.warn("Rejected employee creation: {}", errors);
            throw new InvalidEmployeeException(errors);
        }
        Employee employee = store(request);
        log.info("Created employee {}", employee.getUuid());
        return employee;
    }

    public Employee update(UUID uuid, EmployeeRequestBody request) {
        Employee updated = employees.computeIfPresent(uuid, (id, current) -> applyUpdate(current, request));
        if (updated == null) {
            throw new EmployeeNotFoundException(uuid);
        }
        log.info("Updated employee {}", uuid);
        return updated;
    }

    private Employee applyUpdate(Employee current, EmployeeRequestBody update) {
        if (update == null || hasNoFields(update)) {
            throw new InvalidEmployeeException(List.of("at least one field must be provided"));
        }
        EmployeeRequestBody merged = new EmployeeRequestBody();
        merged.setFirstName(coalesce(update.getFirstName(), current.getFirstName()));
        merged.setLastName(coalesce(update.getLastName(), current.getLastName()));
        merged.setSalary(coalesce(update.getSalary(), current.getSalary()));
        merged.setAge(coalesce(update.getAge(), current.getAge()));
        merged.setJobTitle(coalesce(update.getJobTitle(), current.getJobTitle()));
        merged.setEmail(coalesce(update.getEmail(), current.getEmail()));
        merged.setContractHireDate(coalesce(update.getContractHireDate(), current.getContractHireDate()));
        merged.setContractTerminationDate(
                coalesce(update.getContractTerminationDate(), current.getContractTerminationDate()));

        List<String> errors = validate(merged);
        if (!errors.isEmpty()) {
            log.warn("Rejected update of employee {}: {}", current.getUuid(), errors);
            throw new InvalidEmployeeException(errors);
        }
        return build(current.getUuid(), merged);
    }

    private boolean hasNoFields(EmployeeRequestBody update) {
        return Stream.of(
                        update.getFirstName(),
                        update.getLastName(),
                        update.getSalary(),
                        update.getAge(),
                        update.getJobTitle(),
                        update.getEmail(),
                        update.getContractHireDate(),
                        update.getContractTerminationDate())
                .allMatch(Objects::isNull);
    }

    private <T> T coalesce(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private Employee store(EmployeeRequestBody request) {
        EmployeeImpl employee = build(UUID.randomUUID(), request);
        employees.put(employee.getUuid(), employee);
        return employee;
    }

    private EmployeeImpl build(UUID uuid, EmployeeRequestBody request) {
        EmployeeImpl employee = new EmployeeImpl();
        employee.setUuid(uuid);
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setFullName(employee.getFirstName() + " " + employee.getLastName());
        employee.setSalary(request.getSalary());
        employee.setAge(request.getAge());
        employee.setJobTitle(request.getJobTitle().trim());
        employee.setEmail(request.getEmail().trim());
        employee.setContractHireDate(request.getContractHireDate());
        employee.setContractTerminationDate(request.getContractTerminationDate());
        return employee;
    }

    private List<String> validate(EmployeeRequestBody request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("request body is required");
            return errors;
        }

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            errors.add("firstName is required");
        }

        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            errors.add("lastName is required");
        }

        if (request.getJobTitle() == null || request.getJobTitle().trim().isEmpty()) {
            errors.add("jobTitle is required");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            errors.add("email is required");
        } else if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            errors.add("email is not a valid address");
        }

        if (request.getSalary() == null) {
            errors.add("salary is required");
        } else if (request.getSalary() < 0) {
            errors.add("salary must not be negative");
        }

        if (request.getAge() == null) {
            errors.add("age is required");
        } else if (request.getAge() < MIN_AGE || request.getAge() > MAX_AGE) {
            errors.add("age must be between " + MIN_AGE + " and " + MAX_AGE);
        }

        if (request.getContractHireDate() == null) {
            errors.add("contractHireDate is required");
        } else if (request.getContractTerminationDate() != null
                && request.getContractTerminationDate().isBefore(request.getContractHireDate())) {
            errors.add("contractTerminationDate must not be before contractHireDate");
        }
        return errors;
    }

    private void seedMockEmployees() {
        seed("Ada", "Lovelace", 120000, 36, "Staff Engineer", "ada.lovelace@example.com", "2022-03-01T00:00:00Z", null);
        seed(
                "Grace",
                "Hopper",
                135000,
                45,
                "Engineering Manager",
                "grace.hopper@example.com",
                "2020-07-15T00:00:00Z",
                "2024-12-31T00:00:00Z");
        seed("Alan", "Turing", 110000, 41, "Security Analyst", "alan.turing@example.com", "2023-01-09T00:00:00Z", null);
    }

    private void seed(
            String firstName,
            String lastName,
            int salary,
            int age,
            String jobTitle,
            String email,
            String hireDate,
            String terminationDate) {
        EmployeeRequestBody request = new EmployeeRequestBody();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setSalary(salary);
        request.setAge(age);
        request.setJobTitle(jobTitle);
        request.setEmail(email);
        request.setContractHireDate(Instant.parse(hireDate));
        if (terminationDate != null) {
            request.setContractTerminationDate(Instant.parse(terminationDate));
        }
        store(request);
    }
}
