# DevPilot — AI-Powered GitHub Code Assistant

DevPilot is an AI-powered developer assistant that connects to a user's GitHub account, analyzes GitHub repositories, indexes source code into PostgreSQL with **pgvector**, and enables developers to ask questions and generate architecture diagrams using natural language.

---

## 🌟 Key Features

1. **GitHub OAuth 2.0 Integration**:
   - Securely authenticate users and retrieve repositories via GitHub REST APIs.
2. **Repository Ingestion & AST-Aware Chunking**:
   - Scans full repository trees.
   - Filters out non-source artifacts (`node_modules`, `target`, `build`, binaries).
   - Dedicated parsers for **Java**, **TypeScript/JavaScript**, **Python**, and generic multi-language code.
   - Captures metadata: file path, symbol names, class names, start/end line bounds, commit SHA.
3. **pgvector Embeddings & Hybrid RAG Engine**:
   - Dense embeddings (via Spring AI / OpenAI compatible APIs).
   - Cosine distance vector similarity query combined with keyword matching.
   - Answers questions strictly based on repository source snippets.
   - Cites exact line numbers (`src/main/java/AuthController.java:20-65`) and links directly to code in the UI.
4. **Interactive Architecture Generation**:
   - Detects frontend, controllers/APIs, business services, database entities, and external dependencies.
   - Produces dynamic **Mermaid.js** diagrams rendered directly in the workspace.
5. **Code Flow Analysis**:
   - Traces requests from entrypoint controllers down to persistence layers and third-party APIs.
6. **Dual-Pane Workspace UI**:
   - File tree explorer on the left, ChatGPT-like conversation and interactive architecture diagrams on the right.

---

## 🏗️ Architecture & Technology Stack

```
devpilot/
├── backend/            # Java 21 + Spring Boot 3.3.4
│   ├── src/main/java/com/devpilot/
│   │   ├── ai/         # LLM Client abstractions & prompt engineering
│   │   ├── architecture/ # Architecture detection & Mermaid diagram engine
│   │   ├── config/     # Security, CORS, Async, RestClient configs
│   │   ├── controller/ # REST API endpoints
│   │   ├── dto/        # Request/Response data transfer objects
│   │   ├── embedding/  # Vector generation & pgvector serialization
│   │   ├── entity/     # JPA entities (User, Repository, RepositoryFile, CodeChunk, ChatSession)
│   │   ├── exception/  # Centralized exception handling
│   │   ├── github/     # GitHub REST API client
│   │   ├── parser/     # AST & syntax-aware code chunkers (Java, JS/TS, Python, Generic)
│   │   ├── rag/        # RAG pipeline orchestration
│   │   ├── repository/ # Spring Data JPA & pgvector repositories
│   │   ├── security/   # OAuth2 UserService, JWT TokenProvider, filters
│   │   └── service/    # Ingestion pipeline & repository management
│   ├── pom.xml
│   └── Dockerfile
│
├── client/             # React 18 + Vite + Modern Vanilla CSS
│   ├── src/
│   │   ├── components/ # MermaidViewer, FileTree, ChatInterface, Navbar
│   │   ├── context/    # AuthContext with token persistence
│   │   ├── pages/      # LoginPage, DashboardPage, WorkspacePage, OAuthCallback
│   │   ├── services/   # Axios client with JWT interceptors
│   │   ├── App.jsx
│   │   └── index.css   # Dark modern glassmorphism design system
│   ├── package.json
│   └── Dockerfile
│
├── docker-compose.yml  # PostgreSQL + pgvector, Backend, Client
├── init-pgvector.sql   # SQL extension initialization
└── .env.example
```

---

## 🚀 Quick Start with Docker Compose

Run the entire stack with a single command:

```bash
# 1. Copy environment variables template
cp .env.example .env

# 2. Start PostgreSQL with pgvector, Spring Boot backend, and React client
docker compose up --build
```

- **Frontend Application**: `http://localhost` or `http://localhost:5173`
- **Backend REST API**: `http://localhost:8080`
- **PostgreSQL + pgvector**: `localhost:5432`

---

## 💻 Local Development Setup

### Prerequisites
- **Java 21**
- **Maven 3.9+**
- **Node.js 20+ & npm**
- **PostgreSQL 16** with `pgvector` extension

### 1. Database Setup
```sql
CREATE DATABASE devpilot;
\c devpilot
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

### 2. GitHub OAuth App Setup
1. Go to [GitHub Developer Settings -> OAuth Apps](https://github.com/settings/developers).
2. Click **New OAuth App**:
   - **Application name**: `DevPilot Local`
   - **Homepage URL**: `http://localhost:5173`
   - **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`
3. Generate Client Secret and configure in `.env` or `application.yml`:
   ```bash
   export GITHUB_CLIENT_ID="your_client_id"
   export GITHUB_CLIENT_SECRET="your_client_secret"
   export AI_API_KEY="your_llm_key"
   ```

### 3. Run Backend
```bash
cd devpilot/backend
mvn spring-boot:run
```

### 4. Run Frontend
```bash
cd devpilot/client
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

> **Tip for Offline / Local Demo Testing**:
> If you don't have GitHub OAuth credentials configured immediately, click **"Explore Demo Workspace"** on the login page. This creates an authenticated development session and lets you interact with indexed repositories without requiring external keys.

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/auth/me` | Get authenticated user profile |
| `POST` | `/api/auth/demo-login` | Demo session for local testing |
| `GET` | `/api/repositories` | List analyzed and synced repositories |
| `POST` | `/api/repositories/sync` | Sync authorized repositories from GitHub |
| `GET` | `/api/repositories/{id}` | Get repository details |
| `POST` | `/api/repositories/{id}/analyze` | Trigger background ingestion & vector indexing |
| `GET` | `/api/repositories/{id}/status` | Poll repository indexing progress |
| `GET` | `/api/repositories/{id}/files` | Retrieve hierarchical file tree |
| `GET` | `/api/repositories/{id}/file-content?path=...` | Retrieve raw source code content |
| `POST` | `/api/repositories/{id}/chat` | Query AI assistant using hybrid RAG |
| `POST` | `/api/repositories/{id}/architecture` | Generate Mermaid diagram & component breakdown |
| `GET` | `/api/health` | Health check endpoint |

---

## 🔍 RAG Pipeline & Citation Mechanics

```
User Query ("How does authentication work?")
      │
      ▼
1. Embed Query (Spring AI / text-embedding-3-small)
      │
      ▼
2. Vector Similarity Search (PostgreSQL pgvector cosine distance: c.embedding <=> query_vec)
   + Keyword fallback matching
      │
      ▼
3. Retrieve Top-K Code Chunks with Line Numbers & Class/Symbol metadata
      │
      ▼
4. Context Construction: Fenced code blocks with file headers
      │
      ▼
5. LLM Synthesis: Repository-aware prompt enforcing strict grounding
      │
      ▼
6. Response: Detailed technical explanation + Clickable source file citations
```

---

## 🛡️ Security Best Practices

- **Zero Secret Leakage**: OAuth client secrets and LLM API keys are exclusively handled on the backend.
- **Stateless JWT**: Sessions use cryptographically signed HMAC-SHA256 JWT tokens.
- **Repository Isolation**: All database queries check `user_id = principal.id` to guarantee multi-tenant repository privacy.
- **CORS Filtering**: Strict origin validation restricting cross-site requests.
