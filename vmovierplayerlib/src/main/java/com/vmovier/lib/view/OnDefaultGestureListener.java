package com.vmovier.lib.view;

import android.support.annotation.Nullable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.utils.PlayerLog;

@SuppressWarnings("WeakerAccess")
public class OnDefaultGestureListener extends GestureDetector.SimpleOnGestureListener {
    private static final String TAG = OnDefaultGestureListener.class.getSimpleName();
    protected final IPlayerControlView mPlayerControlView;
    protected final IPlayer mPlayer;

    public static final int AXISTYPE_UNKNOWN = 0;
    public static final int AXISTYPE_X = 1;
    public static final int AXISTYPE_Y_LEFT = 2;
    public static final int AXISTYPE_Y_RIGHT = 3;

    private int mAxisType = AXISTYPE_UNKNOWN;
    private final int mTouchSlop;
    private float mTouchWidthUnit;
    private float mTouchHeightUnit;
    private int mVolumeWhenActionDown;
    private long mPositionWhenActionDown;
    private long mDurationWhenActionDown;
    //避免无意义的Seek.
    private long mLastSeekToPosition;

    private DefaultSeekHelper mDragSeekHelper;

    public OnDefaultGestureListener(@Nullable IPlayer p, @Nullable IPlayerControlView c) {
        this.mPlayer = p;
        this.mPlayerControlView = c;
        if (c != null && c.getContext() != null) {
            final ViewConfiguration configuration = ViewConfiguration.get(c.getContext().getApplicationContext());
            mTouchSlop = configuration.getScaledTouchSlop();
        } else {
            //noinspection deprecation
            mTouchSlop = ViewConfiguration.getTouchSlop();
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        PlayerLog.d(TAG, "onSingleTapConfirmed");
        if (mPlayerControlView == null) return false;
        if (mPlayerControlView.isVisible()) {
            mPlayerControlView.hide();
        } else {
            mPlayerControlView.show();
        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        PlayerLog.d(TAG, "onDown");
        return onTouchDown(e);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        PlayerLog.d(TAG, "onSingleTapUp");
        return super.onSingleTapUp(e);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        PlayerLog.d(TAG, "onDoubleTap");
        if (mPlayer == null) return false;
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.play();
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mTouchHeightUnit == 0 || mTouchWidthUnit == 0) {
            PlayerLog.e(TAG, "mTouchHeightUnit == 0 || mTouchWidthUnit == 0");
            return false;
        }

        float startX = e1.getRawX();
        float startY = e1.getRawY();
        float endY = e2.getRawY();
        float endX = e2.getRawX();
        float yChanged = startY - endY;
        float xChanged = endX - startX;

        switch (mAxisType) {
            case AXISTYPE_UNKNOWN:
                if (Math.abs(xChanged) > mTouchSlop) {
                    mAxisType = AXISTYPE_X;
                    if (mPlayerControlView != null) {
                        mPlayerControlView.hide();
                    }
                } else if (Math.abs(yChanged) > mTouchSlop) {
                    if (startX > (mTouchWidthUnit / 2)) {
                        mAxisType = AXISTYPE_Y_RIGHT;
                    } else {
                        mAxisType = AXISTYPE_Y_LEFT;
                    }
                    if (mPlayerControlView != null) {
                        mPlayerControlView.hide();
                    }
                }
                break;
            case AXISTYPE_X:
            case AXISTYPE_Y_LEFT:
            case AXISTYPE_Y_RIGHT:
                return onSwipe(e1, e2, xChanged / mTouchWidthUnit, yChanged / mTouchHeightUnit, mAxisType);
        }

        return super.onScroll(e1, e2, distanceX, distanceY);
    }

    protected boolean onTouchDown(MotionEvent e) {
        if (mPlayer == null) return false;
        mAxisType = AXISTYPE_UNKNOWN;
        mTouchWidthUnit = getGestureWidthUnit();
        mTouchHeightUnit = getGestureHeightUnit();
        mVolumeWhenActionDown = mPlayer.getVolume();
        mPositionWhenActionDown = mLastSeekToPosition = mPlayer.getCurrentPosition();
        mDurationWhenActionDown = mPlayer.getDuration();
        float mXCoordinateWhenActionDown = e.getRawX();

        if (mDurationWhenActionDown == 0) {
            mDragSeekHelper = null;
        } else {
            mDragSeekHelper = new DefaultSeekHelper(mDurationWhenActionDown, 0,
                    (int)mTouchWidthUnit, mPositionWhenActionDown, mXCoordinateWhenActionDown);
        }

        return true;
    }

    /**
     * @param e1 起始MotionEvent
     * @param e2 结束MotionEvent
     * @param dx x轴滑动距离 与 总距离的比例
     * @param dy y轴滑动距离 与 总距离的比例
     * @param axisType 滑动的类型
     * @return 是否识别出手势
     */
    @SuppressWarnings("UnusedParameters")
    protected boolean onSwipe(MotionEvent e1, MotionEvent e2, float dx, float dy, int axisType) {
        boolean handled = false;
        switch (axisType) {
            case AXISTYPE_X:
                handled = onAxisXChanged(e1, e2);
                break;
            case AXISTYPE_Y_LEFT:
                handled = onAxisLeftYChanged(e1, e2, dy);
                break;
            case AXISTYPE_Y_RIGHT:
                handled = onAxisRightYChanged(e1, e2, dy);
                break;
        }
        return handled;
    }

    /**
     * 播放进度发生改变
     * @param startPosition 开始时的播放进度
     * @param finalPosition 结束时的播放进度
     */
    protected void onPositionChanged(long startPosition, long finalPosition) {
        // do nothing. 上层可以自己实现
    }

    /**
     * @param e1 起始MotionEvent
     * @param e2 结束MotionEvent
     * @return 是否处理
     */
    @SuppressWarnings("UnusedParameters")
    protected boolean onAxisXChanged(MotionEvent e1, MotionEvent e2) {
        if (mPlayer == null) return false;
        long finalPosition = 0;
        if (mDragSeekHelper != null) {
            finalPosition = mDragSeekHelper.computeSeekPosition(e2.getRawX());
        }
        if (finalPosition < 0) {
            finalPosition = 0;
        } else if (finalPosition > mDurationWhenActionDown) {
            finalPosition = mDurationWhenActionDown;
        }
        if (!(mLastSeekToPosition / 1000 == finalPosition / 1000)) {
            // 减少无意义的seek
            mPlayer.seekTo(finalPosition);
        }
        mLastSeekToPosition = finalPosition;
        onPositionChanged(mPositionWhenActionDown, finalPosition);
        return true;
    }

    /**
     * @param e1 起始MotionEvent
     * @param e2 结束MotionEvent
     * @param dy y轴滑动距离 与 总距离的比例
     * @return 是否处理了此次滑动事件
     */
    protected boolean onAxisLeftYChanged(MotionEvent e1, MotionEvent e2, float dy) {
        if (mPlayer == null) return false;
        int index = mVolumeWhenActionDown + (int) (dy * 100);
        if (index > 100) {
            index = 100;
        } else if (index < 0) {
            index = 0;
        }
        mPlayer.setVolume(index);
        return true;
    }

    /**
     * @param e1 起始MotionEvent
     * @param e2 结束MotionEvent
     * @param dy y轴滑动距离 与 总距离的比例
     * @return 是否处理了此次滑动事件
     */
    @SuppressWarnings("UnusedParameters")
    protected boolean onAxisRightYChanged(MotionEvent e1, MotionEvent e2, float dy) {
        if (mPlayer == null) return false;
        int index = mVolumeWhenActionDown + (int) (dy * 100);
        if (index > 100) {
            index = 100;
        } else if (index < 0) {
            index = 0;
        }
        mPlayer.setVolume(index);
        return true;
    }

    protected int getGestureWidthUnit() {
        return mPlayerControlView == null ? 0 : Math.round(mPlayerControlView.getVideoViewWidth() * 0.8f);
    }

    protected int getGestureHeightUnit() {
        return mPlayerControlView == null ? 0 : Math.round(mPlayerControlView.getVideoViewHeight() * 0.8f);
    }
}
