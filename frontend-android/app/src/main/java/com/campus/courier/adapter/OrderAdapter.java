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
            "待接单", "已接单", "取件中", "已完成", "已取消", "异常"
    };

    private final List<JsonObject> data;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(JsonObject order);
    }

    public OrderAdapter(List<JsonObject> data, OnItemClickListener listener) {
        this.data     = data;
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
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        JsonObject o = data.get(position);

        h.tvOrderNo.setText("单号：" + o.get("orderNo").getAsString());
        h.tvTracking.setText("快递：" + o.get("trackingNo").getAsString());
        h.tvPickup.setText("取件：" + o.get("pickupAddress").getAsString());
        h.tvDelivery.setText("送达：" + o.get("deliveryAddress").getAsString());
        h.tvFee.setText("¥" + o.get("fee").getAsString());

        int status = o.get("status").getAsInt();
        h.tvStatus.setText(STATUS_TEXT[Math.min(status, STATUS_TEXT.length - 1)]);

        // 状态颜色
        int color;
        switch (status) {
            case 0: color = 0xFF2196F3; break;  // 蓝：待接单
            case 1: color = 0xFFFF9800; break;  // 橙：已接单
            case 2: color = 0xFF9C27B0; break;  // 紫：取件中
            case 3: color = 0xFF4CAF50; break;  // 绿：已完成
            case 4: color = 0xFF9E9E9E; break;  // 灰：已取消
            default: color = 0xFFF44336;         // 红：异常
        }
        h.tvStatus.setTextColor(color);

        h.itemView.setOnClickListener(v -> listener.onClick(o));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNo, tvTracking, tvPickup, tvDelivery, tvFee, tvStatus;

        ViewHolder(View v) {
            super(v);
            tvOrderNo  = v.findViewById(R.id.tvOrderNo);
            tvTracking = v.findViewById(R.id.tvTracking);
            tvPickup   = v.findViewById(R.id.tvPickup);
            tvDelivery = v.findViewById(R.id.tvDelivery);
            tvFee      = v.findViewById(R.id.tvFee);
            tvStatus   = v.findViewById(R.id.tvStatus);
        }
    }
}
