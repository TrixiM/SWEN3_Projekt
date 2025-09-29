import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { DocumentService, DocumentResponse } from '../../services/document.service';

@Component({
  selector: 'app-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './detail.component.html',
  styleUrls: ['./detail.component.scss']
})
export class DetailComponent implements OnInit {
  document?: DocumentResponse;
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private docService: DocumentService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.docService.getDocument(id).subscribe({
        next: (doc) => {
          this.document = doc;
          this.loading = false;
        },
        error: (err) => {
          console.error(err);
          this.error = 'Document not found';
          this.loading = false;
        }
      });
    }
  }
}
