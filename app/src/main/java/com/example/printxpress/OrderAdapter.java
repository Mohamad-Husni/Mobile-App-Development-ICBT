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

        h.tvTotalPrice.setText(String.format("Total: LKR %.2f", order.getTotalPrice()));
        h.tvStatus.setText(order.getStatus());

        String date = order.getOrderDate();
        h.tvTimestamp.setText(date != null && date.length() >= 10 ? date.substring(0, 10) : "");

        applyStatusStyle(h.tvStatus, order.getStatus());
        applyStepperStyle(h, order.getStatus());

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

    private void applyStepperStyle(OrderViewHolder h, String status) {
        if (status == null) return;
        int dimColor    = Color.parseColor("#BBBBBB");
        int activeAmber = Color.parseColor("#B7791F");
        int activeBlue  = Color.parseColor("#0284C7");
        int activeGreen = Color.parseColor("#2E7D32");

        h.stepProcessing.setBackgroundResource(R.drawable.bg_status_chip);
        h.stepProcessing.setTextColor(dimColor);
        h.stepPrinting.setBackgroundResource(R.drawable.bg_status_chip);
        h.stepPrinting.setTextColor(dimColor);
        h.stepReady.setBackgroundResource(R.drawable.bg_status_chip);
        h.stepReady.setTextColor(dimColor);

        switch (status) {
            case DBHelper.STATUS_PROCESSING:
                h.stepProcessing.setBackgroundResource(R.drawable.bg_status_processing);
                h.stepProcessing.setTextColor(activeAmber);
                break;
            case DBHelper.STATUS_PRINTING:
                h.stepProcessing.setBackgroundResource(R.drawable.bg_status_processing);
                h.stepProcessing.setTextColor(activeAmber);
                h.stepPrinting.setBackgroundResource(R.drawable.bg_status_chip);
                h.stepPrinting.setTextColor(activeBlue);
                break;
            case DBHelper.STATUS_READY:
                h.stepProcessing.setBackgroundResource(R.drawable.bg_status_processing);
                h.stepProcessing.setTextColor(activeAmber);
                h.stepPrinting.setBackgroundResource(R.drawable.bg_status_chip);
                h.stepPrinting.setTextColor(activeBlue);
                h.stepReady.setBackgroundResource(R.drawable.bg_status_ready);
                h.stepReady.setTextColor(activeGreen);
                break;
        }
    }

    @Override
    public int getItemCount() { return orders.size(); }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvOrderDetails, tvArtwork, tvTotalPrice, tvStatus, tvTimestamp;
        TextView stepProcessing, stepPrinting, stepReady;
        MaterialButton btnCancelOrder, btnReschedule;

        OrderViewHolder(@NonNull View v) {
            super(v);
            tvProductName  = v.findViewById(R.id.tvProductName);
            tvOrderDetails = v.findViewById(R.id.tvOrderDetails);
            tvArtwork      = v.findViewById(R.id.tvArtwork);
            tvTotalPrice   = v.findViewById(R.id.tvTotalPrice);
            tvStatus       = v.findViewById(R.id.tvStatus);
            tvTimestamp    = v.findViewById(R.id.tvTimestamp);
            stepProcessing = v.findViewById(R.id.stepProcessing);
            stepPrinting   = v.findViewById(R.id.stepPrinting);
            stepReady      = v.findViewById(R.id.stepReady);
            btnCancelOrder = v.findViewById(R.id.btnCancelOrder);
            btnReschedule  = v.findViewById(R.id.btnReschedule);
        }
    }
}
