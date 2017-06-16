package com.vmovier.lib.view;

import android.animation.Animator;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;

import com.vmovier.player.R;


/**
 * Created by tangye on 16/8/5.
 * Visibility工具
 */
@SuppressWarnings("WeakerAccess, unused")
public class PlayerVisibilityUtils {

    private PlayerVisibilityUtils() {}

    private static final long DEFAULT_DURATION = 200;

    /**
     * 渐隐渐现的Provider
     */
    public static final VisibilityAnimateProvider FADE_PROVIDER = new VisibilityAnimateProvider() {

        @Override
        @NonNull
        public Object onAppear(@NonNull View view) {
            if (view.getVisibility() != View.VISIBLE) {
                ViewCompat.setAlpha(view, 0);
            }
            return view.animate().setDuration(DEFAULT_DURATION).alpha(1);
        }

        @Override
        @NonNull
        public Object onDisappear(@NonNull View view) {
            return view.animate().setDuration(DEFAULT_DURATION).alpha(0);
        }
    };

    /**
     * 收起弹出的Provider
     */
    public static final VisibilityAnimateProvider SLIDE_UP_PROVIDER = new VisibilityAnimateProvider() {

        @Override
        @NonNull
        public Object onAppear(@NonNull View view) {
            return view.animate().setDuration(DEFAULT_DURATION).translationY(0).setInterpolator(new DecelerateInterpolator(1.5f));
        }

        @Override
        @NonNull
        public Object onDisappear(@NonNull View view) {
            return view.animate().setDuration(DEFAULT_DURATION).translationY(-view.getHeight()).setInterpolator(new DecelerateInterpolator(1.5f));
        }
    };

    /**
     * 收起弹出的Provider
     */
    public static final VisibilityAnimateProvider SLIDE_DOWN_PROVIDER = new VisibilityAnimateProvider() {

        @Override
        @NonNull
        public Object onAppear(@NonNull View view) {
            return view.animate().setDuration(DEFAULT_DURATION).translationY(0).setInterpolator(new DecelerateInterpolator(1.5f));
        }

        @Override
        @NonNull
        public Object onDisappear(@NonNull View view) {
            return view.animate().setDuration(DEFAULT_DURATION).translationY(view.getHeight()).setInterpolator(new DecelerateInterpolator(1.5f));
        }
    };

    /**
     * 使用FADE_PROVIDER渐显一个View到View.VISIBLE
     * @param view 指定View
     * @return true表示执行动画成功
     */
    public static boolean fadeIn(View view) {
        return fadeIn(view, -1);
    }

    /**
     * 使用FADE_PROVIDER渐隐一个View到View.GONE
     * @param view 指定VIEW
     * @return true表示执行动画成功
     */
    public static boolean fadeOut(View view) {
        return fadeOut(view, -1);
    }

    /**
     * 使用FADE_PROVIDER渐显一个View到View.VISIBLE
     * @param view 指定View
     * @param duration 指定渐显动画时长
     * @return true表示执行动画成功
     */
    public static boolean fadeIn(View view, long duration) {
        setVisibilityAnimateProvider(view, FADE_PROVIDER);
        if (!isTargetVisible(view)) {
            setTargetVisibility(view, View.VISIBLE, duration);
            return true;
        }
        return false;
    }

    /**
     * 使用FADE_PROVIDER渐隐一个View到View.GONE
     * @param view 指定VIEW
     * @param duration 指定渐隐动画时长
     * @return true表示执行动画成功
     */
    public static boolean fadeOut(View view, long duration) {
        setVisibilityAnimateProvider(view, FADE_PROVIDER);
        if (isTargetVisible(view)) {
            setTargetVisibility(view, View.GONE, duration);
            return true;
        }
        return false;
    }

    /**
     * 在Visible和Gone之间切换View的动画状态
     * @param view 执行Fade操作的View对象
     * @return true则表示，当前view处于隐藏状态，执行显示动画，false则表示，当前view处于显示状态
     */
    public static boolean toggle(View view) {
        setVisibilityAnimateProvider(view, FADE_PROVIDER);
        if (isTargetVisible(view)) {
            fadeOut(view);
            return false;
        } else {
            fadeIn(view);
            return true;
        }
    }

    /**
     * 设置View的Visibility动画提供对象
     * @param view 指定的View
     * @param provider 需要使用的动画提供对象
     */
    public static void setVisibilityAnimateProvider(@NonNull View view, VisibilityAnimateProvider provider) {
        view.setTag(R.id.player_visibility_animator_provider, provider);
    }

