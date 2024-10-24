package com.reliaquest.api.service;

import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeInput;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${reliaquest.api.url}")
    private String apiUrl;

    @Value("${reliaquest.api.retry-max-attempts}")
    private int retryMaxAttempts;

    @Value("${reliaquest.api.retry-initial-delay}")
    private int retryInitialDelay;

    @Value("${reliaquest.api.redis-host}")
    private String redisHost;

    @Value("${reliaquest.api.redis-port}")
    private int redisPort;

    private final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private RawRedisCache rawRedisCache;

    @Autowired
    public void setRawRedisCache() {
        this.rawRedisCache = new RawRedisCache(redisHost, redisPort);
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setRetryMaxAttemps(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public void setRetryInitialDelayMS(int retryInitialDelay) {
        this.retryInitialDelay = retryInitialDelay;
    }

    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        ResponseEntity<List<Employee>> employees = geEmployeesFromCache();
        if (employees != null
                && (!employees.getBody().isEmpty())
                && employees.getBody().size() != 0) {
            return ResponseEntity.ok(employees.getBody());
        }

        int delay = retryInitialDelay;
        logger.debug("Making API call to {} to get all employees.", apiUrl);

        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            logger.debug("Attempt {} of Max Attempts {}.", attempt, retryMaxAttempts);
            try {
                ResponseEntity<ApiResponse<List<Employee>>> response = restTemplate.exchange(
                        apiUrl, HttpMethod.GET, null, new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {});
                logger.debug("API response status code: {}", response.getStatusCode());

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    String errorMessage = "API request failed with status code: "
                            + response.getStatusCode().value()
                            + (response.getBody() != null
                                    ? ", message: " + response.getBody().getStatus()
                                    : "");
                    logger.error(errorMessage);

                    return ResponseEntity.status(response.getStatusCode()).body(null);
                }

                addEmployeesToCache(ResponseEntity.ok(response.getBody().getData()));

                logger.debug(
                        "Successfully retrieved {} employees from API",
                        response.getBody().getData().size());
                return ResponseEntity.ok(response.getBody().getData());
            } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
                if (attempt < retryMaxAttempts) {
                    logger.warn("API call failed (attempt {}/{}), retrying in {}ms", attempt, retryMaxAttempts, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt(); // Restore interrupted status
                        logger.error("Thread interrupted during backoff", ex);
                        return ResponseEntity.internalServerError().build();
                    }
                    delay *= 2; // Exponential backoff
                } else {
                    logger.error("API call failed after {} attempts", retryMaxAttempts, e);
                    return ResponseEntity.internalServerError().build();
                }

            } catch (Exception e) {
                logger.error("Error getting all employees from API", e);
                return ResponseEntity.internalServerError().build();
            }
        }

        throw new IllegalStateException(
                "This should never happen. Max attempts reached without a result or exception.");
    }

    @Override
    public ResponseEntity<Employee> getEmployeeById(String id) {
        Employee employee = geEmployeeFromCache(id);
        if (employee != null) {
            return ResponseEntity.ok(employee);
        }

        int delay = retryInitialDelay;

        logger.debug("Getting employee by ID: {}", id);

        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                String employeeUrl =
                        UriComponentsBuilder.fromHttpUrl(apiUrl).pathSegment(id).toUriString();

                logger.debug("Making API call to: {}", employeeUrl);

                ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                        employeeUrl, HttpMethod.GET, null, new ParameterizedTypeReference<ApiResponse<Employee>>() {});

                logger.debug(
                        "API response status code: {}", response.getStatusCode().value());

                if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.warn("Employee not found for ID: {}", id);

                    return ResponseEntity.notFound().build(); // Return 404 Not Found
                } else if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    String errorMessage = "API request failed for ID: " + id + ", status code: "
                            + response.getStatusCode().value()
                            + (response.getBody() != null
                                    ? ", message: " + response.getBody().getStatus()
                                    : "");
                    logger.error(errorMessage);

                    return ResponseEntity.status(response.getStatusCode()).build();
                }

                ResponseEntity<Employee> employeeResponse =
                        new ResponseEntity<>(response.getBody().getData(), response.getStatusCode());
                addEmployeeToCache(employeeResponse);

                logger.debug(
                        "Successfully retrieved employee id: {}",
                        response.getBody().getData().getId());
                return ResponseEntity.ok(response.getBody().getData());
            } catch (HttpServerErrorException
                    | HttpClientErrorException.TooManyRequests e) { // Catch only the retryable exception
                if (attempt < retryMaxAttempts) {
                    logger.warn("API call failed (attempt {}/{}), retrying in {}ms", attempt, retryMaxAttempts, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt(); // Restore interrupted status
                        logger.error("Thread interrupted during backoff", ex);
                        return ResponseEntity.internalServerError().build();
                    }
                    delay *= 2; // Exponential backoff
                } else {
                    logger.error("API call failed after {} attempts", retryMaxAttempts, e);
                    return ResponseEntity.internalServerError().build();
                }
            } catch (Exception e) {
                logger.error("Error getting employee by ID: " + id, e);
                return ResponseEntity.internalServerError().build();
            }
        }

        throw new IllegalStateException(
                "This should never happen. Max attempts reached without a result or exception.");
    }

    @Override
    public ResponseEntity<Employee> createEmployee(EmployeeInput employeeInput) {
        int delay = retryInitialDelay;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<EmployeeInput> request = new HttpEntity<>(employeeInput, headers);

                logger.debug("Calling external API to create employee at URL: {}", apiUrl);
                ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, request, new ParameterizedTypeReference<ApiResponse<Employee>>() {});

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    logger.warn("Received non-2xx status code from API: {}", response.getStatusCode());
                    return new ResponseEntity<>(response.getStatusCode());
                }

                ResponseEntity<Employee> employee =
                        new ResponseEntity<>(response.getBody().getData(), response.getStatusCode());

                removeEmployeesFromCache();
                addEmployeeToCache(employee);

                logger.debug(
                        "Successfully created employee id: {}",
                        response.getBody().getData().getId());
                return (employee);
            } catch (HttpServerErrorException
                    | HttpClientErrorException.TooManyRequests e) { // Catch only the retryable exception
                if (attempt < retryMaxAttempts) {
                    logger.warn("API call failed (attempt {}/{}), retrying in {}ms", attempt, retryMaxAttempts, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        logger.error("Thread interrupted during backoff", ex);
                        return ResponseEntity.internalServerError().build();
                    }
                    delay *= 2; // Exponential backoff
                } else {
                    logger.error("API call failed after {} attempts", retryMaxAttempts, e);
                    return ResponseEntity.internalServerError().build();
                }
            } catch (Exception e) {
                logger.error("Exception during createEmployee: {}", e.getMessage(), e);
                return ResponseEntity.internalServerError().build();
            }
        }

        throw new IllegalStateException(
                "This should never happen. Max attempts reached without a result or exception.");
    }

    public static boolean hasDuplicateName(List<Employee> allEmployees, String employeeName) {
        int count = 0;
        for (Employee employee : allEmployees) {
            if (employee.getEmployee_name().equals(employeeName)) {
                count++;
                if (count > 1) {
                    return true; // Found more than one occurrence
                }
            }
        }
        return false; // employeeName not found or only appears once
    }

    // This function will delete the first employee it finds with this employee's name
    // This function will fail if the employee's name isn't unique in the list of employees
    public ResponseEntity<String> deleteEmployeeByName(Employee employee) {
        String employeeId = employee.getId();
        String employeeName = employee.getEmployee_name();

        List<Employee> allEmployees = getAllEmployees().getBody();
        if (hasDuplicateName(allEmployees, employeeName)) {
            logger.error("Unable to delete. Duplicate name found for {}", employeeName);
            return new ResponseEntity<>("Employee not found", HttpStatus.CONFLICT);
        }

        int delay = retryInitialDelay;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                logger.debug("Deleting employee with name: {}", employeeName);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                String requestBodyJson = String.format("{\"name\": \"%s\"}", employeeName);
                HttpEntity<String> requestEntity = new HttpEntity<>(requestBodyJson, headers);

                logger.debug("Calling API to delete employee at URL: {}", apiUrl);
                ResponseEntity<ApiResponse<String>> deleteResponse = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.DELETE,
                        requestEntity,
                        new ParameterizedTypeReference<ApiResponse<String>>() {});

                if (deleteResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.warn("Employee not found for deletion (API response): {}", employeeName);
                    return new ResponseEntity<>("Employee not found", HttpStatus.NOT_FOUND);
                } else if (!deleteResponse.getStatusCode().is2xxSuccessful()) {
                    logger.warn(
                            "Failed to delete employee. API Status: {}, Response Body: {}",
                            deleteResponse.getStatusCode(),
                            deleteResponse.getBody());
                    return new ResponseEntity<>(deleteResponse.getBody().getStatus(), deleteResponse.getStatusCode());
                }
                String deleteStatus = deleteResponse.getBody().getData(); // Get the status of the deletion
                if (deleteStatus.equals("true")) {
                    logger.info("Successfully deleted employee: {}", employeeName);
                    removeEmployeeFromCache(employeeId); // Evict from by ID cache
                    removeEmployeesFromCache(); // Evict all employees list from cache
                    return ResponseEntity.ok(employeeName); // Return the name directly
                } else {
                    logger.error("API Endpoint did not return true");
                    return ResponseEntity.internalServerError().build();
                }
            } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
                if (attempt < retryMaxAttempts) {
                    logger.warn("API call failed (attempt {}/{}), retrying in {}ms", attempt, retryMaxAttempts, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        logger.error("Thread interrupted during backoff", ex);
                        return ResponseEntity.internalServerError().build();
                    }
                    delay *= 2; // Exponential backoff
                } else {
                    logger.error("API call failed after {} attempts", retryMaxAttempts, e);
                    return ResponseEntity.internalServerError().build();
                }
            } catch (Exception e) {
                logger.error("Exception during deleteEmployeeById: {}", e.getMessage(), e);
                return ResponseEntity.internalServerError().build();
            }
        }
        throw new IllegalStateException(
                "This should never happen. Max attempts reached without a result or exception.");
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        ResponseEntity<Employee> employeeResponse = getEmployeeById(id);
        if (!employeeResponse.getStatusCode().is2xxSuccessful() || employeeResponse.getBody() == null) {
            return new ResponseEntity<>("Employee not found", HttpStatus.NOT_FOUND);
        }

        return deleteEmployeeByName(employeeResponse.getBody());
    }

    public ResponseEntity<List<Employee>> geEmployeesFromCache() {
        if (this.rawRedisCache == null) {
            return null;
        }
        logger.debug("Checking cache for the list of all employees.");
        return rawRedisCache.getEmployeesFromCache();
    }

    public void addEmployeesToCache(ResponseEntity<List<Employee>> employees) {
        if (this.rawRedisCache == null) {
            return;
        }
        logger.debug(
                "Adding list of all {} employees to cache.", employees.getBody().size());
        rawRedisCache.addEmployeesToCache(employees);
    }

    public void removeEmployeesFromCache() {
        if (this.rawRedisCache == null) {
            return;
        }
        logger.debug("Evicting list of all employee from cache.");
        rawRedisCache.removeEmployeesFromCache();
    }

    public Employee geEmployeeFromCache(String employeeId) {
        if (this.rawRedisCache == null) {
            return null;
        }
        logger.debug("Checking cache for {}", employeeId);
        return rawRedisCache.getEmployeeFromCache(employeeId);
    }

    public void addEmployeeToCache(ResponseEntity<Employee> employeeResponseEntity) {
        if (this.rawRedisCache == null) {
            return;
        }
        logger.debug(
                "Adding employee ID {} to cache.",
                employeeResponseEntity.getBody().getId());
        rawRedisCache.addEmployeeToCache(employeeResponseEntity);
    }

    public void removeEmployeeFromCache(String employeeId) {
        if (this.rawRedisCache == null) {
            return;
        }
        logger.debug("Evicting employee with ID {} from employeeByIDCache", employeeId);
        rawRedisCache.removeEmployeeFromCache(employeeId);
    }
}
