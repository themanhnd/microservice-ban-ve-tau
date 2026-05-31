# Config Repository

This directory serves as the native config repository for Spring Cloud Config Server.

## Purpose

Service-specific configuration files are stored here for local/dev environments.
Each microservice will have its own configuration file named `{service-name}.yml` 
(e.g., `xxxx-booking-service.yml`, `xxxx-order-service.yml`).

## Profile Support

Configuration files can be profile-specific:
- `{service-name}.yml` - default/common config
- `{service-name}-dev.yml` - development profile
- `{service-name}-staging.yml` - staging profile
- `{service-name}-prod.yml` - production profile

## Notes

- In production, switch from `native` profile to `git` profile for git-based config management.
- Sensitive values should be encrypted using the `/encrypt` endpoint.
- Actual service configs will be created in Task 2.3.
