package cas.igsnrr.dronefly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.ProgressDialog;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.PlaybackManager;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKManager;

public class PlaybackActivity extends AppCompatActivity implements View.OnClickListener {

    /// UI视图
    private TextureView mTextureViewPlayback;
    private TextView mTvPlaybackMode;
    private ConstraintLayout mLytMediaButtons;
    private Button mBtnBack, mBtnPlaybackMode, mBtnDownload, mBtnDelete;
    private LinearLayout mLytControlsForMutipleFiles, mLytControlsForSingleFiles;

    private Button mBtnMultiplePrevious, mBtnMultipleNext, mBtnMultipleSelect, mBtnMultipleSelectAll;
    private Button mBtnSinglePrevious, mBtnSingleNext, mBtnSinglePlay;

    private View.OnClickListener mMediaButtonOnClickListener;

    /// 用于接收Playback回放视频流
    // 编码译码器
    private DJICodecManager mCodecManager;
    // VideoFeed视频流数据监听器
    private VideoFeeder.VideoDataListener mVideoDataListener;

    /// 回放管理器
    private PlaybackManager mPlaybackManager;
    // 回放状态
    private SettingsDefinitions.PlaybackMode mPlaybackMode;
    // 当前页面中是否所有的文件都已选择
    private boolean mIsAllFilesInPageSelected;

    // 文件下载对话框
    private ProgressDialog mPgsDlgDownload;

