# Employee API

This project provides a RESTful API for managing employee data.  It uses Spring Boot and integrates with Redis for caching to improve performance.

## Features

* **CRUD Operations:** Create, Read, Update, and Delete employee records.
* **Redis Caching:**  Utilizes Redis on port 6379 for caching employee data to reduce database load and improve response times.
* **RESTful API:**  Provides a clean and consistent API using standard HTTP methods (GET, POST, PUT, DELETE).

## Technologies Used

* Java 21 (or your Java version)
* Spring Boot
* Redis (port 6379)

## Getting Started

### Prerequisites

* Java Development Kit (JDK) 21 (or your version)
* Gradle (or Maven)
* Redis server running on port 6379.

### Building and Running

1. Clone the repository: `git clone <repository_url>`
2. Navigate to the project directory: `cd employee-api`
3. Build the project: `./gradlew build` (or `mvn clean install` for Maven)
4. Run the application: `./gradlew bootRun` (or `mvn spring-boot:run` for Maven)

The API will be accessible at `http://localhost:8111` 

## API Endpoints

| Method | Endpoint        | Description                                  |
|--------|----------------|----------------------------------------------|
| GET    | `/employees/{id}` | Get employee by ID.                         |
| POST   | `/employees`    | Create a new employee.                       |
| DELETE | `/employees/{id}` | Delete an employee.                         |


## Redis Configuration

The application connects to a Redis server running on `localhost:6379`.  You can customize the Redis connection details in the `application.properties` (or `application.yml`) file.

