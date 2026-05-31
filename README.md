# ExamGuard Backend (Spring Boot)

Backend server for ExamGuard, a secure examination and monitoring system responsible for authentication, exam management, monitoring, analytics, reporting, and registrar integration.

---

## Overview

This repository contains the Spring Boot backend application for ExamGuard. It provides REST APIs used by the frontend application and manages all business logic, database operations, reporting, caching, and system integrations.

---

## Technology Stack

* Java 21
* Spring Boot
* PostgreSQL
* Redis
* Maven Wrapper (Included)
* Docker Desktop

---

## Prerequisites

Before running the project, ensure the following software is installed:

### Required

* Java JDK 21+
* Docker Desktop
* Git

### Recommended

* IntelliJ IDEA

### Not Required

Maven installation is **not required** if the repository includes Maven Wrapper (`mvnw` and `mvnw.cmd`).

---

## Verifying Java Installation

Open a terminal and run:

```bash
java -version
```

Expected output:

```text
java version "21.x.x"
```

---

## Verifying Docker Installation

```bash
docker --version
```

Expected output:

```text
Docker version xx.x.x
```

---

## Cloning the Repository

```bash
git clone <backend-repository-url>
cd examguard-backend
```

---

## Starting Required Services

### PostgreSQL Database

Start the database container:

```bash
docker compose up -d
```

Verify the container is running:

```bash
docker ps
```

---

### Redis Cache

If Redis is included in your Docker Compose configuration:

```bash
docker compose up -d
```

Otherwise, start Redis separately according to your local setup.

---

## Configuration

Review the following files before running the application:

```text
src/main/resources/application.properties
src/main/resources/application-dev.properties
src/main/resources/application-prod.properties
```

Common configurations include:

```properties
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=

spring.data.redis.host=
spring.data.redis.port=

registrar.api.base-url=
```

---

## Running the Application

### IntelliJ IDEA (Recommended)

Run:

```text
ExamguardBackendApplication.java
```

---

### Command Line

#### Windows

```cmd
mvnw.cmd spring-boot:run
```

#### macOS / Linux

```bash
./mvnw spring-boot:run
```

---

## Building the Application

### Windows

```cmd
mvnw.cmd clean package
```

### macOS / Linux

```bash
./mvnw clean package
```

Generated JAR files will be available in:

```text
target/
```

---

## Default Local Environment

### Backend API

```text
http://localhost:8080
```

### PostgreSQL

```text
localhost:5432
```

### Redis

```text
localhost:6379
```

---

## Database Setup

Example Docker configuration:

```yaml
services:
  postgres:
    image: postgres:16
    container_name: examguard-postgres

    environment:
      POSTGRES_DB: examguard
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password

    ports:
      - "5432:5432"

    volumes:
      - examguard_postgres_data:/var/lib/postgresql/data

volumes:
  examguard_postgres_data:
```

---

## Troubleshooting

### Java Not Found

```bash
java -version
```

If not recognized:

* Reinstall JDK 21
* Configure JAVA_HOME
* Restart terminal or IDE

---

### Docker Not Running

Verify Docker Desktop is started before executing:

```bash
docker compose up -d
```

---

### Port 8080 Already In Use

Find and stop the application currently using port 8080 or update:

```properties
server.port=8081
```

---

### Database Connection Failed

Verify:

* PostgreSQL container is running
* Database credentials are correct
* Port 5432 is available
* Application properties are configured correctly

---

### Redis Connection Failed

Verify:

* Redis service is running
* Redis host and port configuration are correct

---

## Development Notes

Current development configuration uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This automatically creates and updates database tables based on entity definitions during development.

For production deployments, database migrations should be managed separately.
