# Elasticsearch Search Integration - Frontend

## Overview

The frontend has been updated to use **Elasticsearch** for document search instead of client-side filtering. This enables full-text search across OCR-extracted PDF content.

---

## What Changed

### **Before** (Client-Side Search)
- Searched only document metadata (title, filename, tags, status)
- All filtering done in JavaScript after loading all documents
- Could NOT search PDF content

### **After** (Elasticsearch Search)
- Searches OCR-extracted text from PDFs
- Searches document titles and content simultaneously
- Falls back to client-side filtering if Elasticsearch fails
- Shows visual indicator when searching OCR text

---

## Files Modified

### 1. **`frontend/paperless-ui/js/utils.js`**

**Added Elasticsearch endpoint:**
```javascript
SEARCH: (query) => `/documents/search?q=${encodeURIComponent(query)}`
```

### 2. **`frontend/paperless-ui/js/main.js`**

**Replaced `filterDocuments()` function:**
- Now async to support API calls
- Uses Elasticsearch when search term is present
- Falls back to client-side filtering on errors
- Shows/hides search indicator

**Key changes:**
```javascript
async function filterDocuments() {
    const searchTerm = searchInput?.value.trim() || '';
    
    if (searchTerm) {
        // Use Elasticsearch for full-text search
        const searchResults = await apiRequest(API_CONFIG.ENDPOINTS.SEARCH(searchTerm));
        documentsToDisplay = mapSearchResultsToDocuments(searchResults);
    } else {
        // No search - show all documents
        documentsToDisplay = allDocuments;
    }
    
    // Still apply client-side filters (content type, size, status)
    applyFilters(documentsToDisplay);
}
```

### 3. **`frontend/paperless-ui/index.html`**

**Added search indicator badge:**
```html
<span id="search-indicator" class="...">Searching OCR text</span>
```

**Updated placeholder text:**
```
"Search documents and content..." (was "Search documents...")
```

---

## How It Works

### **User Types Search Query**

1. User types "invoice" in search bar
2. Debounced search triggers after 300ms
3. Frontend calls: `GET /api/documents/search?q=invoice`

### **Backend Queries Elasticsearch**

4. Backend's `DocumentSearchService` queries Elasticsearch index
5. Elasticsearch searches in both `title` and `content` fields
6. Returns matching documents with relevance scoring

### **Frontend Displays Results**

7. Search results mapped back to full document objects
8. Client-side filters (type, size, status) still applied
9. Documents displayed in table
10. Green "Searching OCR text" badge shown

---

## What You Can Search For

✅ **OCR Extracted Text**
- Invoice numbers: "INV-2024-001"
- Amounts: "1234" or "$1,234.56"
- Dates: "January 2024"
- Customer names: "Acme Corporation"
- Contract terms: "payment terms"
- Any text from PDFs

✅ **Document Metadata**
- Titles: "Quarterly Report"
- Original filenames: "invoice.pdf"

❌ **Cannot Search** (not indexed in Elasticsearch)
- Tags (use metadata filters)
- File sizes
- Upload dates
- Document status

---

## API Endpoints

### **Search Documents** (Elasticsearch)
```
GET /api/documents/search?q=<query>
```

**Example:**
```bash
curl http://localhost:8080/api/documents/search?q=invoice
```

**Response:**
```json
[
  {
    "documentId": "123e4567-...",
    "title": "Invoice 2024",
    "contentSnippet": "INVOICE #001...",
    "totalCharacters": 4532,
    "totalPages": 3,
    "language": "eng",
    "confidence": 89
  }
]
```

### **Alternative Endpoints**

**Search Title Only:**
```
GET /api/documents/search/title?q=invoice
```

**Search Content Only:**
```
GET /api/documents/search/content?q=payment
```

---

## Features

### **Debouncing**
- Search triggers 300ms after user stops typing
- Prevents excessive API calls
- Smoother user experience

