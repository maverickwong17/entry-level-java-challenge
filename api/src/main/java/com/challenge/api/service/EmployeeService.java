package com.challenge.api.service;

import com.challenge.api.exception.EmployeeNotFoundException;
import com.challenge.api.exception.InvalidEmployeeException;
import com.challenge.api.model.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import com.challenge.api.model.EmployeeImpl;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
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
        return all;
    }

    public Employee getByUuid(UUID uuid) {
        Employee employee = employees.get(uuid);
        if (employee == null) {
            throw new EmployeeNotFoundException(uuid);
        }
        return employee;
    }

    public Employee create(CreateEmployeeRequest request) {
        List<String> errors = validate(request);
        if (!errors.isEmpty()) {
            log.warn("Rejected employee creation: {}", errors);
            throw new InvalidEmployeeException(errors);
        }
        Employee employee = store(request);
        log.info("Created employee {}", employee.getUuid());
        return employee;
    }

    private Employee store(CreateEmployeeRequest request) {
        EmployeeImpl employee = new EmployeeImpl();
        employee.setUuid(UUID.randomUUID());
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setFullName(employee.getFirstName() + " " + employee.getLastName());
        employee.setSalary(request.getSalary());
        employee.setAge(request.getAge());
        employee.setJobTitle(request.getJobTitle().trim());
        employee.setEmail(request.getEmail().trim());
        employee.setContractHireDate(request.getContractHireDate());
        employee.setContractTerminationDate(request.getContractTerminationDate());
        employees.put(employee.getUuid(), employee);
        return employee;
    }

    private List<String> validate(CreateEmployeeRequest request) {
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
        seed("Ada", "Lovelace", 120000, 36, "Staff Engineer", "ada.lovelace@example.com", "2022-03-01T00:00:00Z");
        seed("Grace", "Hopper", 135000, 45, "Engineering Manager", "grace.hopper@example.com", "2020-07-15T00:00:00Z");
        seed("Alan", "Turing", 110000, 41, "Security Analyst", "alan.turing@example.com", "2023-01-09T00:00:00Z");
    }

    private void seed(
            String firstName, String lastName, int salary, int age, String jobTitle, String email, String hireDate) {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setSalary(salary);
        request.setAge(age);
        request.setJobTitle(jobTitle);
        request.setEmail(email);
        request.setContractHireDate(Instant.parse(hireDate));
        store(request);
    }
}
