package com.example.printxpress;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClick(Order order, int position);
    }

    public interface OnRescheduleClickListener {
        void onRescheduleClick(Order order, int position);
    }

    private final List<Order> orders;
    private final OnCancelClickListener cancelListener;
    private final OnRescheduleClickListener rescheduleListener;

    public OrderAdapter(List<Order> orders, OnCancelClickListener cancelListener,
                        OnRescheduleClickListener rescheduleListener) {
        this.orders = orders;
        this.cancelListener = cancelListener;
        this.rescheduleListener = rescheduleListener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_card, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder h, int position) {
        Order order = orders.get(position);

        List<OrderItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (OrderItem item : items) {
                sb.append(item.getProductId()).append(" x").append(item.getQuantity()).append("\n");
            }
            h.tvProductName.setText(sb.toString().trim());
        } else {
            h.tvProductName.setText("Order #" + order.getLocalId());
        }

        int itemCount = (items != null) ? items.size() : 0;
        h.tvOrderDetails.setText(itemCount + " item(s)  •  " + order.getDeliveryType());

        String artwork = (items != null && !items.isEmpty()
                && items.get(0).getArtworkFileName() != null
                && !items.get(0).getArtworkFileName().isEmpty())
                ? items.get(0).getArtworkFileName() : "None";
        h.tvArtwork.setText("Artwork: " + artwork);

        h.tvTotalPrice.setText(String.format("Total: $%.2f", order.getTotalPrice()));
        h.tvStatus.setText(order.getStatus());

        String date = order.getOrderDate();
        h.tvTimestamp.setText(date != null && date.length() >= 10 ? date.substring(0, 10) : "");

        applyStatusStyle(h.tvStatus, order.getStatus());

        boolean isProcessing = DBHelper.STATUS_PROCESSING.equals(order.getStatus());
        h.btnCancelOrder.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
        h.btnReschedule.setVisibility(isProcessing ? View.VISIBLE : View.GONE);

        h.btnCancelOrder.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) cancelListener.onCancelClick(order, pos);
        });
        h.btnReschedule.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) rescheduleListener.onRescheduleClick(order, pos);
        });
    }

    private void applyStatusStyle(TextView tv, String status) {
        if (status == null) return;
        switch (status) {
            case DBHelper.STATUS_PROCESSING:
                tv.setBackgroundResource(R.drawable.bg_status_processing);
                tv.setTextColor(Color.parseColor("#B7791F")); break;
            case DBHelper.STATUS_PRINTING:
                tv.setBackgroundResource(R.drawable.bg_status_chip);
                tv.setTextColor(Color.parseColor("#0284C7")); break;
            case DBHelper.STATUS_READY:
                tv.setBackgroundResource(R.drawable.bg_status_ready);
                tv.setTextColor(Color.parseColor("#2E7D32")); break;
            case DBHelper.STATUS_CANCELLED:
                tv.setBackgroundResource(R.drawable.bg_cancel_button);
                tv.setTextColor(Color.parseColor("#C62828")); break;
            default:
                tv.setBackgroundResource(R.drawable.bg_status_chip);
                tv.setTextColor(Color.parseColor("#5F7371"));
        }
    }

    @Override
    public int getItemCount() { return orders.size(); }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvOrderDetails, tvArtwork, tvTotalPrice, tvStatus, tvTimestamp;
        MaterialButton btnCancelOrder, btnReschedule;

        OrderViewHolder(@NonNull View v) {
            super(v);
            tvProductName  = v.findViewById(R.id.tvProductName);
            tvOrderDetails = v.findViewById(R.id.tvOrderDetails);
            tvArtwork      = v.findViewById(R.id.tvArtwork);
            tvTotalPrice   = v.findViewById(R.id.tvTotalPrice);
            tvStatus       = v.findViewById(R.id.tvStatus);
            tvTimestamp    = v.findViewById(R.id.tvTimestamp);
            btnCancelOrder = v.findViewById(R.id.btnCancelOrder);
            btnReschedule  = v.findViewById(R.id.btnReschedule);
        }
    }
}
