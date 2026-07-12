package com.example.printxpress;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    private final List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_card, parent, false);
        return new NotifViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder h, int position) {
        Notification n = notifications.get(position);
        h.tvMessage.setText(n.getMessage());
        h.tvTime.setText(n.getCreatedAt() != null ? n.getCreatedAt() : "");
        h.viewDot.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() { return notifications.size(); }

    static class NotifViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        View viewDot;

        NotifViewHolder(@NonNull View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvNotifMessage);
            tvTime    = v.findViewById(R.id.tvNotifTime);
            viewDot   = v.findViewById(R.id.viewUnreadDot);
        }
    }
}
