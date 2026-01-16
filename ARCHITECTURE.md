docker compose up
│
├── postgres
│   ├── database: paperless
│   ├── user: backend_user
│   ├── user: batch_user
│   └── tables
│
├── backend
│   └── uses backend_user
│
└── document-access-batch-service
└── uses batch_user
