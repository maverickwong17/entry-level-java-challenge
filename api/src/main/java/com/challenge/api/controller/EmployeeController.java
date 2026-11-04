package com.challenge.api.controller;

import com.challenge.api.model.EmployeeRequestBody;
import com.challenge.api.model.Employee;
import com.challenge.api.service.EmployeeService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Fill in the missing aspects of this Spring Web REST Controller. Don't forget to add a Service layer.
 */
@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    /**
     * @implNote Need not be concerned with an actual persistence layer. Generate mock Employee models as necessary.
     * @return One or more Employees.
     */
    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeService.getAll();
    }

    /**
     * @implNote Need not be concerned with an actual persistence layer. Generate mock Employee model as necessary.
     * @param uuid Employee UUID
     * @return Requested Employee if exists
     */
    @GetMapping("/{uuid}")
    public Employee getEmployeeByUuid(@PathVariable("uuid") UUID uuid) {
        return employeeService.getByUuid(uuid);
    }

    /**
     * @implNote Need not be concerned with an actual persistence layer.
     * @param requestBody hint!
     * @return Newly created Employee
     */
    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody EmployeeRequestBody requestBody) {
        Employee created = employeeService.create(requestBody);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{uuid}")
                .buildAndExpand(created.getUuid())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{uuid}")
    public Employee updateEmployee(
            @PathVariable("uuid") UUID uuid, @RequestBody EmployeeRequestBody requestBody) {
        return employeeService.update(uuid, requestBody);
    }
}
