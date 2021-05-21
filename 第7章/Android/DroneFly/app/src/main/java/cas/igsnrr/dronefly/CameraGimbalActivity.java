package cas.igsnrr.dronefly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.GimbalState;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.Lens;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.sdkmanager.DJISDKManager;

public class CameraGimbalActivity extends AppCompatActivity implements View.OnClickListener {

    // PFV显示区域
    private TextureView mTextureViewFPV;
    // 返回按钮
    private Button mBtnBack;
    // 相机相关视图和控件
    private Button mBtnCameraMode, mBtnTakePicture, mBtnRecord, mBtnExposureMode, mBtnISO;
    private TextView mTvCameraMode, mTvRecordingTime ,mTvExposureMode;
    // 云台相关视图和控件
    private Button mBtnExpandGimbalPitch, mBtnMoveGimbalPitch, mBtnGimbalMode;
    private TextView mTvGimbalMode;
    // 相机模式
    private SettingsDefinitions.CameraMode mCameraMode;
    // 是否正在录像，是否正在存储照片
    private boolean mIsCameraRecording, mIsCameraStoringPhoto;
    // 录像时间
    private int mRecordingTime;
    // 云台模式
    private GimbalMode mGimbalMode;

    // 图传显示相关对象
    // VideoFeed视频流数据监听器
    private VideoFeeder.VideoDataListener mVideoDataListener;
    // 编码译码器
    private DJICodecManager mCodecManager;

