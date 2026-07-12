package com.example.printxpress;

public class CartItem {
    private Product product;
    private int quantity;
    private String artworkFileName;

    public CartItem(Product product, int quantity, String artworkFileName) {
        this.product = product;
        this.quantity = quantity;
        this.artworkFileName = artworkFileName;
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getArtworkFileName() { return artworkFileName; }
    public void setArtworkFileName(String artworkFileName) { this.artworkFileName = artworkFileName; }

    public double getSubtotal() { return quantity * product.getBasePrice(); }
}
