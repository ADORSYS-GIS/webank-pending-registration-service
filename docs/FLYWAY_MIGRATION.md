# Flyway Database Migration Documentation

## Overview
This document describes the implementation of Flyway database migrations in the Pending Registration Service. The migration system replaces the previous Hibernate auto-update mechanism (`hibernate.ddl-auto=update`) with a more controlled and versioned approach using Flyway.

## Implementation Details

### 1. Dependencies
The following dependency has been added to the project's `pom.xml`:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### 2. Configuration
Flyway has been configured in `application-postgres.properties` with the following settings:
```properties
# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.schemas=public
spring.flyway.validate-on-migrate=true
```

### 3. Migration Structure
Migrations are stored in `src/main/resources/db/migration/` following the naming convention:
- `V{version}__{description}.sql`
- Example: `V1__initial_schema.sql`

### 4. Current Schema
The initial migration (`V1__initial_schema.sql`) includes:

#### Enum Types
- `personal_info_status`: PENDING, APPROVED, REJECTED
- `otp_status`: PENDING, COMPLETE, INCOMPLETE
- `user_documents_status`: PENDING, APPROVED, REJECTED

#### Tables
1. `personal_information_table`
   - Primary key: `account_id`
   - Stores user personal information and KYC status
   - Includes fields for document verification and OTP management

2. `otp_requests`
   - Primary key: `id` (UUID)
   - Manages OTP requests and verification
   - Includes index on `public_key_hash`
   - Tracks creation and update timestamps

3. `user_documents`
   - Primary key: `account_id`
   - Stores user document references
   - Includes status tracking for document verification

## Migration Process

### Development Environment
1. Create new migration:
   ```bash
   # Create new migration file
   touch src/main/resources/db/migration/V{next_version}__{description}.sql
   ```

2. Test migration:
   ```bash
   # Run application with Flyway enabled
   ./mvnw spring-boot:run
   ```

### Production Environment
1. Backup database before migration
2. Run application with Flyway enabled
3. Monitor migration logs
4. Verify database state after migration

## Best Practices

### Creating New Migrations
1. Always create new migrations for schema changes
2. Use descriptive names for migration files
3. Test migrations in development first
4. Include both up and down migrations when possible
5. Keep migrations idempotent

### Version Control
1. Commit migration files with related code changes
2. Never modify existing migration files
3. Use clear commit messages for migration changes

### Rollback Strategy
1. Keep database backups
2. Document rollback procedures
3. Test rollback scenarios in development

## Troubleshooting

### Common Issues
1. Migration failures
   - Check database connection
   - Verify migration file syntax
   - Ensure proper versioning
   - Note: Some SQL linters may show errors for PostgreSQL-specific syntax (e.g., ENUM types)
     - These errors can be safely ignored as the syntax is valid for PostgreSQL
     - The migration scripts are specifically designed for PostgreSQL

2. Validation errors
   - Compare schema with entity classes
   - Check for missing columns or constraints
   - Verify enum type definitions

### Recovery Steps
1. Identify failed migration
2. Check migration logs
3. Fix migration script
4. Clean Flyway schema history if needed
5. Re-run migration

## Maintenance

### Regular Tasks
1. Monitor migration history
2. Clean up old migration files
3. Update documentation
4. Review migration performance

### Security Considerations
1. Secure database credentials
2. Limit migration access
3. Audit migration changes
4. Backup before migrations

## Deployment Process

### 1. Creating New Migrations for Deployment
When deploying new features that require database changes:

1. Create a new migration file:
   ```bash
   # Format: V{version}__{description}.sql
   # Example: V2__add_user_preferences.sql
   touch src/main/resources/db/migration/V2__add_user_preferences.sql
   ```

2. Write the migration SQL:
   ```sql
   -- V2__add_user_preferences.sql
   ALTER TABLE personal_information_table 
   ADD COLUMN preferences JSONB;
   ```

3. Test the migration locally:
   ```bash
   # Run with dev profile
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### 2. CI/CD Pipeline Integration

#### Pipeline Configuration
Add these steps to your CI/CD pipeline:

```yaml
# Example GitLab CI configuration
deploy:
  stage: deploy
  script:
    # Backup database (important!)
    - pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME > backup_$(date +%Y%m%d_%H%M%S).sql
    
    # Deploy application
    - ./mvnw clean package -DskipTests
    - java -jar target/*.jar --spring.profiles.active=prod
```

#### Environment-Specific Configuration
Create environment-specific properties:

```properties
# application-prod.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.schemas=public
spring.flyway.validate-on-migrate=true
spring.flyway.clean-disabled=true  # Important for production!
```

### 3. Deployment Checklist

#### Pre-Deployment
1. [ ] Create database backup
2. [ ] Review migration scripts
3. [ ] Test migrations in staging environment
4. [ ] Verify rollback procedures
5. [ ] Schedule deployment during low-traffic period

#### During Deployment
1. [ ] Monitor application logs
2. [ ] Watch for Flyway migration messages
3. [ ] Verify database changes
4. [ ] Check application functionality

#### Post-Deployment
1. [ ] Verify all migrations completed successfully
2. [ ] Check application logs for errors
3. [ ] Monitor application performance
4. [ ] Keep backup for at least 24 hours

### 4. Rollback Procedures

#### Automated Rollback
1. Restore database from backup:
   ```bash
   psql -h $DB_HOST -U $DB_USER -d $DB_NAME < backup_file.sql
   ```

2. Redeploy previous version:
   ```bash
   ./mvnw clean package -DskipTests
   java -jar target/*.jar --spring.profiles.active=prod
   ```

#### Manual Rollback
If automated rollback isn't possible:
1. Create rollback migration:
   ```sql
   -- V2_1__rollback_user_preferences.sql
   ALTER TABLE personal_information_table 
   DROP COLUMN preferences;
   ```

2. Apply rollback:
   ```bash
   ./mvnw flyway:repair
   ```

### 5. Monitoring and Maintenance

#### Health Checks
Add these endpoints to your application:
```properties
# application-prod.properties
management.endpoints.web.exposure.include=health,flyway
management.endpoint.health.show-details=always
```

#### Regular Maintenance Tasks
1. Monitor migration history:
   ```sql
   SELECT * FROM flyway_schema_history 
   ORDER BY installed_rank DESC;
   ```

2. Clean up old backups:
   ```bash
   find /backup/directory -name "backup_*.sql" -mtime +7 -delete
   ```

3. Review migration performance:
   ```sql
   SELECT version, 
          description, 
          execution_time 
   FROM flyway_schema_history 
   ORDER BY installed_rank DESC;
   ```

### 6. Security Considerations

#### Production Security
1. Use encrypted database credentials
2. Restrict database access
3. Implement audit logging
4. Regular security reviews

#### Access Control
1. Limit database access to authorized personnel
2. Use separate credentials for migrations
3. Implement IP whitelisting
4. Regular access review

## Additional Resources
- [Flyway Documentation](https://flywaydb.org/documentation)
- [Spring Boot Database Migration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/) 