    // region Activity生命周期

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_gimbal);
        // 初始化UI界面
        initUI();
        // 初始化监听器
        initListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到Activity时刷新曝光模式
        showExposureMode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除监听器
        removeListener();
    }

    // endregion

    // region UI&监听器设置

    // 初始化UI
    private void initUI() {
        // 获取UI视图、控件对象
        mTextureViewFPV = findViewById(R.id.texture_fpv);
        mBtnBack = findViewById(R.id.btn_back);
        mBtnCameraMode = findViewById(R.id.btn_camera_mode);
        mBtnTakePicture = findViewById(R.id.btn_take_picture);
        mBtnRecord = findViewById(R.id.btn_record);
        mBtnExposureMode = findViewById(R.id.btn_exposure_mode);
        mBtnISO = findViewById(R.id.btn_iso);
        mBtnExpandGimbalPitch = findViewById(R.id.btn_expand_gimbal_pitch);
        mBtnMoveGimbalPitch = findViewById(R.id.btn_move_gimbal_pitch);
        mBtnGimbalMode = findViewById(R.id.btn_gimbal_mode);
        mTvCameraMode = findViewById(R.id.tv_camera_mode);
        mTvRecordingTime = findViewById(R.id.tv_recording_time);
        mTvExposureMode = findViewById(R.id.tv_exposure_mode);
        mTvGimbalMode = findViewById(R.id.tv_gimbal_mode);

        // 刷新曝光模式
        showExposureMode();
    }

    // 更新UI
    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 显示当前相机模式
                String cameraModeString = cameraModeToString(mCameraMode);
                mTvCameraMode.setText("当前相机模式:" + cameraModeString);

                // 设置与显示录像时间与按钮文字
                if (mIsCameraRecording) {
                    mBtnRecord.setText("停止录像");
                    mTvRecordingTime.setText("录像时间：" + mRecordingTime + "秒");
                } else {
                    mBtnRecord.setText("录像");
                    mTvRecordingTime.setText("录像时间：未录像");
                }

                // 显示当前云台模式
                String gimbalModeString = gimbalModeToString(mGimbalMode);
                mTvGimbalMode.setText("当前云台模式:" + gimbalModeString);


            }
        });
    }

    // 显示曝光模式
    private void showExposureMode() {
        // 刷新显示相机曝光模式
        Camera camera = getCamera();
        if (camera != null ){
            // 获取曝光模式
            camera.getExposureMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ExposureMode>() {
                @Override
                public void onSuccess(final SettingsDefinitions.ExposureMode exposureMode) {
                    // 在UI中刷新曝光模式
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvExposureMode.setText("相机曝光模式:" + exposureModeToString(exposureMode));
                        }
                    });
                }

                @Override
                public void onFailure(DJIError djiError) {
                    showToast("获取相机曝光模式错误!" + djiError.getDescription());
                }
            });
        }

    }

    // 初始化监听器
    private void initListener() {
        // 为用于显示图传数据的TextureView设置监听器
        mTextureViewFPV.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                // 在SurfaceTexture可用时创建解码译码器
                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(CameraGimbalActivity.this, surface, width, height);
                    fitTextureViewToFPV();
                    mCodecManager.setOnVideoSizeChangedListener(new DJICodecManager.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(int i, int i1) {
                            // fitTextureViewToFPV();
                        }
                    });
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // fitTextureViewToFPV();
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
        mVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] bytes, int i) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(bytes, i);
                }
            }
        };

        // 为VideoFeed设置监听器对象，获取图传视频流
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mVideoDataListener);

        // 设置按钮监听器
        mBtnBack.setOnClickListener(this);
        mBtnCameraMode.setOnClickListener(this);
        mBtnTakePicture.setOnClickListener(this);
        mBtnRecord.setOnClickListener(this);
        mBtnExposureMode.setOnClickListener(this);
        mBtnISO.setOnClickListener(this);
        mBtnExpandGimbalPitch.setOnClickListener(this);
        mBtnMoveGimbalPitch.setOnClickListener(this);
        mBtnGimbalMode.setOnClickListener(this);

        // 设置相机状态回调
        Camera camera = getCamera();
        if (camera != null) {
            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(@NonNull SystemState systemState) {
                    // 获取当前的相机模式
                    mCameraMode = systemState.getMode();
                    // 获取当前是否处于录像状态
                    mIsCameraRecording = systemState.isRecording();
                    // 获取当前是否处于保存像片的状态
                    mIsCameraStoringPhoto = systemState.isStoringPhoto();
                    // 获取当前的录像时间
                    mRecordingTime = systemState.getCurrentVideoRecordingTimeInSeconds();
                    // 刷新UI界面
                    updateUI();
                }
            });
        }

        // 设置云台状态回调
        Gimbal gimbal = getGimbal();
        if (gimbal != null) {
            gimbal.setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(@NonNull GimbalState gimbalState) {
                    mGimbalMode = gimbalState.getMode();
                    updateUI();
                }
            });
        }
    }

    // 移除监听器
    private void removeListener() {
        // 移除VideoFeed的视频流数据监听器
        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mVideoDataListener);

        // 移除相机回调
        Camera camera = getCamera();
        if (camera != null) {
            camera.setSystemStateCallback(null);
        }

        // 移除云台回调
        Gimbal gimbal = getGimbal();
        if (gimbal != null) {
            gimbal.setStateCallback(null);
        }
    }

    // endregion

    // region UI事件

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back: back();break;
            case R.id.btn_camera_mode: changeCameraMode();break;
            case R.id.btn_take_picture: takePicture();break;
            case R.id.btn_record: record();break;
            case R.id.btn_exposure_mode: changeExposureMode();break;
            case R.id.btn_iso: changeCameraISO();break;
            case R.id.btn_expand_gimbal_pitch: expandGimbalPitch();break;
            case R.id.btn_move_gimbal_pitch: moveGimbalPitch();break;
            case R.id.btn_gimbal_mode: changeGimbalMode(); break;
            default: break;
        }
    }

    // 返回主界面
    private void back() {
        this.finish();
    }

    // 改变相机模式
    private void changeCameraMode() {
        Camera camera = getCamera();
        if (camera != null ){
            if (mCameraMode == SettingsDefinitions.CameraMode.SHOOT_PHOTO) {
                // 如果处在拍照模式，则进入录像模式
                setCameraMode(camera, SettingsDefinitions.CameraMode.RECORD_VIDEO);
            } else{
                // 如果处在非拍照模式，则进入拍照模式
                setCameraMode(camera, SettingsDefinitions.CameraMode.SHOOT_PHOTO);
            }
        }
    }

    // 设置指定的相机模式
    private void setCameraMode(Camera camera, final SettingsDefinitions.CameraMode cameraMode) {

        camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    showToast("相机模式设置成功:" + cameraModeToString(cameraMode));
                } else {
                    showToast("相机模式设置失败:" + djiError.getDescription());
                }
            }
        });
    }

    // 拍照
    private void takePicture() {
        final Camera camera = getCamera();
        if (camera != null ){
            // 判断是否为拍照模式
            if (mCameraMode != SettingsDefinitions.CameraMode.SHOOT_PHOTO) {
                showToast("未处在拍照模式下!");
                return;
            }
            // 判断是否正在存储照片数据
            if (mIsCameraStoringPhoto) {
                showToast("相机繁忙，请稍后!");
                return;
            }
            // 设置单拍模式
            camera.setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                if (djiError == null) {
                    // 开始拍照
                    camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError == null) {
                                showToast("拍照成功!");
                            } else {
                                showToast("拍照失败:" + djiError.getDescription());
                            }
                        }
                    });
                }
                }
            });
        }
    }

    // 录像
    private void record() {
        final Camera camera = getCamera();
        if (camera != null ){
            // 判断是否处在录像模式
            if (mCameraMode != SettingsDefinitions.CameraMode.RECORD_VIDEO) {
                showToast("未处在录像模式下!");
                return;
            }
            if (mIsCameraRecording) {
                // 正录像时，停止录像。
                stopRecording(camera);
                mBtnRecord.setText("录像");
            } else {
                // 未录像时，开始录像。
                startRecording(camera);
                mBtnRecord.setText("停止录像");
            }
        }
    }

    // 开始录像
    private void startRecording(Camera camera) {
        // 开始录像
        camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
            if (djiError == null) {
                showToast("开始录像成功!");
            } else {
                showToast("开始录像失败:" + djiError.getDescription());
            }
            }
        });
    }

    // 停止录像
    private void stopRecording(Camera camera) {
        // 停止录像
        camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
            if (djiError == null) {
                showToast("停止录像成功!");
            } else {
                showToast("停止录像失败:" + djiError.getDescription());
            }
            }
        });
    }

    // 改变曝光模式
    private void changeExposureMode() {
        final Camera camera = getCamera();
        if (camera != null) {
            // 获取曝光模式
            camera.getExposureMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ExposureMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.ExposureMode exposureMode) {
                    if (exposureMode == SettingsDefinitions.ExposureMode.PROGRAM) {
                        // 当曝光模式处在自动模式下，则设置为手动模式
                        setExposureMode(camera, SettingsDefinitions.ExposureMode.MANUAL);
                    } else {
                        // 当曝光模式处在其他模式下，则设置为自动模式
                        setExposureMode(camera, SettingsDefinitions.ExposureMode.PROGRAM);
                    }
                }

                @Override
                public void onFailure(DJIError djiError) {
                    showToast("获取曝光模式错误:" + djiError.getDescription());
                }
            });

        }
    }

    // 设置指定的曝光模式
    private void setExposureMode(Camera camera, final SettingsDefinitions.ExposureMode exposureMode) {
        // 设置曝光模式
        camera.setExposureMode(exposureMode, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
            if (djiError == null) {
                showToast("相机曝光模式设置成功:" + exposureModeToString(exposureMode));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvExposureMode.setText("相机曝光模式:" + exposureModeToString(exposureMode));
                    }
                });
            }
            }
        });
    }

    // 改变ISO
    private void changeCameraISO() {
        final Camera camera = getCamera();
        if (camera != null) {
            // 获取相机所处的曝光模式
            camera.getExposureMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ExposureMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.ExposureMode exposureMode) {
                    // 判断是否处在手动模式下
                    // 获取并判断曝光模式的相关代码非必须。事实上在自动模式下，可通过下面的代码设置ISO，但是不起效果。
                    if (exposureMode != SettingsDefinitions.ExposureMode.MANUAL){
                        showToast("未处在手动曝光模式下!");
                        return;
                    }
                    // 获取相机所支持的ISO
                    final SettingsDefinitions.ISO[] isos = camera.getCapabilities().ISORange();
                    // 弹出设置设置ISO对话框
                    AlertDialog.Builder builder = new AlertDialog.Builder(CameraGimbalActivity.this)
                            .setTitle("请设置ISO:")
                            .setNegativeButton("取消", null);
                    String[] isoStrings = new String[isos.length];
                    for (int i = 0; i < isos.length; i ++) {
                        isoStrings[i] = isoToString(isos[i]);
                    }
                    builder.setItems(isoStrings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 获取需要设置的ISO
                            final SettingsDefinitions.ISO iso = isos[which];
                            // 设置ISO
                            camera.setISO(iso, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError == null) {
                                        showToast("改变ISO成功:" + isoToString(iso));
                                    } else {
                                        showToast("改变ISO失败:" + djiError.getDescription());
                                    }
                                }
                            });
                        }
                    });
                    builder.show();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    showToast("获取相机曝光模式错误!" + djiError.getDescription());
                }
            });

        }
    }

    // 扩展云台俯仰角度
    private void expandGimbalPitch() {
        final Gimbal gimbal = getGimbal();
        if (gimbal != null) {
            // 获取当前是否已经扩展云台俯仰角度
            gimbal.getPitchRangeExtensionEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(final Boolean enabled) {
                    // 当已扩展俯仰角度时，则禁用扩展；当未扩展抚养角度时，则启用扩展。
                    gimbal.setPitchRangeExtensionEnabled(!enabled, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError == null) {
                                showToast("云台俯仰扩展设置成功: " + !enabled);
                            }else {
                                showToast("云台俯仰扩展设置失败!" + djiError.getDescription());
                            }
                        }
                    });
                }

                @Override
                public void onFailure(DJIError djiError) {
                    showToast("云台俯仰扩展设置获取失败!" + djiError.getDescription());
                }
            });
        }
    }

    // 移动云台俯仰角度
    private void moveGimbalPitch() {
        Gimbal gimbal = getGimbal();
        if (gimbal != null) {
            // 在未扩展云台俯仰角度时，角度范围为0～-90。其中相机向前时，俯仰角度为0；相机向下时，俯仰角度为-90。
            Rotation rotation = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).pitch(-45).build();
            // 移动云台俯仰角度
            gimbal.rotate(rotation, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        showToast("云台俯仰角度设置成功!");
                    }else {
                        showToast("云台俯仰角度设置失败!" + djiError.getDescription());
                    }
                }
            });
        }
    }

    // 改变云台模式
    private void changeGimbalMode() {
        final Gimbal gimbal = getGimbal();
        if (gimbal != null) {
            if (mGimbalMode == GimbalMode.FPV) {
                // 当云台模式处于FPV模式时，则切换到跟随模式。
                setGimbalMode(gimbal, GimbalMode.YAW_FOLLOW);
            } else {
                // 当云台模式处于其他模式时，则切换到FPV模式。
                setGimbalMode(gimbal, GimbalMode.FPV);
            }
        }
    }

    // 设置指定的云台模式
    private void setGimbalMode(Gimbal gimbal, final GimbalMode gimbalMode) {
        gimbal.setMode(gimbalMode, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    showToast("云台模式设置成功:" + gimbalModeToString(gimbalMode));
                } else {
                    showToast("云台模式设置失败!" + djiError.getDescription());
                }
            }
        });
    }

    // endregion

    // region 图传

    // 使TextureView的宽高比适合视频流
    private void fitTextureViewToFPV() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 用于获取屏幕高度和宽度的DisplayMetrics对象。
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                // 图传视频的宽高比
                double videoratio = mCodecManager.getVideoWidth() * 1.0 / mCodecManager.getVideoHeight();
                // 设备屏幕的宽高比
                double textureratio = dm.widthPixels * 1.0 / dm.heightPixels;
                if (videoratio == textureratio) {
                    // 无需调整，直接返回
                    return;
                }
                // 开始设置TextureView的宽度和高度
                ViewGroup.LayoutParams layoutParams = mTextureViewFPV.getLayoutParams();
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
                mTextureViewFPV.setLayoutParams(layoutParams);
                // 通知编码译码器TextureView的宽度和高度的变化
                mCodecManager.onSurfaceSizeChanged(layoutParams.width, layoutParams.height, 0);

            }
        });
    }

    // endregion

    // region 获取相机与云台对象

    // 获得无人机（或手持云台相机）的相机对象
    private Camera getCamera() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            return product.getCamera();
        }
        return null;
    }

    // 获得无人机（或手持云台相机）的云台对象
    private Gimbal getGimbal() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            return product.getGimbal();
        }
        return null;
    }

    // endregion

    // region 枚举值与字符串的转换

    // 相机模式枚举值转字符串
    private String cameraModeToString(SettingsDefinitions.CameraMode cameraMode) {
        switch (cameraMode)
        {
            case SHOOT_PHOTO:
                return "SHOOT_PHOTO 拍照模式";
            case RECORD_VIDEO:
                return "RECORD_VIDEO 录像模式";
            case PLAYBACK:
                return "PLAYBACK 回放模式";
            case MEDIA_DOWNLOAD:
                return "MEDIA_DOWNLOAD 媒体下载模式";
            case BROADCAST:
                return "BROADCAST 直播模式";
            case UNKNOWN:
                return "UNKNOWN 未知模式";
            default:
                return "N/A";
        }
    }

    // 相机曝光模式枚举值转字符串
    private String exposureModeToString(SettingsDefinitions.ExposureMode exposureMode) {
        switch (exposureMode)
        {
            case PROGRAM:
                return "PROGRAM 自动模式";
            case SHUTTER_PRIORITY:
                return "SHUTTER_PRIORITY 快门优先";
            case APERTURE_PRIORITY:
                return "APERTURE_PRIORITY 光圈优先";
            case MANUAL:
                return "MANUAL 手动模式";
            case UNKNOWN:
                return "UNKNOWN 未知模式";
            default:
                return "N/A";
        }
    }

    // 相机ISO枚举值转字符串
    private String isoToString(SettingsDefinitions.ISO iso) {
        switch (iso)
        {
            case AUTO: return "自动ISO";
            case ISO_100: return "100";
            case ISO_200: return "200";
            case ISO_400: return "400";
            case ISO_800: return "800";
            case ISO_1600: return "1600";
            case ISO_3200: return "3200";
            case ISO_6400: return "6400";
            case ISO_12800: return "12800";
            case ISO_25600: return "25600";
            case FIXED: return "固定ISO";
            case UNKNOWN: return "未知";
            default: return "N/A";
        }
    }

    // 云台模式枚举值转字符串
    private String gimbalModeToString(GimbalMode gimbalMode) {
        switch (gimbalMode)
        {
            case FREE:
                return "FREE 自由模式";
            case FPV:
                return "FPV FPV模式";
            case YAW_FOLLOW:
                return "YAW_FOLLOW 跟随模式";
            case UNKNOWN:
                return "UNKNOWN 未知模式";
            default:
                return "N/A";
        }
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


