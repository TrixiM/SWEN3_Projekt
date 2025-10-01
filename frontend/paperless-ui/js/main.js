// API base URL - when running in Docker, nginx will proxy /api/ to backend /v1/
const API_BASE = '/api';

// DOM elements
let documentsContainer;
let fileInput;
let uploadButton;

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    initializeDOM();
    loadDocuments();
    setupEventListeners();
});

function initializeDOM() {
    documentsContainer = document.getElementById('documents-list');
    fileInput = document.querySelector('input[type="file"]');
    
    // Create documents list container if it doesn't exist
    if (!documentsContainer) {
        documentsContainer = document.createElement('div');
        documentsContainer.id = 'documents-list';
        documentsContainer.className = 'mt-4';
        document.body.appendChild(documentsContainer);
    }
}

function setupEventListeners() {
    // Add upload button if it doesn't exist
    if (!document.getElementById('upload-btn')) {
        const uploadBtn = document.createElement('button');
        uploadBtn.id = 'upload-btn';
        uploadBtn.className = 'btn btn-primary mt-2';
        uploadBtn.textContent = 'Upload Document';
        uploadBtn.onclick = handleFileUpload;
        fileInput.parentElement.appendChild(uploadBtn);
    }
}

// Fetch and display all documents
async function loadDocuments() {
    try {
        console.log('Loading documents...');
        const response = await fetch(`${API_BASE}/documents`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const documents = await response.json();
        console.log('Documents loaded:', documents);
        displayDocuments(documents);
    } catch (error) {
        console.error('Error loading documents:', error);
        documentsContainer.innerHTML = `
            <div class="alert alert-danger" role="alert">
                <h4>Error loading documents</h4>
                <p>${error.message}</p>
                <p><small>Make sure the backend is running and accessible.</small></p>
            </div>
        `;
    }
}

// Display documents in the UI
function displayDocuments(documents) {
    if (!documents || documents.length === 0) {
        documentsContainer.innerHTML = `
            <div class="alert alert-info" role="alert">
                <h4>No documents found</h4>
                <p>Upload your first document to get started!</p>
            </div>
        `;
        return;
    }
    
    const documentsList = documents.map(doc => `
        <div class="card mb-3">
            <div class="card-body">
                <h5 class="card-title">${escapeHtml(doc.title)}</h5>
                <p class="card-text">
                    <strong>Filename:</strong> ${escapeHtml(doc.originalFilename)}<br>
                    <strong>Content Type:</strong> ${escapeHtml(doc.contentType)}<br>
                    <strong>Size:</strong> ${formatBytes(doc.sizeBytes)}<br>
                    <strong>Status:</strong> <span class="badge bg-secondary">${escapeHtml(doc.status)}</span><br>
                    <strong>Created:</strong> ${new Date(doc.createdAt).toLocaleString()}
                </p>
                <button class="btn btn-danger btn-sm" onclick="deleteDocument('${doc.id}')">
                    Delete
                </button>
            </div>
        </div>
    `).join('');
    
    documentsContainer.innerHTML = `
        <h3>Documents (${documents.length})</h3>
        ${documentsList}
    `;
}

// Handle file upload
async function handleFileUpload() {
    const file = fileInput.files[0];
    if (!file) {
        alert('Please select a file first!');
        return;
    }
    
    try {
        // For now, we'll create a basic document record
        // In a real implementation, you'd upload the file to S3 first
        const documentData = {
            title: file.name,
            originalFilename: file.name,
            contentType: file.type,
            sizeBytes: file.size,
            bucket: 'demo-bucket',
            objectKey: `documents/${Date.now()}-${file.name}`,
            checksumSha256: 'dummy-checksum' // In reality, you'd calculate this
        };
        
        console.log('Uploading document:', documentData);
        
        const response = await fetch(`${API_BASE}/documents`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(documentData)
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        console.log('Document uploaded:', result);
        
        // Clear the file input and reload documents
        fileInput.value = '';
        loadDocuments();
        
        // Show success message
        showMessage('Document uploaded successfully!', 'success');
        
    } catch (error) {
        console.error('Error uploading document:', error);
        showMessage(`Error uploading document: ${error.message}`, 'danger');
    }
}

// Delete a document
async function deleteDocument(documentId) {
    if (!confirm('Are you sure you want to delete this document?')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/documents/${documentId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        console.log('Document deleted:', documentId);
        loadDocuments();
        showMessage('Document deleted successfully!', 'success');
        
    } catch (error) {
        console.error('Error deleting document:', error);
        showMessage(`Error deleting document: ${error.message}`, 'danger');
    }
}

// Utility functions
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

function showMessage(message, type) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.insertBefore(alertDiv, document.body.firstChild);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, 5000);
}