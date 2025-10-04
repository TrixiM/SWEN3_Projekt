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
