package com.reliaquest.api.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import com.reliaquest.api.service.EmployeeServiceImpl;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@SpringBootTest()
public class EmployeeServiceTests {

    @Mock
    private RestTemplate restTemplate; // Mock RestTemplate

    @InjectMocks
    private EmployeeServiceImpl employeeService; // Use the concrete class for unit tests

    @BeforeEach
    void setUp() {
        // Set the values directly
        employeeService.setRetryMaxAttemps(3);
        employeeService.setApiUrl("http://localhost:8080/api/v1/employees");
        employeeService.setRetryInitialDelayMS(1000);
    }

    @Test
    void getAllEmployees_success() {
        List<Employee> mockEmployees = List.of(new Employee(), new Employee());
        ApiResponse<List<Employee>> mockApiResponse = new ApiResponse<>("success", mockEmployees);
        ResponseEntity<ApiResponse<List<Employee>>> mockResponseEntity =
                new ResponseEntity<>(mockApiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()))
                .thenReturn(mockResponseEntity);

        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size()); // Check the size of the returned list

        // Verify that restTemplate.exchange was called exactly once
        verify(restTemplate, times(1))
                .exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers
                                .<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any() // Type-safe matcher
                        );
    }

    @Test
    void getAllEmployees_apiError_retriesAndFails() {
        // Mock the RestTemplate to throw an exception for each retry attempt
        when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers
                                .<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any() // Type-safe matcher
                        ))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)); // Simulate API error

        // Call the service method
        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Expecting 500 after retries

        verify(restTemplate, times(3))
                .exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any());
    }

    @Test
    void getAllEmployees_tooManyRequests_retriesAndFails() {
        when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()))
                .thenThrow(HttpClientErrorException.TooManyRequests.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, null, null));

        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(restTemplate, times(3))
                .exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any());
    }

    @Test
    void getAllEmployees_nullResponse_returnsInternalServerError() {
        when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()))
                .thenReturn(null); // Simulate a null response from RestTemplate

        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Expect 500 for null response
        verify(restTemplate, times(1))
                .exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers
                                .<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()); // Called only once
    }

    @Test
    void getAllEmployees_emptyEmployeeList_returnsEmptyList() {
        ApiResponse<List<Employee>> mockApiResponse =
                new ApiResponse<>("success", Collections.emptyList()); // Empty list
        ResponseEntity<ApiResponse<List<Employee>>> mockResponseEntity =
                new ResponseEntity<>(mockApiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()))
                .thenReturn(mockResponseEntity);

        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty()); // Asserting empty list
        verify(restTemplate, times(1))
                .exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any());
    }

    @Test
    void getAllEmployees_unexpectedException_returnsInternalServerError() {
        when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()))
                .thenThrow(new RuntimeException("Something unexpected happened!")); // Simulate an unexpected exception

        ResponseEntity<List<Employee>> response = employeeService.getAllEmployees();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Expect 500
        verify(restTemplate, times(1))
                .exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers
                                .<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()); // Called only once
    }

    @Test
    void getEmployeeById_success() {
        String testId = "123";
        Employee mockEmployee = new Employee(); // Create a mock employee
        ApiResponse<Employee> mockApiResponse = new ApiResponse<>("success", mockEmployee);
        ResponseEntity<ApiResponse<Employee>> mockResponseEntity = new ResponseEntity<>(mockApiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any()))
                .thenReturn(mockResponseEntity);

        ResponseEntity<Employee> response = employeeService.getEmployeeById(testId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Add assertions to verify the returned employee's properties
        verify(restTemplate, times(1))
                .exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any());
    }

    @Test
    void getEmployeeById_notFound() {
        String testId = "404"; // Simulate an ID that's not found

        when(restTemplate.exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND)); // Mock 404 response

        ResponseEntity<Employee> response = employeeService.getEmployeeById(testId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode()); // Expect 404
        verify(restTemplate, times(1))
                .exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any()); // Called only once
    }

    @Test
    void getEmployeeById_serverError_retriesAndFails() {
        String testId = "500";

        when(restTemplate.exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any()))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseEntity<Employee> response = employeeService.getEmployeeById(testId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Expect 500 after retries
        verify(restTemplate, times(3))
                .exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any());
    }

    @Test
    void getEmployeeById_tooManyRequests_retriesAndFails() {
        String testId = "429";

        when(restTemplate.exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any()))
                .thenThrow(HttpClientErrorException.TooManyRequests.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, null, null));

        ResponseEntity<Employee> response = employeeService.getEmployeeById(testId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(restTemplate, times(3))
                .exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any());
    }

    @Test
    void getEmployeeById_nullResponse_returnsInternalServerError() {
        String testId = "null";
        when(restTemplate.exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any()))
                .thenReturn(null);

        ResponseEntity<Employee> response = employeeService.getEmployeeById(testId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(restTemplate, times(1))
                .exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any());
    }

    @Test
    void getEmployeeById_unexpectedException_returnsInternalServerError() {
        String testId = "exception";

        when(restTemplate.exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any()))
                .thenThrow(new RuntimeException("Unexpected Exception"));

        ResponseEntity<Employee> response = employeeService.getEmployeeById(testId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(restTemplate, times(1))
                .exchange(
                        contains(testId),
                        any(HttpMethod.class),
                        any(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any());
    }

    @Test
    void createEmployee_success() {
        String randomName = "Test Employee " + UUID.randomUUID().toString().substring(0, 8);

        EmployeeInput mockInput = new EmployeeInput();
        mockInput.setName(randomName);
        mockInput.setSalary(65000);
        mockInput.setAge(30);
        mockInput.setTitle("Software Engineer");
        mockInput.setEmail("newemp@company.com");

        Employee mockEmployee = new Employee(); // Create a mock employee object
        mockEmployee.setId("new-employee-id"); // Set the ID
        mockEmployee.setEmployee_name(randomName);

        ApiResponse<Employee> mockApiResponse = new ApiResponse<>("success", mockEmployee);
        ResponseEntity<ApiResponse<Employee>> mockResponseEntity =
                new ResponseEntity<>(mockApiResponse, HttpStatus.CREATED); // Use CREATED (201)

        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any()))
                .thenReturn(mockResponseEntity);

        ResponseEntity<Employee> response = employeeService.createEmployee(mockInput);

        assertEquals(HttpStatus.CREATED, response.getStatusCode()); // Check for 201
        assertNotNull(response.getBody());
        assertEquals("new-employee-id", response.getBody().getId()); // Verify returned ID
        assertEquals(randomName, response.getBody().getEmployee_name()); // Verify other properties

        verify(restTemplate, times(1))
                .exchange(
                        anyString(),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any());
    }

    @Test
    void createEmployee_serverError_retriesAndFails() {
        String randomName = "Test Employee " + UUID.randomUUID().toString().substring(0, 8);

        EmployeeInput mockInput = new EmployeeInput();
        mockInput.setName(randomName);
        mockInput.setSalary(65000);
        mockInput.setAge(30);
        mockInput.setTitle("Software Engineer");
        mockInput.setEmail("newemp@company.com");

        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any()))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseEntity<Employee> response = employeeService.createEmployee(mockInput);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(restTemplate, times(3))
                .exchange(
                        anyString(),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Employee>>>any());
    }

    @Test
    void deleteEmployeeById_success() {
        String testId = "12318";
        String testName = "Test Success";

        // Mock getEmployeeById to return a successful response
        Employee mockEmployee = new Employee();
        mockEmployee.setId(testId);
        mockEmployee.setEmployee_name(testName);

        ApiResponse<String> mockApiResponse =
                new ApiResponse<>("success", "true"); // Assuming API returns "success" and "true"
        ResponseEntity<ApiResponse<String>> mockResponseEntity = new ResponseEntity<>(mockApiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.DELETE),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<String>>>any()))
                .thenReturn(mockResponseEntity);

        List<Employee> mockEmployees = List.of(
                new Employee("12318", "Test Success", 100000, 30, "title", "email"),
                new Employee("12319", "Test Employee", 200000, 40, "title", "email"));
        ApiResponse<List<Employee>> mockApiResponse1 = new ApiResponse<>("success", mockEmployees);
        ResponseEntity<ApiResponse<List<Employee>>> mockResponseEntity1 =
                new ResponseEntity<>(mockApiResponse1, HttpStatus.OK);

        when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()))
                .thenReturn(mockResponseEntity1);

        ResponseEntity<String> response = employeeService.deleteEmployeeByName(mockEmployee);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testName, response.getBody()); // Verify the returned name
        verify(restTemplate, times(1))
                .exchange(
                        anyString(),
                        eq(HttpMethod.DELETE),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<String>>>any());
    }

    @Test
    void deleteEmployeeById_serverError_retriesAndFails() {
        String testId = "50018";
        String testName = "Test RetriesAndFails";

        // Mock getEmployeeById to return a successful response (so the delete API is called)
        Employee mockEmployee = new Employee();
        mockEmployee.setId(testId);
        mockEmployee.setEmployee_name(testName);

        when(restTemplate.exchange(
                        anyString(),
                        eq(HttpMethod.DELETE),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<String>>>any()))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        List<Employee> mockEmployees = List.of(
                new Employee("50018", "Test RetriesAndFails", 100000, 30, "title", "email"),
                new Employee("50019", "Test Employee", 200000, 40, "title", "email"));
        ApiResponse<List<Employee>> mockApiResponse1 = new ApiResponse<>("success", mockEmployees);
        ResponseEntity<ApiResponse<List<Employee>>> mockResponseEntity1 =
                new ResponseEntity<>(mockApiResponse1, HttpStatus.OK);

        when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()))
                .thenReturn(mockResponseEntity1);

        ResponseEntity<String> response = employeeService.deleteEmployeeByName(mockEmployee);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(restTemplate, times(3))
                .exchange(
                        anyString(),
                        eq(HttpMethod.DELETE),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<String>>>any());
    }

    @Test
    void deleteEmployeeById_duplicateNameFails() {
        List<Employee> mockEmployees = List.of(
                new Employee("1230", "Test Employee", 100000, 30, "title", "email"),
                new Employee("1231", "Test Employee", 200000, 40, "title", "email"));
        ApiResponse<List<Employee>> mockApiResponse = new ApiResponse<>("success", mockEmployees);
        ResponseEntity<ApiResponse<List<Employee>>> mockResponseEntity =
                new ResponseEntity<>(mockApiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        isNull(),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<List<Employee>>>>any()))
                .thenReturn(mockResponseEntity);

        ResponseEntity<String> response = employeeService.deleteEmployeeByName(mockEmployees.get(0));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(restTemplate, times(0))
                .exchange(
                        anyString(),
                        eq(HttpMethod.DELETE),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<String>>>any());
    }
}
