package com.vmovier.lib.view;

@SuppressWarnings("WeakerAccess")
class DefaultSeekHelper {
    /** currentPosition */
    long Y;
    /** video duration. */
    long d;
    int p;
    int q;
    float X;
    float a, b, m;

    float leftTouch, rightTouch;
    int touchRange;
    /** 剩余时长. */
    long rightDuration;

    /**
     * helper initialization
     * @param d 总时长 duration 不能为0
     * @param p 触摸左侧X坐标 （通常为padding）
     * @param q 触摸右侧X坐标 （通常为screenWidth-padding）
     * @param Y 当前currentPosition
     * @param X touchDown的X坐标
     */
    public DefaultSeekHelper(long d, int p, int q, long Y, float X) {
        this.d = d;
        this.p = p;
        this.q = q;
        this.Y = Y;
        this.X = X;

        leftTouch = X - p;
        rightTouch = q - X;
        touchRange = q - p;
        rightDuration = d - Y;

        m = (q + p) / 2;


        if (Y * touchRange >= d * leftTouch) {
            // touch点相对偏左
            if (Y * touchRange > 2 * d * leftTouch) {
                // touch点特别偏左
                // 取touch中点为视频结束点
                // 得到 一次函数系数a,b
                a = (Y - d) / (X - m);
            } else {
                // touch点不是特别偏左
                // 取p作为视频起始点
                // 得到 一次函数系数a,b
                if (X == p) {
                    a = (Y - d) / (X - m);
                } else {
                    a = (float) Y / (X - p);
                }
            }
        } else {
            // touch点相对偏右
            if (rightDuration * touchRange > 2 * d * rightTouch) {
                // touch点特别偏右
                // 取touch中点为视频起始点
                // 得到 一次函数系数a,b
                a = Y / (X - m);
            } else {
                // touch点不是特别偏右
                // 取q作为视频结束点
                // 得到 一次函数系数a,b
                if (X == q) {
                    a = Y / (X - m);
                } else {
                    a = (float) (Y - d) / (X - q);
                }

            }
        }

        b = Y - a * X;
    }

    /**
     *
     * @param x touch position between [0, screenWidth]
     * @return a seek position y between [0, d]
     */
    public int computeSeekPosition(float x) {
        return Math.round(a * x + b);
    }
    
}
