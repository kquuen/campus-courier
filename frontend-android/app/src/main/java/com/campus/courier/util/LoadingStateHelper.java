package com.campus.courier.util;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.campus.courier.R;
import com.google.android.material.snackbar.Snackbar;

/**
 * 加载状态管理工具类
 * 统一处理应用中的加载、错误和空状态显示
 */
public class LoadingStateHelper {

    /**
     * 显示全屏加载状态
     */
    public static View showFullScreenLoading(Activity activity) {
        View loadingView = LayoutInflater.from(activity).inflate(R.layout.layout_loading_fullscreen, null);
        
        // 添加到根视图
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(loadingView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        
        return loadingView;
    }

    /**
     * 隐藏全屏加载状态
     */
    public static void hideFullScreenLoading(Activity activity, View loadingView) {
        if (loadingView != null) {
            ViewGroup rootView = activity.findViewById(android.R.id.content);
            rootView.removeView(loadingView);
        }
    }

    /**
     * 显示局部加载状态（在指定容器内）
     */
    public static View showLocalLoading(ViewGroup container, String message) {
        View loadingView = LayoutInflater.from(container.getContext()).inflate(R.layout.layout_loading_local, null);
        
        TextView tvMessage = loadingView.findViewById(R.id.tvLoadingMessage);
        if (message != null) {
            tvMessage.setText(message);
        }
        
        container.removeAllViews();
        container.addView(loadingView);
        
        return loadingView;
    }

    /**
     * 隐藏局部加载状态
     */
    public static void hideLocalLoading(ViewGroup container, View contentView) {
        container.removeAllViews();
        if (contentView != null) {
            container.addView(contentView);
        }
    }

    /**
     * 显示空状态
     */
    public static View showEmptyState(ViewGroup container, String message, int iconResId) {
        View emptyView = LayoutInflater.from(container.getContext()).inflate(R.layout.layout_empty_state, null);
        
        TextView tvMessage = emptyView.findViewById(R.id.tvEmptyMessage);
        if (message != null) {
            tvMessage.setText(message);
        }
        
        // 如果有图标资源，设置图标
        if (iconResId != 0) {
            // 这里可以添加图标设置逻辑
        }
        
        container.removeAllViews();
        container.addView(emptyView);
        
        return emptyView;
    }

    /**
     * 显示错误状态
     */
    public static View showErrorState(ViewGroup container, String message, String buttonText, View.OnClickListener retryListener) {
        View errorView = LayoutInflater.from(container.getContext()).inflate(R.layout.layout_error_state, null);
        
        TextView tvMessage = errorView.findViewById(R.id.tvErrorMessage);
        if (message != null) {
            tvMessage.setText(message);
        }
        
        TextView btnRetry = errorView.findViewById(R.id.btnRetry);
        if (buttonText != null) {
            btnRetry.setText(buttonText);
        }
        
        if (retryListener != null) {
            btnRetry.setOnClickListener(retryListener);
        }
        
        container.removeAllViews();
        container.addView(errorView);
        
        return errorView;
    }

    /**
     * 显示Snackbar提示（成功）
     */
    public static void showSuccessSnackbar(View view, String message) {
        showSnackbar(view, message, R.color.success, Snackbar.LENGTH_SHORT);
    }

    /**
     * 显示Snackbar提示（错误）
     */
    public static void showErrorSnackbar(View view, String message) {
        showSnackbar(view, message, R.color.error, Snackbar.LENGTH_LONG);
    }

    /**
     * 显示Snackbar提示（警告）
     */
    public static void showWarningSnackbar(View view, String message) {
        showSnackbar(view, message, R.color.warning, Snackbar.LENGTH_LONG);
    }

    /**
     * 显示Snackbar提示（信息）
     */
    public static void showInfoSnackbar(View view, String message) {
        showSnackbar(view, message, R.color.info, Snackbar.LENGTH_SHORT);
    }

    private static void showSnackbar(View view, String message, int colorRes, int duration) {
        if (view == null) return;
        
        Snackbar snackbar = Snackbar.make(view, message, duration);
        
        // 设置背景颜色
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(view.getContext(), colorRes));
        
        // 设置文本颜色和多行显示
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(view.getContext(), R.color.text_on_primary));
        textView.setMaxLines(3); // 允许最多3行，避免文字折叠
        
        snackbar.show();
    }

    /**
     * 显示确认对话框
     */
    public static void showConfirmDialog(Context context, String title, String message,
                                         String positiveText, String negativeText,
                                         Runnable onConfirm, Runnable onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Dialog_CampusCourier);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, (dialog, which) -> {
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton(negativeText, (dialog, which) -> {
                    if (onCancel != null) onCancel.run();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 显示进度对话框
     */
    public static AlertDialog showProgressDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Dialog_CampusCourier);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
        
        TextView tvMessage = view.findViewById(R.id.tvProgressMessage);
        if (message != null) {
            tvMessage.setText(message);
        }
        
        builder.setView(view);
        builder.setCancelable(false);
        
        return builder.show();
    }

    /**
     * 设置SwipeRefreshLayout的颜色主题
     */
    public static void setupSwipeRefreshTheme(SwipeRefreshLayout swipeRefreshLayout) {
        if (swipeRefreshLayout == null) return;
        
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(swipeRefreshLayout.getContext(), R.color.primary),
                ContextCompat.getColor(swipeRefreshLayout.getContext(), R.color.accent),
                ContextCompat.getColor(swipeRefreshLayout.getContext(), R.color.success)
        );
        
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(swipeRefreshLayout.getContext(), R.color.surface)
        );
    }

    /**
     * 显示骨架屏（简化版）
     */
    public static View showSkeletonScreen(ViewGroup container, int layoutResId) {
        View skeletonView = LayoutInflater.from(container.getContext()).inflate(layoutResId, null);
        
        // 这里可以添加骨架屏动画逻辑
        // 目前先简单显示加载布局
        
        container.removeAllViews();
        container.addView(skeletonView);
        
        return skeletonView;
    }
}