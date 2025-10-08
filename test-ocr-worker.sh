#!/bin/bash

# Test script for OCR Worker implementation
# This script helps verify that the OCR worker is properly receiving messages

echo "================================================"
echo "OCR Worker Implementation Test"
echo "================================================"
echo ""

# Check if services are running
echo "1. Checking if services are running..."
echo ""

if docker ps | grep -q "ocr-worker"; then
    echo "✅ OCR Worker container is running"
else
    echo "❌ OCR Worker container is NOT running"
    echo "   Run: docker-compose up -d"
    exit 1
fi

if docker ps | grep -q "rest-server"; then
    echo "✅ REST Server container is running"
else
    echo "❌ REST Server container is NOT running"
    echo "   Run: docker-compose up -d"
    exit 1
fi

if docker ps | grep -q "rabbitmq"; then
    echo "✅ RabbitMQ container is running"
else
    echo "❌ RabbitMQ container is NOT running"
    echo "   Run: docker-compose up -d"
    exit 1
fi

echo ""
echo "2. Testing document upload and OCR worker message processing..."
echo ""

# Create a test PDF (simple text file as placeholder)
echo "Creating test file..."
echo "This is a test document for OCR processing" > /tmp/test-document.txt

# Upload document
echo "Uploading document to REST server..."
RESPONSE=$(curl -s -X POST http://localhost:8080/v1/documents \
  -F "file=@/tmp/test-document.txt" \
  -F "title=OCR Worker Test Document")

if [ $? -eq 0 ]; then
    echo "✅ Document uploaded successfully"
    echo "Response: $RESPONSE"
else
    echo "❌ Failed to upload document"
    exit 1
fi

echo ""
echo "3. Checking OCR Worker logs..."
echo "   (Last 20 lines - look for 'OCR WORKER RECEIVED')"
echo ""
docker-compose logs --tail=20 ocr-worker

echo ""
echo "================================================"
echo "Test Complete!"
echo "================================================"
echo ""
echo "To view live OCR worker logs, run:"
echo "  docker-compose logs -f ocr-worker"
echo ""
echo "To view RabbitMQ management UI:"
echo "  http://localhost:15672 (guest/guest)"
echo ""

# Cleanup
rm /tmp/test-document.txt
