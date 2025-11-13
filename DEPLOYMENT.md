# Deployment Guide

This guide provides step-by-step instructions for deploying the Document Management System in various environments.

## Table of Contents

- [Local Development](#local-development)
- [Docker Compose Production](#docker-compose-production)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Cloud Deployment](#cloud-deployment)
- [Environment Variables](#environment-variables)
- [Database Migration](#database-migration)
- [Monitoring & Logging](#monitoring--logging)
- [Backup & Recovery](#backup--recovery)
- [Troubleshooting](#troubleshooting)

## Local Development

### Prerequisites

- Docker Desktop or Docker Engine + Docker Compose
- Java 21 JDK
- Maven 3.9+
- Git
- Google Gemini API Key

### Setup Steps

1. **Clone Repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/swen-3.git
   cd swen-3
   ```

2. **Configure Environment**
   ```bash
   # Create .env file
   cat > .env << EOF
   GEMINI_API_KEY=your-api-key-here
   EOF
   ```

3. **Start Services**
   ```bash
   docker-compose up --build
   ```

4. **Verify Services**
   ```bash
   # Check all containers are running
   docker-compose ps
   
   # Check backend health
   curl http://localhost:8080/actuator/health
   
   # Check frontend
   curl http://localhost
   ```

5. **Access Applications**
   - Frontend: http://localhost
   - Backend API: http://localhost:8080
   - API Docs: http://localhost:8080/swagger-ui.html
   - RabbitMQ: http://localhost:15672 (guest/guest)
   - MinIO: http://localhost:9001 (minioadmin/minioadmin)
   - PgAdmin: http://localhost:5050 (admin@admin.com/adminpassword)
   - Elasticsearch: http://localhost:9200

## Docker Compose Production

### Production Configuration

1. **Update docker-compose.yml for Production**

   Create `docker-compose.prod.yml`:

   ```yaml
   version: '3.8'
   
   services:
     rest:
       build:
         context: ./backend
         dockerfile: DOCKERFILE.REST
       container_name: rest-server
       restart: always
       environment:
         DATABASE_HOST: postgres
         DATABASE_PORT: 5432
         DATABASE_NAME: ${DB_NAME:-documentdb}
         DATABASE_USER: ${DB_USER}
         DATABASE_PASSWORD: ${DB_PASSWORD}
         MINIO_ENDPOINT: http://minio:9000
         MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY}
         MINIO_SECRET_KEY: ${MINIO_SECRET_KEY}
         MINIO_BUCKET_NAME: documents
         ELASTICSEARCH_URIS: http://elasticsearch:9200
       ports:
         - "8080:8080"
       depends_on:
         - postgres
         - minio
         - elasticsearch
       networks:
         - app-network
       deploy:
         resources:
           limits:
             cpus: '2'
             memory: 2G
           reservations:
             cpus: '1'
             memory: 1G
   
     postgres:
       image: postgres:16
       container_name: postgres
       restart: always
       environment:
         POSTGRES_USER: ${DB_USER}
         POSTGRES_PASSWORD: ${DB_PASSWORD}
         POSTGRES_DB: ${DB_NAME:-documentdb}
       ports:
         - "5432:5432"
       volumes:
         - postgres_data:/var/lib/postgresql/data
         - ./init.sql:/docker-entrypoint-initdb.d/init.sql:ro
       networks:
         - app-network
       deploy:
         resources:
           limits:
             cpus: '1'
             memory: 1G
   
     # Add other services with resource limits...
   ```

2. **Create Production Environment File**

   ```bash
   cat > .env.production << EOF
   # Database
   DB_NAME=documentdb
   DB_USER=produser
   DB_PASSWORD=$(openssl rand -base64 32)
   
   # MinIO
   MINIO_ACCESS_KEY=$(openssl rand -base64 12)
   MINIO_SECRET_KEY=$(openssl rand -base64 32)
   
   # RabbitMQ
   RABBITMQ_USER=admin
   RABBITMQ_PASSWORD=$(openssl rand -base64 32)
   
   # Gemini API
   GEMINI_API_KEY=your-production-api-key
   
   # Application
   SPRING_PROFILES_ACTIVE=production
   LOG_LEVEL=INFO
   EOF
   
   chmod 600 .env.production
   ```

3. **Start Production Services**

   ```bash
   docker-compose -f docker-compose.prod.yml --env-file .env.production up -d
   ```

4. **Configure Reverse Proxy (Nginx)**

   ```nginx
   # /etc/nginx/sites-available/dms
   
   upstream backend {
       server localhost:8080;
   }
   
   server {
       listen 80;
       server_name dms.example.com;
       
       # Redirect to HTTPS
       return 301 https://$server_name$request_uri;
   }
   
   server {
       listen 443 ssl http2;
       server_name dms.example.com;
       
       ssl_certificate /etc/letsencrypt/live/dms.example.com/fullchain.pem;
       ssl_certificate_key /etc/letsencrypt/live/dms.example.com/privkey.pem;
       
       # Frontend
       location / {
           proxy_pass http://localhost:80;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
       
       # Backend API
       location /api {
           proxy_pass http://backend;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           
           # Increase timeout for large uploads
           proxy_read_timeout 300s;
           proxy_send_timeout 300s;
           client_max_body_size 100M;
       }
       
       # WebSocket support (if needed)
       location /ws {
           proxy_pass http://backend;
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection "upgrade";
       }
   }
   ```

5. **SSL Certificate with Let's Encrypt**

   ```bash
   # Install certbot
   sudo apt install certbot python3-certbot-nginx
   
   # Obtain certificate
   sudo certbot --nginx -d dms.example.com
   
   # Auto-renewal (cron)
   sudo crontab -e
   # Add: 0 12 * * * /usr/bin/certbot renew --quiet
   ```

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (v1.24+)
- kubectl configured
- Helm 3
- Container registry access

### Deployment Steps

1. **Create Namespace**

   ```bash
   kubectl create namespace dms
   ```

2. **Create Secrets**

   ```bash
   # Database secrets
   kubectl create secret generic db-credentials \
     --from-literal=username=admin \
     --from-literal=password=$(openssl rand -base64 32) \
     -n dms
   
   # MinIO secrets
   kubectl create secret generic minio-credentials \
     --from-literal=accesskey=$(openssl rand -base64 12) \
     --from-literal=secretkey=$(openssl rand -base64 32) \
     -n dms
   
   # Gemini API secret
   kubectl create secret generic gemini-api \
     --from-literal=api-key=your-api-key \
     -n dms
   ```

3. **Deploy PostgreSQL**

   ```yaml
   # postgres-deployment.yaml
   apiVersion: v1
   kind: PersistentVolumeClaim
   metadata:
     name: postgres-pvc
     namespace: dms
   spec:
     accessModes:
       - ReadWriteOnce
     resources:
       requests:
         storage: 10Gi
   ---
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: postgres
     namespace: dms
   spec:
     replicas: 1
     selector:
       matchLabels:
         app: postgres
     template:
       metadata:
         labels:
           app: postgres
       spec:
         containers:
         - name: postgres
           image: postgres:16
           env:
           - name: POSTGRES_USER
             valueFrom:
               secretKeyRef:
                 name: db-credentials
                 key: username
           - name: POSTGRES_PASSWORD
             valueFrom:
               secretKeyRef:
                 name: db-credentials
                 key: password
           - name: POSTGRES_DB
             value: documentdb
           ports:
           - containerPort: 5432
           volumeMounts:
           - name: postgres-storage
             mountPath: /var/lib/postgresql/data
         volumes:
         - name: postgres-storage
           persistentVolumeClaim:
             claimName: postgres-pvc
   ---
   apiVersion: v1
   kind: Service
   metadata:
     name: postgres
     namespace: dms
   spec:
     selector:
       app: postgres
     ports:
     - port: 5432
       targetPort: 5432
   ```

   ```bash
   kubectl apply -f postgres-deployment.yaml
   ```

4. **Deploy Backend**

   ```yaml
   # backend-deployment.yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: backend
     namespace: dms
   spec:
     replicas: 3
     selector:
       matchLabels:
         app: backend
     template:
       metadata:
         labels:
           app: backend
       spec:
         containers:
         - name: backend
           image: your-registry/dms-backend:latest
           ports:
           - containerPort: 8080
           env:
           - name: DATABASE_HOST
             value: postgres
           - name: DATABASE_USER
             valueFrom:
               secretKeyRef:
                 name: db-credentials
                 key: username
           - name: DATABASE_PASSWORD
             valueFrom:
               secretKeyRef:
                 name: db-credentials
                 key: password
           livenessProbe:
             httpGet:
               path: /actuator/health/liveness
               port: 8080
             initialDelaySeconds: 60
             periodSeconds: 10
           readinessProbe:
             httpGet:
               path: /actuator/health/readiness
               port: 8080
             initialDelaySeconds: 30
             periodSeconds: 5
           resources:
             requests:
               memory: "1Gi"
               cpu: "500m"
             limits:
               memory: "2Gi"
               cpu: "1000m"
   ---
   apiVersion: v1
   kind: Service
   metadata:
     name: backend
     namespace: dms
   spec:
     selector:
       app: backend
     ports:
     - port: 8080
       targetPort: 8080
     type: LoadBalancer
   ```

   ```bash
   kubectl apply -f backend-deployment.yaml
   ```

5. **Deploy Workers with HPA**

   ```yaml
   # ocr-worker-deployment.yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: ocr-worker
     namespace: dms
   spec:
     replicas: 2
     selector:
       matchLabels:
         app: ocr-worker
     template:
       metadata:
         labels:
           app: ocr-worker
       spec:
         containers:
         - name: ocr-worker
           image: your-registry/dms-ocr:latest
           resources:
             requests:
               memory: "2Gi"
               cpu: "1000m"
             limits:
               memory: "4Gi"
               cpu: "2000m"
   ---
   apiVersion: autoscaling/v2
   kind: HorizontalPodAutoscaler
   metadata:
     name: ocr-worker-hpa
     namespace: dms
   spec:
     scaleTargetRef:
       apiVersion: apps/v1
       kind: Deployment
       name: ocr-worker
     minReplicas: 2
     maxReplicas: 10
     metrics:
     - type: Resource
       resource:
         name: cpu
         target:
           type: Utilization
           averageUtilization: 70
   ```

   ```bash
   kubectl apply -f ocr-worker-deployment.yaml
   ```

6. **Ingress Configuration**

   ```yaml
   # ingress.yaml
   apiVersion: networking.k8s.io/v1
   kind: Ingress
   metadata:
     name: dms-ingress
     namespace: dms
     annotations:
       cert-manager.io/cluster-issuer: letsencrypt-prod
       nginx.ingress.kubernetes.io/proxy-body-size: "100m"
   spec:
     ingressClassName: nginx
     tls:
     - hosts:
       - dms.example.com
       secretName: dms-tls
     rules:
     - host: dms.example.com
       http:
         paths:
         - path: /api
           pathType: Prefix
           backend:
             service:
               name: backend
               port:
                 number: 8080
         - path: /
           pathType: Prefix
           backend:
             service:
               name: frontend
               port:
                 number: 80
   ```

   ```bash
   kubectl apply -f ingress.yaml
   ```

## Cloud Deployment

### AWS Deployment

1. **Using ECS with Fargate**

   ```bash
   # Create ECS cluster
   aws ecs create-cluster --cluster-name dms-cluster
   
   # Register task definitions (see task-definition.json)
   aws ecs register-task-definition --cli-input-json file://task-definition.json
   
   # Create service
   aws ecs create-service \
     --cluster dms-cluster \
     --service-name backend \
     --task-definition dms-backend \
     --desired-count 3 \
     --launch-type FARGATE \
     --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
   ```

2. **Using RDS for PostgreSQL**

   ```bash
   aws rds create-db-instance \
     --db-instance-identifier dms-postgres \
     --db-instance-class db.t3.medium \
     --engine postgres \
     --engine-version 16.0 \
     --master-username admin \
     --master-user-password $(aws secretsmanager get-random-password --output text --query RandomPassword) \
     --allocated-storage 100 \
     --backup-retention-period 7 \
     --vpc-security-group-ids sg-xxx
   ```

### Azure Deployment

1. **Using Azure Container Instances**

   ```bash
   # Create resource group
   az group create --name dms-rg --location eastus
   
   # Create container group
   az container create \
     --resource-group dms-rg \
     --name dms-backend \
     --image your-registry/dms-backend:latest \
     --cpu 2 --memory 4 \
     --ports 8080 \
     --environment-variables \
       DATABASE_HOST=postgres.postgres.database.azure.com \
       DATABASE_USER=admin \
       DATABASE_PASSWORD=secure-password
   ```

### Google Cloud Deployment

1. **Using Cloud Run**

   ```bash
   # Build and push image
   gcloud builds submit --tag gcr.io/PROJECT_ID/dms-backend
   
   # Deploy to Cloud Run
   gcloud run deploy dms-backend \
     --image gcr.io/PROJECT_ID/dms-backend \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --set-env-vars DATABASE_HOST=postgres,DATABASE_USER=admin
   ```

## Environment Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_HOST` | PostgreSQL host | `postgres` |
| `DATABASE_PORT` | PostgreSQL port | `5432` |
| `DATABASE_NAME` | Database name | `documentdb` |
| `DATABASE_USER` | DB username | `admin` |
| `DATABASE_PASSWORD` | DB password | `securepass123` |
| `MINIO_ENDPOINT` | MinIO endpoint | `http://minio:9000` |
| `MINIO_ACCESS_KEY` | MinIO access key | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO secret key | `minioadmin` |
| `ELASTICSEARCH_URIS` | Elasticsearch URL | `http://elasticsearch:9200` |
| `GEMINI_API_KEY` | Google Gemini API key | `AIza...` |
| `RABBITMQ_HOST` | RabbitMQ host | `rabbitmq` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `LOG_LEVEL` | Logging level | `INFO` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |
| `MAX_FILE_SIZE` | Max upload size | `100MB` |
| `CIRCUIT_BREAKER_THRESHOLD` | Circuit breaker failure threshold | `50%` |

## Database Migration

### Flyway Migration

1. **Create Migration Files**

   ```sql
   -- V1__initial_schema.sql
   CREATE TABLE documents (
       id UUID PRIMARY KEY,
       title VARCHAR(255) NOT NULL,
       original_filename VARCHAR(255) NOT NULL,
       content_type VARCHAR(127) NOT NULL,
       size_bytes BIGINT NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT NOW()
   );
   ```

2. **Run Migrations**

   ```bash
   # Automatic on startup
   # Or manually:
   mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/documentdb
   ```

## Monitoring & Logging

### Prometheus Metrics

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'dms-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
```

### Grafana Dashboard

Import dashboard ID: 4701 (JVM Micrometer)

### ELK Stack Integration

```yaml
# logstash.conf
input {
  tcp {
    port => 5000
    codec => json
  }
}

filter {
  if [application] == "dms-backend" {
    mutate {
      add_tag => ["backend"]
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "dms-logs-%{+YYYY.MM.dd}"
  }
}
```

## Backup & Recovery

### Database Backup

```bash
# Automated backup script
#!/bin/bash
BACKUP_DIR="/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)

docker exec postgres pg_dump -U admin documentdb > "$BACKUP_DIR/backup_$DATE.sql"

# Keep only last 7 days
find $BACKUP_DIR -name "backup_*.sql" -mtime +7 -delete

# Upload to S3
aws s3 cp "$BACKUP_DIR/backup_$DATE.sql" s3://dms-backups/postgres/
```

### MinIO Backup

```bash
# Mirror to backup location
mc mirror minio/documents s3/backup-bucket/documents
```

### Restore Procedures

```bash
# Restore PostgreSQL
docker exec -i postgres psql -U admin documentdb < backup_20240101_120000.sql

# Restore MinIO
mc mirror s3/backup-bucket/documents minio/documents
```

## Troubleshooting

### Common Issues

**Services Won't Start**
```bash
# Check logs
docker-compose logs -f service-name

# Verify environment variables
docker-compose config

# Reset everything
docker-compose down -v && docker-compose up --build
```

**Database Connection Errors**
```bash
# Check PostgreSQL is accessible
docker exec -it postgres psql -U admin -d documentdb

# Verify credentials
echo $DATABASE_PASSWORD
```

**High Memory Usage**
```bash
# Check container stats
docker stats

# Adjust Java heap size
-Xmx2g -Xms1g
```

**Queue Backlogs**
```bash
# Check RabbitMQ queues
curl -u guest:guest http://localhost:15672/api/queues

# Scale workers
docker-compose up -d --scale ocr-worker=5
```

---

**For additional help, see:**
- [README.md](README.md) - Quick start guide
- [STABILITY_IMPROVEMENTS.md](STABILITY_IMPROVEMENTS.md) - Resilience patterns
- [GitHub Issues](https://github.com/YOUR_USERNAME/swen-3/issues) - Report problems