### **Fallback Mechanism**
```javascript
try {
    // Try Elasticsearch
    searchResults = await elasticsearchSearch(query);
} catch (error) {
    // Fallback to client-side filtering
    console.warn('Elasticsearch failed, using client-side search');
    clientSideFilter();
}
```

### **Visual Feedback**
- Green badge appears when searching OCR text
- Badge hidden when search box is empty
- Console logs show Elasticsearch activity

### **Combined Filtering**
- Elasticsearch handles text search
- Client-side handles type/size/status filters
- Best of both approaches

---

## Example Searches

### **Invoice Search**
```
Search: "invoice 2024"
→ Finds all documents with "invoice" OR "2024" in content/title
→ Results ranked by relevance
```

### **Customer Search**
```
Search: "Acme Corporation"
→ Finds documents mentioning this customer
→ Searches OCR text from contracts, invoices, etc.
```

### **Amount Search**
```
Search: "5000"
→ Finds documents with this amount
→ Works with various number formats
```

---

## Testing

### **Test Elasticsearch Search**

1. Upload a PDF with text (e.g., invoice)
2. Wait for OCR processing to complete
3. Type a word from the PDF in search bar
4. Should see green "Searching OCR text" badge
5. Document should appear in results

### **Test Fallback**

1. Stop Elasticsearch service
2. Type in search bar
3. Should see fallback warning in console
4. Should still search metadata (title, filename)

### **Console Logs**

When searching, you should see:
```
🔍 Searching with Elasticsearch: invoice
✅ Found 3 documents in Elasticsearch
```

On fallback:
```
Error during search: ...
⚠️ Elasticsearch search failed, falling back to client-side filtering
```

---

## Architecture

```
┌─────────────────┐
│   User Types    │
│   "invoice"     │
└────────┬────────┘
         │ (debounced)
         ▼
┌─────────────────────────┐
│  filterDocuments()      │
│  (JavaScript)           │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  GET /api/documents/    │
│  search?q=invoice       │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  DocumentSearchService  │
│  (Backend)              │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  Elasticsearch          │
│  Query: title OR        │
│         content         │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  Search Results         │
│  (with snippets)        │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  Map to Full Docs       │
│  Apply Filters          │
│  Display in Table       │
└─────────────────────────┘
```

---

## Performance Considerations

### **Pros**
- Fast full-text search (Elasticsearch is optimized for this)
- Searches across all OCR text without loading PDFs
- Scales to thousands of documents
- Relevance ranking

### **Cons**
- Additional API call per search
- Requires Elasticsearch to be running
- Small delay for network request

### **Optimization**
- Debouncing reduces API calls
- Results cached in browser until new search
- Fallback ensures system always works

---

## Troubleshooting

### **Search Returns No Results**

1. Check if OCR has completed for the document
2. Verify Elasticsearch is running: `docker ps | grep elasticsearch`
3. Check if document is indexed: View OCR worker logs
4. Try searching for document title (should always work)

### **Search Is Slow**

1. Check Elasticsearch health: `curl http://localhost:9200/_cluster/health`
2. Check index size: Large indices may be slower
3. Consider adding pagination for large result sets

### **"Searching OCR text" Badge Doesn't Appear**

1. Check browser console for errors
2. Verify `search-indicator` element exists in HTML
3. Check if search term is being trimmed to empty string

---

## Future Enhancements

### **Possible Improvements**
- Highlight matching text in results
- Add fuzzy search (typo tolerance)
- Filter by date ranges
- Search suggestions/autocomplete
- Search history
- Advanced search syntax (AND, OR, NOT)
- Export search results

---

## Related Documentation

- [PDF Upload Flow](./PDF_UPLOAD_FLOW.md)
- [Elasticsearch Configuration](./backend/src/main/resources/application.properties)
- [Search API Documentation](./backend/src/main/java/fhtw/wien/controller/DocumentSearchController.java)
