package com.campus.courier.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campus.courier.R;
import com.google.gson.JsonObject;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private static final String[] STATUS_TEXT = {
            "\u5f85\u63a5\u5355",
            "\u5df2\u63a5\u5355",
            "\u53d6\u4ef6\u4e2d",
            "\u914d\u9001\u4e2d",
            "\u5df2\u5b8c\u6210",
            "\u5df2\u53d6\u6d88",
            "\u5f02\u5e38"
    };

    private final List<JsonObject> data;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(JsonObject order);
    }

    public OrderAdapter(List<JsonObject> data, OnItemClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject order = data.get(position);

        holder.tvOrderNo.setText("\u5355\u53f7\uff1a" + order.get("orderNo").getAsString());
        holder.tvTracking.setText("\u5feb\u9012\uff1a" + order.get("trackingNo").getAsString());
        holder.tvPickup.setText("\u53d6\u4ef6\uff1a" + order.get("pickupAddress").getAsString());
        holder.tvDelivery.setText("\u9001\u8fbe\uff1a" + order.get("deliveryAddress").getAsString());
        holder.tvFee.setText("\u00a5" + order.get("fee").getAsString());

        int status = order.get("status").getAsInt();
        holder.tvStatus.setText(STATUS_TEXT[Math.min(status, STATUS_TEXT.length - 1)]);
        holder.tvStatus.setTextColor(statusColor(status));

        holder.itemView.setOnClickListener(v -> listener.onClick(order));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private int statusColor(int status) {
        switch (status) {
            case 0:
                return 0xFF2196F3;
            case 1:
                return 0xFFFF9800;
            case 2:
                return 0xFF9C27B0;
            case 3:
                return 0xFF009688;
            case 4:
                return 0xFF4CAF50;
            case 5:
                return 0xFF9E9E9E;
            default:
                return 0xFFF44336;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvOrderNo;
        final TextView tvTracking;
        final TextView tvPickup;
        final TextView tvDelivery;
        final TextView tvFee;
        final TextView tvStatus;

        ViewHolder(View view) {
            super(view);
            tvOrderNo = view.findViewById(R.id.tvOrderNo);
            tvTracking = view.findViewById(R.id.tvTracking);
            tvPickup = view.findViewById(R.id.tvPickup);
            tvDelivery = view.findViewById(R.id.tvDelivery);
            tvFee = view.findViewById(R.id.tvFee);
            tvStatus = view.findViewById(R.id.tvStatus);
        }
    }
}
