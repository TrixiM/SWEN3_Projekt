# Rest API Endpoints -  DocumentController

## Endpoints

### 1. Create Document

**Method:** `POST`  
**Endpoint:** `/v1/documents`  
**Consumes:** `multipart/form-data`  
**Produces:** `application/json`

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<DocumentResponse> create(
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title,
        @RequestParam(value = "tags", required = false) List<String> tags
)
```

#### Description
Uploads a new document along with metadata (title, optional tags). The file is validated and converted into a `Document` entity, which is passed to `DocumentService.create()`.

#### Parameters
| Name | Type | Required | Description |
|------|------|---------|--------------|
| file | MultipartFile | X       | The PDF or file to upload |
| title | String | X       | The document title |
| tags | List<String> |         | Optional document tags |

#### Responses
| Status | Description |
|--------|--------------|
| 201 Created | Document successfully uploaded |
| 400 Bad Request | File is empty or invalid request |
| 500 Internal Server Error | Data access or service failure |

---

### 2. **Update Document**

**Method:** `PUT`  
**Endpoint:** `/v1/documents/{id}`  
**Consumes:** `application/json`

```java
@PutMapping("{id}")
public ResponseEntity<DocumentResponse> update(@PathVariable UUID id, @RequestBody Document updateRequest)
```

#### Description
Updates editable fields of an existing document. Non-editable or null fields are ignored. The service updates timestamps automatically.

#### Parameters
| Name | Type | Required | Description |
|------|------|----------|--------------|
| id | UUID | X        | Unique document identifier |
| updateRequest | Document | X        | JSON body with updated fields |

#### Responses
| Status | Description |
|--------|--------------|
| 200 OK | Document successfully updated |
| 404 Not Found | Document ID does not exist |
| 400 Bad Request | Invalid update payload |

---

### 3. **Get All Documents**

**Method:** `GET`  
**Endpoint:** `/v1/documents`  
**Produces:** `application/json`

```java
@GetMapping
public List<DocumentResponse> getAll()
```

#### Description
Retrieves all stored documents, mapped into `DocumentResponse` DTOs. Delegates to `service.getAll()`.

#### Responses
| Status | Description |
|--------|--------------|
| 200 OK | List of documents returned |
| 500 Internal Server Error | Repository access failure |

---

### 4. **Get Document by ID**

**Method:** `GET`  
**Endpoint:** `/v1/documents/{id}`  
**Produces:** `application/json`

```java
@GetMapping("{id}")
public DocumentResponse get(@PathVariable UUID id)
```

#### Description
Fetches a single document’s metadata using its ID.

#### Responses
| Status | Description |
|--------|--------------|
| 200 OK | Document found |
| 404 Not Found | No document with that ID |

---

### 5. **Get Document Content (PDF)**

**Method:** `GET`  
**Endpoint:** `/v1/documents/{id}/content`  
**Produces:** `application/pdf`

```java
@GetMapping("{id}/content")
public ResponseEntity<byte[]> getContent(@PathVariable UUID id)
```

#### Description
Returns the binary PDF data of the specified document with appropriate headers (`Content-Type: application/pdf`).

#### Responses
| Status | Description |
|--------|--------------|
| 200 OK | Returns PDF binary content |
| 404 Not Found | No PDF data found |

---

### 6. **Render Specific PDF Page**

**Method:** `GET`  
**Endpoint:** `/v1/documents/{id}/pages/{pageNumber}`  
**Query Params:** `scale` (optional, default = 1.5)  
**Produces:** `image/png`

```java
@GetMapping("{id}/pages/{pageNumber}")
public ResponseEntity<byte[]> renderPage(@PathVariable UUID id, @PathVariable int pageNumber, @RequestParam(defaultValue = "1.5") float scale)
```

#### Description
Renders a specific page from the document as a PNG image.  
Delegates rendering logic to `service.renderPdfPage(id, pageNumber, scale)`.

#### Responses
| Status | Description |
|--------|--------------|
| 200 OK | Returns rendered PNG image bytes |
| 400 Bad Request | Invalid page number |
| 404 Not Found | Document or page not found |

---

### 7. **Get Page Count**

**Method:** `GET`  
**Endpoint:** `/v1/documents/{id}/pages/count`  
**Produces:** `application/json`

```java
@GetMapping("{id}/pages/count")
public ResponseEntity<Integer> getPageCount(@PathVariable UUID id)
```

#### Description
Returns the total number of pages in the specified PDF document.

#### Responses
| Status | Description |
|--------|--------------|
| 200 OK | Page count returned |
| 404 Not Found | Document not found |

---

### 8. **Delete Document**

**Method:** `DELETE`  
**Endpoint:** `/v1/documents/{id}`

```java
@DeleteMapping("{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable UUID id)
```

#### Description
Deletes the document with the given ID. The service handles existence checks and deletion logic.

#### Responses
| Status | Description |
|--------|--------------|
| 204 No Content | Document successfully deleted |
| 404 Not Found | Document not found |