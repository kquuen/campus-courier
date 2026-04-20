package com.campus.courier.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

/**
 * 现代化支付对话框Fragment
 * 替换原来的简单AlertDialog，提供更好的用户体验
 */
public class PaymentDialogFragment extends DialogFragment {

    public interface PaymentCallback {
        void onPaymentSuccess();
        void onPaymentFailure(String error);
        void onPaymentCancelled();
    }

    private static final String ARG_ORDER_ID = "order_id";
    private static final String ARG_ORDER_FEE = "order_fee";

    private long orderId;
    private double orderFee;
    private PaymentCallback callback;
    
    private LinearLayout paymentOptionsLayout;
    private TextView tvOrderFee;
    private TextView tvSelectedMethod;
    private Button btnConfirmPayment;
    private ProgressBar progressBar;
    private View selectedOptionView;
    
    private int selectedPayType = 1; // 默认微信支付
    
    public PaymentDialogFragment() {
        // 必须的空构造函数
    }

    public static PaymentDialogFragment newInstance(long orderId, double orderFee) {
        PaymentDialogFragment fragment = new PaymentDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ORDER_ID, orderId);
        args.putDouble(ARG_ORDER_FEE, orderFee);
        fragment.setArguments(args);
        return fragment;
    }

    public void setPaymentCallback(PaymentCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getLong(ARG_ORDER_ID);
            orderFee = getArguments().getDouble(ARG_ORDER_FEE);
        }
        // 设置对话框样式
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Dialog_CampusCourier);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.Dialog_CampusCourier);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_payment, null);
        builder.setView(view);
        
        initViews(view);
        setupPaymentOptions();
        setupConfirmButton();
        
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            // 设置对话框宽度和动画
            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setWindowAnimations(R.style.DialogAnimation_CampusCourier);
        }
    }

    private void initViews(View view) {
        paymentOptionsLayout = view.findViewById(R.id.paymentOptionsLayout);
        tvOrderFee = view.findViewById(R.id.tvOrderFee);
        tvSelectedMethod = view.findViewById(R.id.tvSelectedMethod);
        btnConfirmPayment = view.findViewById(R.id.btnConfirmPayment);
        progressBar = view.findViewById(R.id.progressBar);
        
        TextView tvOrderId = view.findViewById(R.id.tvOrderId);
        tvOrderId.setText(String.format("订单号: %d", orderId));
        tvOrderFee.setText(String.format("¥%.2f", orderFee));
        
        // 初始选择微信支付
        tvSelectedMethod.setText("微信支付");
        selectedPayType = 1;
    }

    private void setupPaymentOptions() {
        // 清除现有选项
        paymentOptionsLayout.removeAllViews();
        
        // 支付选项配置
        PaymentOption[] options = {
            new PaymentOption(1, "微信支付", R.drawable.ic_wechat, R.color.payment_wechat),
            new PaymentOption(2, "支付宝支付", R.drawable.ic_alipay, R.color.payment_alipay),
            new PaymentOption(3, "余额支付", R.drawable.ic_balance, R.color.payment_balance),
            new PaymentOption(4, "校园卡支付", R.drawable.ic_card, R.color.payment_card)
        };
        
        for (PaymentOption option : options) {
            View optionView = createPaymentOptionView(option);
            paymentOptionsLayout.addView(optionView);
            
            // 默认选择第一个
            if (option.type == 1) {
                selectOptionView(optionView, option);
            }
        }
    }

    private View createPaymentOptionView(PaymentOption option) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_payment_option, null);
        
        ImageView ivIcon = view.findViewById(R.id.ivIcon);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvDesc = view.findViewById(R.id.tvDesc);
        ImageView ivSelected = view.findViewById(R.id.ivSelected);
        
        // 设置图标和文本
        // 注意：这里使用占位图标，实际项目应添加相应图标资源
        try {
            ivIcon.setImageResource(option.iconRes);
        } catch (Exception e) {
            // 如果图标不存在，使用默认图标
            ivIcon.setImageResource(android.R.drawable.ic_menu_help);
        }
        
        tvName.setText(option.name);
        tvDesc.setText(getPaymentDescription(option.type));
        
        // 设置点击事件
        view.setOnClickListener(v -> {
            selectOptionView(view, option);
        });
        
        return view;
    }

    private String getPaymentDescription(int payType) {
        switch (payType) {
            case 1: return "安全便捷的微信支付";
            case 2: return "支付宝快捷支付";
            case 3: return "使用账户余额支付";
            case 4: return "校园卡一卡通支付";
            default: return "其他支付方式";
        }
    }

    private void selectOptionView(View view, PaymentOption option) {
        // 取消之前的选择
        if (selectedOptionView != null) {
            ImageView prevSelected = selectedOptionView.findViewById(R.id.ivSelected);
            prevSelected.setVisibility(View.GONE);
            selectedOptionView.setBackgroundResource(R.drawable.bg_payment_option_unselected);
        }
        
        // 设置新的选择
        ImageView ivSelected = view.findViewById(R.id.ivSelected);
        ivSelected.setVisibility(View.VISIBLE);
        view.setBackgroundResource(R.drawable.bg_payment_option_selected);
        
        // 更新选择状态
        selectedOptionView = view;
        selectedPayType = option.type;
        tvSelectedMethod.setText(option.name);
    }

    private void setupConfirmButton() {
        btnConfirmPayment.setOnClickListener(v -> {
            // 显示加载状态
            btnConfirmPayment.setEnabled(false);
            btnConfirmPayment.setText("支付处理中...");
            progressBar.setVisibility(View.VISIBLE);
            
            // 调用支付API
            processPayment();
        });
    }

    private void processPayment() {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("payType", selectedPayType);
        
        ApiClient.post("/api/payment/pay", body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                requireActivity().runOnUiThread(() -> {
                    if (data != null && data.isJsonObject()) {
                        JsonObject obj = data.getAsJsonObject();
                        boolean needCallback = obj.has("needCallback") && obj.get("needCallback").getAsBoolean();
                        
                        if (needCallback && obj.has("paymentNo")) {
                            String paymentNo = obj.get("paymentNo").getAsString();
                            showThirdPartyPaymentConfirm(paymentNo);
                        } else {
                            // 直接支付成功
                            handlePaymentSuccess();
                        }
                    } else {
                        // 直接支付成功
                        handlePaymentSuccess();
                    }
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    resetPaymentButton();
                    Toast.makeText(getContext(), "支付失败: " + message, Toast.LENGTH_LONG).show();
                    
                    if (callback != null) {
                        callback.onPaymentFailure(message);
                    }
                });
            }
        });
    }

    private void showThirdPartyPaymentConfirm(String paymentNo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("模拟第三方支付")
                .setMessage("请确认您已在微信/支付宝完成支付\n确认后将通知服务端支付成功")
                .setPositiveButton("确认已支付", (dialog, which) -> {
                    confirmThirdPartyPayment(paymentNo);
                })
                .setNegativeButton("取消支付", (dialog, which) -> {
                    resetPaymentButton();
                    if (callback != null) {
                        callback.onPaymentCancelled();
                    }
                    dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void confirmThirdPartyPayment(String paymentNo) {
        ApiClient.post("/api/payment/callback/" + paymentNo, new Object(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                requireActivity().runOnUiThread(() -> {
                    handlePaymentSuccess();
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    resetPaymentButton();
                    Toast.makeText(getContext(), "支付确认失败: " + message, Toast.LENGTH_LONG).show();
                    
                    if (callback != null) {
                        callback.onPaymentFailure(message);
                    }
                });
            }
        });
    }

    private void handlePaymentSuccess() {
        resetPaymentButton();
        Toast.makeText(getContext(), "支付成功！", Toast.LENGTH_LONG).show();
        
        if (callback != null) {
            callback.onPaymentSuccess();
        }
        
        dismiss();
    }

    private void resetPaymentButton() {
        btnConfirmPayment.setEnabled(true);
        btnConfirmPayment.setText("确认支付");
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onCancel(@NonNull android.content.DialogInterface dialog) {
        super.onCancel(dialog);
        if (callback != null) {
            callback.onPaymentCancelled();
        }
    }

    // 支付选项数据类
    private static class PaymentOption {
        int type;
        String name;
        int iconRes;
        int colorRes;
        
        PaymentOption(int type, String name, int iconRes, int colorRes) {
            this.type = type;
            this.name = name;
            this.iconRes = iconRes;
            this.colorRes = colorRes;
        }
    }
}