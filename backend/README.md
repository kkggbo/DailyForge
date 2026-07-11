# DailyForge Backend

Spring Boot backend for DailyForge.

## Stack

- Java 21
- Spring Boot 3.2.5
- MyBatis-Plus
- MySQL 8.0
- Redis 7

## Structure

- `src/main/java/com/dailyforge` application code
- `src/main/resources/db/migration` manual SQL initialization scripts

## Notes

- `pom.xml` targets Java 21.
- Flyway has been removed from runtime dependencies for now.
- Execute `V1__init_schema.sql` and `V2__seed_base_data.sql` manually before starting the backend.
