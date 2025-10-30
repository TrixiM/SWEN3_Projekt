/**
 * Shared utility functions for the frontend application.
 * Centralizes common functionality to eliminate duplication.
 */

// API configuration
export const API_CONFIG = {
    BASE_URL: '/api',
    ENDPOINTS: {
        DOCUMENTS: '/documents',
        DOCUMENT_BY_ID: (id) => `/documents/${id}`,
        DOCUMENT_CONTENT: (id) => `/documents/${id}/content`,
        DOCUMENT_PAGES: (id, pageNumber, scale = 1.5) => `/documents/${id}/pages/${pageNumber}?scale=${scale}`,
        DOCUMENT_PAGE_COUNT: (id) => `/documents/${id}/pages/count`,
        SEARCH: (query) => `/documents/search?q=${encodeURIComponent(query)}`
    }
};

// Toast notification types
export const TOAST_TYPES = {
    SUCCESS: 'success',
    ERROR: 'danger', 
    INFO: 'info',
    WARNING: 'warning'
};

/**
 * Shows a toast notification message.
 * 
 * @param {string} message - The message to display
 * @param {string} type - The type of toast (success, danger, info, warning)
 * @param {number} duration - Duration in milliseconds (default: 3000)
 */
export function showMessage(message, type = TOAST_TYPES.INFO, duration = 3000) {
    const toast = document.createElement('div');
    const bgColor = getToastBackgroundColor(type);
    
    toast.className = `fixed top-4 right-4 ${bgColor} text-white px-6 py-3 rounded-lg shadow-lg z-50 transition-opacity duration-300`;
    toast.textContent = message;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'polite');

    document.body.appendChild(toast);

    // Auto-remove after specified duration
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 300);
    }, duration);
}

/**
 * Gets the background color class for toast notifications.
 * 
 * @param {string} type - The toast type
 * @returns {string} The CSS class for the background color
 */
function getToastBackgroundColor(type) {
    const colorMap = {
        [TOAST_TYPES.SUCCESS]: 'bg-green-500',
        [TOAST_TYPES.ERROR]: 'bg-red-500',
        [TOAST_TYPES.INFO]: 'bg-blue-500',
        [TOAST_TYPES.WARNING]: 'bg-yellow-500'
    };
    return colorMap[type] || colorMap[TOAST_TYPES.INFO];
}

/**
 * Safely escapes HTML content to prevent XSS attacks.
 * 
 * @param {string} text - The text to escape
 * @returns {string} The escaped HTML
 */
export function escapeHtml(text) {
    if (typeof text !== 'string') {
        return '';
    }
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Formats bytes into human-readable format.
 * 
 * @param {number} bytes - The number of bytes
 * @param {number} decimals - Number of decimal places (default: 2)
 * @returns {string} Formatted size string
 */
export function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

/**
 * Formats a date string into a relative or absolute format.
 * 
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted date string
 */
export function formatDate(dateString) {
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

/**
 * Makes an API request with proper error handling and retry logic with exponential backoff.
 * 
 * @param {string} url - The API endpoint URL
 * @param {object} options - Fetch options
 * @param {number} maxRetries - Maximum number of retry attempts (default: 3)
 * @returns {Promise} Promise that resolves to response data or rejects with error
 */
export async function apiRequest(url, options = {}, maxRetries = 3) {
    const fullUrl = API_CONFIG.BASE_URL + url;
    
    for (let attempt = 0; attempt <= maxRetries; attempt++) {
        try {
            const response = await fetch(fullUrl, {
                ...options,
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                }
            });

            if (!response.ok) {
                // Don't retry on 4xx errors (client errors)
                if (response.status >= 400 && response.status < 500) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                
                // Retry on 5xx errors (server errors)
                if (attempt < maxRetries) {
                    const delay = calculateBackoffDelay(attempt);
                    console.warn(`API request failed (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${delay}ms...`);
                    await sleep(delay);
                    continue;
                }
                
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            // Handle different response types
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            } else {
                return response;
            }
        } catch (error) {
            // Network errors or fetch failures
            if (attempt < maxRetries && isRetryableError(error)) {
                const delay = calculateBackoffDelay(attempt);
                console.warn(`API request failed (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${delay}ms...`, error);
                await sleep(delay);
                continue;
            }
            
            console.error('API request failed:', error);
            throw error;
        }
    }
}

/**
 * Calculates exponential backoff delay.
 * 
 * @param {number} attempt - Current attempt number (0-indexed)
 * @returns {number} Delay in milliseconds
 */
function calculateBackoffDelay(attempt) {
    const baseDelay = 1000; // 1 second
    const maxDelay = 10000; // 10 seconds
    const exponentialDelay = baseDelay * Math.pow(2, attempt);
    const jitter = Math.random() * 500; // Add up to 500ms jitter
    return Math.min(exponentialDelay + jitter, maxDelay);
}

/**
 * Checks if an error is retryable.
 * 
 * @param {Error} error - The error to check
 * @returns {boolean} True if retryable
 */
function isRetryableError(error) {
    // Retry on network errors, timeouts, and connection issues
    const retryableErrors = [
        'NetworkError',
        'TypeError', // Often caused by network failures
        'Failed to fetch',
        'Network request failed',
        'Load failed'
    ];
    
    return retryableErrors.some(msg => 
        error.message.includes(msg) || error.name === msg
    );
}

/**
 * Sleep utility for delay.
 * 
 * @param {number} ms - Milliseconds to sleep
 * @returns {Promise} Promise that resolves after delay
 */
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Validates file upload requirements.
 * 
 * @param {File} file - The file to validate
 * @param {object} options - Validation options
 * @returns {object} Validation result with isValid and error message
 */
export function validateFile(file, options = {}) {
    const {
        maxSize = 10 * 1024 * 1024, // 10MB default
        allowedTypes = ['application/pdf', 'image/jpeg', 'image/png'],
        required = true
    } = options;

    if (required && !file) {
        return { isValid: false, error: 'File is required' };
    }

    if (file.size > maxSize) {
        return { 
            isValid: false, 
            error: `File size (${formatBytes(file.size)}) exceeds maximum allowed size (${formatBytes(maxSize)})` 
        };
    }

    if (!allowedTypes.includes(file.type)) {
        return { 
            isValid: false, 
            error: `File type ${file.type} is not allowed. Allowed types: ${allowedTypes.join(', ')}` 
        };
    }

    return { isValid: true };
}

/**
 * Debounces a function call.
 * 
 * @param {Function} func - The function to debounce
 * @param {number} delay - Delay in milliseconds
 * @returns {Function} Debounced function
 */
export function debounce(func, delay) {
    let timeoutId;
    return function (...args) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => func.apply(this, args), delay);
    };
}

/**
 * Creates a DOM element with specified attributes and content.
 * 
 * @param {string} tagName - The tag name
 * @param {object} attributes - Element attributes
 * @param {string|Node} content - Element content
 * @returns {HTMLElement} Created element
 */
export function createElement(tagName, attributes = {}, content = '') {
    const element = document.createElement(tagName);
    
    Object.entries(attributes).forEach(([key, value]) => {
        if (key === 'className') {
            element.className = value;
        } else {
            element.setAttribute(key, value);
        }
    });
    
    if (typeof content === 'string') {
        element.textContent = content;
    } else if (content instanceof Node) {
        element.appendChild(content);
    }
    
    return element;
}