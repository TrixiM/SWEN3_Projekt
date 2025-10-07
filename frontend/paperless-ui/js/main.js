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
        // Enhanced search filter - search across multiple fields including tags
        const matchesSearch = !searchTerm ||
            doc.title.toLowerCase().includes(searchTerm) ||
            doc.originalFilename.toLowerCase().includes(searchTerm) ||
            doc.contentType.toLowerCase().includes(searchTerm) ||
            doc.status.toLowerCase().includes(searchTerm) ||
            formatBytes(doc.sizeBytes).toLowerCase().includes(searchTerm) ||
            (doc.id && doc.id.toLowerCase().includes(searchTerm)) ||
            (doc.tags && doc.tags.some(tag => tag.toLowerCase().includes(searchTerm)));

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
        const isPdf = doc.contentType === 'application/pdf';
        const rowClass = isPdf ? 'cursor-pointer' : '';
        const onClickAttr = isPdf ? `onclick="openPdfPreview('${doc.id}', '${escapeHtml(doc.title)}')"` : '';
        
        // Format tags for display
        const tagsHtml = doc.tags && doc.tags.length > 0 
            ? `<div class="flex flex-wrap gap-1 mt-1">${doc.tags.map(tag => 
                `<span class="px-2 py-0.5 text-xs bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300 rounded">${escapeHtml(tag)}</span>`
              ).join('')}</div>`
            : '';

        return `
            <tr class="border-b border-border-light dark:border-border-dark hover:bg-background-light dark:hover:bg-background-dark transition-colors ${rowClass}" ${onClickAttr}>
                <td class="px-6 py-4">
                    <div class="font-medium text-foreground-light dark:text-foreground-dark flex items-center gap-2">
                        ${isPdf ? '<span class="material-symbols-outlined text-red-500 text-sm">picture_as_pdf</span>' : ''}
                        ${escapeHtml(doc.title)}
                    </div>
                    <div class="text-xs text-muted-light dark:text-muted-dark">${escapeHtml(doc.originalFilename)}</div>
                    ${tagsHtml}
                </td>
                <td class="px-6 py-4 text-muted-light dark:text-muted-dark">${escapeHtml(doc.contentType)}</td>
                <td class="px-6 py-4 text-muted-light dark:text-muted-dark">${formatBytes(doc.sizeBytes)}</td>
                <td class="px-6 py-4">
                    <span class="px-2 py-1 text-xs font-medium rounded-full ${statusClass}">${escapeHtml(doc.status)}</span>
                </td>
                <td class="px-6 py-4 text-muted-light dark:text-muted-dark">${formatDate(doc.createdAt)}</td>
                <td class="px-6 py-4">
                    <button onclick="event.stopPropagation(); editDocument('${doc.id}')" 
                            class="text-blue-500 hover:text-blue-700 dark:hover:text-blue-300 transition-colors">
                        <span class="material-symbols-outlined text-xl">edit</span>
                    </button>
                    <button onclick="event.stopPropagation(); deleteDocument('${doc.id}')" class="text-red-500 hover:text-red-700 dark:hover:text-red-400 transition-colors">
                        <span class="material-symbols-outlined text-xl">delete</span>
                    </button>
                </td>
            </tr>
        `;
    }).join('');

    documentsTbody.innerHTML = rows;
}

function editDocument(documentId) {
    // Find the document in our allDocuments array
    const doc = allDocuments.find(d => d.id === documentId);
    if (!doc) return;

    // Save the document in localStorage so the next page can read it
    localStorage.setItem("selectedDocument", JSON.stringify(doc));

    // Redirect to details page
    window.location.href = "docDetail.html";
}

// Get status badge class

