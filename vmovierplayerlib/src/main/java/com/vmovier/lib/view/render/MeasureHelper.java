package com.vmovier.lib.view.render;

import android.view.View;

import com.vmovier.lib.utils.PlayerLog;

import java.lang.ref.WeakReference;

/**
 * 非常关键的一个类 用来测量 计算VideoView renderView 的大小
 * @author bb
 */
@SuppressWarnings("unused")
final class MeasureHelper {
    private static final String TAG = MeasureHelper.class.getSimpleName();

    private WeakReference<View> mWeakView;

    private int mVideoWidth;
    private int mVideoHeight;

    private int mVideoSarNum;
    private int mVideoSarDen;
    /** 屏幕旋转的程度 */
    private int mVideoRotationDegree;
    /** the final width and height. */
    private int mMeasuredWidth;
    private int mMeasuredHeight;
    /** 默认视频比例 */
    private int mCurrentScaleType = IRenderView.SCALE_FIT_PARENT;

    MeasureHelper(View view) {
        mWeakView = new WeakReference<>(view);
    }

    View getView() {
        if (mWeakView == null) {
            return null;
        }
        return mWeakView.get();
    }

    void setVideoSize(int videoWidth, int videoHeight) {
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;
    }

    void setVideoSampleAspectRatio(int videoSarNum, int videoSarDen) {
        mVideoSarNum = videoSarNum;
        mVideoSarDen = videoSarDen;
    }

    void setVideoRotation(int videoRotationDegree) {
        mVideoRotationDegree = videoRotationDegree;
    }

