package com.challenge.api.model;

import java.time.Instant;
import lombok.Data;

@Data
public class EmployeeRequestBody {

    private String firstName;
    private String lastName;
    private Integer salary;
    private Integer age;
    private String jobTitle;
    private String email;
    private Instant contractHireDate;
    private Instant contractTerminationDate;
}
