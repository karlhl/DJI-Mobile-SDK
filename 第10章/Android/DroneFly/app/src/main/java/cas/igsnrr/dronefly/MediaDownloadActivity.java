package cas.igsnrr.dronefly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.FetchMediaTask;
import dji.sdk.media.FetchMediaTaskContent;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;
import dji.sdk.sdkmanager.DJISDKManager;

public class MediaDownloadActivity extends AppCompatActivity implements View.OnClickListener {

    /// 视图与控件
    // 媒体文件显示列表
    private RecyclerView mMediaFileRecyclerView;
    // 媒体文件图片显示视图
    private ImageView mIvMediaFile;
    // 媒体文件视频播放视图
    private ConstraintLayout mLayoutVideo;
    // 视频回放TextureView
    private TextureView mTvPlayback;
    // 视频回放相关按钮
    private Button mBtnVideoPlay, mBtnVideoPause, mBtnVideoGoto, mBtnVideoBack;
    // 视频回放状态文本框
    private TextView mTvVideoInformation;
    // 文件下载对话框
    private ProgressDialog mPgsDlgDownload;

    /// 以下对象用于显示媒体文件列表
    // 媒体文件显示列表的Adapter
    private MediaFileAdapter mMediaFileAdapter;
    // 媒体管理器
    private MediaManager mMediaManager;
    // 媒体任务调度器
    private FetchMediaTaskScheduler mScheduler;

    /// 以下对象用于接收视频回放视频流
    // 编码译码器
    private DJICodecManager mCodecManager;
    // VideoFeed视频流数据监听器
    private VideoFeeder.VideoDataListener mVideoDataListener;
    // 当前视频预览的媒体文件索引
    private int mCurrentMediaFileIndex;

    // 文件列表状态
    private MediaManager.FileListState mFileListState= MediaManager.FileListState.UNKNOWN;
    // 文件列表状态监听器
    private MediaManager.FileListStateListener mFileListStateListener = new MediaManager.FileListStateListener() {
        @Override
        public void onFileListStateChange(MediaManager.FileListState fileListState) {
            mFileListState = fileListState;
        }
    };