function getStatusClass(status) {
    switch (status) {
        case 'NEW':
            return 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900 dark:text-indigo-300';
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

    // Get tags input if exists
    const tagsInput = document.getElementById('tags-input');
    let tags = [];
    if (tagsInput && tagsInput.value.trim()) {
        // Split by comma and trim each tag
        tags = tagsInput.value.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
    }

    // Upload each file
    for (const file of files) {
        try {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('title', file.name);
            
            // Append each tag as a separate form field
            if (tags.length > 0) {
                tags.forEach(tag => {
                    formData.append('tags', tag);
                });
            }

            console.log('Uploading document:', file.name, 'with tags:', tags);

            const response = await fetch(`${API_BASE}/documents`, {
                method: 'POST',
                body: formData
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

    // Clear the file input, tags input, and reload documents
    fileInput.value = '';
    if (tagsInput) {
        tagsInput.value = '';
    }
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

// PDF Preview functionality
let currentDocumentId = null;
let currentPage = 1;
let totalPages = 0;

async function openPdfPreview(documentId, title) {
    const modal = document.getElementById('pdf-modal');
    const modalTitle = document.getElementById('pdf-modal-title');
    const image = document.getElementById('pdf-image');
    const loading = document.getElementById('pdf-loading');
    const error = document.getElementById('pdf-error');

    // Show modal
    modal.classList.remove('hidden');
    modalTitle.textContent = title;

    // Hide image and error, show loading
    image.style.display = 'none';
    loading.style.display = 'block';
    error.classList.add('hidden');

    try {
        // Fetch page count from backend
        const countResponse = await fetch(`${API_BASE}/documents/${documentId}/pages/count`);
        if (!countResponse.ok) {
            throw new Error(`Failed to load PDF: ${countResponse.status}`);
        }

        totalPages = await countResponse.json();
        currentDocumentId = documentId;
        currentPage = 1;

        // Update page counter
        document.getElementById('page-count').textContent = totalPages;
        document.getElementById('page-num').textContent = currentPage;

        // Render first page
        await renderPage(currentPage);

        // Hide loading, show image
        loading.style.display = 'none';
        image.style.display = 'block';

    } catch (err) {
        console.error('Error loading PDF:', err);
        loading.style.display = 'none';
        error.classList.remove('hidden');
        error.textContent = `Error loading PDF: ${err.message}`;
    }
}

async function renderPage(pageNum) {
    if (!currentDocumentId) return;

    try {
        const image = document.getElementById('pdf-image');
        const loading = document.getElementById('pdf-loading');

        // Show loading while fetching
        loading.style.display = 'block';
        image.style.display = 'none';

        // Fetch rendered page from backend
        const response = await fetch(`${API_BASE}/documents/${currentDocumentId}/pages/${pageNum}?scale=1.5`);
        if (!response.ok) {
            throw new Error(`Failed to render page: ${response.status}`);
        }

        const blob = await response.blob();
        const imageUrl = URL.createObjectURL(blob);

        // Update image
        image.src = imageUrl;

        // Update page number display
        document.getElementById('page-num').textContent = pageNum;

        // Update button states
        document.getElementById('prev-page').disabled = pageNum === 1;
        document.getElementById('next-page').disabled = pageNum === totalPages;

        // Hide loading, show image
        loading.style.display = 'none';
        image.style.display = 'block';

    } catch (err) {
        console.error('Error rendering page:', err);
        const error = document.getElementById('pdf-error');
        error.classList.remove('hidden');
        error.textContent = `Error rendering page: ${err.message}`;
    }
}

function closePdfModal() {
    const modal = document.getElementById('pdf-modal');
    modal.classList.add('hidden');

    // Clean up
    const image = document.getElementById('pdf-image');
    if (image.src) {
        URL.revokeObjectURL(image.src);
        image.src = '';
    }
    currentDocumentId = null;
    currentPage = 1;
    totalPages = 0;
}

function previousPage() {
    if (currentPage <= 1) return;
    currentPage--;
    renderPage(currentPage);
}

function nextPage() {
    if (currentPage >= totalPages) return;
    currentPage++;
    renderPage(currentPage);
}

// Close modal when clicking outside
document.addEventListener('DOMContentLoaded', function() {
    const modal = document.getElementById('pdf-modal');
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closePdfModal();
            }
        });
    }
});