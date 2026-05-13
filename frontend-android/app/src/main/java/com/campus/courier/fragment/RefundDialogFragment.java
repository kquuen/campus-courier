package com.campus.courier.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class RefundDialogFragment extends DialogFragment {

    public interface RefundCallback {
        void onRefundSuccess();
        void onRefundFailure(String error);
        void onRefundCancelled();
    }

    private static final String ARG_ORDER_ID = "order_id";
    private static final String ARG_REFUND_AMOUNT = "refund_amount";

    private long orderId;
    private double refundAmount;
    private RefundCallback callback;

    private Button btnConfirmRefund;
    private ProgressBar progressBar;

    public static RefundDialogFragment newInstance(long orderId, double refundAmount) {
        RefundDialogFragment fragment = new RefundDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ORDER_ID, orderId);
        args.putDouble(ARG_REFUND_AMOUNT, refundAmount);
        fragment.setArguments(args);
        return fragment;
    }

    public void setRefundCallback(RefundCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getLong(ARG_ORDER_ID);
            refundAmount = getArguments().getDouble(ARG_REFUND_AMOUNT);
        }
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Dialog_CampusCourier);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.Dialog_CampusCourier);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_refund, null);
        builder.setView(view);

        initViews(view);
        setupConfirmButton();

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setWindowAnimations(R.style.DialogAnimation_CampusCourier);
        }
    }

    private void initViews(View view) {
        TextView tvOrderId = view.findViewById(R.id.tvOrderId);
        TextView tvRefundAmount = view.findViewById(R.id.tvRefundAmount);
        btnConfirmRefund = view.findViewById(R.id.btnConfirmRefund);
        progressBar = view.findViewById(R.id.progressBar);

        tvOrderId.setText(String.format("\u8ba2\u5355\u53f7  %d", orderId));
        tvRefundAmount.setText(String.format("\u00a5%.2f", refundAmount));
    }

    private void setupConfirmButton() {
        btnConfirmRefund.setOnClickListener(v -> {
            btnConfirmRefund.setEnabled(false);
            btnConfirmRefund.setText("\u9000\u6b3e\u5904\u7406\u4e2d...");
            progressBar.setVisibility(View.VISIBLE);
            requestRefund();
        });
    }

    private void requestRefund() {
        ApiClient.post("/api/order/" + orderId + "/refund-cancel", new Object(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runUiSafely(() -> handleRefundSuccess());
            }

            @Override
            public void onError(String message) {
                runUiSafely(() -> {
                    resetRefundButton();
                    Toast.makeText(getContext(), "\u9000\u6b3e\u5931\u8d25: " + message, Toast.LENGTH_LONG).show();
                    if (callback != null) {
                        callback.onRefundFailure(message);
                    }
                });
            }
        });
    }

    private void handleRefundSuccess() {
        resetRefundButton();
        Toast.makeText(getContext(), "\u6f14\u793a\u9000\u6b3e\u6210\u529f", Toast.LENGTH_LONG).show();
        if (callback != null) {
            callback.onRefundSuccess();
        }
        safeDismiss();
    }

    private void resetRefundButton() {
        if (btnConfirmRefund == null || progressBar == null) {
            return;
        }
        btnConfirmRefund.setEnabled(true);
        btnConfirmRefund.setText("\u786e\u8ba4\u6f14\u793a\u9000\u6b3e");
        progressBar.setVisibility(View.GONE);
    }

    private void runUiSafely(Runnable action) {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(action);
    }

    private void safeDismiss() {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (getDialog() == null) {
            return;
        }
        try {
            dismiss();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCancel(@NonNull android.content.DialogInterface dialog) {
        super.onCancel(dialog);
        if (callback != null) {
            callback.onRefundCancelled();
        }
    }
}