    // 视频回放状态
    private MediaFile.VideoPlaybackStatus mVideoPlaybackStatus = MediaFile.VideoPlaybackStatus.UNKNOWN;
    // 视频回放状态监听器
    private MediaManager.VideoPlaybackStateListener mVideoPlaybackStateListener = new MediaManager.VideoPlaybackStateListener() {
        @Override
        public void onUpdate(MediaManager.VideoPlaybackState videoPlaybackState) {
            mVideoPlaybackStatus = videoPlaybackState.getPlaybackStatus();
            if (videoPlaybackState.getPlayingMediaFile() == null)
                return;
            String strOutput = "";
            // 获取视频回放文件名
            strOutput += ("文件名:" + videoPlaybackState.getPlayingMediaFile().getFileName() + "\n");
            // 获取视频回放缓存比例
            strOutput += ("视频回放缓存比例:" + videoPlaybackState.getCachedPercentage() + "%\n");
            // 获取视频回放缓存位置
            strOutput += ("视频回放缓存位置:" + videoPlaybackState.getCachedPosition() + "秒\n");
            // 获取视频回放状态
            strOutput += ("视频回放状态:" + videoPlaybackState.getPlaybackStatus() + "\n");
            // 获取视频回放位置
            strOutput += ("视频回放位置:" + videoPlaybackState.getPlayingPosition() + "秒\n");
            final String strFinalOutput = strOutput;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvVideoInformation.setText(strFinalOutput);
                }
            });
        }
    };

    // 内部类： 媒体文件显示列表的Adapter
    public class MediaFileAdapter extends RecyclerView.Adapter<MediaFileAdapter.MediaFileHolder> {

        // 内部类：媒体文件显示列表的ViewHolder
        public class MediaFileHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            // 媒体文件对象
            private MediaFile mMediaFile;
            // 子视图
            private ImageView mIvThumbnail;
            private TextView mTvFilename, mTvFilesize, mTvFiledate;
            private Button mBtnPreview, mBtnDownload, mBtnDelete;

            public MediaFileHolder(@NonNull View itemView) {
                super(itemView);
                // 初始化子视图
                mIvThumbnail = itemView.findViewById(R.id.iv_thumbnail); // 媒体文件缩略图
                mTvFilename = itemView.findViewById(R.id.tv_media_file_name); // 文件名称文本框
                mTvFilesize = itemView.findViewById(R.id.tv_media_file_size); // 文件大小文本框
                mTvFiledate = itemView.findViewById(R.id.tv_media_file_date); // 文件创建时间文本框
                mBtnPreview = itemView.findViewById(R.id.btn_preview); // 【查看】按钮
                mBtnDownload = itemView.findViewById(R.id.btn_download); // 【下载】按钮
                mBtnDelete = itemView.findViewById(R.id.btn_delete); // 【删除】按钮
                // 初始化按钮单击事件
                mBtnPreview.setOnClickListener(this);
                mBtnDownload.setOnClickListener(this);
                mBtnDelete.setOnClickListener(this);
            }
            public void bind(MediaFile mediaFile) {
                mMediaFile = mediaFile;
                mTvFilename.setText(mMediaFile.getFileName());
                mTvFilesize.setText("" + mMediaFile.getFileSize() + "Bytes");
                mTvFiledate.setText(mMediaFile.getDateCreated());
                mIvThumbnail.setImageBitmap(mediaFile.getThumbnail());
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_preview: preview();break;
                    case R.id.btn_download: download();break;
                    case R.id.btn_delete: delete();break;
                    default: break;
                }
            }

            // 预览照片和视频功能
            private void preview() {
                // 预览照片
                if (mMediaFile.getMediaType() == MediaFile.MediaType.JPEG
                        || mMediaFile.getMediaType() == MediaFile.MediaType.RAW_DNG)
                {
                    showImagePreview(mMediaFile);
                }
                // 预览视频
                if (mMediaFile.getMediaType() == MediaFile.MediaType.MOV
                        || mMediaFile.getMediaType() == MediaFile.MediaType.MP4)
                {
                    mCurrentMediaFileIndex = mMediaFiles.indexOf(mMediaFile);
                    showVideoPreview();
                }

            }

            // 下载媒体文件
            private void download() {
                // 设置下载位置
                File downloadDir = new File(getExternalFilesDir(null) + "/media/");
                // 开始下载文件
                mMediaFile.fetchFileData(downloadDir, null, new DownloadListener<String>() {
                    @Override
                    public void onFailure(DJIError error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPgsDlgDownload.cancel();
                            }
                        });
                        showToast("文件下载失败!");
                    }
                    @Override
                    public void onProgress(long total, long current) {
                    }
                    @Override
                    public void onRateUpdate(final long total, final long current, long persize) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int tmpProgress = (int) (1.0 * current / total * 100);
                                mPgsDlgDownload.setProgress(tmpProgress);
                            }
                        });
                    }
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
                    public void onSuccess(String filePath) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mPgsDlgDownload.dismiss();
                            }
                        });
                        showToast("文件下载成功,下载位置为:" + filePath);
                    }
                });
            }

            // 删除媒体文件
            private void delete() {
                // 设置需要删除的媒体文件
                ArrayList<MediaFile> deleteFiles = new ArrayList<MediaFile>();
                deleteFiles.add(mMediaFile);
                // 删除媒体文件
                mMediaManager.deleteFiles(deleteFiles, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
                    @Override
                    public void onSuccess(List<MediaFile> mediaFiles, DJICameraError djiCameraError) {
                        if (djiCameraError != null) {
                            showToast("部分媒体文件删除失败!" + djiCameraError.getDescription());
                            return;
                        }
                        // 更新数据和视图
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int removedIndex = mMediaFileAdapter.mMediaFiles.indexOf(mMediaFile);
                                mMediaFileAdapter.mMediaFiles.remove(mMediaFile);
                                mMediaFileAdapter.notifyItemRemoved(removedIndex);
                            }
                        });
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        showToast("媒体文件删除失败!" + djiError.getDescription());
                    }
                });
            }
        }

        // 媒体文件列表
        private List<MediaFile> mMediaFiles;

        public MediaFileAdapter(List<MediaFile> mediaFiles) {
            mMediaFiles = mediaFiles;
        }

        @NonNull
        @Override
        public MediaFileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_media_file, parent, false);
            return new MediaFileHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MediaFileHolder holder, int position) {
            MediaFile mediaFile = mMediaFiles.get(position);
            holder.bind(mediaFile);
        }

        @Override
        public int getItemCount() {
            return mMediaFiles.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_download);
        // 初始化UI
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 初始化媒体管理器
        initMediaManager();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 重置媒体管理器
        unsetMediaManager();
    }

    // 初始化UI
    private void initUI() {
        // 初始化文件显示列表
        mMediaFileRecyclerView = findViewById(R.id.media_file_recycler_view);
        mMediaFileRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 初始化文件下载对话框
        mPgsDlgDownload = new ProgressDialog(MediaDownloadActivity.this);
        mPgsDlgDownload.setTitle("媒体文件下载中...");
        mPgsDlgDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mPgsDlgDownload.setCanceledOnTouchOutside(false);
        mPgsDlgDownload.setCancelable(true);
        mPgsDlgDownload.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mMediaManager != null) {
                    mMediaManager.exitMediaDownloading();
                }
            }
        });

        // 获取显示媒体图片视图
        mIvMediaFile = findViewById(R.id.media_file_image_view);
        mIvMediaFile.setVisibility(View.GONE);
        mIvMediaFile.setBackgroundColor(Color.BLACK);

        // 获取显示媒体视频的相关视图
        mLayoutVideo = findViewById(R.id.media_file_video_view);
        mLayoutVideo.setVisibility(View.GONE);
        mLayoutVideo.setBackgroundColor(Color.BLACK);
        mTvPlayback = findViewById(R.id.texture_playback);
        mBtnVideoPlay = findViewById(R.id.btn_video_play);
        mBtnVideoPause = findViewById(R.id.btn_video_pause);
        mBtnVideoGoto = findViewById(R.id.btn_video_goto);
        mBtnVideoBack = findViewById(R.id.btn_video_back);
        mTvVideoInformation = findViewById(R.id.tv_video_information);

        mBtnVideoPlay.setOnClickListener(this);
        mBtnVideoPause.setOnClickListener(this);
        mBtnVideoGoto.setOnClickListener(this);
        mBtnVideoBack.setOnClickListener(this);
    }

    // 初始化媒体管理器
    private void initMediaManager() {
        Camera camera = getCamera();
        // 判断相机对象非空，且支持媒体下载模式
        if (camera == null) {
            mMediaFileAdapter.mMediaFiles.clear();
            mMediaFileAdapter.notifyDataSetChanged();
            showToast("相机对象获取错误!");
            return;
        }
        if (!camera.isMediaDownloadModeSupported()) {
            showToast("当前相机不支持媒体下载模式!");
            return;
        }
        // 获取媒体管理器
        mMediaManager = camera.getMediaManager();
        if (mMediaManager == null) {
            showToast("媒体管理器错误!");
            return;
        }
        // 设置媒体管理器监听器
        mMediaManager.addUpdateFileListStateListener(mFileListStateListener);
        // 设置视频回放状态监听器
        mMediaManager.addMediaUpdatedVideoPlaybackStateListener(mVideoPlaybackStateListener);
        // 获取媒体任务调度器
        mScheduler = camera.getMediaManager().getScheduler();
        // 设置当前相机模式为媒体下载模式
        camera.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    showToast("相机模式设置错误!" + djiError.getDescription());
                    return;
                }
                // 判断当前的文件列表
                if ((mFileListState == MediaManager.FileListState.SYNCING) || (mFileListState == MediaManager.FileListState.DELETING)){
                    showToast("媒体管理器正忙!");
                    return;
                }
                showToast("开始获取媒体列表!");
                // 开始获取媒体文件列表
                mMediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast("获取媒体文件列表错误!" + djiError.getDescription());
                            return;
                        }
                        // 未完成时清理媒体列表
                        if (mFileListState == MediaManager.FileListState.INCOMPLETE) {
                            mMediaFileAdapter.mMediaFiles.clear();
                        }
                        // 媒体文件列表
                        List<MediaFile> mediaFiles = mMediaManager.getSDCardFileListSnapshot();
                        showMediaFileList(mediaFiles);
                    }
                });
            }
        });
    }

    // 显示媒体文件列表
    private void showMediaFileList(final List<MediaFile> mediaFiles) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMediaFileAdapter == null) {
                    // 初始化媒体文件列表
                    mMediaFileAdapter = new MediaFileAdapter(mediaFiles);
                    mMediaFileRecyclerView.setAdapter(mMediaFileAdapter);
                } else {
                    // 刷新媒体文件列表
                    mMediaFileAdapter.notifyDataSetChanged();
                }
                // 通过媒体任务调度器下载媒体文件缩略图
                mScheduler.resume(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        getThumbnails();
                    }
                });
            }
        });
    }

    // 下载媒体文件缩略图
    private void getThumbnails() {
        // 获取媒体文件列表时被用户取消
        if (mMediaFileAdapter.mMediaFiles.size() <= 0) {
            // showToast("无媒体文件!");
            return;
        }
        for (int i = 0; i < mMediaFileAdapter.mMediaFiles.size(); i++) {
            FetchMediaTask task = new FetchMediaTask(mMediaFileAdapter.mMediaFiles.get(i), FetchMediaTaskContent.THUMBNAIL, new FetchMediaTask.Callback() {
                @Override
                public void onUpdate(MediaFile mediaFile, FetchMediaTaskContent fetchMediaTaskContent, DJIError djiError) {
                    if (null == djiError) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mMediaFileAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            });
            mScheduler.moveTaskToEnd(task);
        }
    }

    // 重置媒体管理器
    private void unsetMediaManager() {
        // 重置媒体管理器对象
        if (mMediaManager != null) {
            // 如果正在回放视频，则停止回放。
            mMediaManager.stop(null);
            // 取消媒体管理器监听器
            mMediaManager.removeFileListStateCallback(mFileListStateListener);
            // 取消视频回放状态监听器
            mMediaManager.removeMediaUpdatedVideoPlaybackStateListener(mVideoPlaybackStateListener);
            // 如果正在下载媒体，则取消下载。
            mMediaManager.exitMediaDownloading();
            // 如果媒体任务调度器存在任务，则移除所有任务
            if (mScheduler != null) {
                mScheduler.removeAllTasks();
            }
        }

        // 相机退出媒体下载模式
        Camera camera = getCamera();
        if (camera != null) {
            camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        showToast("修改相机模式失败!" + djiError.getDescription());
                    }
                }
            });
        }

        // 清除媒体文件列表
        if (mMediaFileAdapter != null && mMediaFileAdapter.mMediaFiles != null) {
            mMediaFileAdapter.mMediaFiles.clear();
        }
    }

    // 预览照片
    private void showImagePreview(MediaFile mediaFile) {
        // 创建下载照片数据预览图任务
        final FetchMediaTask task =
            new FetchMediaTask(mediaFile, FetchMediaTaskContent.PREVIEW, new FetchMediaTask.Callback() {
                @Override
                public void onUpdate(final MediaFile mediaFile, FetchMediaTaskContent fetchMediaTaskContent, DJIError error) {
                    if (error != null) {
                        showToast("照片数据获取失败!" + error.getDescription());
                        return;
                    }
                    if (mediaFile.getPreview() == null) {
                        showToast("照片数据为空!");
                        return;
                    }
                    // 获取预览图
                    final Bitmap previewBitmap = mediaFile.getPreview();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 显示预览图
                            mIvMediaFile.setImageBitmap(previewBitmap);
                            mIvMediaFile.setVisibility(View.VISIBLE);
                            mIvMediaFile.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mIvMediaFile.setImageBitmap(null);
                                    mIvMediaFile.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                }
            });

        // 提交任务到调度器
        mMediaManager.getScheduler().resume(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    mMediaManager.getScheduler().moveTaskToNext(task);
                } else {
                    showToast("调度器启动任务失败!" + error.getDescription());
                }
            }
        });
    }

    // 预览视频
    private void showVideoPreview() {

        if (!mMediaManager.isVideoPlaybackSupported()) {
            showToast("当前设备不支持视频回放功能!");
            return;
        }

        mLayoutVideo.setVisibility(View.VISIBLE);

        // 为用于显示图传数据的TextureView设置监听器
        mTvPlayback.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                                  int width, int height) {
                // 在SurfaceTexture可用时创建解码译码器
                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(MediaDownloadActivity.this,
                            surface, width, height);
                    // 在创建编码译码器之后调整TextureView的高度和宽度
                    fitTextureViewToFPV();
                    // 在视频流尺寸发生变化时调整TextureView的高度和宽度
                    mCodecManager.setOnVideoSizeChangedListener(new
                        DJICodecManager.OnVideoSizeChangedListener() {
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
        // 自动开始回放视频
        videoPlay();
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
                ViewGroup.LayoutParams layoutParams = mTvPlayback.getLayoutParams();
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
                mTvPlayback.setLayoutParams(layoutParams);
                // 通知编码译码器TextureView的宽度和高度的变化
                mCodecManager.onSurfaceSizeChanged(layoutParams.width, layoutParams.height, 0);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_video_play: videoPlay();break;
            case R.id.btn_video_pause: videoPause();break;
            case R.id.btn_video_goto: videoGoto();break;
            case R.id.btn_video_back: videoBack();break;
            default: break;
        }
    }

    // 播放/停止视频回放
    private void videoPlay() {

        // 播放或停止视频回放
        if (mVideoPlaybackStatus == MediaFile.VideoPlaybackStatus.STOPPED
                || mVideoPlaybackStatus == MediaFile.VideoPlaybackStatus.UNKNOWN) {
            // 当停止时开始回放视频
            MediaFile mediaFile = mMediaFileAdapter.mMediaFiles.get(mCurrentMediaFileIndex);
            mMediaManager.playVideoMediaFile(mediaFile, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast("播放失败:" + djiError.getDescription());
                    } else {
                        showToast("播放成功!");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBtnVideoPlay.setText("停止");
                                mBtnVideoPause.setEnabled(true);
                                mBtnVideoPause.setText("暂停");
                            }
                        });
                    }
                }
            });
        } else {
            // 停止回放视频
            mMediaManager.stop(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast("停止失败:" + djiError.getDescription());
                    } else {
                        showToast("停止成功!");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBtnVideoPlay.setText("播放");
                                mBtnVideoPause.setEnabled(false);
                            }
                        });
                    }
                }
            });
        }

    }

    // 暂停/继续视频回放
    private void videoPause() {
        // 当视频暂停时，继续播放
        if (mVideoPlaybackStatus == MediaFile.VideoPlaybackStatus.PAUSED) {
            mMediaManager.resume(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast("继续失败:" + djiError.getDescription());
                    } else {
                        showToast("继续成功!");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBtnVideoPause.setText("暂停");
                            }
                        });
                    }
                }
            });
        }
        // 当视频播放时，暂停播放
        if (mVideoPlaybackStatus == MediaFile.VideoPlaybackStatus.PLAYING) {
            mMediaManager.pause(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast("暂停失败:" + djiError.getDescription());
                    } else {
                        showToast("暂停成功!");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBtnVideoPause.setText("继续");
                            }
                        });
                    }
                }
            });
        }

    }

    // 跳转视频回放位置
    private void videoGoto() {
        if (mVideoPlaybackStatus == MediaFile.VideoPlaybackStatus.STOPPED
                || mVideoPlaybackStatus == MediaFile.VideoPlaybackStatus.UNKNOWN) {
            showToast("视频停止或状态错误。");
            return;
        }

        mMediaManager.moveToPosition(0, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    showToast("跳转到视频开头失败:" + djiError.getDescription());
                } else {
                    showToast("跳转到视频开头成功!");
                }
            }
        });
    }

    // 结束视频回放
    private void videoBack() {
        // 停止回放
        mMediaManager.stop(null);
        // 将当前回放视频在媒体文件列表中的索引设置为无效值
        mCurrentMediaFileIndex = -1;
        // 隐藏视频回放界面
        mLayoutVideo.setVisibility(View.GONE);
        // 移除VideoFeed的视频流数据监听器
        VideoFeeder.getInstance().getPrimaryVideoFeed()
                .removeVideoDataListener(mVideoDataListener);
    }


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
}
