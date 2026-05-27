# ExamGuard Backend

<div align="center">

Backend server for the ExamGuard secure examination and monitoring system.

Java 21 • Spring Boot • PostgreSQL • Maven

</div>

---

## Overview

ExamGuard is a secure examination platform designed for managing examinations, monitoring activities, analytics, reporting, and registrar integration.

---

## Requirements

Install the following before running the project:

* Java JDK 21+
* Maven 3.9+
* Git
* Docker Desktop

Recommended:

* IntelliJ IDEA

---

## Quick Start

For most users, setup only requires the following commands:

```bash
git clone <backend-repository-url>

cd examguard-backend

docker compose up -d

mvn clean spring-boot:run
```

Backend should run at:

```text
http://localhost:8080
```

---

## Setup Instructions

### Step 1: Clone repository

```bash
git clone <backend-repository-url>

cd examguard-backend
```

---

### Step 2: Start PostgreSQL database

Run:

```bash
docker compose up -d
```

Verify container:

```bash
docker ps
```

Expected:

```text
examguard-postgres
```

---

### Step 3: Configure application properties

Open:

```text
src/main/resources/application.properties
```

Update:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/examguard
spring.datasource.username=postgres
spring.datasource.password=password

spring.jpa.hibernate.ddl-auto=update

registrar.api.base-url=<fixed-registrar-api-url>
```

Notes:

* `ddl-auto=update` automatically creates and updates database tables based on entities.
* Registrar URL should remain fixed and should not be changed.

---

### Step 4: Install project dependencies

```bash
mvn clean install
```

---

### Step 5: Run backend

```bash
mvn spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

---

## Database Configuration

Create the following file in project root:

```text
docker-compose.yml
```

Paste:

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

## Automatic Setup Behavior

Upon startup:

* PostgreSQL container starts
* Spring Boot connects automatically
* Database tables are automatically created or updated
* Registrar API configuration is loaded
* Backend becomes available on port `8080`

No manual database creation is required.

---

## Running Project Daily

Start database:

```bash
docker compose up -d
```

Run backend:

```bash
mvn spring-boot:run
```

Run frontend normally.

---

## Useful Commands

### Start database

```bash
docker compose up -d
```

### Stop database

```bash
docker compose down
```

### Reset database completely

Warning: Deletes all database data.

```bash
docker compose down -v
```

### Install dependencies

```bash
mvn clean install
```

### Run backend

```bash
mvn spring-boot:run
```

---

## Common Errors

### Port 5432 already in use

Change Docker port:

```yaml
ports:
  - "5433:5432"
```

Update:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/examguard
```

---

### Backend starts but frontend cannot connect

Check:

* Backend is running
* Backend URL is correct
* Frontend API base URL is correct
* CORS configuration exists

---

## Recommended Workflow

For group members:

```bash
git clone <backend-repository-url>

cd examguard-backend

docker compose up -d

mvn clean spring-boot:run
```

The backend will automatically initialize and synchronize database tables during startup.
