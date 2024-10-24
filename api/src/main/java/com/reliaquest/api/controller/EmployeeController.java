package com.reliaquest.api.controller;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import com.reliaquest.api.service.EmployeeService;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/employees")
public class EmployeeController implements IEmployeeController<Employee, EmployeeInput> {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            logger.info(
                    "Successfully retrieved {} employees", response.getBody().size());
            return ResponseEntity.ok(response.getBody()); // Return 200 OK with the list of employees
        } else if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            logger.error("Failed to retrieve employees from service");
            return ResponseEntity.internalServerError().build();
        } else {
            logger.warn("Unexpected response from service: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).build();
        }
    }

    @GetMapping("/search/{searchString}")
    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(@PathVariable("searchString") String searchString) {
        logger.debug("Received GET request to search for employees by name: {}", searchString);

        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.warn("Failed to retrieve employees for search. Status code: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return ResponseEntity.internalServerError().build();
            } else { // Other error status codes
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        }

        List<Employee> allEmployees = response.getBody();
        logger.debug("Retrieved {} employees from API", allEmployees.size());

        List<Employee> filteredEmployees = allEmployees.stream()
                .filter(employee -> employee.getEmployee_name().toLowerCase().contains(searchString.toLowerCase()))
                .collect(Collectors.toList());

        logger.info("Returning {} employees matching the search criteria", filteredEmployees.size());
        return ResponseEntity.ok(filteredEmployees);
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<Employee> getEmployeeById(@PathVariable("id") String id) {
        logger.info("Received GET request for employee with ID: {}", id);
        ResponseEntity<Employee> response = employeeService.getEmployeeById(id);
        logger.info("Returning response with status code: {}", response.getStatusCode());
        return response;
    }

    @GetMapping("/highestSalary")
    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        logger.info("Received GET request for highest employee salary");

        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.warn(
                    "Failed to retrieve employees for highest salary calculation. Status code: {}",
                    response.getStatusCode());
            if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return ResponseEntity.internalServerError().build(); // 500 error
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        }

        List<Employee> employees = response.getBody();

        if (employees.isEmpty()) {
            logger.info("No employees found, returning 0");
            return ResponseEntity.ok(0); // 200 OK with 0
        }

        int highestSalary =
                employees.stream().mapToInt(Employee::getEmployee_salary).max().orElse(0);

        logger.info("Returning highest salary: {}", highestSalary);
        return ResponseEntity.ok(highestSalary); // 200 OK with highest salary
    }

    @GetMapping("/topTenHighestEarningEmployeeNames")
    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        logger.info("Received GET request for top 10 highest earning employee names");

        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.warn(
                    "Failed to retrieve employees for top 10 salary calculation. Status code: {}",
                    response.getStatusCode());
            if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return ResponseEntity.internalServerError().build();
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        }

        List<Employee> employees = response.getBody();
        if (employees.isEmpty()) {
            logger.info("No employees found, returning empty list");
            return ResponseEntity.ok(Collections.emptyList()); // 200 OK with empty list
        }
        List<String> topTenNames = employees.stream()
                .sorted(Comparator.comparingInt(Employee::getEmployee_salary).reversed()) // Sort by salary descending
                .limit(10) // Take the top 10
                .map(Employee::getEmployee_name) // Extract the names
                .collect(Collectors.toList());

        logger.debug("Top ten highest earning employee names: {}", topTenNames);
        return ResponseEntity.ok(topTenNames);
    }

    @PostMapping
    @Override
    public ResponseEntity<Employee> createEmployee(@RequestBody EmployeeInput employeeInput) {
        logger.info("Received POST request to create employee: {}", employeeInput.getName());
        ResponseEntity<Employee> response = employeeService.createEmployee(employeeInput);
        logger.info("Returning response with status code: {}", response.getStatusCode());
        return response;
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<String> deleteEmployeeById(@PathVariable("id") String id) {
        logger.info("Received DELETE request for employee with ID: {}", id);
        ResponseEntity<String> response = employeeService.deleteEmployeeById(id);
        logger.info("Returning response with status code: {}", response.getStatusCode());
        return response;
    }
}
