package com.reliaquest.api.integration.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import com.reliaquest.api.service.EmployeeService;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest()
public class EmployeeServiceIT {

    @Autowired
    private EmployeeService employeeService;

    private boolean isRedisRunning() {
        try (Socket socket = new Socket("localhost", 6379)) {
            return true; // Redis is up
        } catch (IOException e) {
            return false; // Port is closed or unreachable
        }
    }

    private boolean isServerRunning() {
        String baseUrl = "http://localhost:8112/api/v1/employee";
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NOT_FOUND;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void createAndDeleteEmployee_success() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");
        Assumptions.assumeTrue(isServerRunning(), "Test server is not running, skipping test.");

        String randomName = "Test Employee " + UUID.randomUUID().toString().substring(0, 8);

        EmployeeInput newEmployeeInput = new EmployeeInput();
        newEmployeeInput.setName(randomName);
        newEmployeeInput.setSalary(65000);
        newEmployeeInput.setAge(30);
        newEmployeeInput.setTitle("Software Engineer");
        newEmployeeInput.setEmail("newemp@company.com");

        ResponseEntity<Employee> createResponse = employeeService.createEmployee(newEmployeeInput);

        assertAll(
                () -> assertEquals(HttpStatus.OK, createResponse.getStatusCode()),
                () -> assertNotNull(createResponse.getBody()),
                () -> assertEquals(randomName, createResponse.getBody().getEmployee_name()));

        String createdEmployeeId = createResponse.getBody().getId();

        ResponseEntity<String> deleteResponse = employeeService.deleteEmployeeById(createdEmployeeId);
        assertAll(
                () -> assertEquals(HttpStatus.OK, deleteResponse.getStatusCode()),
                () -> assertEquals(randomName, deleteResponse.getBody()) // Assert on the returned name
                );
    }
}
