package com.cipher.model;

/**
 * Shared message envelope sent over WebSocket as JSON.
 * type: "LOGIN" | "MESSAGE" | "HISTORY" | "USER_LIST" | "ERROR"
 */
public class ChatMessage {

    public enum Type { LOGIN, MESSAGE, HISTORY, USER_LIST, ERROR }

    private Type   type;
    private String sender;
    private String recipient;   // null = broadcast
    private String content;
    private String timestamp;
    private long   id;

    public ChatMessage() {}

    public ChatMessage(Type type, String sender, String recipient, String content, String timestamp) {
        this.type      = type;
        this.sender    = sender;
        this.recipient = recipient;
        this.content   = content;
        this.timestamp = timestamp;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public Type   getType()      { return type; }
    public void   setType(Type t){ this.type = t; }

    public String getSender()    { return sender; }
    public void   setSender(String s){ this.sender = s; }

    public String getRecipient() { return recipient; }
    public void   setRecipient(String r){ this.recipient = r; }

    public String getContent()   { return content; }
    public void   setContent(String c){ this.content = c; }

    public String getTimestamp() { return timestamp; }
    public void   setTimestamp(String t){ this.timestamp = t; }

    public long   getId()        { return id; }
    public void   setId(long id) { this.id = id; }

    @Override
    public String toString() {
        return "ChatMessage{type=" + type + ", sender='" + sender + "', recipient='" + recipient + "', content='" + content + "'}";
    }
}
