
    document.addEventListener("DOMContentLoaded", () => {
    const stored = localStorage.getItem("selectedDocument");
    if (!stored) return;

    const doc = JSON.parse(stored);

    // Fill the fields
    document.getElementById("title").textContent = doc.title;
    document.getElementById("filename").textContent = doc.originalFilename;
    document.getElementById("content-type").textContent = doc.contentType;
    document.getElementById("size").textContent = (doc.sizeBytes / (1024*1024)).toFixed(2) + " MB";
    document.getElementById("status").textContent = doc.status;
    document.getElementById("created").textContent = new Date(doc.createdAt).toLocaleString();
    document.getElementById("updated").textContent = new Date(doc.updatedAt).toLocaleString();
    document.getElementById("checksum").textContent = doc.checksum || "—";
    document.getElementById("summary").textContent = doc.summary || "No summary available.";

    // Example tags (if available)
    const tagsContainer = document.querySelector("#tags-container");
    if (tagsContainer && doc.tags) {
    tagsContainer.innerHTML = doc.tags.map(tag =>
    `<span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary/10 text-primary dark:bg-primary/20 dark:text-primary">${tag}</span>`
    ).join("");
}
});
