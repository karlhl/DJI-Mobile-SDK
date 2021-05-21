package cas.igsnrr.dronefly;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

public class CameraGimbalActivity extends AppCompatActivity implements View.OnClickListener {

    // PFV显示区域
    private TextureView mTextureViewFPV;
    // 返回按钮
    private Button mBtnBack;

    // VideoFeed视频流数据监听器
    private VideoFeeder.VideoDataListener mVideoDataListener;
    // 编码译码器
    private DJICodecManager mCodecManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_gimbal);
        initUI();
        initListener();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListener();
    }

    // 初始化UI界面
    private void initUI() {
        mTextureViewFPV = findViewById(R.id.texture_fpv);
        mBtnBack = findViewById(R.id.btn_back);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back: back();break;
            default: break;
        }
    }

    // 返回主界面
    private void back() {
        this.finish();
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
                            fitTextureViewToFPV();
                        }
                    });
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
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
        mVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] bytes, int i) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(bytes, i);
                }
            }
        };

        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mVideoDataListener);

        mBtnBack.setOnClickListener(this);

    }

    // 移除监听器
    private void removeListener() {
        // 移除VideoFeed的视频流数据监听器
        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mVideoDataListener);

    }

    // 在主线程中显示提示
    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

}


