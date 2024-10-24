package com.reliaquest.api.unit.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.reliaquest.api.controller.EmployeeController;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import com.reliaquest.api.service.EmployeeService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@SpringBootTest()
public class EmployeeControllerTests {
    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    @Test
    void getAllEmployees_success() {
        List<Employee> mockEmployees = List.of(new Employee(), new Employee());
        ResponseEntity<List<Employee>> mockServiceResponse = new ResponseEntity<>(mockEmployees, HttpStatus.OK);
        when(employeeService.getAllEmployees()).thenReturn(mockServiceResponse);
        ResponseEntity<List<Employee>> controllerResponse = employeeController.getAllEmployees();

        assertNotNull(controllerResponse);
        assertEquals(HttpStatus.OK, controllerResponse.getStatusCode());
        assertNotNull(controllerResponse.getBody());
        assertEquals(2, controllerResponse.getBody().size());
    }

    @Test
    void getAllEmployees_internalServerError() {
        ResponseEntity<List<Employee>> mockServiceResponse = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        when(employeeService.getAllEmployees()).thenReturn(mockServiceResponse);

        ResponseEntity<List<Employee>> controllerResponse = employeeController.getAllEmployees();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controllerResponse.getStatusCode());
        assertNull(controllerResponse.getBody());
    }

    @Test
    void getAllEmployees_otherError() {
        ResponseEntity<List<Employee>> mockServiceResponse =
                new ResponseEntity<>(HttpStatus.BAD_REQUEST); // Example: 400 Bad Request
        when(employeeService.getAllEmployees()).thenReturn(mockServiceResponse);

        ResponseEntity<List<Employee>> controllerResponse = employeeController.getAllEmployees();

        assertEquals(HttpStatus.BAD_REQUEST, controllerResponse.getStatusCode()); // Should return the same status code
        assertNull(controllerResponse.getBody());
    }

    @Test
    void getEmployeesByNameSearch_success() {
        String searchString = "test";
        List<Employee> mockEmployees = List.of(
                new Employee("123", "Test Employee 1", 1000, 30, "", "1"),
                new Employee("124", "Other Employee", 2000, 40, "", "2"),
                new Employee("125", "Another Test Employee", 3000, 25, "", "3"));
        ResponseEntity<List<Employee>> mockServiceResponse = new ResponseEntity<>(mockEmployees, HttpStatus.OK);

        when(employeeService.getAllEmployees()).thenReturn(mockServiceResponse);

        ResponseEntity<List<Employee>> response = employeeController.getEmployeesByNameSearch(searchString);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size()); // Two employees match "test"


        List<String> filteredNames =
                response.getBody().stream().map(Employee::getEmployee_name).collect(Collectors.toList());
        assertTrue(filteredNames.contains("Test Employee 1"));
        assertTrue(filteredNames.contains("Another Test Employee"));
    }

    @Test
    void getEmployeesByNameSearch_noMatch() {
        String searchString = "xyz"; // No employee names contain "xyz"
        List<Employee> mockEmployees = List.of(
                new Employee("123", "Test Employee 1", 1000, 30, "title1", "1@email"),
                new Employee("124", "Other Employee", 2000, 40, "title2", "2@email"),
                new Employee("125", "Another Test Employee", 3000, 25, "title3", "3@email"));
        ResponseEntity<List<Employee>> mockServiceResponse = new ResponseEntity<>(mockEmployees, HttpStatus.OK);

        when(employeeService.getAllEmployees()).thenReturn(mockServiceResponse);

        ResponseEntity<List<Employee>> response = employeeController.getEmployeesByNameSearch(searchString);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty()); // The filtered list should be empty
    }

    @Test
    void getEmployeesByNameSearch_serverError() {
        String searchString = "test";

        when(employeeService.getAllEmployees()).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseEntity<List<Employee>> response = employeeController.getEmployeesByNameSearch(searchString);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getEmployeeById_success() {
        String testId = "123";
        Employee mockEmployee = new Employee();
        mockEmployee.setId(testId);
        mockEmployee.setEmployee_name("Test Employee 1245");

        ResponseEntity<Employee> mockServiceResponse = new ResponseEntity<>(mockEmployee, HttpStatus.OK);
        when(employeeService.getEmployeeById(testId)).thenReturn(mockServiceResponse);

        ResponseEntity<Employee> response = employeeController.getEmployeeById(testId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testId, response.getBody().getId()); // Verify the returned employee's ID
        assertEquals("Test Employee 1245", response.getBody().getEmployee_name());
    }

    @Test
    void getEmployeeById_notFound() {
        String testId = "404";

        when(employeeService.getEmployeeById(testId)).thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        ResponseEntity<Employee> response = employeeController.getEmployeeById(testId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getEmployeeById_serverError() {
        String testId = "500";

        when(employeeService.getEmployeeById(testId))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseEntity<Employee> response = employeeController.getEmployeeById(testId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getHighestSalaryOfEmployees_success() {
        List<Employee> mockEmployees = List.of(
                new Employee("111", "Employee 1", 50000, 30, "title 111", "111@email.com"),
                new Employee("222", "Employee 2", 60000, 35, "title 222", "222@email.com"),
                new Employee("333", "Employee 3", 75000, 40, "title 333", "333@email.com"));
        when(employeeService.getAllEmployees()).thenReturn(new ResponseEntity<>(mockEmployees, HttpStatus.OK));

        ResponseEntity<Integer> response = employeeController.getHighestSalaryOfEmployees();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(75000, response.getBody()); // Expected highest salary
    }

    @Test
    void getHighestSalaryOfEmployees_emptyList() {
        when(employeeService.getAllEmployees())
                .thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));

        ResponseEntity<Integer> response = employeeController.getHighestSalaryOfEmployees();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody()); // Expected 0 for empty list
    }

    @Test
    void getHighestSalaryOfEmployees_serverError() {
        when(employeeService.getAllEmployees()).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseEntity<Integer> response = employeeController.getHighestSalaryOfEmployees();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_success() {
        List<Employee> mockEmployees = List.of(
                new Employee("1", "Emp1", 10000, 30, "title", "email"),
                new Employee("2", "Emp2", 20000, 35, "title", "email"),
                new Employee("3", "Emp3", 30000, 40, "title", "email"),
                new Employee("4", "Emp4", 15000, 28, "title", "email"),
                new Employee("5", "Emp5", 25000, 32, "title", "email"),
                new Employee("6", "Emp6", 35000, 45, "title", "email"),
                new Employee("7,", "Emp7", 12000, 25, "title", "email"),
                new Employee("8", "Emp8", 22000, 38, "title", "email"),
                new Employee("9", "Emp9", 40000, 42, "title", "email"),
                new Employee("10", "Emp10", 18000, 31, "title", "email"),
                new Employee("11", "Emp11", 28000, 36, "title", "email") // More than 10 employees
                );

        when(employeeService.getAllEmployees()).thenReturn(new ResponseEntity<>(mockEmployees, HttpStatus.OK));

        ResponseEntity<List<String>> response = employeeController.getTopTenHighestEarningEmployeeNames();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().size()); // Should return top 10

        // Verify the order of the top 10 names (important!)
        List<String> expectedTopTenNames =
                List.of("Emp9", "Emp6", "Emp3", "Emp11", "Emp5", "Emp8", "Emp2", "Emp10", "Emp4", "Emp7");
        assertEquals(expectedTopTenNames, response.getBody());
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_emptyList() {
        when(employeeService.getAllEmployees())
                .thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));

        ResponseEntity<List<String>> response = employeeController.getTopTenHighestEarningEmployeeNames();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_serverError() {
        when(employeeService.getAllEmployees()).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseEntity<List<String>> response = employeeController.getTopTenHighestEarningEmployeeNames();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody()); // Or assertNull, depending on your error handling
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

        Employee mockCreatedEmployee = new Employee();
        mockCreatedEmployee.setId("new-employee-id");
        mockCreatedEmployee.setEmployee_name(randomName);
        mockCreatedEmployee.setEmployee_salary(65000);
        mockCreatedEmployee.setEmployee_age(30);
        mockCreatedEmployee.setEmployee_title("Software Engineer");
        mockCreatedEmployee.setEmployee_email("newemp@company.com");

        ResponseEntity<Employee> mockServiceResponse = new ResponseEntity<>(mockCreatedEmployee, HttpStatus.CREATED);
        when(employeeService.createEmployee(mockInput)).thenReturn(mockServiceResponse);

        ResponseEntity<Employee> response = employeeController.createEmployee(mockInput);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("new-employee-id", response.getBody().getId()); // Verify the ID
        assertEquals(randomName, response.getBody().getEmployee_name());
        assertEquals(65000, response.getBody().getEmployee_salary());
        assertEquals("Software Engineer", response.getBody().getEmployee_title());
        assertEquals("newemp@company.com", response.getBody().getEmployee_email());
    }

    @Test
    void createEmployee_serverError() {
        EmployeeInput mockInput = new EmployeeInput();
        mockInput.setName("Test Employee 9521");

        when(employeeService.createEmployee(mockInput))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseEntity<Employee> response = employeeController.createEmployee(mockInput);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void deleteEmployeeById_success() {
        String testId = "123";
        String deletedEmployeeName = "Deleted Employee";
        ResponseEntity<String> mockServiceResponse = new ResponseEntity<>(deletedEmployeeName, HttpStatus.OK);
        when(employeeService.deleteEmployeeById(testId)).thenReturn(mockServiceResponse);

        ResponseEntity<String> response = employeeController.deleteEmployeeById(testId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(deletedEmployeeName, response.getBody()); // Verify the returned name
    }

    @Test
    void deleteEmployeeById_notFound() {
        String testId = "404";
        when(employeeService.deleteEmployeeById(testId)).thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        ResponseEntity<String> response = employeeController.deleteEmployeeById(testId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
