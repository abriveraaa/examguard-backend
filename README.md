# ExamGuard Backend | Spring Boot

Backend server for ExamGuard, a secure examination and monitoring system for managing exams, monitoring activities, analytics, reporting, and registrar integration.

---

## System Requirements

* Java JDK 21+
* Maven 3.9+
* Docker Desktop

Built using:

* Spring Boot
* PostgreSQL
* Maven

---

## How to Use

### Preparation

* Clone the repository

```bash
git clone <backend-repository-url>
cd examguard-backend
```

* Start PostgreSQL

```bash
docker compose up -d
```

* Run backend

```bash
mvn clean spring-boot:run
```

---

Database tables are automatically created and synchronized on startup.

Registrar API uses the configured fixed URL:

```properties
registrar.api.base-url=<registrar-url>
```

Backend URL:

```text
http://localhost:8080
```

---

## Database Configuration

Create:

```text
docker-compose.yml
```

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

## Notes

Current setup uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

which automatically creates and updates database tables from entity classes.
