# qidian-crawler

Spring Boot + Vue3 demo project for crawling **publicly accessible** Qidian novel metadata/catalog and **free** chapter content only.

## Compliance notice

- This project is for learning/research.
- Only crawl publicly accessible pages.
- Do not bypass login/paywalls, do not scrape paid chapters.
- Use low frequency and respect the website's ToS/robots.

## Repo structure

- `backend/`: Spring Boot (REST API + crawler)
- `frontend/`: Vue3 (simple UI)

## 1) Create database

Run this once in MySQL:

```sql
CREATE DATABASE IF NOT EXISTS qidian_crawler
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

Or use the provided script: `sql/create_database.sql`.

## 2) Configure backend DB password (do NOT commit passwords)

Set environment variable:

Windows PowerShell:

```powershell
$env:QIDIAN_DB_PASSWORD = "<your_password>"
```

Git Bash:

```bash
export QIDIAN_DB_PASSWORD="<your_password>"
```

## 3) Start backend

```bash
# from repo root
cd backend
mvn spring-boot:run
```

Backend default: `http://localhost:8080`

## 4) Start frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend default: `http://localhost:5173`

## API quick test

- POST `http://localhost:8080/api/crawl/novel`

  - body: `{ "bookId": "123456" }` or `{ "url": "https://www.qidian.com/book/123456/" }`

- GET `http://localhost:8080/api/novels`
- GET `http://localhost:8080/api/novels/{id}`
- GET `http://localhost:8080/api/novels/{id}/chapters`
