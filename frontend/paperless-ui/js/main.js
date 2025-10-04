// API base URL - nginx proxies /api/ to backend service
const API_BASE = '/api';

// DOM elements
let documentsTbody;
let fileInput;
let searchInput;
let filterContentType;
let filterSize;
let filterStatus;

// Store all documents for filtering
let allDocuments = [];

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    initializeDOM();
    loadDocuments();
    setupEventListeners();
});

function initializeDOM() {
    documentsTbody = document.getElementById('documents-tbody');
    fileInput = document.querySelector('input[type="file"]');
    searchInput = document.getElementById('search-input');
    filterContentType = document.getElementById('filter-content-type');
    filterSize = document.getElementById('filter-size');
    filterStatus = document.getElementById('filter-status');
}

function setupEventListeners() {
    // File upload on file input change
    if (fileInput) {
        fileInput.addEventListener('change', handleFileUpload);
    }

    // Search input
    if (searchInput) {
        searchInput.addEventListener('input', filterDocuments);
    }

    // Filter dropdowns
    if (filterContentType) {
        filterContentType.addEventListener('change', filterDocuments);
    }
    if (filterSize) {
        filterSize.addEventListener('change', filterDocuments);
    }
    if (filterStatus) {
        filterStatus.addEventListener('change', filterDocuments);
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

        allDocuments = await response.json();
        console.log('Documents loaded:', allDocuments);
        filterDocuments();
    } catch (error) {
        console.error('Error loading documents:', error);
        if (documentsTbody) {
            documentsTbody.innerHTML = `
                <tr class="border-b border-border-light dark:border-border-dark">
                    <td colspan="6" class="px-6 py-8 text-center text-red-500">
                        Error loading documents: ${escapeHtml(error.message)}
                        <br><span class="text-sm text-muted-light dark:text-muted-dark">Make sure the backend is running and accessible.</span>
                    </td>
                </tr>
            `;
        }
    }
}

// Filter documents based on search and filters
function filterDocuments() {
    if (!allDocuments || !documentsTbody) return;

    const searchTerm = searchInput?.value.toLowerCase() || '';
    const contentTypeFilter = filterContentType?.value || '';
    const sizeFilter = filterSize?.value || '';
    const statusFilter = filterStatus?.value || '';

    const filtered = allDocuments.filter(doc => {
        // Search filter
        const matchesSearch = !searchTerm ||
            doc.title.toLowerCase().includes(searchTerm) ||
            doc.originalFilename.toLowerCase().includes(searchTerm);

        // Content type filter
        const matchesContentType = !contentTypeFilter ||
            doc.contentType.startsWith(contentTypeFilter);

        // Size filter
        let matchesSize = true;
        if (sizeFilter === 'small') {
            matchesSize = doc.sizeBytes < 1024 * 1024; // < 1MB
        } else if (sizeFilter === 'medium') {
            matchesSize = doc.sizeBytes >= 1024 * 1024 && doc.sizeBytes <= 10 * 1024 * 1024; // 1-10MB
        } else if (sizeFilter === 'large') {
            matchesSize = doc.sizeBytes > 10 * 1024 * 1024; // > 10MB
        }

        // Status filter
        const matchesStatus = !statusFilter || doc.status === statusFilter;

        return matchesSearch && matchesContentType && matchesSize && matchesStatus;
    });

    displayDocuments(filtered);
}

// Display documents in the table
function displayDocuments(documents) {
    if (!documents || documents.length === 0) {
        documentsTbody.innerHTML = `
            <tr class="border-b border-border-light dark:border-border-dark">
                <td colspan="6" class="px-6 py-8 text-center text-muted-light dark:text-muted-dark">
                    ${allDocuments.length === 0 ? 'No documents found. Upload your first document to get started!' : 'No documents match your filters.'}
                </td>
            </tr>
        `;
        return;
    }

    const rows = documents.map(doc => {
        const statusClass = getStatusClass(doc.status);
        return `
            <tr class="border-b border-border-light dark:border-border-dark hover:bg-background-light dark:hover:bg-background-dark transition-colors">
                <td class="px-6 py-4">
                    <div class="font-medium text-foreground-light dark:text-foreground-dark">${escapeHtml(doc.title)}</div>
                    <div class="text-xs text-muted-light dark:text-muted-dark">${escapeHtml(doc.originalFilename)}</div>
                </td>
                <td class="px-6 py-4 text-muted-light dark:text-muted-dark">${escapeHtml(doc.contentType)}</td>
                <td class="px-6 py-4 text-muted-light dark:text-muted-dark">${formatBytes(doc.sizeBytes)}</td>
                <td class="px-6 py-4">
                    <span class="px-2 py-1 text-xs font-medium rounded-full ${statusClass}">${escapeHtml(doc.status)}</span>
                </td>
                <td class="px-6 py-4 text-muted-light dark:text-muted-dark">${formatDate(doc.createdAt)}</td>
                <td class="px-6 py-4">
                    <button onclick="deleteDocument('${doc.id}')" class="text-red-500 hover:text-red-700 dark:hover:text-red-400 transition-colors">
                        <span class="material-symbols-outlined text-xl">delete</span>
                    </button>
                </td>
            </tr>
        `;
    }).join('');

    documentsTbody.innerHTML = rows;
}

// Get status badge class
function getStatusClass(status) {
    switch (status) {
        case 'UPLOADED':
            return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
        case 'OCR_PENDING':
            return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
        case 'OCR_IN_PROGRESS':
            return 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300';
        case 'OCR_COMPLETED':
            return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
        case 'OCR_FAILED':
            return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300';
        default:
            return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
    }
}

// Handle file upload
async function handleFileUpload() {
    const files = fileInput.files;
    if (!files || files.length === 0) {
        return;
    }

    // Upload each file
    for (const file of files) {
        try {
            // For now, we'll create a basic document record
            // In a real implementation, you'd upload the file to S3 first
            const documentData = {
                title: file.name,
                originalFilename: file.name,
                contentType: file.type || 'application/octet-stream',
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

            // Show success message
            showMessage(`Document "${file.name}" uploaded successfully!`, 'success');

        } catch (error) {
            console.error('Error uploading document:', error);
            showMessage(`Error uploading "${file.name}": ${error.message}`, 'danger');
        }
    }

    // Clear the file input and reload documents
    fileInput.value = '';
    loadDocuments();
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

function formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) {
        return 'Today';
    } else if (days === 1) {
        return 'Yesterday';
    } else if (days < 7) {
        return `${days} days ago`;
    } else {
        return date.toLocaleDateString();
    }
}

function showMessage(message, type) {
    // Create a notification toast
    const toast = document.createElement('div');
    const bgColor = type === 'success' ? 'bg-green-500' : type === 'danger' ? 'bg-red-500' : 'bg-blue-500';
    toast.className = `fixed top-4 right-4 ${bgColor} text-white px-6 py-3 rounded-lg shadow-lg z-50 transition-opacity duration-300`;
    toast.textContent = message;

    document.body.appendChild(toast);

    // Auto-remove after 3 seconds
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 300);
    }, 3000);
}