package com.reliaquest.api.service;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface EmployeeService {
    ResponseEntity<List<Employee>> getAllEmployees();

    ResponseEntity<Employee> getEmployeeById(String id);

    ResponseEntity<Employee> createEmployee(EmployeeInput employeeInput);

    ResponseEntity<String> deleteEmployeeById(String id);
}
