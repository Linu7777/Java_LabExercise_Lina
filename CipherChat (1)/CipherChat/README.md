# CipherChat

A real-time Java desktop chat application built with:

| Layer      | Technology                              |
|------------|-----------------------------------------|
| UI         | Java Swing (dark theme)                 |
| Network    | WebSocket (Jetty server / Java-WebSocket client) |
| Database   | SQLite via JDBC (auto-created on first run) |
| Serialization | Gson (JSON over WebSocket)           |
| Build      | Maven multi-module                      |

---

## Project Structure

```
CipherChat/
├── pom.xml                  ← root multi-module POM
├── server/
│   └── src/main/java/com/cipher/
│       ├── server/ChatServer.java        ← Jetty entry point (port 8080)
│       ├── handler/ChatWebSocketHandler  ← WebSocket message routing
│       ├── handler/ChatWebSocketServlet  ← Jetty servlet
│       ├── db/DatabaseManager.java       ← SQLite (users + messages)
│       └── model/ChatMessage.java        ← shared message DTO
└── client/
    └── src/main/java/com/cipher/
        ├── client/ChatClient.java        ← entry point (shows login, connects)
        ├── network/ServerConnection.java ← WebSocket client wrapper
        ├── ui/LoginDialog.java           ← login screen
        ├── ui/ChatWindow.java            ← main chat UI
        └── model/ChatMessage.java        ← shared message DTO
```

---

## Quick Start in IntelliJ IDEA

### 1. Open the project
- **File → Open** → select the `CipherChat` folder (the one with the root `pom.xml`)
- IntelliJ will auto-detect the Maven multi-module layout
- Wait for Maven to download dependencies (~30 seconds)

### 2. Run the Server
- Open **Run Configurations** dropdown → select **CipherChat Server**
- Click ▶ Run
- You should see: `CipherChat Server started on :8080`
- The SQLite DB (`cipher_chat.db`) is created automatically in the server working directory

### 3. Run the Client (one or more instances!)
- Open **Run Configurations** dropdown → select **CipherChat Client**
- Click ▶ Run (you can run multiple clients simultaneously)
- A login dialog appears — use any of the seeded accounts:

| Username | Password |
|----------|----------|
| alice    | pass123  |
| bob      | pass123  |
| dana     | pass123  |
| erin     | pass123  |
| frank    | pass123  |

- Or type a **new username** to auto-register

---

## Features

- **Global chat** — messages visible to all connected users
- **Direct messages** — click a user in the right panel to DM them
- **Message history** — last 50 global messages loaded on login
- **Online user list** — live list of connected users
- **Persistent storage** — all messages saved to SQLite
- **Multi-client** — run multiple client instances on one machine
- **Auto-register** — new usernames register automatically

---

## Build Fat JARs (optional)

```bash
mvn clean package -DskipTests

# Run server
java -jar server/target/cipher-server-jar-with-dependencies.jar

# Run client
java -jar client/target/cipher-client-jar-with-dependencies.jar
```

---

## WebSocket Protocol

All messages are JSON `ChatMessage` objects:

```json
// Login
{ "type": "LOGIN", "sender": "alice", "content": "pass123" }

// Send global message
{ "type": "MESSAGE", "sender": "alice", "recipient": null, "content": "Hello!" }

// Send DM
{ "type": "MESSAGE", "sender": "alice", "recipient": "bob", "content": "Hey Bob" }
```

Server responses include `HISTORY`, `USER_LIST`, `ERROR`, and `MESSAGE` types.

---

## Requirements

- Java 17+
- Maven 3.8+
- IntelliJ IDEA (Community or Ultimate)
