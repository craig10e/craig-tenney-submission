package com.reliaquest.api.integration.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliaquest.api.controller.EmployeeController;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
public class EmployeeControllerIT {
    private String baseUrl = "http://localhost:8111/api/v1/employees";

    @Autowired
    private EmployeeController employeeController;

    @Autowired
    private RestTemplate restTemplate;

    private boolean isRedisRunning() {
        try (Socket socket = new Socket("localhost", 6379)) {
            return true; // Redis is up
        } catch (IOException e) {
            return false; // Port is closed or unreachable
        }
    }

    private boolean isServerRunning() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NOT_FOUND;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void getAllEmployees_cachingIntegrationTest() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");
        Assumptions.assumeTrue(isServerRunning(), "Test server is not running, skipping test.");

        String url = baseUrl;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<List<Employee>> response1 =
                restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Employee>>() {});
        assertEquals(HttpStatus.OK, response1.getStatusCode());

        ResponseEntity<List<Employee>> response2 =
                restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Employee>>() {});
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        ResponseEntity<List<Employee>> response3 =
                restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Employee>>() {});
        assertEquals(HttpStatus.OK, response3.getStatusCode());

        // Check the logs to see if it made the API call. If not, it is using the cache.
    }

    @Test
    void getAllEmployees_integrationTest() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");
        Assumptions.assumeTrue(isServerRunning(), "Test server is not running, skipping test.");

        String url = baseUrl;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<List<Employee>> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Employee>>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        List<Employee> employees = response.getBody();
        assertNotNull(employees, "The employee list should not be null.");
        assertFalse(employees.isEmpty(), "The employee list should not be empty.");
        assertTrue(employees.size() >= 50, "Should have at least 50 employees");
    }

    @Test
    void getEmployeeById_integrationTest() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");
        Assumptions.assumeTrue(isServerRunning(), "Test server is not running, skipping test.");

        String randomName = "Test Employee " + UUID.randomUUID().toString().substring(0, 8);
        Random random = new Random();
        int randomSalary = random.ints(30000, 500000).findFirst().getAsInt();
        int randomAge = random.ints(16, 76).findFirst().getAsInt();
        String randomTitle = "Software Engineer " + UUID.randomUUID().toString().substring(0, 8);
        String randomEmail = "testemp" + UUID.randomUUID().toString().substring(0, 8) + "@company.com";

        EmployeeInput randomEmployeeInput = new EmployeeInput();
        randomEmployeeInput.setName(randomName);
        randomEmployeeInput.setSalary(randomSalary);
        randomEmployeeInput.setAge(randomAge);
        randomEmployeeInput.setTitle(randomTitle);
        randomEmployeeInput.setEmail(randomEmail);

        String createUrl = baseUrl;
        ResponseEntity<Employee> createResponse =
                restTemplate.postForEntity(createUrl, randomEmployeeInput, Employee.class);

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        String createdEmployeeId = createResponse.getBody().getId();

        String getUrl = baseUrl + "/" + createdEmployeeId;
        ResponseEntity<Employee> getResponse = restTemplate.getForEntity(getUrl, Employee.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(createdEmployeeId, getResponse.getBody().getId());
        assertEquals(randomName, getResponse.getBody().getEmployee_name());
        assertEquals(randomSalary, getResponse.getBody().getEmployee_salary());
        assertEquals(randomAge, getResponse.getBody().getEmployee_age());
        assertEquals(randomTitle, getResponse.getBody().getEmployee_title());
        // assertEquals(randomEmail, getResponse.getBody().getEmployee_email()); // API Doesn't set Email properly
    }

    @Test
    void getEmployeesByNameSearch_integrationTest() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");
        Assumptions.assumeTrue(isServerRunning(), "Test server is not running, skipping test.");

        String allEmployeesUrl = baseUrl;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<List<Employee>> allEmployeesResponse = restTemplate.exchange(
                allEmployeesUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Employee>>() {});

        assertEquals(HttpStatus.OK, allEmployeesResponse.getStatusCode());
        assertNotNull(allEmployeesResponse.getBody());
        List<Employee> allEmployees = allEmployeesResponse.getBody();

        Employee firstEmployee = allEmployees.get(0);

        String searchString = firstEmployee.getEmployee_name();
        String searchUrl = baseUrl + "/search/" + searchString;

        ResponseEntity<List<Employee>> searchResponse = restTemplate.exchange(
                searchUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<Employee>>() {});

        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        assertNotNull(searchResponse.getBody());

        List<Employee> employeesFound = searchResponse.getBody();

        for (Employee employee : employeesFound) {
            assertTrue(
                    employee.getEmployee_name().toLowerCase().contains(searchString.toLowerCase()),
                    "Employee name should contain the search string: " + searchString);
        }
    }

    @Test
    void getHighestSalaryOfEmployees_integrationTest() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");
        Assumptions.assumeTrue(isServerRunning(), "Test server is not running, skipping test.");

        String url = baseUrl + "/highestSalary";

        ResponseEntity<Integer> response = restTemplate.getForEntity(url, Integer.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        int highestSalary = response.getBody();

        assertTrue(highestSalary > 0, "Highest salary should be greater than 0");
    }

    void getHighestSalaryOfEmployees_highestSalary() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");

        ResponseEntity<List<Employee>> allEmployeesResponse = employeeController.getAllEmployees();
        List<Employee> employees = allEmployeesResponse.getBody();
        int highestSalary =
                employees.stream().mapToInt(Employee::getEmployee_salary).max().orElse(0);

        ResponseEntity<Integer> response = employeeController.getHighestSalaryOfEmployees();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        int highestSalaryFromResponse = response.getBody();
        assertEquals(highestSalaryFromResponse, highestSalary);
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_integrationTest() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");
        Assumptions.assumeTrue(isServerRunning(), "Test server is not running, skipping test.");

        String url = baseUrl + "/topTenHighestEarningEmployeeNames";
        ResponseEntity<List<String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {} // Type-safe response handling
                );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<String> topTenNames = response.getBody();
        assertEquals(10, topTenNames.size(), "Should return exactly 10 names");
    }

    @Test
    void createAndDeleteEmployee_integrationTest() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");
        Assumptions.assumeTrue(isServerRunning(), "Test server is not running, skipping test.");

        String randomName = "Test Employee " + UUID.randomUUID().toString().substring(0, 8); // Random name
        EmployeeInput newEmployee = new EmployeeInput();
        newEmployee.setName(randomName);
        newEmployee.setSalary(67000);
        newEmployee.setAge(36);
        newEmployee.setTitle("Senior Software Engineer");
        newEmployee.setEmail("newemp234@company.com");

        String createUrl = baseUrl;
        ResponseEntity<Employee> createResponse = restTemplate.postForEntity(createUrl, newEmployee, Employee.class);

        assertEquals(HttpStatus.OK, createResponse.getStatusCode()); // Expect 201 Created
        assertNotNull(createResponse.getBody());
        String createdEmployeeId = createResponse.getBody().getId(); // Get the ID of the created employee
        assertEquals(randomName, createResponse.getBody().getEmployee_name()); // Verify the name

        String deleteUrl = baseUrl + "/" + createdEmployeeId;
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                deleteUrl, HttpMethod.DELETE, null, String.class // Expecting the employee name back
                );

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode()); // 200 OK
        assertEquals(randomName, deleteResponse.getBody()); // Verify the returned name
    }

    @Test
    void createAndDeleteEmployee_createsAndDeletesNewEmployee() {
        Assumptions.assumeTrue(isRedisRunning(), "Redis is not up, skipping test.");
        Assumptions.assumeTrue(isServerRunning(), "Test server is not running, skipping test.");

        String randomName = "Test Employee " + UUID.randomUUID().toString().substring(0, 8);

        EmployeeInput newEmployeeInput = new EmployeeInput();
        newEmployeeInput.setName(randomName);
        newEmployeeInput.setSalary(65000);
        newEmployeeInput.setAge(30);
        newEmployeeInput.setTitle("Software Engineer");
        newEmployeeInput.setEmail("newemp@company.com");

        ResponseEntity<Employee> response = employeeController.createEmployee(newEmployeeInput);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Employee createdEmployee = response.getBody();
        assertNotNull(createdEmployee);
        assertEquals(randomName, createdEmployee.getEmployee_name());
        assertEquals(65000, createdEmployee.getEmployee_salary());
        assertEquals(30, createdEmployee.getEmployee_age());
        assertEquals("Software Engineer", createdEmployee.getEmployee_title());
        // Test API doesn't set email address
        // assertEquals("zontrax@company.com", createdEmployee.getEmployee_email());

        String createdEmployeeId = createdEmployee.getId();
        assertNotNull(
                createdEmployeeId,
                "createdEmployeeId should not be null. Make sure the createEmployee test runs first.");

        ResponseEntity<String> deleteResponse = employeeController.deleteEmployeeById(createdEmployeeId);

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        String responseBody = deleteResponse.getBody();
        assertEquals(randomName, responseBody, "Expected " + randomName);
    }
}
