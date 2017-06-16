package com.vmovier.player;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.vmovier.lib.player.IPlayerFactory;
import com.vmovier.lib.player.MediaError;
import com.vmovier.lib.player.VideoSize;
import com.vmovier.lib.player.VideoViewDataSource;
import com.vmovier.lib.utils.PlayerLog;
import com.vmovier.lib.view.BasicVideoView;
import com.vmovier.lib.view.IVideoListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BasicVideoViewActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, Runnable {

    public static final String TAG = BasicVideoViewActivity.class.getSimpleName();

    public static final String[] videoTitles = new String[] {
            "DEFAULT", "NULL", "test"
    };

    public static final String[] videoUrls = new String[] {
            "",
            "",
            "http://vjs.zencdn.net/v/oceans.mp4",
    };

    @BindView(R.id.BasicVideoView)
    BasicVideoView mBasicVideoView;
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
    @BindView(R.id.playerTypeTv) TextView mPlayerTypeTv;
    @BindView(R.id.LoadingProgressBar)
    View mLoadingProgressBar;
    @BindView(R.id.SelectDataSource)
    Spinner mDataSourceSpinner;

    public static final String SP_PLAYER = "sp_player";
    public static final String SP_MUTED = "sp_muted";
    public static final String SP_LOOP = "sp_loop";

    //竖屏的时候 播放器的高度. 应该在onCreate的时候计算成功.
    protected int mPortraitPlayerFrameHeight = 0;
    private boolean isPortrait = true;
    private SharedPreferences mSp;
    private IPlayer mPlayer;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_video_view);
        ButterKnife.bind(this);
        mainHandler = new Handler();
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


        int windowWidth = BaseUtil.getScreenWidth(this);
        mPortraitPlayerFrameHeight = Math.round(windowWidth / 16f * 9f);
        if (mPortraitPlayerFrameHeight == 0) {
            mPortraitPlayerFrameHeight = BaseUtil.dip2px(this, 202.5f);
        }
        requestVideoLayoutParam();

        mPlayer = IPlayerFactory.newInstance(this);
        mPlayer.setAutoPlay(true);
        mPlayer.setLoop(isLoop);
        mPlayer.addVideoListener(mVideoListener);
        mPlayer.setPlayerType(isExo ? IPlayer.PLAYERTYPE_EXO : IPlayer.PLAYERTYPE_ANDROIDMEDIA);
        mPlayer.setMuted(isMuted);

        mBasicVideoView.setPosterUrl("http://cs.vmoiver.com/Uploads/Magic/post/2016-12-05/584513723605e.jpg");

        ArrayAdapter<String> urlsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, videoTitles);
        urlsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDataSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PlayerLog.d(TAG, "onItemSelected Position : " + position);
                if (position == 0) {
                    return;
                }
                String url = videoUrls[position];
                VideoViewDataSource dataSource = new VideoViewDataSource(TextUtils.isEmpty(url) ? Uri.EMPTY : Uri.parse(url));
                mPlayer.setMediaDataSource(dataSource);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mDataSourceSpinner.setAdapter(urlsAdapter);
        mDataSourceSpinner.setVisibility(View.VISIBLE);

        mVolumeSeekBar.setMax(100);
        mVolumeSeekBar.setProgress(mPlayer.getVolume());
        mVolumeSeekBar.setOnSeekBarChangeListener(mVolumeSeekListener);

        mSeekBar.setMax(1000);
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        mainHandler.postDelayed(this, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PlayerLog.d("Lifecycle", "onDestroy");
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler = null;
        mPlayer.stopPlayback();
        mPlayer.removeVideoListener(mVideoListener);
        mPlayer = null;
    }

    @Override
    public void run() {
        updateProgress();
        mainHandler.postDelayed(this, 1000);
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
            long duration = mPlayer.getDuration();
            long newPosition = (duration * progress) / 1000L;
            // 如果duration 为0, 则记录1下 seek的比例
            mPlayer.seekTo(newPosition);
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
        long currentPosition = mPlayer.getCurrentPosition();
        long duration = mPlayer.getDuration();
        if (mSeekBar != null) {
            if (duration > 0) {
                long pos = 1000L * currentPosition / duration;
                mSeekBar.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mSeekBar.setSecondaryProgress(percent * 10);
        }
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
        mPlayer.play();
    }

    @OnClick(R.id.pause)
    void pause() {
        mPlayer.pause();
    }


    @OnClick(R.id.bindView)
    void bindView() {
        mBasicVideoView.setVisibility(View.VISIBLE);
        mBasicVideoView.setPlayer(mPlayer);
    }

    @OnClick(R.id.unBindView)
    void unBindView() {
        mBasicVideoView.setVisibility(View.GONE);
        mBasicVideoView.setPlayer(null);
    }

    @OnClick(R.id.stopPlayback)
    void stopPlayBack() {
        mPlayer.stopPlayback();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mPlayerSwitch) {
            SharedPreferences.Editor e = mSp.edit();
            e.putBoolean(SP_PLAYER, isChecked);
            e.apply();
            mPlayer.setPlayerType(isChecked ? IPlayer.PLAYERTYPE_EXO : IPlayer.PLAYERTYPE_ANDROIDMEDIA);
        } else if (buttonView == mMutedSwitch) {
            SharedPreferences.Editor e = mSp.edit();
            e.putBoolean(SP_MUTED, isChecked);
            e.apply();
            mPlayer.setMuted(isChecked);
        } else if (buttonView == mLoopSwitch) {
            SharedPreferences.Editor e = mSp.edit();
            e.putBoolean(SP_LOOP, isChecked);
            e.apply();
            mPlayer.setLoop(isChecked);
        }
    }

    IVideoListener mVideoListener = new IVideoListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            String stateString = "";
            switch (newState) {
                case IPlayer.STATE_IDLE:
                    stateString = "STATE_IDLE";
                    break;
                case IPlayer.STATE_PLAYING:
                    stateString = "STATE_PLAYING";
                    int playerType = mPlayer.getPlayerType();
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
                    MediaError mediaError = mPlayer.getMediaError();
                    if (mediaError != null) {
                        stateString += " 错误类型为:" + mediaError.getErrorCode();
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
        public void onVolumeChanged(int startVolume, int finalVolume) {
            if (mVolumeSeekBar != null) {
                mVolumeSeekBar.setProgress(finalVolume);
            }
        }

        @Override
        public void onVideoSizeChanged(IPlayer mp, VideoSize videoSize) {

        }
    };

    private SeekBar.OnSeekBarChangeListener mVolumeSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }
            // 变更声音
            mPlayer.setVolume(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

}