    // region Activity生命周期
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        // 初始化UI界面
        initUI();

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 初始化回放相关对象
        initPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 重置回放相关对象
        resetPlayback();
    }
    // endregion

    //  region UI界面与事件
   private void initUI() {
       mTextureViewPlayback = findViewById(R.id.texture_playback); // 视频流显示TextureView

       mLytControlsForMutipleFiles = findViewById(R.id.lyt_controls_for_mutiple_files); // 多文件控件组
       mLytControlsForSingleFiles = findViewById(R.id.lyt_controls_for_single_file); // 单文件控件组
       mLytMediaButtons = findViewById(R.id.lyt_media_buttons); // 文件选择按钮组
       mTvPlaybackMode = findViewById(R.id.tv_playback_mode); // 回放状态文本框
       mBtnBack = findViewById(R.id.btn_back); // 返回按钮
       mBtnPlaybackMode = findViewById(R.id.btn_playback_mode); // 切换单文件/多文件模式按钮
       mBtnDownload = findViewById(R.id.btn_download); // 下载按钮
       mBtnDelete = findViewById(R.id.btn_delete); // 删除按钮

       mBtnMultiplePrevious = findViewById(R.id.btn_multiple_previous); // 上一页按钮
       mBtnMultipleNext = findViewById(R.id.btn_multiple_next); // 下一页按钮
       mBtnMultipleSelect = findViewById(R.id.btn_multiple_select); // 选择按钮
       mBtnMultipleSelectAll = findViewById(R.id.btn_multiple_select_all); // 全选按钮

       mBtnSinglePrevious = findViewById(R.id.btn_single_previous); // 上一个按钮
       mBtnSingleNext = findViewById(R.id.btn_single_next); // 下一个按钮
       mBtnSinglePlay = findViewById(R.id.btn_single_play); // 播放按钮

       mBtnBack.setOnClickListener(this);
       mBtnPlaybackMode.setOnClickListener(this);
       mBtnDownload.setOnClickListener(this);
       mBtnDelete.setOnClickListener(this);

       mBtnMultiplePrevious.setOnClickListener(this);
       mBtnMultipleNext.setOnClickListener(this);
       mBtnMultipleSelect.setOnClickListener(this);
       mBtnMultipleSelectAll.setOnClickListener(this);

       mBtnSinglePrevious.setOnClickListener(this);
       mBtnSingleNext.setOnClickListener(this);
       mBtnSinglePlay.setOnClickListener(this);

       // 设置媒体文件按钮监听器
       mMediaButtonOnClickListener = new View.OnClickListener() {
           @Override
           public void onClick(View v) {
                int mediaIndex = Integer.parseInt((String) v.getTag());
               // 当处在单文件模式下，单击无效
               if (mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_PHOTO_PREVIEW
                       || mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_VIDEO_PLAYBACK_PAUSED
                       || mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_VIDEO_PLAYBACK_START) {
                    return;
               }
               // 当处在多文件编辑模式下，选择/取消选择该媒体文件
               if (mPlaybackMode == SettingsDefinitions.PlaybackMode.MULTIPLE_FILES_EDIT) {
                   mPlaybackManager.toggleFileSelectionAtIndex(mediaIndex);
               }
               // 当处在多文件预览模式下，进入该媒体文件的单文件预览模式
               if (mPlaybackMode == SettingsDefinitions.PlaybackMode.MULTIPLE_MEDIA_FILE_PREVIEW ) {
                   mPlaybackManager.enterSinglePreviewModeWithIndex(mediaIndex);
                   mLytControlsForSingleFiles.setVisibility(View.VISIBLE);
                   mLytControlsForMutipleFiles.setVisibility(View.GONE);
                   mLytMediaButtons.setVisibility(View.GONE);
               }
           }
       };
       findViewById(R.id.btn_media_1).setOnClickListener(mMediaButtonOnClickListener);
       findViewById(R.id.btn_media_2).setOnClickListener(mMediaButtonOnClickListener);
       findViewById(R.id.btn_media_3).setOnClickListener(mMediaButtonOnClickListener);
       findViewById(R.id.btn_media_4).setOnClickListener(mMediaButtonOnClickListener);
       findViewById(R.id.btn_media_5).setOnClickListener(mMediaButtonOnClickListener);
       findViewById(R.id.btn_media_6).setOnClickListener(mMediaButtonOnClickListener);
       findViewById(R.id.btn_media_7).setOnClickListener(mMediaButtonOnClickListener);
       findViewById(R.id.btn_media_8).setOnClickListener(mMediaButtonOnClickListener);

       // 初始化文件下载对话框
       mPgsDlgDownload = new ProgressDialog(PlaybackActivity.this);
       mPgsDlgDownload.setTitle("媒体文件下载中...");
       mPgsDlgDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
       mPgsDlgDownload.setCanceledOnTouchOutside(false);
       mPgsDlgDownload.setCancelable(false);

       // 控制按钮全部不显示
       mLytControlsForSingleFiles.setVisibility(View.GONE);
       mLytControlsForMutipleFiles.setVisibility(View.GONE);
       mLytMediaButtons.setVisibility(View.GONE);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back: back(); break;
            case R.id.btn_playback_mode: changePlaybackMode(); break;
            case R.id.btn_download: download(); break;
            case R.id.btn_delete: delete(); break;
            case R.id.btn_multiple_previous: toPreviousPage(); break;
            case R.id.btn_multiple_next: toNextPage(); break;
            case R.id.btn_multiple_select: select(); break;
            case R.id.btn_multiple_select_all: selectAll(); break;
            case R.id.btn_single_previous: toPreviousFile(); break;
            case R.id.btn_single_next: toNextFile(); break;
            case R.id.btn_single_play: playvideo(); break;
        }
    }

    // 返回
    private void back() {
        this.finish();
    }

    // 切换单文件/多文件模式
    private void changePlaybackMode() {
        if (mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_PHOTO_PREVIEW
                || mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_VIDEO_PLAYBACK_PAUSED
                || mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_VIDEO_PLAYBACK_START) {
            // 当处在单文件模式下，进入多文件模式
            mPlaybackManager.enterMultiplePreviewMode();
            // 设置用于多文件控制的控件可见
            mLytControlsForSingleFiles.setVisibility(View.GONE);
            mLytControlsForMutipleFiles.setVisibility(View.VISIBLE);
            mLytMediaButtons.setVisibility(View.VISIBLE);
        } else {
            // 当处在多文件模式下，进入单文件模式
            mPlaybackManager.enterSinglePreviewModeWithIndex(0);
            // 设置用于单文件控制的控件可见
            mLytControlsForMutipleFiles.setVisibility(View.GONE);
            mLytControlsForSingleFiles.setVisibility(View.VISIBLE);
            mLytMediaButtons.setVisibility(View.GONE);
        }
    }

    // 下载
    private void download() {
        if (mPlaybackMode != SettingsDefinitions.PlaybackMode.MULTIPLE_FILES_EDIT) {
            showToast("请进入多文件编辑模式下，选择下载文件!");
            return;
        }
        // 设置下载位置
        final File downloadDir = new File(getExternalFilesDir(null) + "/media/");
        mPlaybackManager.downloadSelectedFiles(downloadDir, new PlaybackManager.FileDownloadCallback() {
            @Override
            public void onStart() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPgsDlgDownload.incrementProgressBy(-mPgsDlgDownload.getProgress()); // 将下载进度设置为0
                        mPgsDlgDownload.show();
                    }
                });
            }

            @Override
            public void onEnd() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mPgsDlgDownload.dismiss();
                    }
                });
                showToast("文件下载成功,下载位置为:" + downloadDir);
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPgsDlgDownload.cancel();
                    }
                });
                showToast("文件下载失败!");
            }

            @Override
            public void onProgressUpdate(int i) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mPgsDlgDownload.incrementProgressBy(-mPgsDlgDownload.getProgress() + i);
                    }
                });
            }
        });
    }

    // 删除
    private void delete() {
        // 多文件编辑模式下，删除当前选择的文件
        if (mPlaybackMode.equals(SettingsDefinitions.PlaybackMode.MULTIPLE_FILES_EDIT)) {
            mPlaybackManager.deleteAllSelectedFiles();

        }
        // 单文件预览模式下，删除当前文件
        if(mPlaybackMode.equals(SettingsDefinitions.PlaybackMode.SINGLE_PHOTO_PREVIEW)){
            mPlaybackManager.deleteCurrentPreviewFile();
        }
    }


    // 上一页（多文件控制）
    private void toPreviousPage() {
        mPlaybackManager.proceedToPreviousMultiplePreviewPage();
    }

    // 下一页（多文件控制）
    private void toNextPage() {
        mPlaybackManager.proceedToNextMultiplePreviewPage();
    }

    // 选择（多文件控制）
    private void select() {
        // 当处在多文件预览模式下，进入多文件编辑模式
        if (mPlaybackMode == SettingsDefinitions.PlaybackMode.MULTIPLE_MEDIA_FILE_PREVIEW) {
            mPlaybackManager.enterMultipleEditMode();
        }
        // 当处在多文件编辑模式下，进入多文件预览模式
        if (mPlaybackMode == SettingsDefinitions.PlaybackMode.MULTIPLE_FILES_EDIT) {
            mPlaybackManager.exitMultipleEditMode();
        }
    }

    // 全选（多文件控制）
    private void selectAll() {
        // 如果当前所有的文件都已经选择,则取消所有的选择
        if (mIsAllFilesInPageSelected) {
            mPlaybackManager.unselectAllFiles();
            return;
        }
        // 选择当前页面中所有的文件
        mPlaybackManager.selectAllFilesInPage();

    }

    // 上一个（单文件控制）
    private void toPreviousFile() {
        mPlaybackManager.proceedToPreviousSinglePreviewPage();
    }

    // 下一个（单文件控制）
    private void toNextFile() {
        mPlaybackManager.proceedToNextSinglePreviewPage();
    }

    // 播放 （单文件控制）
    private void playvideo() {
        // 当视频暂停或停止时，播放视频
        if (mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_VIDEO_PLAYBACK_PAUSED
                || mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_PHOTO_PREVIEW) {
            mPlaybackManager.playVideo();
            mBtnSinglePlay.setText("停止");
        }
        // 当视频播放时，暂停视频
        if (mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_VIDEO_PLAYBACK_START) {
            mPlaybackManager.stopVideo();
            mBtnSinglePlay.setText("播放");
        }
    }

    // endregion

    // region 初始化与重置回放相关对象

    // 初始化回放相关对象
    private void initPlayback() {

        // 为用于显示回放视频流数据的TextureView设置监听器
        mTextureViewPlayback.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                                  int width, int height) {
                // 在SurfaceTexture可用时创建解码译码器
                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(PlaybackActivity.this,
                            surface, width, height);
                    // 在创建编码译码器之后调整TextureView的高度和宽度
                    fitTextureViewToFPV();
                    // 在视频流尺寸发生变化时调整TextureView的高度和宽度
                    mCodecManager.setOnVideoSizeChangedListener(new DJICodecManager.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(int i, int i1) {
                            fitTextureViewToFPV();
                        }
                    });

                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                    int width, int height) {
                // 在SurfaceTexture尺寸变化后调整TextureView的尺寸
                fitTextureViewToFPV();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                // 在SurfaceTexture销毁时释放解码译码器
                if (mCodecManager != null) {
                    mCodecManager.cleanSurface();
                    mCodecManager = null;
                }
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        // 为VideoFeed设置视频流数据监听器
        if (mVideoDataListener == null) {
            mVideoDataListener = new VideoFeeder.VideoDataListener() {
                @Override
                public void onReceive(byte[] bytes, int i) {
                    if (mCodecManager != null) {
                        mCodecManager.sendDataToDecoder(bytes, i);
                    }
                }
            };
        }

        VideoFeeder.getInstance().getPrimaryVideoFeed()
                .addVideoDataListener(mVideoDataListener);

        Camera camera = getCamera();
        // 判断相机对象非空，且支持回放模式
        if (camera == null) {
            showToast("相机对象获取错误!");
            return;
        }
        if (!camera.isPlaybackSupported()) {
            showToast("当前相机不支持回放模式!");
            return;
        }
        // 设置相机模式为回放(Playback)模式
        camera.setMode(SettingsDefinitions.CameraMode.PLAYBACK, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    showToast("相机模式设置错误!" + djiError.getDescription());
                }
            }
        });

        // 获取回放管理器
        mPlaybackManager = camera.getPlaybackManager();
        if (mPlaybackManager == null) {
            showToast("回放管理器错误!");
            return;
        }
        // 设置回放状态回调
        mPlaybackManager.setPlaybackStateCallback(new PlaybackManager.PlaybackState.CallBack() {
            @Override
            public void onUpdate(PlaybackManager.PlaybackState playbackState) {
                // 获取回放状态状态
                mPlaybackMode = playbackState.getPlaybackMode();
                // 获取当前页面的媒体是否已经全部选中
                mIsAllFilesInPageSelected = playbackState.areAllFilesInPageSelected();
                // 设置UI界面
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 显示回放状态信息
                        String playbackMode = playbackModetoString(playbackState.getPlaybackMode());
                        mTvPlaybackMode.setText("回放状态:" + playbackMode);
                        // 设置播放按钮
                        if (playbackState.getFileType() == SettingsDefinitions.FileType.VIDEO) {
                            mBtnSinglePlay.setVisibility(View.VISIBLE);
                        } else {
                            mBtnSinglePlay.setVisibility(View.GONE);
                        }
                        if (mPlaybackMode == SettingsDefinitions.PlaybackMode.SINGLE_VIDEO_PLAYBACK_START) {
                            mBtnSinglePlay.setText("停止");
                        } else {
                            mBtnSinglePlay.setText("播放");
                        }

                    }
                });
            }
        });
        // 进入单文件模式
        mPlaybackManager.enterSinglePreviewModeWithIndex(0);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLytControlsForSingleFiles.setVisibility(View.VISIBLE);
                mLytControlsForMutipleFiles.setVisibility(View.GONE);
                mLytMediaButtons.setVisibility(View.GONE);
            }
        });
    }


    // 使TextureView的宽高比适合视频流
    private void fitTextureViewToFPV() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 用于获取屏幕高度和宽度的DisplayMetrics对象。
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                // 图传视频的宽高比
                double videoratio = mCodecManager.getVideoWidth() * 1.0
                        / mCodecManager.getVideoHeight();
                // 设备屏幕的宽高比
                double textureratio = dm.widthPixels * 1.0 / dm.heightPixels;
                if (videoratio == textureratio) {
                    // 无需调整，直接返回
                    return;
                }
                // 开始设置TextureView的宽度和高度
                ViewGroup.LayoutParams layoutParams = mTextureViewPlayback.getLayoutParams();
                if (videoratio > textureratio) {
                    // 如果视频宽高比更大，则使TextureView的宽度占满屏幕，设置其高度满足图传的宽高比
                    layoutParams.height = (int) (dm.widthPixels / videoratio);
                    layoutParams.width = dm.widthPixels;
                }
                if (videoratio < textureratio) {
                    // 如果设备宽高比更大，则使TextureView的高度占满屏幕，设置其宽度满足图传的宽高比
                    layoutParams.height = dm.heightPixels;
                    layoutParams.width = (int) (dm.heightPixels * videoratio);
                }
                // 设置TextureView的宽度和高度
                mTextureViewPlayback.setLayoutParams(layoutParams);
                // 通知编码译码器TextureView的宽度和高度的变化
                mCodecManager.onSurfaceSizeChanged(layoutParams.width, layoutParams.height, 0);
            }
        });
    }

    // 重置回放相关对象
    private void resetPlayback() {

        // 移除相机回调
        Camera camera = getCamera();
        if (camera != null) {
            camera.setSystemStateCallback(null);

            // 设置相机模式为拍照模式
            camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast("相机模式设置错误!" + djiError.getDescription());
                    }
                }
            });
        }
        // 取消回放状态回调
        if (mPlaybackManager != null)
            mPlaybackManager.setPlaybackStateCallback(null);

        // 移除VideoFeed的视频流数据监听器
        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mVideoDataListener);
    }

    // endregion

    // region 获取相机对象

    // 获得无人机的相机对象
    private Camera getCamera() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            return product.getCamera();
        }
        return null;
    }
    // endregion

    // region 其他

    // 在主线程中显示提示
    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // endregion

    // region 枚举值与字符串的转换

    // 相机模式枚举值转字符串
    private String playbackModetoString(SettingsDefinitions.PlaybackMode playbackMode) {
        switch (playbackMode)
        {
            case SINGLE_PHOTO_PREVIEW:
                return "SINGLE_PHOTO_PREVIEW 单一文件预览";
            case SINGLE_VIDEO_PLAYBACK_START:
                return "SINGLE_VIDEO_PLAYBACK_START 单一视频文件播放中";
            case SINGLE_VIDEO_PLAYBACK_PAUSED:
                return "SINGLE_VIDEO_PLAYBACK_PAUSED 单一视频文件暂停中";
            case MULTIPLE_FILES_EDIT:
                return "MULTIPLE_FILES_EDIT 多文件编辑";
            case MULTIPLE_MEDIA_FILE_PREVIEW:
                return "MULTIPLE_MEDIA_FILE_PREVIEW 多文件预览";
            case MEDIA_FILE_DOWNLOAD:
                return "MEDIA_FILE_DOWNLOAD 下载中";
            case UNKNOWN:
                return "UNKNOWN 未知";
            default:
                return "N/A";
        }
    }

    // endregion
}
