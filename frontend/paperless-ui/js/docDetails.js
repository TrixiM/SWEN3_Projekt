import { 
    API_CONFIG, 
    TOAST_TYPES, 
    showMessage, 
    apiRequest 
} from './utils.js';

document.addEventListener("DOMContentLoaded", () => {
    const stored = localStorage.getItem("selectedDocument");
    if (!stored) return;

    const doc = JSON.parse(stored);
    const fields = ["title", "filename", "content-type", "size", "status", "created", "updated", "checksum", "summary"];
    const editableFields = ["title", "status", "summary"];

    function fillFields() {
        document.getElementById("title").value = doc.title || "";
        document.getElementById("filename").value = doc.originalFilename || "";
        document.getElementById("content-type").value = doc.contentType || "";
        document.getElementById("size").value = (doc.sizeBytes / (1024 * 1024)).toFixed(2) + " MB";
        document.getElementById("status").value = doc.status || "";
        document.getElementById("created").value = new Date(doc.createdAt).toLocaleString();
        document.getElementById("updated").value = new Date(doc.updatedAt).toLocaleString();
        document.getElementById("checksum").value = doc.checksum || "—";
        document.getElementById("summary").value = doc.summary || "No summary available.";
    }

    fillFields();

    // Auto-refresh document data to check for summary updates
    let refreshInterval;
    async function refreshDocumentData() {
        try {
            const updated = await apiRequest(API_CONFIG.ENDPOINTS.DOCUMENT_BY_ID(doc.id));
            
            // Only update if summary has changed and we're not in edit mode
            if (updated.summary && updated.summary !== doc.summary) {
                const summaryField = document.getElementById("summary");
                if (summaryField && summaryField.readOnly) {
                    doc.summary = updated.summary;
                    summaryField.value = updated.summary;
                    console.log('✅ Summary updated:', updated.summary.substring(0, 100) + '...');
                    
                    // Stop refreshing once we have a summary
                    if (refreshInterval) {
                        clearInterval(refreshInterval);
                        refreshInterval = null;
                    }
                }
            }
        } catch (error) {
            console.error('Failed to refresh document data:', error);
        }
    }
    
    // Only auto-refresh if no summary exists yet
    if (!doc.summary || doc.summary === "No summary available.") {
        refreshInterval = setInterval(refreshDocumentData, 5000); // Check every 5 seconds
        console.log('🔄 Auto-refresh enabled - checking for summary updates...');
    }

    // Make fields read-only initially
    fields.forEach(id => {
        const el = document.getElementById(id);
        if (el.tagName === "SELECT") {
            el.disabled = true;
        } else {
            el.readOnly = true;
        }
    });
    // Hide Save and Cancel initially
    document.getElementById("cancel").classList.add("hidden");
    document.getElementById("save").classList.add("hidden");

    // Fill tags if available
    const tagsContainer = document.querySelector("#tags-container");
    if (tagsContainer && doc.tags) {
        tagsContainer.innerHTML = doc.tags.map(tag =>
            `<span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary/10 text-primary dark:bg-primary/20 dark:text-primary">${tag}</span>`
        ).join("");
    }

    // Buttons
    const addTagBtn = document.getElementById("addTag");
    const removeTagBtn = document.getElementById("removeTag");
    const editBtn = document.getElementById("editDoc");
    const deleteBtn = document.getElementById("deleteDoc");
    const cancelBtn = document.getElementById("cancel");
    const saveBtn = document.getElementById("save");
    const home = document.getElementById("home");

    // Delete a document
    async function deleteDocument(documentId) {
        if (!confirm('Are you sure you want to delete this document?')) {
            return;
        }

        try {
            await apiRequest(API_CONFIG.ENDPOINTS.DOCUMENT_BY_ID(documentId), {
                method: 'DELETE'
            });

            console.log('Document deleted:', documentId);

        } catch (error) {
            console.error('Error deleting document:', error);
            showMessage(`Error deleting document: ${error.message}`, TOAST_TYPES.ERROR);
        }
    }
    // showMessage function now imported from utils.js


    deleteBtn.addEventListener("click", () => {
        if (!doc.id) return;
        deleteDocument(doc.id)
            .then(() => {
                sessionStorage.setItem("bannerMessage", `Document "${doc.title}" has been deleted`);
                window.location.href = "index.html";
            })
            .catch(err => {
                console.error("Delete failed:", err);
                showMessage(`Error deleting document: ${err.message}`, 'danger');
            });
    });

    addTagBtn.addEventListener("click", async () => {
        const tag = prompt("Enter a tag");
        if (!tag || !tag.trim()) return;
        
        const trimmedTag = tag.trim();
        
        if (!Array.isArray(doc.tags))
            doc.tags = [];

        // Prevent duplicates
        if (doc.tags.includes(trimmedTag)) {
            showMessage(`Tag "${trimmedTag}" already exists.`, TOAST_TYPES.INFO);
            return;
        }
        
        doc.tags.push(trimmedTag);

        // Create the tag element
        const tagEl = document.createElement("span");
        tagEl.textContent = trimmedTag;
        tagEl.className = `
        inline-flex items-center px-3 py-1 
        rounded-full text-sm font-medium 
        bg-primary/10 text-primary 
        dark:bg-primary/20 dark:text-primary
    `;
        tagEl.addEventListener("click", () => {
            tagEl.classList.toggle("selected");
            tagEl.classList.toggle("bg-red-100");
            tagEl.classList.toggle("text-red-700");
            tagEl.classList.toggle("border");
            tagEl.classList.toggle("border-red-300");
        })

        // Append to container
        const tagsContainer = document.querySelector("#tags-container");
        tagsContainer.appendChild(tagEl);

        // Immediately persist the tag to backend
        const updatedDoc = {
            id: doc.id,
            title: doc.title,
            originalFilename: doc.originalFilename,
            contentType: doc.contentType,
            status: doc.status,
            sizeBytes: doc.sizeBytes,
            tags: doc.tags
        };

        try {
            const updated = await updateDocument(doc.id, updatedDoc);
            doc.tags = updated.tags || doc.tags; // Update from server response
            showMessage(`Tag "${trimmedTag}" added successfully!`, TOAST_TYPES.SUCCESS);
        } catch (err) {
            console.error("Failed to add tag:", err);
            // Rollback on failure
            doc.tags = doc.tags.filter(t => t !== trimmedTag);
            tagEl.remove();
            showMessage(`Error adding tag: ${err.message}`, TOAST_TYPES.ERROR);
        }
    })

    removeTagBtn.addEventListener("click", async () => {
        const getSelectedTag = document.querySelectorAll(".selected");
        if (getSelectedTag.length === 0) {
            showMessage('Please select tags to remove by clicking on them', TOAST_TYPES.INFO);
            return;
        }

        // Store tags to remove and their elements
        const tagsToRemove = [];
        const elementsToRemove = [];
        
        getSelectedTag.forEach(tagEl => {
            const tagText = tagEl.textContent;
            tagsToRemove.push(tagText);
            elementsToRemove.push(tagEl);
        });

        // Remove from array
        const originalTags = [...doc.tags];
        doc.tags = doc.tags.filter(t => !tagsToRemove.includes(t));

        // Remove from DOM
        elementsToRemove.forEach(el => el.remove());

        // Immediately persist the change to backend
        const updatedDoc = {
            id: doc.id,
            title: doc.title,
            originalFilename: doc.originalFilename,
            contentType: doc.contentType,
            status: doc.status,
            sizeBytes: doc.sizeBytes,
            tags: doc.tags
        };

        try {
            const updated = await updateDocument(doc.id, updatedDoc);
            doc.tags = updated.tags || doc.tags; // Update from server response
            showMessage(`Tag(s) removed successfully!`, TOAST_TYPES.SUCCESS);
        } catch (err) {
            console.error("Failed to remove tags:", err);
            // Rollback on failure
            doc.tags = originalTags;
            // Re-add elements to DOM
            const tagsContainer = document.querySelector("#tags-container");
            elementsToRemove.forEach(el => tagsContainer.appendChild(el));
            showMessage(`Error removing tags: ${err.message}`, TOAST_TYPES.ERROR);
        }
    })


    home.addEventListener("click", () => {
        window.location.href="index.html";
    })

    // Update a doc
    async function updateDocument(documentId, updatedData) {
        try {
            const updated = await apiRequest(API_CONFIG.ENDPOINTS.DOCUMENT_BY_ID(documentId), {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(updatedData)
            });

            console.log('Document updated:', updated);
            return updated;

        } catch (error) {
            console.error('Error updating document:', error);
            showMessage(`Error updating document: ${error.message}`, TOAST_TYPES.ERROR);
            throw error;
        }
    }

    // Edit button
    editBtn.addEventListener("click", () => {
        editableFields.forEach(id => {
            const el = document.getElementById(id);
            if (el.tagName === "SELECT") {
                el.disabled = false;
            } else {
                el.readOnly = false;
            }
        });        editBtn.classList.add("hidden");
        deleteBtn.classList.add("hidden");
        cancelBtn.classList.remove("hidden");
        saveBtn.classList.remove("hidden");
    });

    // Cancel button
    cancelBtn.addEventListener("click", () => {
        fillFields();
        fields.forEach(id => {
            const el = document.getElementById(id);
            if (el.tagName === "SELECT") {
                el.disabled = true;
            } else {
                el.readOnly = true;
            }
        });        editBtn.classList.remove("hidden");
        deleteBtn.classList.remove("hidden");
        cancelBtn.classList.add("hidden");
        saveBtn.classList.add("hidden");
    });

    saveBtn.addEventListener("click", async () => {
        if (!doc.id) return;

        // Gather updated values from form
        const updatedDoc = {
            id: doc.id,
            title: document.getElementById("title").value,
            originalFilename: doc.filename,
            contentType: doc.contentType,
            status: document.getElementById("status").value,
            summary: document.getElementById("summary").value,
            sizeBytes: doc.sizeBytes,
            createdAt: doc.createdAt,
            updatedAt: new Date().toISOString(),
            checksum: doc.checksum,
            tags: doc.tags || []
        };

        try {
            const updated = await updateDocument(doc.id, updatedDoc);

            // Save banner message and redirect back
            sessionStorage.setItem("bannerMessage", `Document "${updated.title}" was updated successfully!`);
            window.location.href = "index.html";
        } catch (err) {
            console.error("Update failed:", err);
            showMessage(`Error updating document: ${err.message}`, TOAST_TYPES.ERROR);
        }
    });

});
