const API_BASE = '/api';

document.addEventListener("DOMContentLoaded", () => {
    const stored = localStorage.getItem("selectedDocument");
    if (!stored) return;

    const doc = JSON.parse(stored);
    const fields = ["title", "filename", "content-type", "size", "status", "created", "updated", "checksum", "summary"];

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

    // Make fields read-only initially
    fields.forEach(id => document.getElementById(id).readOnly = true);

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
    const editBtn = document.getElementById("editDoc");
    const deleteBtn = document.getElementById("deleteDoc");
    const cancelBtn = document.getElementById("cancel");
    const saveBtn = document.getElementById("save");
    const home = document.getElementById("home")

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

        } catch (error) {
            console.error('Error deleting document:', error);
            showMessage(`Error deleting document: ${error.message}`, 'danger');
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


    home.addEventListener("click", () => {
        window.location.href="index.html";
    })
    // Edit button
    editBtn.addEventListener("click", () => {
        fields.forEach(id => document.getElementById(id).readOnly = false);
        editBtn.classList.add("hidden");
        deleteBtn.classList.add("hidden");
        cancelBtn.classList.remove("hidden");
        saveBtn.classList.remove("hidden");
    });

    // Cancel button
    cancelBtn.addEventListener("click", () => {
        fillFields();
        fields.forEach(id => document.getElementById(id).readOnly = true);
        editBtn.classList.remove("hidden");
        deleteBtn.classList.remove("hidden");
        cancelBtn.classList.add("hidden");
        saveBtn.classList.add("hidden");
    });
});
