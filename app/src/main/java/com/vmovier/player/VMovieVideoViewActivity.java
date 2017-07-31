package com.vmovier.player;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.player.MediaError;
import com.vmovier.lib.player.VideoViewDataSource;
import com.vmovier.lib.utils.PlayerLog;
import com.vmovier.lib.view.VMovieVideoView;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VMovieVideoViewActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = VMovieVideoViewActivity.class.getSimpleName();
    public static final String[] videoTitles = new String[] {
            "DEFAULT", "NULL", "test"
    };

    public static final String[] videoUrls = new String[] {
            "",
            "",
            "http://vjs.zencdn.net/v/oceans.mp4",
    };

    @BindView(R.id.VMovieVideoView)
    VMovieVideoView mVMovieVideoView;
    @BindView(R.id.SeekBar)
    SeekBar mSeekBar;
    @BindView(R.id.VolumeSeekBar) SeekBar mVolumeSeekBar;
    @BindView(R.id.playerLayout)
    FrameLayout mPlayerRootLayout;
    @BindView(R.id.StateTv)
    TextView mStateView;
    @BindView(R.id.loopSwitch)
    SwitchCompat mLoopSwitch;
    @BindView(R.id.playerSwitch)
    SwitchCompat mPlayerSwitch;
    @BindView(R.id.mutedSwitch) SwitchCompat mMutedSwitch;
    @BindView(R.id.allowMeteredNetworkSwitch) SwitchCompat mAllowMeteredNetworkSwitch;
    @BindView(R.id.playerTypeTv) TextView mPlayerTypeTv;
    @BindView(R.id.LoadingProgressBar)
    View mLoadingProgressBar;
    @BindView(R.id.SelectDataSource)
    Spinner mDataSourceSpinner;

    public static final String SP_PLAYER = "sp_player";
    public static final String SP_MUTED = "sp_muted";
    public static final String SP_LOOP = "sp_loop";
    public static final String SP_ALLOWMETEREDNETWORKSWITCH = "sp_allowmeterednetworkswitch";


    public static final String VIDEO_SOURCE = "video_source";
    private static final int UPDATE_PROGRESS = 10;

    //竖屏的时候 播放器的高度. 应该在onCreate的时候计算成功.
    protected int mPortraitPlayerFrameHeight = 0;
    private boolean isPortrait = true;
    private SharedPreferences mSp;
    private Handler mainHandler;
    private static String ISPLAYING_WHENPAUSE = "isplaying_whenpause";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vmovie_videoview);

        ButterKnife.bind(this);
        Bundle bundle = getIntent().getExtras();
        VideoViewDataSource d = null;
        if (bundle != null) {
            d = bundle.getParcelable(VIDEO_SOURCE);
        }

        mSp = getPreferences(MODE_PRIVATE);
        boolean isExo = mSp.getBoolean(SP_PLAYER, true);
        mPlayerSwitch.setChecked(isExo);
        mPlayerSwitch.setOnCheckedChangeListener(this);

        boolean isMuted = mSp.getBoolean(SP_MUTED, false);
        mMutedSwitch.setChecked(isMuted);
        mMutedSwitch.setOnCheckedChangeListener(this);

        boolean isLoop = mSp.getBoolean(SP_LOOP, false);
        mLoopSwitch.setChecked(isLoop);
        mLoopSwitch.setOnCheckedChangeListener(this);

        boolean allow = mSp.getBoolean(SP_ALLOWMETEREDNETWORKSWITCH, false);
        mAllowMeteredNetworkSwitch.setChecked(allow);
        mAllowMeteredNetworkSwitch.setOnCheckedChangeListener(this);

        int windowWidth = BaseUtil.getScreenWidth(this);
        mPortraitPlayerFrameHeight = Math.round(windowWidth / 16f * 9f);
        if (mPortraitPlayerFrameHeight == 0) {
            mPortraitPlayerFrameHeight = BaseUtil.dip2px(this, 202.5f);
        }
        requestVideoLayoutParam();

        final boolean isRestore = (savedInstanceState != null);
        if (isRestore) {
            PlayerLog.d(TAG, "savedInstanceState != null 播放器自己恢复");
            paused = savedInstanceState.getBoolean(ISPLAYING_WHENPAUSE);
        } else {
            PlayerLog.d(TAG, "savedInstanceState == null 重新初始化播放器");
            mVMovieVideoView.setAutoPlay(true);
            mVMovieVideoView.setPlayerType(isExo ? IPlayer.PLAYERTYPE_EXO : IPlayer.PLAYERTYPE_ANDROIDMEDIA);
            mVMovieVideoView.setMuted(isMuted);
            mVMovieVideoView.setLoop(isLoop);
            mVMovieVideoView.setAllowMeteredNetwork(allow);
            mVMovieVideoView.setPosterUrl("http://cs.vmoiver.com/Uploads/Magic/post/2016-12-05/584513723605e.jpg");
            if (d != null) {
                mVMovieVideoView.setMediaDataSource(d);
            }
        }
        mVMovieVideoView.addVMovieVideoViewListener(mVideoListener);
        mVMovieVideoView.setOnGenerateGestureDetectorListener(new TestOnGenerateGestureDetectorListener());

        ArrayAdapter<String> urlsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, videoTitles);
        urlsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDataSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PlayerLog.d(TAG, "onItemSelected Position : " + position);
                if (position == 0) {
                    return;
                }
                if (!isRestore) {
                    String url = videoUrls[position];
                    VideoViewDataSource dataSource = new VideoViewDataSource(TextUtils.isEmpty(url) ? Uri.EMPTY : Uri.parse(url));
                    mVMovieVideoView.setMediaDataSource(dataSource);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mDataSourceSpinner.setAdapter(urlsAdapter);
        mDataSourceSpinner.setVisibility(View.VISIBLE);

        mVolumeSeekBar.setMax(100);
        mVolumeSeekBar.setProgress(mVMovieVideoView.getVolume());
        mVolumeSeekBar.setOnSeekBarChangeListener(mVolumeSeekListener);

        mSeekBar.setMax(1000);
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        Message m = Message.obtain();
        m.what = UPDATE_PROGRESS;
        mainHandler = new VideoHandler(this);
        mainHandler.sendMessageDelayed(m, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler = null;
        mVMovieVideoView.removeVMovieVideoViewListener(mVideoListener);
        mVMovieVideoView.stopPlayback();
        mVMovieVideoView = null;
    }


    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        /**
         * 通知用户已经开始一个触摸拖动手势。客户端可能需要使用这个来禁用seekbar的滑动功能。
         */
        public void onStartTrackingTouch(SeekBar bar) {
        }

        /**
         * 通知进度已经被修改。客户端可以使用fromUser参数区分用户触发的改变还是编程触发的改变。
         * @param bar 当前被修改进度的SeekBar
         * @param progress 当前的进度值。此值的取值范围为0到max之间。Max为用户通过setMax(int)设置的值，默认为100
         * @param fromUser 如果是用户触发的改变则返回True
         */
        public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }
            long duration = mVMovieVideoView.getDuration();
            long newPosition = (duration * progress) / 1000L;
            // 如果duration 为0, 则记录1下 seek的比例
            mVMovieVideoView.seekTo(newPosition);
        }

        /**
         * 通知用户触摸手势已经结束。客户端可能需要使用这个来启用seekbar的滑动功能
         *
         * @param bar
         */
        public void onStopTrackingTouch(SeekBar bar) {
        }
    };

    /** 根据 播放进度 更新 ControlView */
    private void updateProgress() {
        long currentPosition = mVMovieVideoView.getCurrentPosition();
        long duration = mVMovieVideoView.getDuration();
        if (mSeekBar != null) {
            if (duration > 0) {
                long pos = 1000L * currentPosition / duration;
                mSeekBar.setProgress((int) pos);
            }
            int percent = mVMovieVideoView.getBufferPercentage();
            mSeekBar.setSecondaryProgress(percent * 10);
        }
        Message m = Message.obtain();
        m.what = UPDATE_PROGRESS;
        mainHandler.sendMessageDelayed(m, 1000);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        requestVideoLayoutParam();
    }

    private void requestVideoLayoutParam() {
        if (mPlayerRootLayout != null) {
            ViewGroup.LayoutParams lp = mPlayerRootLayout.getLayoutParams();
            if (isPortrait && mPortraitPlayerFrameHeight != lp.height) {
                lp.height = mPortraitPlayerFrameHeight;
                mPlayerRootLayout.setLayoutParams(lp);
            } else if (!isPortrait && ViewGroup.LayoutParams.MATCH_PARENT != lp.height) {
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mPlayerRootLayout.setLayoutParams(lp);
            }
        }
    }

    @OnClick(R.id.play)
    void play() {
        mVMovieVideoView.play();
    }

    @OnClick(R.id.pause)
    void pause() {
        mVMovieVideoView.pause();
    }

    @OnClick(R.id.Suspend)
    void suspend() {
        mVMovieVideoView.suspend();
    }

    @OnClick(R.id.resume)
    void resume() {
        mVMovieVideoView.resume();
    }

    @OnClick(R.id.retry)
    void retry() {
        mVMovieVideoView.retry();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mPlayerSwitch) {
            SharedPreferences.Editor e = mSp.edit();
            e.putBoolean(SP_PLAYER, isChecked);
            e.apply();
            mVMovieVideoView.setPlayerType(isChecked ? IPlayer.PLAYERTYPE_EXO : IPlayer.PLAYERTYPE_ANDROIDMEDIA);
        } else if (buttonView == mMutedSwitch){
            SharedPreferences.Editor e = mSp.edit();
            e.putBoolean(SP_MUTED, isChecked);
            e.apply();
            mVMovieVideoView.setMuted(isChecked);
        } else if (buttonView == mLoopSwitch) {
            SharedPreferences.Editor e = mSp.edit();
            e.putBoolean(SP_LOOP, isChecked);
            e.apply();
            mVMovieVideoView.setLoop(isChecked);
        } else if (buttonView == mAllowMeteredNetworkSwitch) {
            SharedPreferences.Editor e = mSp.edit();
            e.putBoolean(SP_ALLOWMETEREDNETWORKSWITCH, isChecked);
            e.apply();
            mVMovieVideoView.setAllowMeteredNetwork(isChecked);
        }
    }

    VMovieVideoView.IVMovieVideoViewListener mVideoListener = new VMovieVideoView.IVMovieVideoViewListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            String stateString = "";
            switch (newState) {
                case IPlayer.STATE_IDLE:
                    stateString = "STATE_IDLE";
                    break;
                case IPlayer.STATE_PLAYING:
                    stateString = "STATE_PLAYING";
                    int playerType = mVMovieVideoView.getPlayerType();
                    if (playerType == IPlayer.PLAYERTYPE_ANDROIDMEDIA) {
                        mPlayerTypeTv.setText("Android 原生播放器");
                    } else if (playerType == IPlayer.PLAYERTYPE_EXO) {
                        mPlayerTypeTv.setText("ExoPlayer");
                    }
                    break;
                case IPlayer.STATE_BUFFERING:
                    stateString = "STATE_BUFFERING";
                    mLoadingProgressBar.setVisibility(View.VISIBLE);
                    break;
                case IPlayer.STATE_PAUSING:
                    stateString = "STATE_PAUSING";
                    break;
                case IPlayer.STATE_COMPLETED:
                    stateString = "STATE_COMPLETED";
                    break;
                case IPlayer.STATE_ERROR:
                    stateString = "STATE_ERROR";
                    MediaError mediaError = mVMovieVideoView.getMediaError();
                    if (mediaError != null) {
                        if (mediaError.getErrorCode() == MediaError.ERROR_METERED_NETWORK) {
                            stateString += "错误类型:不允许在移动网络下播放";
                        } else {
                            stateString += " 错误类型:" + mediaError.getErrorCode();
                        }
                    }
                    break;
                case IPlayer.STATE_PREPARING:
                    stateString = "STATE_PREPARING";
                    mLoadingProgressBar.setVisibility(View.VISIBLE);
                    break;
            }
            mStateView.setText(stateString);
            if (newState != IPlayer.STATE_BUFFERING && newState != IPlayer.STATE_PREPARING) {
                mLoadingProgressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public void onVolumeChanged(int oldVolume, int newVolume) {
            if (mVolumeSeekBar != null) {
                mVolumeSeekBar.setProgress(newVolume);
            }
            PlayerLog.d(TAG, "onVolumeChanged oldVolume is " + oldVolume + " , newVolume is "  + newVolume);
        }

    };

    private SeekBar.OnSeekBarChangeListener mVolumeSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }
            // 变更声音
            mVMovieVideoView.setVolume(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };
    private boolean paused = false;

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            mVMovieVideoView.play();
            paused = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVMovieVideoView != null && mVMovieVideoView.isPlaying()) {
            mVMovieVideoView.pause();
            paused = true;
        }
    }

    static class VideoHandler extends Handler {
        final WeakReference<VMovieVideoViewActivity> reference;

        public VideoHandler(VMovieVideoViewActivity a) {
            reference = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(Message msg) {
            VMovieVideoViewActivity activity = reference.get();
            if (activity == null) return;
            activity.updateProgress();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ISPLAYING_WHENPAUSE, paused);
    }
}
