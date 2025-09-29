import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DocumentResponse {
  id: string;
  title: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  bucket: string;
  objectKey: string;
  storageUri: string;
  checksumSha256: string;
  status: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private api = '/api/v1/documents'; // nginx will proxy /api → backend

  constructor(private http: HttpClient) {}

  getDocuments(): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(this.api);
  }

  getDocument(id: string): Observable<DocumentResponse> {
    return this.http.get<DocumentResponse>(`${this.api}/${id}`);
  }

  createDocument(doc: any): Observable<DocumentResponse> {
    return this.http.post<DocumentResponse>(this.api, doc);
  }

  deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
