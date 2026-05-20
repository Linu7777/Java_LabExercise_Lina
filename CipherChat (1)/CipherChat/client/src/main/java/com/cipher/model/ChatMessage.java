package com.cipher.model;

/**
 * Mirror of server ChatMessage — shared DTO over the wire.
 */
public class ChatMessage {

    public enum Type { LOGIN, MESSAGE, HISTORY, USER_LIST, ERROR }

    private Type   type;
    private String sender;
    private String recipient;
    private String content;
    private String timestamp;
    private long   id;

    public ChatMessage() {}

    public ChatMessage(Type type, String sender, String recipient, String content) {
        this.type      = type;
        this.sender    = sender;
        this.recipient = recipient;
        this.content   = content;
    }

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
}
