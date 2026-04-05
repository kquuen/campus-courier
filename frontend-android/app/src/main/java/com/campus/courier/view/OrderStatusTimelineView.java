package com.campus.courier.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
 * 订单状态时间线视图
 * 显示订单从发布到完成的完整状态流转过程
 */
public class OrderStatusTimelineView extends LinearLayout {

    private static final String[] STATUS_LABELS = {
            "待接单", "已接单", "取件中", "已完成", "已取消", "异常"
    };
    
    private static final String[] STATUS_DESCRIPTIONS = {
            "订单已发布，等待代取员接单",
            "代取员已接单，准备前往取件",
            "代取员正在取件途中",
            "订单已完成送达",
            "订单已取消",
            "订单出现异常"
    };

    private int currentStatus = 0;
    private List<StatusItem> statusItems = new ArrayList<>();
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

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);
        
        // 读取属性
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OrderStatusTimelineView);
        lineColor = a.getColor(R.styleable.OrderStatusTimelineView_lineColor, 
                ContextCompat.getColor(context, R.color.gray_300));
        activeColor = a.getColor(R.styleable.OrderStatusTimelineView_activeColor,
                ContextCompat.getColor(context, R.color.primary));
        inactiveColor = a.getColor(R.styleable.OrderStatusTimelineView_inactiveColor,
                ContextCompat.getColor(context, R.color.gray_400));
        circleRadius = a.getDimensionPixelSize(R.styleable.OrderStatusTimelineView_circleRadius, 12);
        lineWidth = a.getDimensionPixelSize(R.styleable.OrderStatusTimelineView_lineWidth, 4);
        a.recycle();
        
        // 初始化画笔
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(lineWidth);
        linePaint.setStyle(Paint.Style.STROKE);
        
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(inactiveColor);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(2);
        
        circleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleFillPaint.setColor(activeColor);
        circleFillPaint.setStyle(Paint.Style.FILL);
        
        // 初始化状态项
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
    }

    public void setCurrentStatus(int status) {
        if (status < 0 || status >= STATUS_LABELS.length) {
            return;
        }
        
        this.currentStatus = status;
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
            
            // 更新文本颜色
            int textColor = isActive ? activeColor : inactiveColor;
            item.labelView.setTextColor(textColor);
            item.descriptionView.setTextColor(isActive ? 
                    ContextCompat.getColor(getContext(), R.color.text_secondary) : 
                    ContextCompat.getColor(getContext(), R.color.text_hint));
            
            // 更新状态标签
            String statusText = STATUS_LABELS[i];
            if (isCurrent) {
                statusText += " (当前)";
            }
            item.labelView.setText(statusText);
            
            // 更新描述
            item.descriptionView.setText(STATUS_DESCRIPTIONS[i]);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制连接线
        drawConnectingLines(canvas);
        
        // 绘制状态圆点
        drawStatusCircles(canvas);
    }

    private void drawConnectingLines(Canvas canvas) {
        if (statusItems.size() < 2) return;
        
        for (int i = 0; i < statusItems.size() - 1; i++) {
            StatusItem current = statusItems.get(i);
            StatusItem next = statusItems.get(i + 1);
            
            int centerY1 = current.getCircleCenterY();
            int centerY2 = next.getCircleCenterY();
            
            // 判断是否应该用实线（当前状态之前的线）还是虚线（之后的线）
            boolean isActiveLine = i < currentStatus;
            if (isActiveLine) {
                linePaint.setColor(activeColor);
                linePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                linePaint.setColor(lineColor);
                linePaint.setStyle(Paint.Style.STROKE);
            }
            
            // 绘制垂直线
            int lineX = current.getCircleCenterX();
            canvas.drawLine(lineX, centerY1, lineX, centerY2, linePaint);
            
            i++;
        }
    }

    private void drawStatusCircles(Canvas canvas) {
        for (int i = 0; i < statusItems.size(); i++) {
            StatusItem item = statusItems.get(i);
            int centerX = item.getCircleCenterX();
            int centerY = item.getCircleCenterY();
            
            boolean isActive = i <= currentStatus;
            
            if (isActive) {
                // 绘制实心圆
                canvas.drawCircle(centerX, centerY, circleRadius, circleFillPaint);
                // 绘制外圈
                circlePaint.setColor(activeColor);
                canvas.drawCircle(centerX, centerY, circleRadius + 2, circlePaint);
            } else {
                // 绘制空心圆
                circlePaint.setColor(inactiveColor);
                canvas.drawCircle(centerX, centerY, circleRadius, circlePaint);
            }
            
            // 如果当前状态，添加内圈
            if (i == currentStatus) {
                Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                innerPaint.setColor(Color.WHITE);
                innerPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerX, centerY, circleRadius / 2, innerPaint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        // 确保有足够空间显示所有状态
        int height = getPaddingTop() + getPaddingBottom();
        for (StatusItem item : statusItems) {
            height += item.container.getMeasuredHeight();
        }
        
        setMeasuredDimension(getMeasuredWidth(), height);
    }

    // 内部类：状态项
    private class StatusItem {
        View container;
        TextView labelView;
        TextView descriptionView;
        int index;
        
        StatusItem(int index) {
            this.index = index;
            initView();
        }
        
        private void initView() {
            // 创建容器
            container = new LinearLayout(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16, 0, 16);
            container.setLayoutParams(params);
            container.setPadding(circleRadius * 3, 8, 0, 8);
            
            // 创建垂直布局
            LinearLayout contentLayout = new LinearLayout(getContext());
            contentLayout.setOrientation(VERTICAL);
            LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            contentLayout.setLayoutParams(contentParams);
            
            // 创建状态标签
            labelView = new TextView(getContext());
            labelView.setText(STATUS_LABELS[index]);
            labelView.setTextSize(16);
            labelView.setTextColor(inactiveColor);
            labelView.setTypeface(labelView.getTypeface(), android.graphics.Typeface.BOLD);
            
            // 创建状态描述
            descriptionView = new TextView(getContext());
            descriptionView.setText(STATUS_DESCRIPTIONS[index]);
            descriptionView.setTextSize(12);
            descriptionView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_hint));
            descriptionView.setPadding(0, 4, 0, 0);
            
            // 添加到布局
            contentLayout.addView(labelView);
            contentLayout.addView(descriptionView);
            ((LinearLayout) container).addView(contentLayout);
        }
        
        int getCircleCenterX() {
            return circleRadius + container.getPaddingLeft() / 2;
        }
        
        int getCircleCenterY() {
            return container.getTop() + container.getHeight() / 2;
        }
    }
}