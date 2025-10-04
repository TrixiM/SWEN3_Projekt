# Problems and Solutions - DMS Project

## Overview
This document outlines the problems encountered while setting up the Document Management System (DMS) and their solutions.

## Problem 1: Nginx Configuration Error

### Issue Description
When running `docker-compose up --build`, the frontend container was failing with the following error:

```
2025/09/25 20:12:32 [emerg] 1#1: "server" directive is not allowed here in /etc/nginx/nginx.conf:1
nginx: [emerg] "server" directive is not allowed here in /etc/nginx/nginx.conf:1
```

The container kept restarting and exiting with code 1.

### Root Cause
The `nginx.conf` file was incorrectly structured. It contained only a `server` block at the root level:

```nginx
server {
    listen 80;
    
    root /usr/share/nginx/html;
    index index.html;
    
    # Angular routing
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # Proxy API requests to backend
    location /api/ {
        proxy_pass http://rest:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Solution
Nginx requires `server` blocks to be nested inside an `http` block, and the configuration file must also include an `events` block. The corrected `nginx.conf`:

```nginx
events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    
    sendfile on;
    keepalive_timeout 65;
    
    server {
        listen 80;

        root /usr/share/nginx/html;
        index index.html;

        # Angular routing
        location / {
            try_files $uri $uri/ /index.html;
        }

        # Proxy API requests to backend
        location /api/ {
            proxy_pass http://rest:8080/;  # 'rest' = backend service name in Docker network
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

### Files Modified
- `frontend/nginx.conf`

## Problem 2: Angular App Not Loading (Default Nginx Welcome Page)

### Issue Description
After fixing the nginx configuration, the containers started successfully, but accessing `http://localhost/` showed the default "Welcome to nginx!" page instead of the Angular application.

### Root Cause
The issue was in the Dockerfile's COPY command. Modern Angular (17+) builds output files to a `browser` subdirectory within the `dist` folder. The Dockerfile was copying from the wrong path:

```dockerfile
COPY --from=build /app/dist/paperless-ui /usr/share/nginx/html
```

This copied both the Angular app files (in the `browser/` subdirectory) and the default nginx files, with the default nginx `index.html` taking precedence.

### Investigation Steps
1. Checked container file structure: `docker-compose exec frontend ls -la /usr/share/nginx/html`
2. Found both Angular files and default nginx files present
3. Discovered Angular files were in a `browser/` subdirectory
4. Verified the default nginx `index.html` was being served

### Solution
Updated the Dockerfile to copy specifically from the `browser` subdirectory:

```dockerfile
COPY --from=build /app/dist/paperless-ui/browser /usr/share/nginx/html
```

### Files Modified
- `frontend/Dockerfile`

## Problem 3: Testing Backend Connectivity

### Issue Description
Needed to verify that the backend API was functioning correctly and that the nginx proxy configuration was working.

### Solution
Tested all REST API endpoints using PowerShell commands:

#### Direct Backend Testing (port 8080):
```powershell
# GET all documents
Invoke-RestMethod -Uri "http://localhost:8080/v1/documents" -Method GET -ContentType "application/json"

# POST create document
$body = @{
    title = "Test Document"
    originalFilename = "test.pdf"
    contentType = "application/pdf"
    sizeBytes = 1024
    bucket = "test-bucket"
    objectKey = "documents/test.pdf"
    checksumSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/v1/documents" -Method POST -Body $body -ContentType "application/json"

# GET specific document
Invoke-RestMethod -Uri "http://localhost:8080/v1/documents/{id}" -Method GET -ContentType "application/json"

# DELETE document
Invoke-RestMethod -Uri "http://localhost:8080/v1/documents/{id}" -Method DELETE -ContentType "application/json"
```

#### Proxy Testing (through nginx):
```powershell
# Same commands but using http://localhost/api/v1/documents instead of http://localhost:8080/v1/documents
Invoke-RestMethod -Uri "http://localhost/api/v1/documents" -Method GET -ContentType "application/json"
```

### Test Results
✅ All CRUD operations working
✅ Both direct backend access and nginx proxy working
✅ Database persistence confirmed
✅ Data validation working correctly

## System Architecture

### Services
- **Frontend**: Angular app served by nginx (port 80)
- **Backend**: Spring Boot REST API (port 8080)
- **Database**: PostgreSQL (port 5432)
- **Admin**: pgAdmin (port 8081)

### API Endpoints
- `GET /v1/documents` - Retrieve all documents
- `POST /v1/documents` - Create new document
- `GET /v1/documents/{id}` - Get specific document
- `DELETE /v1/documents/{id}` - Delete document

### Docker Network
All services communicate through the `app-network` Docker network. The nginx proxy forwards `/api/` requests to the backend service named `rest`.

## Final Status
✅ **RESOLVED** - All issues have been fixed and the system is fully operational.

### Access Points
- Frontend: http://localhost/
- Backend API (direct): http://localhost:8080/v1/documents
- Backend API (via proxy): http://localhost/api/v1/documents
- pgAdmin: http://localhost:8081/

### Commands to Start
```bash
docker-compose up --build
```

## Lessons Learned

1. **Nginx Configuration**: Always include both `events` and `http` blocks in nginx.conf
2. **Angular Build Output**: Modern Angular versions output to a `browser/` subdirectory
3. **Docker Debugging**: Use `docker-compose exec` to inspect container file systems
4. **API Testing**: Test both direct backend access and proxy routing separately
5. **Container Restart**: Use `docker-compose down` followed by `docker-compose up --build` for clean rebuilds