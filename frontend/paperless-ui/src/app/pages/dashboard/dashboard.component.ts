import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DocumentService, DocumentResponse } from '../../services/document.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  documents: DocumentResponse[] = [];
  loading = true;
  error = '';

  constructor(private docService: DocumentService) {}

  ngOnInit(): void {
    this.docService.getDocuments().subscribe({
      next: (docs) => {
        this.documents = docs;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'Encountered error when loading documents';
        this.loading = false;
      }
    });
  }
}
