# How to Start - xxxx Microservices System

## Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Maven 3.9+
- Docker & Docker Compose
- At least 8GB RAM allocated to Docker

## Quick Start (Docker Compose)

### 1. Build all services

```bash
mvn clean package -DskipTests
```

### 2. Start the entire system

```bash
docker-compose up -d
```

### 3. Verify services are running

```bash
docker-compose ps
```

## Service Startup Order

Docker Compose handles the startup order via `depends_on` with healthchecks:

1. **Infrastructure**: MySQL, Redis, Kafka/Zookeeper, Elasticsearch
2. **Observability**: Prometheus, Grafana, Logstash, Kibana, Zipkin
3. **Platform Services**: Discovery (Eureka) → Config Server → Gateway
4. **Business Services**: Booking, Order, Payment, Ticket, Inventory, User, Event

## Access Points

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Config Server | http://localhost:8888 |
| Swagger UI (Gateway) | http://localhost:8080/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin123) |
| Kibana | http://localhost:5601 |
| Zipkin | http://localhost:9411 |
| Kafka (external) | localhost:9094 |
| MySQL | localhost:3316 (root/root123) |
| Redis | localhost:6319 |

## Running Individual Services (Development)

For local development without Docker, start services in this order:

```bash
# 1. Start infrastructure (MySQL, Redis, Kafka) via Docker
docker-compose up -d mysql redis zookeeper kafka

# 2. Start Discovery Service
cd xxxx-discovery
mvn spring-boot:run

# 3. Start Config Service
cd xxxx-config
mvn spring-boot:run

# 4. Start Gateway
cd xxxx-gateway
mvn spring-boot:run

# 5. Start any business service
cd xxxx-order-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Building Docker Images Individually

Using the root multi-stage Dockerfile:

```bash
docker build --build-arg SERVICE_NAME=xxxx-order-service -t xxxx-order-service .
```

Or using individual service Dockerfiles (requires pre-built JAR):

```bash
cd xxxx-order-service
mvn clean package -DskipTests
docker build -t xxxx-order-service .
```

## Stopping the System

```bash
docker-compose down
```

To also remove volumes (data will be lost):

```bash
docker-compose down -v
```

## Troubleshooting

### Service not registering with Eureka
- Check that Discovery service is healthy: `curl http://localhost:8761/actuator/health`
- Verify `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` environment variable

### Database connection issues
- Ensure MySQL is healthy: `docker-compose logs mysql`
- Check that init scripts ran: databases should exist in MySQL

### Kafka connection issues
- Verify Zookeeper is running: `docker-compose logs zookeeper`
- Check Kafka health: `docker-compose logs kafka`

### Out of memory
- Increase Docker memory allocation (recommended: 8GB+)
- Reduce JVM heap for non-critical services via `JAVA_OPTS` environment variable