    /**
     * 获取指定View的最终Visibility
     * @param view 指定的View
     * @return 返回View的最终Visibility,可能是GONE INVISIBLE VISIBLE中的一个
     */
    public static int getTargetVisibility(View view) {
        TransitionObject t = PlayerViewTagUtils.getTag(view, R.id.player_target_visibility, TransitionObject.class);
        if (t != null) return t.visibility;
        return view.getVisibility();
    }

    /**
     * 获取指定View的最终是否显示
     * @param view 指定View
     * @return 如果View最终会显示出来, 返回true, 否则返回false
     */
    public static boolean isTargetVisible(View view) {
        int visibility = getTargetVisibility(view);
        return visibility == View.VISIBLE && view.getParent() != null;
    }

    /**
     * 设置View的最终Visibility
     * @param view 指定View
     * @param visibility 要设置的最终的Visibility
     * @see #setTargetVisibility(View, int, long)
     */
    public static void setTargetVisibility(final View view, final int visibility) {
        setTargetVisibility(view, visibility, -1);
    }

    /**
     * 设置View的最终Visibility
     * @param view 指定View
     * @param visibility 要设置的最终的Visibility
     * @param duration 设置动画的时间，-1则表示依赖provider提供的动画默认时长
     */
    public static void setTargetVisibility(final View view, final int visibility, long duration) {
        TransitionObject t = PlayerViewTagUtils.getTag(view, R.id.player_target_visibility, TransitionObject.class);
        if (t == null) {
            t = new TransitionObject();
            t.visibility = view.getVisibility();
            view.setTag(R.id.player_target_visibility, t);
        }
        if (t.visibility == visibility) return;
        if (t.visibility != View.VISIBLE && visibility != View.VISIBLE) {
            t.visibility = visibility;
            return;
        }
        t.visibility = visibility;
        if (t.animator != null) {
            t.animator.cancel();
        }

        VisibilityAnimateProvider provider = PlayerViewTagUtils.getTag(view, R.id.player_visibility_animator_provider, VisibilityAnimateProvider.class);

        if (provider != null) {
            t.animator = new AnimatorWrapper(
                    visibility == View.VISIBLE ? provider.onAppear(view) : provider.onDisappear(view)
            );

            if (duration >= 0) {
                t.animator.setDuration(duration);
            }

            final TransitionObject _t = t;

            t.animator.addListener(new Animator.AnimatorListener() {

                private boolean canceled;

                @Override
                public void onAnimationStart(Animator animation) {
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    _t.animator.removeListener(this);
                    if (!canceled) {
                        view.setVisibility(visibility);
                        if (visibility != View.VISIBLE) {
                            ViewCompat.setAlpha(view, 1);
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    canceled = true;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });

            t.animator.postStart();
        } else {
            view.setVisibility(visibility);
        }
    }

    /** 动画提供者对象 */
    public interface VisibilityAnimateProvider {
        /**
         * 当View出现时的动画
         * @param view 对应的View
         * @return 返回Animator或者ViewPropertyAnimator动画对象
         */
        @NonNull
        Object onAppear(@NonNull View view);
        /**
         * 当View消失时的动画
         * @param view 对应的View
         * @return 返回Animator或者ViewPropertyAnimator动画对象
         */
        @NonNull
        Object onDisappear(@NonNull View view);
    }

    private static class TransitionObject {
        int visibility;
        AnimatorWrapper animator;
    }

    @SuppressWarnings("ConstantConditions")
    private static class AnimatorWrapper implements Runnable {

        final Animator a;
        final ViewPropertyAnimator b;

        final boolean c;
        final Handler h = new Handler();

        boolean posted;

        private AnimatorWrapper(Object animator) {
            if (animator instanceof Animator) {
                a = (Animator) animator;
                b = null;
                c = true;
            } else if (animator instanceof ViewPropertyAnimator) {
                a = null;
                b = (ViewPropertyAnimator) animator;
                c = false;
            } else {
                throw new IllegalArgumentException("can only accept Animator or ViewPropertyAnimator");
            }
        }

        public void run() {
            if (c && !a.isRunning()) start();
        }

        void start() {
            if (c) {
                if (posted) h.removeCallbacks(this);
                a.start();
                posted = false;
            }
            else b.start();
        }

        void setStartDelay(long startDelay) {
            if (c) a.setStartDelay(startDelay);
            else b.setStartDelay(startDelay);
        }

        void setDuration(long duration) {
            if (c) a.setDuration(duration);
            else b.setDuration(duration);
        }

        void postStart() {
            if (c) {
                h.post(this);
                posted = true;
            }
        }

        void cancel() {
            if (c) a.cancel();
            else b.cancel();
        }

        void addListener(Animator.AnimatorListener listener) {
            if (c) a.addListener(listener);
            else b.setListener(listener);
        }

        void removeListener(Animator.AnimatorListener listener) {
            if (c) a.removeListener(listener);
            else b.setListener(null);
        }
    }
}
