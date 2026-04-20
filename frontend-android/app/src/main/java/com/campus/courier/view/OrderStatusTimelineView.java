package com.campus.courier.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.campus.courier.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Order status timeline view.
 */
public class OrderStatusTimelineView extends LinearLayout {

    private static final String[] STATUS_LABELS = {
            "\u5f85\u63a5\u5355",
            "\u5df2\u63a5\u5355",
            "\u53d6\u4ef6\u4e2d",
            "\u914d\u9001\u4e2d",
            "\u5df2\u5b8c\u6210",
            "\u5df2\u53d6\u6d88",
            "\u5f02\u5e38"
    };

    private static final String[] STATUS_DESCRIPTIONS = {
            "\u8ba2\u5355\u5df2\u53d1\u5e03\uff0c\u7b49\u5f85\u4ee3\u53d6\u5458\u63a5\u5355",
            "\u4ee3\u53d6\u5458\u5df2\u63a5\u5355\uff0c\u51c6\u5907\u524d\u5f80\u53d6\u4ef6",
            "\u4ee3\u53d6\u5458\u6b63\u5728\u53d6\u4ef6\u9014\u4e2d",
            "\u4ee3\u53d6\u5458\u6b63\u5728\u914d\u9001\u9014\u4e2d",
            "\u8ba2\u5355\u5df2\u5b8c\u6210\u9001\u8fbe",
            "\u8ba2\u5355\u5df2\u53d6\u6d88",
            "\u8ba2\u5355\u51fa\u73b0\u5f02\u5e38"
    };

    private int currentStatus = 0;
    private final List<StatusItem> statusItems = new ArrayList<>();
    private Paint linePaint;
    private Paint circlePaint;
    private Paint circleFillPaint;
    private int lineColor;
    private int activeColor;
    private int inactiveColor;
    private int circleRadius;
    private int lineWidth;

    public OrderStatusTimelineView(Context context) {
        super(context);
        init(context, null);
    }

    public OrderStatusTimelineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public OrderStatusTimelineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setOrientation(VERTICAL);
        setWillNotDraw(false);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OrderStatusTimelineView);
        lineColor = a.getColor(
                R.styleable.OrderStatusTimelineView_lineColor,
                ContextCompat.getColor(context, R.color.gray_300));
        activeColor = a.getColor(
                R.styleable.OrderStatusTimelineView_activeColor,
                ContextCompat.getColor(context, R.color.primary));
        inactiveColor = a.getColor(
                R.styleable.OrderStatusTimelineView_inactiveColor,
                ContextCompat.getColor(context, R.color.gray_400));
        circleRadius = a.getDimensionPixelSize(
                R.styleable.OrderStatusTimelineView_circleRadius,
                12);
        lineWidth = a.getDimensionPixelSize(
                R.styleable.OrderStatusTimelineView_lineWidth,
                4);
        a.recycle();

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(lineWidth);
        linePaint.setStyle(Paint.Style.STROKE);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(2);

        circleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleFillPaint.setColor(activeColor);
        circleFillPaint.setStyle(Paint.Style.FILL);

        initStatusItems();
    }

    private void initStatusItems() {
        removeAllViews();
        statusItems.clear();

        for (int i = 0; i < STATUS_LABELS.length; i++) {
            StatusItem item = new StatusItem(i);
            statusItems.add(item);
            addView(item.container);
        }
        updateStatusItems();
    }

    public void setCurrentStatus(int status) {
        if (status < 0 || status >= STATUS_LABELS.length) {
            return;
        }
        currentStatus = status;
        updateStatusItems();
        invalidate();
    }

    public int getCurrentStatus() {
        return currentStatus;
    }

    private void updateStatusItems() {
        for (int i = 0; i < statusItems.size(); i++) {
            StatusItem item = statusItems.get(i);
            boolean isActive = i <= currentStatus;
            boolean isCurrent = i == currentStatus;

            int titleColor = isActive ? activeColor : inactiveColor;
            int descColor = isActive
                    ? ContextCompat.getColor(getContext(), R.color.text_secondary)
                    : ContextCompat.getColor(getContext(), R.color.text_hint);

            item.labelView.setTextColor(titleColor);
            item.descriptionView.setTextColor(descColor);
            item.labelView.setText(isCurrent
                    ? STATUS_LABELS[i] + " (\u5f53\u524d)"
                    : STATUS_LABELS[i]);
            item.descriptionView.setText(STATUS_DESCRIPTIONS[i]);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawConnectingLines(canvas);
        drawStatusCircles(canvas);
    }

    private void drawConnectingLines(Canvas canvas) {
        if (statusItems.size() < 2) {
            return;
        }

        for (int i = 0; i < statusItems.size() - 1; i++) {
            StatusItem current = statusItems.get(i);
            StatusItem next = statusItems.get(i + 1);

            linePaint.setColor(i < currentStatus ? activeColor : lineColor);
            int lineX = current.getCircleCenterX();
            canvas.drawLine(
                    lineX,
                    current.getCircleCenterY(),
                    lineX,
                    next.getCircleCenterY(),
                    linePaint);
        }
    }

    private void drawStatusCircles(Canvas canvas) {
        for (int i = 0; i < statusItems.size(); i++) {
            StatusItem item = statusItems.get(i);
            int centerX = item.getCircleCenterX();
            int centerY = item.getCircleCenterY();
            boolean isActive = i <= currentStatus;

            if (isActive) {
                canvas.drawCircle(centerX, centerY, circleRadius, circleFillPaint);
                circlePaint.setColor(activeColor);
                canvas.drawCircle(centerX, centerY, circleRadius + 2, circlePaint);
            } else {
                circlePaint.setColor(inactiveColor);
                canvas.drawCircle(centerX, centerY, circleRadius, circlePaint);
            }

            if (i == currentStatus) {
                Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                innerPaint.setColor(Color.WHITE);
                innerPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerX, centerY, circleRadius / 2f, innerPaint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = getPaddingTop() + getPaddingBottom();
        for (StatusItem item : statusItems) {
            height += item.container.getMeasuredHeight();
        }
        setMeasuredDimension(getMeasuredWidth(), height);
    }

    private class StatusItem {
        private final View container;
        private final TextView labelView;
        private final TextView descriptionView;

        StatusItem(int index) {
            LinearLayout root = new LinearLayout(getContext());
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16, 0, 16);
            root.setLayoutParams(params);
            root.setPadding(circleRadius * 3, 8, 0, 8);
            root.setOrientation(VERTICAL);

            labelView = new TextView(getContext());
            labelView.setText(STATUS_LABELS[index]);
            labelView.setTextSize(16);
            labelView.setTextColor(inactiveColor);
            labelView.setTypeface(labelView.getTypeface(), android.graphics.Typeface.BOLD);

            descriptionView = new TextView(getContext());
            descriptionView.setText(STATUS_DESCRIPTIONS[index]);
            descriptionView.setTextSize(12);
            descriptionView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_hint));
            descriptionView.setPadding(0, 4, 0, 0);

            root.addView(labelView);
            root.addView(descriptionView);
            container = root;
        }

        int getCircleCenterX() {
            return circleRadius + container.getPaddingLeft() / 2;
        }

        int getCircleCenterY() {
            return container.getTop() + container.getHeight() / 2;
        }
    }
}
