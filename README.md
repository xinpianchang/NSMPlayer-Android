NSMPlayer 可植入多个基于不同播放器内核的功能强大的视频播放器。默认提供了基于 ExoPlayer/AndroidMediaPlayer 为内核的播放器。

## Features

- 播放器和渲染层可以分离
- 播放器支持销毁和恢复功能
- 播放器底层使用层次状态机来管理播放器的状态
- 添加是否允许 3G/4G 网络播放功能
- 播放器的控制面板 UI 和播放器分离，支持完全自定义控制面板的 UI
- 支持循环播放、自动播放等播放器的默认功能

## Supported Formats

[MediaPlayer](https://developer.android.com/guide/topics/media/media-formats.html) 支持的媒体格式  
[ExoPlayer](https://google.github.io/ExoPlayer/supported-formats.html) 支持的媒体格式


## Requirements

```Java
defaultConfig {
    minSdkVersion 19
    targetSdkVersion 23
}
```

## Installation

### Download
- Gradle
```
# required
allprojects {
    repositories {
        jcenter()
    }
}

dependencies {
    compile 'com.vmovier.libs:player:2.5.4' 
}
```

## Getting Started
1. 在你的 `Application.onCreate()` 时 加入此行代码。
```Java
Player.init(this);
```
2. 你要把我们提供的 `VMovieVideoView` 加入到 xml 中。
```XML
<com.vmovier.lib.view.VMovieVideoView
    android:id="@+id/videoView"
    android:layout_width="match_parent"
    android:layout_height="220dp"/>             
```
3. 你需要在代码中拿到 `VMovieVideoView` 的实例，你可以用以下几行简单代码进行一个影片的播放。
```Java
VideoViewDataSource d = new VideoViewDataSource(Uri.parse("你希望播放的影片资源地址"))
mVMovieVideoView.setMediaDataSource(d);
mVMovieVideoView.play();        
```
## How To Use 
### 项目架构图
![架构图](https://github.com/xinpianchang/NSMPlayer-Android/raw/master/Player.png)
结构图说明:
1. 整体分为三条路线 `IPlayer`、 `BasicVideoView` 、 `IRenderView` .
2. `IPlayer` 为对外暴露的播放器接口， 其中 `VMoviePlayer` 是 `IPlayer` 的具体实现， `VMoviePlayer` 继承了 `StateMachine` 。内部持有底层`IInternalPlayer` 的成员变量。其中 `IInternalPlayer` 是 底层播放器的行为接口， 后续如果有新的播放器内核加入， 只需要implement `IInternalPlayer`接口，完成新的实现，既可快速的加入到项目中。现在已经实现的播放器内核有谷歌现在力推的 `ExoPlayer` ,以及 `AndroidMediaplayer`。
`VMoviePlayer` 内部定义了一套自己的 State，来协调管理不同的播放器。 在 `VMoviePlayer` 中适配不同播放器的异同性， 保证了 `VMoviePlayer` 对外只会暴露出一套行为规范。
PlayerState 图如下:  
  ![状态图](https://github.com/xinpianchang/NSMPlayer-Android/raw/master/PlayerState.png)  

    状态说明:
  - IdleState 是整个播放器的起始状态，Error 状态是播放器出错之后的状态，二者都属于播放器无法工作的状态，为 UnWorkingState。 
  - PreparingState 是播放器在初始化的状态， 在初始化完成后,会跳入 PreparedStat。 如果初始化失败,会跳入 ErrorState。
  - PreparedState 是播放器加载完成的状态， 其中分为两个子状态, Played 和 Paused。
  - Played 为播放器 播放状态，分为 Playing 正在播放状态以及 buffering 正在缓冲状态。
  - Paused 为播放器 暂停状态，分为 Pausing 暂停状态, 以及 Completed 播放完成状态。
  - `VMoviePlayer` 从初始化到设置播放视频资源，到开始加载 再到开始播放，再到暂停销毁阶段，整个生命周期中都是不依赖 `Surface` 的， `Surface` 可以在 任何状态下根据自身的需要被设置进 `VMoviePlayer` 中。
3. `IRenderView` 是渲染 View 的行为接口， 可以设置渲染 View 的 ScaleType ，以及添加 RenderView Create changed Destory的回调监听，现有的具体实现只有 `SurfaceViewRenderView` ，未来会加入 `TextureViewRenderView`。
4. `BasicVideoView` 是比较简单的一个 ViewGroup 实现，内部定义了一些简单的 View 层的实现， 比如海报的显示逻辑，比如RenderView的构造逻辑。  `BasicVideoView` 是串联 `IPlayer` 和 `IRenderView` 的地方，二者是他的成员变量， 在 `BasicVideoView` 中定义了二者之间的生命周期关系，使用者可以自由的组合 `BasicVideoView` 与 `IPlayer` ，完成自己的需求，而 `VMovieVideoView` 是一个比较定制化的 `BasicVideoView` ， 内部完成了一套非常完整的  `IPlayer`和 `IRenderView`协作的逻辑，使用者可以非常方便的完成视频播放。  
5. 结构图中部分类只选取比较有代表性的接口展示，完整接口需要去项目中看。

### `VMovieVideoView` 的一些具体使用细节
#### 在 XML 中 `VMovieVideoView` 进行设置
```XML
<com.vmovier.lib.view.VMovieVideoView
    android:id="@+id/VMovieVideoView"
    app:playerType="exo_MediaPlayer" // 播放器类型 可选选项: 1 exo_MediaPlayer 2 android_MediaPlayer
    app:needShowPosterView="true" // 是否显示海报 可选选项: 1 true 2 false
    app:renderViewType="render_surface_view" //渲染View的类型 1 render_surface_view 2 render_texture_view
    app:scaleType="scale_fit_parent" // 影片渲染的比例 可选选项: 1 scale_fit_parent 2 scale_fill_parent 3 scale_wrap_content 4  scale_match_parent 5 scale_16_9_fit_parent 6 scale_4_3_fit_parent
    app:muted="false"   // 是否静音 可选选项: 1 true 2 false
    app:loop="false"    // 是否循环播放 可选选项: 1 true 2 false
    app:autoPlay="false"  // 是否自动播放 可选选项: 1 true 2 false
    app:useController="true"    // 是否使用 ControllerView 可选选项: 1 true 2 false
    app:controllerShowTimeoutMs="3000" // ControllerView 显示的时间
    app:defaultControlViewMode="portrait_inset" // ControllerView 的Mode 可选选项: 1 portrait_inset 竖屏小屏 2 portrait_fullscreen 竖屏全屏 3 landscape_fullscreen 横屏全屏 三种模式
    app:landscapeViewControllerLayoutId="@layout/videoplayer_landscape_control" // 横屏全屏下你的自定义 ControllerView 布局
    app:portraitFullScreenViewControllerLayoutId="@layout/videoplayer_portrait_fullscreen_control" // 竖屏全屏下你的自定义 ControllerView 的布局
    app:portraitInsetViewControllerLayoutId="@layout/videoplayer_portrait_inset_control" // 竖屏小屏下你的自定义 ControllerView 布局
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```
#### 在代码中对 `VMovieVideoView` 进行不同的设置
```Java
mVMovieVideoView.setPlayerType(IPlayer.PLAYERTYPE_EXO); // 设置播放器类型
mVMovieVideoView.setNeedShowPosterView(true); // 是否显示海报
mVMovieVideoView.setMuted(isMuted); // 是否静音
mVMovieVideoView.setLoop(isLoop); // 是否循环播放
mVMovieVideoView.setAutoPlay(true);  // 是否自动播放
mVMovieVideoView.setRenderType(1); //设置渲染类型
mVMovieVideoView.setAllowMeteredNetwork(allow); // 设置是否允许移动网络播放
mVMovieVideoView.setScreenMode(BasicVideoView.PLAYERSCREENMODE_PORTRAIT_INSET);// ControllerView 的 Mode 分为竖屏小屏 竖屏全屏 横屏全屏三种模式
mVMovieVideoView.setVolume(0-100); // 设置音量
mVMovieVideoView.setUseController(true); //是否使用控制View
mVMovieVideoView.setPosterUrl("你希望播放的影片的海报地址");
VideoViewDataSource d = new VideoViewDataSource(Uri.parse("你希望播放的影片资源地址"));
mVMovieVideoView.setMediaDataSource(d); // 如果你设置了AutoPlay 为true 那么设置完播放资源之后他就会自动加载,否则你需要手动调用play()       
```
    
#### 监听播放器的状态变化

```Java
mVMovieVideoView.addVideoStateListener(new IVideoStateListener() {
    @Override
    public void onStateChanged(int oldState, int newState){
        // 播放状态发生改变后会回调该方法，oldState 为之前的状态，newState 为当前状态。
        switch (newState) {
            case IPlayer.STATE_IDLE:
                break;
            case IPlayer.STATE_PLAYING:
                break;
            case IPlayer.STATE_BUFFERING:
                break;
            case IPlayer.STATE_PAUSING:
                break;
            case IPlayer.STATE_COMPLETED:
                break;
            case IPlayer.STATE_ERROR:
                break;
            case IPlayer.STATE_PREPARING:
                break;
         }
    }
        
    @Override
    public void onVolumeChanged(int oldVolume, int newVolume) {
         // 播放器的声音发生改变后的回调
    }
}) 
```
#### 监听网络变化
如果你希望播放器在移动网络下 3G/4G 依然可以进行加载播放的话，那么请在播放之前设置:
```Java
mVMovieVideoView.setAllowMeteredNetwork(true);
```
否则的话播放器在 3G/4G 网络下是无法进行播放的，并且播放器会把这个事件作为一个 Error 事件汇报给你， 你可以这样去监听到这个行为
```Java
mVMovieVideoView.addVideoStateListener(new IVideoStateListener() {
    @Override
    public void onStateChanged(int oldState, int newState) {
        // 播放状态发生改变后会回调该方法, oldState 为之前的状态, newState 为当前状态
        switch (newState) {
             ...
            case IPlayer.STATE_ERROR:
                MediaError mediaError = mVMovieVideoView.getMediaError();
                if (mediaError != null) {
                     if (mediaError.getErrorCode() == MediaError.ERROR_METERED_NETWORK) {
                          // 你可以在这里去弹出对话框或者做别的操作去提示用户是否允许移动网络播放, 如果用户同意
                          // 请调用mVMovieVideoView.setAllowMeteredNetwork(true); 即可恢复播放
                      } else {
                           stateString += " 错误类型:" + mediaError.getErrorCode();
                      }
                }
                break;
             ...
        }
    }
    
    @Override
    public void onVolumeChanged(int oldVolume, int newVolume) {
        // 播放器的声音发生改变后的回调
    }
})        
```

#### 销毁和恢复播放器
`VMovieVideoView` 支持随时销毁和恢复，销毁的时候内部会保持当前播放器的一切状态，(包括播放进度,播放地址,以及一些播放器的设置)  
你可以这样进行销毁
```Java
mVMovieVideoView.suspend();
```
当你需要恢复播放器的时候
```Java
mVMovieVideoView.resume();
```
这个地方要特殊说明一下，rusume()方法 只有在播放器调用过suspend()以后调用才会有作用，平时调用是不会有回应的。

当你在一个 `Activity` 中使用 `VMovieVideoView` 的时候，你可以在 `onStop()` 中调用 `mVMovieVideoView.suspend()` 进行销毁播放器。 在 `Activity` 的 `onStart()` 中去调用 `mVMovieVideoView.resume()` 进行恢复， 当然这都仅仅是建议，具体如何使用需要看你自己的业务要求。
值得一提的是，如果你在 `Activity.onStop()` 之后调用过 `mVMovieVideoView.suspend()` 的话，接下来就算你的 `Activity` 被系统回收掉，等你再回来这个`Activity` 的时候，`VMovieVideoView` 依然能恢复到原本的状态，不过这个时候你要防止在你 `Activity.onCreate()` 中再次去初始化播放器，那样会破坏掉你之前存储的播放状态。
下面是一个简单的例子
```Java
class YourActivity extends Activity {
    private boolean mIsPlayingWhenActivityPause = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // 你什么都不需要做, 播放器会自己进行恢复
        } else {
            // 这说明Activity是初次创建
            VideoViewDataSource d = new VideoViewDataSource(Uri.parse("你希望播放的影片资源地址"))
            mVMovieVideoView.setMediaDataSource(d);
            mVMovieVideoView.play();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        VLog.logMethod(TAG);
        mVMovieVideoView.resume();
    }

    @Override
    public void onStop() {
        super.onStop();
        VLog.logMethod(TAG);
        mVMovieVideoView.suspend();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVMovieVideoView.isPlaying()) {
            mVMovieVideoView.pause();
            mIsPlayingWhenActivityPause = true;
        } else {
            mIsPlayingWhenActivityPause = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsPlayingWhenActivityPause) {
            mVMovieVideoView.play();
            mIsPlayingWhenActivityPause = false;
        }
    }
}      
```


#### 出错后恢复播放器
在监听到播放器发生错误以后 调用 `mVMovieVideoView.retry()` 去重新试图恢复播放器出错之前的状态
```Java
mVMovieVideoView.retry()
``` 

#### 定制自己的 touch 处理
`VMovieVideoView` 提供了一套默认 touch 处理,包括单击 双击 滑动，对应会有 播放 暂停音量 进度 亮度等的改变，详情可以看 view 包下的`DefaultOnGenerateGestureDetectorListener` 和 `DefaultOnGestureListener`， 但是我们也提供了给使用者自己自定义的机会.使用者可以调用
```Java
VMovieVideoView.setOnGenerateGestureDetectorListener(your OnGenerateGestureDetectorListener); 
```
来自己处理所有的 touch 事件。

## Common Problems

播放器的默认内核是基于 ExoPlayer，所以支持的最低版本也是基于 ExoPlayer 的最低支持版本。

## Support (支持)

在使用过程中有任何问题和疑问都可以在github上给我提issue，我会尽快一一回复。

## Licenses

NSMPlayer 使用 MIT 许可证，详情见 LICENSE 文件。
