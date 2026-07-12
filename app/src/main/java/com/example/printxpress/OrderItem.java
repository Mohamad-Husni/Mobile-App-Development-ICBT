package com.example.printxpress;

public class OrderItem {
    private long itemId;
    private long orderId;
    private String firebaseItemId;
    private String productId;
    private String productName;
    private int quantity;
    private double unitPrice;
    private String artworkFileName;

    public OrderItem() {}

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public String getFirebaseItemId() { return firebaseItemId; }
    public void setFirebaseItemId(String firebaseItemId) { this.firebaseItemId = firebaseItemId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getArtworkFileName() { return artworkFileName; }
    public void setArtworkFileName(String artworkFileName) { this.artworkFileName = artworkFileName; }

    public double getSubtotal() { return quantity * unitPrice; }
}
