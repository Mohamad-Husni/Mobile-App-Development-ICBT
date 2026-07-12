package com.example.printxpress;

import java.util.List;

public class Order {

    private long localId;
    private String firebaseOrderId;
    private String customerId;
    private String orderDate;
    private String deliveryType;
    private String status;
    private double totalPrice;
    private List<OrderItem> items;

    public Order() {}

    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    public String getFirebaseOrderId() { return firebaseOrderId; }
    public void setFirebaseOrderId(String firebaseOrderId) { this.firebaseOrderId = firebaseOrderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
