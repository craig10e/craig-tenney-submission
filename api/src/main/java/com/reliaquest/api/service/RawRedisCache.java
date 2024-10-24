package com.reliaquest.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.model.Employee;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

public class RawRedisCache {
    private static final Logger logger = LoggerFactory.getLogger(RawRedisCache.class);
    private final String host;
    private final int port;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RawRedisCache(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void addEmployeesToCache(ResponseEntity<List<Employee>> employees) {
        String key = "employees";
        String value = serializeEmployees(employees.getBody()); // Serialize the List<Employee>

        if (value != null) {
            sendCommand(key, value, "SET");
        }
    }

    public ResponseEntity<List<Employee>> getEmployeesFromCache() {
        String key = "employees";
        String value = sendCommand(key, null, "GET");

        if (value != null) {
            List<Employee> employees = deserializeEmployees(value);
            if (employees != null) {
                return ResponseEntity.ok(employees);
            }
        }
        return null;
    }

    public void removeEmployeesFromCache() {
        String key = "employees";
        sendCommand(key, null, "DEL");
    }

    private String serializeEmployees(List<Employee> employees) {
        try {
            return objectMapper.writeValueAsString(employees);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing employees: {}", e.getMessage());
            return null;
        }
    }

    private List<Employee> deserializeEmployees(String employeesJson) {
        try {
            return objectMapper.readValue(employeesJson, new TypeReference<List<Employee>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing employees: {}", e.getMessage());
            return null;
        }
    }

    public void addEmployeeToCache(ResponseEntity<Employee> employeeResponseEntity) {
        String employeeId = employeeResponseEntity.getBody().getId();
        String key = "employeeById:" + employeeId;
        String value = serializeEmployee(employeeResponseEntity.getBody());

        if (value == null) { // Serialization failed
            return;
        }

        sendCommand(key, value, "SET");
    }

    public Employee getEmployeeFromCache(String employeeId) {
        String key = "employeeById:" + employeeId;
        String value = sendCommand(key, null, "GET");

        return value != null ? deserializeEmployee(value) : null;
    }

    public void removeEmployeeFromCache(String employeeId) {
        String key = "employeeById:" + employeeId;
        sendCommand(key, null, "DEL");
    }

    private String serializeEmployee(Employee employee) {
        try {
            return objectMapper.writeValueAsString(employee);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing employee: {}", e.getMessage());
            return null;
        }
    }

    private Employee deserializeEmployee(String employeeJson) {
        try {
            return objectMapper.readValue(employeeJson, Employee.class);
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing employee: {}", e.getMessage());
            return null;
        }
    }

    private String sendCommand(String key, String value, String commandType) { // SET, GET, DEL
        try (Socket socket = new Socket(host, port);
                OutputStream os = socket.getOutputStream();
                InputStream is = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String command;
            String response;

            if ("SET".equals(commandType)) {
                command = "*3\r\n$3\r\nSET\r\n$" + key.length() + "\r\n" + key + "\r\n$" + value.length() + "\r\n"
                        + value + "\r\n";
                os.write(command.getBytes());
                os.flush();
                response = reader.readLine();
                logger.debug("SET Response: " + response);

            } else if ("GET".equals(commandType)) {
                command = "*2\r\n$3\r\nGET\r\n$" + key.length() + "\r\n" + key + "\r\n";
                os.write(command.getBytes());
                os.flush();

                String responseLine = reader.readLine(); // Read the response line ($<length>)

                if (responseLine != null && responseLine.startsWith("$")) {
                    int valueLength = Integer.parseInt(responseLine.substring(1)); // Get the length

                    if (valueLength > 0) {
                        char[] buffer = new char[valueLength];
                        int charsRead = reader.read(buffer, 0, valueLength); // Read the value

                        if (charsRead == valueLength) {
                            reader.readLine(); // Consume the remaining CRLF
                            return new String(buffer); // Return the actual value
                        } else {
                            logger.error("Error reading value from Redis: Short read");
                        }
                    } else if (valueLength == -1) {
                        logger.debug("Key not found in Redis");
                    }

                } else {
                    logger.error("Invalid Redis response: {}", responseLine);
                }
            } else if ("DEL".equals(commandType)) {
                command = "*2\r\n$3\r\nDEL\r\n$" + key.length() + "\r\n" + key + "\r\n";
                os.write(command.getBytes());
                os.flush();
                response = reader.readLine();
                logger.debug("DEL Response: " + response);

            } else if ("PING".equals(commandType)) {
                command = "*1\r\n$4\r\nPING\r\n";
                os.write(command.getBytes());
                os.flush();
                response = reader.readLine();
                logger.debug("PING Response: " + response);
            }

        } catch (IOException e) {
            logger.error("Error communicating with Redis: {}", e.getMessage());
        }
        return null;
    }
}
