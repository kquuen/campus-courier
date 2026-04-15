"""
生成论文泳道图 - 系统业务流程活动图
需要安装: pip install matplotlib pillow
"""

import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch
import numpy as np

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'Arial Unicode MS']
plt.rcParams['axes.unicode_minus'] = False

def create_swimlane_diagram():
    fig, ax = plt.subplots(1, 1, figsize=(14, 10))
    ax.set_xlim(0, 14)
    ax.set_ylim(0, 10)
    ax.axis('off')

    # 颜色定义
    colors = {
        'user': '#E6F3FF',      # 浅蓝色 - 普通用户
        'courier': '#FFF5E6',   # 浅橙色 - 代取员
        'admin': '#F0FFF0',     # 浅绿色 - 管理员
        'arrow': '#333333',
        'text': '#000000'
    }

    # 泳道高度和位置
    lane_height = 2.8
    lane_y = {
        'user': 6.5,
        'courier': 3.3,
        'admin': 0.1
    }

    # 绘制泳道背景
    lanes = [
        ('普通用户', 0.5, lane_y['user'], colors['user']),
        ('代取员', 0.5, lane_y['courier'], colors['courier']),
        ('管理员', 0.5, lane_y['admin'], colors['admin'])
    ]

    for name, x, y, color in lanes:
        # 泳道背景
        rect = FancyBboxPatch((x, y), 13, lane_height,
                               boxstyle="round,pad=0.02,rounding_size=0.1",
                               facecolor=color, edgecolor='#333333', linewidth=1.5)
        ax.add_patch(rect)
        # 泳道标题
        ax.text(x + 0.3, y + lane_height/2, name, fontsize=14, fontweight='bold',
                va='center', ha='left', color='#333333')

    # 流程框样式
    def draw_box(ax, x, y, text, color='white'):
        box = FancyBboxPatch((x-0.6, y-0.25), 1.8, 0.5,
                             boxstyle="round,pad=0.02,rounding_size=0.08",
                             facecolor=color, edgecolor='#333333', linewidth=1)
        ax.add_patch(box)
        ax.text(x+0.3, y, text, fontsize=9, ha='center', va='center')

    def draw_arrow(ax, start, end, color='#333333'):
        ax.annotate('', xy=end, xytext=start,
                   arrowprops=dict(arrowstyle='->', color=color, lw=1.5))

    # ========== 普通用户流程 ==========
    user_x = 2.5
    user_y = 7.8

    draw_box(ax, user_x, user_y, '注册登录', '#FFE4B5')
    draw_arrow(ax, (user_x, user_y-0.25), (user_x, user_y-0.6))
    draw_box(ax, user_x, user_y-0.8, '发布需求', '#87CEEB')
    draw_arrow(ax, (user_x+0.6, user_y-1.05), (user_x+1.2, user_y-1.05))

    # ========== 代取员流程 ==========
    courier_x = 5.5
    courier_y = 4.5

    draw_box(ax, courier_x, courier_y, '查看待接订单', '#FFFACD')
    draw_arrow(ax, (courier_x, courier_y-0.25), (courier_x, courier_y-0.6))
    draw_box(ax, courier_x, courier_y-0.8, '接单', '#90EE90')
    draw_arrow(ax, (courier_x, courier_y-1.05), (courier_x, courier_y-1.4))
    draw_box(ax, courier_x, courier_y-1.6, '取件并更新状态', '#FFD700')
    draw_arrow(ax, (courier_x, courier_y-1.85), (courier_x, courier_y-2.2))
    draw_box(ax, courier_x, courier_y-2.4, '配送并更新状态', '#FFA500')

    # 连接箭头：发布需求 -> 查看待接订单
    ax.annotate('', xy=(courier_x-0.6, courier_y), xytext=(user_x+0.6, user_y-0.8),
               arrowprops=dict(arrowstyle='->', color='#333333', lw=1.2,
                              connectionstyle='arc3,rad=0.1'))

    # ========== 普通用户后续流程 ==========
    user2_x = 8.5
    user2_y = 7.0

    draw_box(ax, user2_x, user2_y, '查看进度', '#DDA0DD')
    draw_arrow(ax, (user2_x, user2_y-0.25), (user2_x, user2_y-0.55))
    draw_box(ax, user2_x, user2_y-0.75, '支付订单', '#98FB98')
    draw_arrow(ax, (user2_x, user2_y-1.0), (user2_x, user2_y-1.3))
    draw_box(ax, user2_x, user2_y-1.5, '评价服务', '#F0E68C')

    # 连接箭头：配送 -> 查看进度
    ax.annotate('', xy=(user2_x-0.6, user2_y+0.1), xytext=(courier_x+0.6, courier_y-2.4),
               arrowprops=dict(arrowstyle='->', color='#333333', lw=1.2,
                              connectionstyle='arc3,rad=0.2'))

    # ========== 管理员流程 ==========
    admin_x = 11.5
    admin_y = 1.5

    draw_box(ax, admin_x, admin_y, '用户管理', '#B0E0E6')
    draw_arrow(ax, (admin_x, admin_y-0.25), (admin_x, admin_y-0.6))
    draw_box(ax, admin_x, admin_y-0.8, '异常监控', '#FFB6C1')

    # ========== 标题 ==========
    ax.text(7, 9.5, '图3-1 系统业务流程活动图（泳道图）', fontsize=14,
            fontweight='bold', ha='center', va='center')

    # 保存图片
    plt.tight_layout()
    plt.savefig('diagram_3_1_swimlane.png', dpi=300, bbox_inches='tight',
                facecolor='white', edgecolor='none')
    print('图片已生成: diagram_3_1_swimlane.png')
    plt.close()

if __name__ == '__main__':
    create_swimlane_diagram()
