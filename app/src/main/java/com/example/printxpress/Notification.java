package com.example.printxpress;

public class Notification {
    private long notifId;
    private String customerId;
    private String type;
    private String message;
    private boolean isRead;
    private String createdAt;

    public Notification() {}

    public long getNotifId() { return notifId; }
    public void setNotifId(long notifId) { this.notifId = notifId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