    /**
     * Must be called by View.onMeasure(int, int)
     *
     * @param widthMeasureSpec 父布局给出的宽度 Spec
     * @param heightMeasureSpec 父布局给出的高度 Spec
     */
    void doMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        PlayerLog.d(TAG, "-------------------------- measure start -----------------------------");
        PlayerLog.d(TAG, "  mVideoWidth -->" + mVideoWidth
                  + ", mVideoHeight -->" + mVideoHeight
                  + "mCurrentScaleType -->" + mCurrentScaleType
                  + "mVideoRotationDegree -->" + mVideoRotationDegree);
        // 如果屏幕是往左 或者往右 旋转了 90度, 交换宽高.
        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) {
            int tempSpec = widthMeasureSpec;
            widthMeasureSpec  = heightMeasureSpec;
            heightMeasureSpec = tempSpec;
        }
        // 根据给出的父布局的 Spec 以及video 本身的 width 和height 算出默认的videoView size.
        int width = View.getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = View.getDefaultSize(mVideoHeight, heightMeasureSpec);
        PlayerLog.d(TAG,"View.getDefaultWidthSize  -->" + width
                + "View.getDefaultHeightSize  -->" + height);
        // 比例如果是 match, 则直接把父类给出的 Spec 赋值给 width 和 height.
        if (mCurrentScaleType == IRenderView.SCALE_MATCH_PARENT) {
            width = widthMeasureSpec;
            height = heightMeasureSpec;
        } else if (mVideoWidth > 0 && mVideoHeight > 0) {
            int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);

            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                // 算出父类给出的 viewGroup 大小 宽高比
                float specAspectRatio = (float) widthSpecSize / (float) heightSpecSize;
                // 根据AspectRatio 算出视频的比例.
                float displayAspectRatio;
                switch (mCurrentScaleType) {
                    case IRenderView.SCALE_16_9_FIT_PARENT:
                        displayAspectRatio = 16.0f / 9.0f;
                        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) {
                            displayAspectRatio = 1.0f / displayAspectRatio;
                        }
                        break;
                    case IRenderView.SCALE_4_3_FIT_PARENT:
                        displayAspectRatio = 4.0f / 3.0f;
                        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) {
                            displayAspectRatio = 1.0f / displayAspectRatio;
                        }
                        break;
                    case IRenderView.SCALE_FIT_PARENT:
                    case IRenderView.SCALE_FILL_PARENT:
                    case IRenderView.SCALE_WRAP_CONTENT:
                    default:
                        displayAspectRatio = (float) mVideoWidth / (float) mVideoHeight;
                        if (mVideoSarNum > 0 && mVideoSarDen > 0) {
                            // DAR = SAR × PAR.
                            // Sar Sample Aspect Ratio 采样纵横比。即视频横向对应的像素个数比上视频纵向的像素个数。即为我们通常提到的分辨率。比如VGA图像640/480 = 4:3，D-1 PAL图像720/576 = 5:4
                            // PAR，Pixel Aspect Ratio 像素宽高比。如果把像素想象成一个长方形，PAR即为这个长方形的长与宽的比。当长宽比为1时，这时的像素我们成为方形像素。
                            // DAR，Display Aspect Ratio 显示宽高比。即最终播放出来的画面的宽与高之比。
                            displayAspectRatio = displayAspectRatio * mVideoSarNum / mVideoSarDen;
                        }
                        break;
                }
                // 比较video 自身的宽高比 和 viewGroup的 宽高比.
                boolean shouldBeWider = displayAspectRatio > specAspectRatio;
                PlayerLog.d(TAG, "displayAspectRatio is " + displayAspectRatio
                        + " , specAspectRatio is " + specAspectRatio
                        + " , mVideoSarNum is " + mVideoSarNum
                        + " , mVideoSarDen is " + mVideoSarDen);
                switch (mCurrentScaleType) {
                    case IRenderView.SCALE_FIT_PARENT:
                    case IRenderView.SCALE_16_9_FIT_PARENT:
                    case IRenderView.SCALE_4_3_FIT_PARENT:
                        if (shouldBeWider) {
                            // too wide, fix width
                            width = widthSpecSize;
                            height = (int) (width / displayAspectRatio);
                        } else {
                            // too high, fix height
                            height = heightSpecSize;
                            width = (int) (height * displayAspectRatio);
                        }
                        break;
                    case IRenderView.SCALE_FILL_PARENT:
                        if (shouldBeWider) {
                            // not high enough, fix height
                            height = heightSpecSize;
                            width = (int) (height * displayAspectRatio);
                        } else {
                            // not wide enough, fix width
                            width = widthSpecSize;
                            height = (int) (width / displayAspectRatio);
                        }
                        break;
                    case IRenderView.SCALE_WRAP_CONTENT:
                    default:
                        if (shouldBeWider) {
                            // too wide, fix width
                            width = Math.min(mVideoWidth, widthSpecSize);
                            height = (int) (width / displayAspectRatio);
                        } else {
                            // too high, fix height
                            height = Math.min(mVideoHeight, heightSpecSize);
                            width = (int) (height * displayAspectRatio);
                        }
                        break;
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                PlayerLog.d(TAG, "widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY");
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if (mVideoWidth * height < width * mVideoHeight) {
                    //PlayerLog.i("@@@", "image too wide, correcting");
                    width = height * mVideoWidth / mVideoHeight;
                } else if (mVideoWidth * height > width * mVideoHeight) {
                    //PlayerLog.i("@@@", "image too tall, correcting");
                    height = width * mVideoHeight / mVideoWidth;
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY) {
                PlayerLog.d(TAG, "widthSpecMode == View.MeasureSpec.EXACTLY");
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = width * mVideoHeight / mVideoWidth;
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == View.MeasureSpec.EXACTLY) {
                PlayerLog.d(TAG, "heightSpecMode == View.MeasureSpec.EXACTLY");
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * mVideoWidth / mVideoHeight;
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                PlayerLog.d(TAG, "neither the width nor the height are fixed, try to use actual video size");
                width = mVideoWidth;
                height = mVideoHeight;
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * mVideoWidth / mVideoHeight;
                }
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
        } else {
            PlayerLog.d(TAG, "no size yet, just adopt the given spec sizes");
        }

        mMeasuredWidth = width;
        mMeasuredHeight = height;

        PlayerLog.d(TAG, "the final MeasuredWidth --> " + mMeasuredWidth + " , the final MeasuredHeight -->" + mMeasuredHeight);

        PlayerLog.d(TAG, "-------------------------- measure final -----------------------------");
    }

    int getMeasuredWidth() {
        return mMeasuredWidth;
    }

    int getMeasuredHeight() {
        return mMeasuredHeight;
    }

    public void setScaleType(int scaleType) {
        mCurrentScaleType = scaleType;
    }

    public boolean isVideoSizeFetched() {
        return mVideoHeight != 0 && mVideoWidth != 0;
    }

}
