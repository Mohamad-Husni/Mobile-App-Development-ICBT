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

public class OperatorOrderAdapter extends RecyclerView.Adapter<OperatorOrderAdapter.OpOrderViewHolder> {

    public interface OnUpdateStatusListener {
        void onUpdateStatus(Order order, int position);
    }

    private final List<Order> orders;
    private final OnUpdateStatusListener listener;

    public OperatorOrderAdapter(List<Order> orders, OnUpdateStatusListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OpOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_operator_order_card, parent, false);
        return new OpOrderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OpOrderViewHolder h, int position) {
        Order order = orders.get(position);

        String fid = order.getFirebaseOrderId();
        h.tvOrderId.setText("Order #" + (fid != null && fid.length() >= 6
                ? fid.substring(0, 6).toUpperCase() : String.valueOf(order.getLocalId())));

        h.tvCustomerEmail.setText(order.getCustomerId());

        int itemCount = (order.getItems() != null) ? order.getItems().size() : 0;
        h.tvItemsSummary.setText(itemCount + " item(s)  •  " + order.getDeliveryType());
        h.tvOpTotal.setText(String.format("$%.2f", order.getTotalPrice()));

        h.tvOpStatus.setText(order.getStatus());
        applyStatusStyle(h.tvOpStatus, order.getStatus());

        boolean canUpdate = !DBHelper.STATUS_READY.equals(order.getStatus())
                && !DBHelper.STATUS_CANCELLED.equals(order.getStatus());
        h.btnUpdateStatus.setVisibility(canUpdate ? View.VISIBLE : View.GONE);
        h.btnUpdateStatus.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onUpdateStatus(order, pos);
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
            default:
                tv.setBackgroundResource(R.drawable.bg_status_chip);
                tv.setTextColor(Color.parseColor("#5F7371"));
        }
    }

    public void updateStatus(int position, String newStatus) {
        if (position >= 0 && position < orders.size()) {
            orders.get(position).setStatus(newStatus);
            notifyItemChanged(position);
        }
    }

    @Override
    public int getItemCount() { return orders.size(); }

    static class OpOrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOpStatus, tvCustomerEmail, tvItemsSummary, tvOpTotal;
        MaterialButton btnUpdateStatus;

        OpOrderViewHolder(@NonNull View v) {
            super(v);
            tvOrderId      = v.findViewById(R.id.tvOrderId);
            tvOpStatus     = v.findViewById(R.id.tvOpStatus);
            tvCustomerEmail = v.findViewById(R.id.tvCustomerEmail);
            tvItemsSummary = v.findViewById(R.id.tvItemsSummary);
            tvOpTotal      = v.findViewById(R.id.tvOpTotal);
            btnUpdateStatus = v.findViewById(R.id.btnUpdateStatus);
        }
    }
}
