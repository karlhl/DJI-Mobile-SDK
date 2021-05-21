package cas.igsnrr.dronefly;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import dji.common.accessory.AccessoryAggregationState;
import dji.common.accessory.SettingsDefinitions;
import dji.common.accessory.SpeakerState;
import dji.common.accessory.SpotlightState;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.keysdk.AccessoryAggregationKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.sdk.accessory.AccessoryAggregation;
import dji.sdk.accessory.beacon.Beacon;
import dji.sdk.accessory.speaker.AudioFileInfo;
import dji.sdk.accessory.speaker.Speaker;
import dji.sdk.accessory.speaker.TransmissionListener;
import dji.sdk.accessory.spotlight.Spotlight;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

public class AccessoryAggregationActivity extends AppCompatActivity implements View.OnClickListener {

    // UI视图
    private TextView mTvConnectionState;
    private Button mBtnRecord, mBtnStopPlaying, mBtnChangePlayMode;
    private Button mBtnEnableBeacon, mBtnEnableSpotlight;
    private SeekBar mSbVolume, mSbBrightness;

    // 音频录制工具
    private AudioRecorderHandler mRecorderHandler;

    // 是否正在录音
    private boolean mIsRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessory_aggregation);
        // 初始化UI
        initUI();
        // 初始化监听器
        initListener();
        // 初始化附件状态
        initAggregationState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除监听器
        removeListener();
    }

    // 初始化UI界面
    private void initUI() {
        mTvConnectionState = findViewById(R.id.tv_connection_state);

        mBtnRecord = findViewById(R.id.btn_record);
        mBtnStopPlaying = findViewById(R.id.btn_stop_playing);
        mBtnChangePlayMode = findViewById(R.id.btn_change_play_mode);
        mBtnEnableBeacon = findViewById(R.id.btn_enable_beacon);
        mBtnEnableSpotlight = findViewById(R.id.btn_enable_spotlight);

        mSbVolume = findViewById(R.id.sb_volume);
        mSbBrightness = findViewById(R.id.sb_brightness);

    }

    // 初始化附件状态
    private void initAggregationState() {
        AccessoryAggregation accessoryAggregation = getAccessoryAggregation();
        if (accessoryAggregation != null) {
            AccessoryAggregationState state = accessoryAggregation.getAccessoryAggregationState();
            updateAggregationState(state);
        }
    }

    // 更新附件状态
    private void updateAggregationState(final AccessoryAggregationState state) {
        // 更新附件连接状态
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String strConnectionState = "";
                strConnectionState += state.isSpeakerConnected() ? "喊话器已连接!" : "";
                strConnectionState += state.isBeaconConnected() ? "夜航灯已连接!" : "";
                strConnectionState += state.isSpotlightConnected() ? "探照灯已连接!" : "";
                if (strConnectionState.equals("")) {
                    strConnectionState = "附件未连接!";
                }
                mTvConnectionState.setText(strConnectionState);
            }
        });

        // 显示喊话器音量
        if (state.isSpeakerConnected()) {
            DJIKey volumeKey = AccessoryAggregationKey.createSpeakerKey(AccessoryAggregationKey.SPEAKER_VOLUME);
            int volume = (int) KeyManager.getInstance().getValue(volumeKey);
            mSbVolume.setProgress(volume);
        }

        // 显示探照灯亮度
        if (state.isSpotlightConnected()) {
            DJIKey brightnessKey = AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_BRIGHTNESS);
            int brightness = (int) KeyManager.getInstance().getValue(brightnessKey);
            mSbBrightness.setProgress(brightness);
        }

    }

    // 初始化监听器
    private void initListener() {

        // 监听按钮单击事件
        mBtnRecord.setOnClickListener(this);
        mBtnStopPlaying.setOnClickListener(this);
        mBtnChangePlayMode.setOnClickListener(this);
        mBtnEnableBeacon.setOnClickListener(this);
        mBtnEnableSpotlight.setOnClickListener(this);

        // 监听附件集合，获取附件的连接信息
        AccessoryAggregation accessoryAggregation = getAccessoryAggregation();
        if (accessoryAggregation != null) {
            accessoryAggregation.setStateCallback(new AccessoryAggregationState.Callback() {
                @Override
                public void onUpdate(AccessoryAggregationState state) {
                    // 显示当前的附件连接状态
                    updateAggregationState(state);
                }
            });
        }

        // 监听喊话器音量设置
        mSbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Speaker speaker = getSpeaker();
                if (speaker != null) {
                    speaker.setVolume(i, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError == null) {
                                showToast("设置音量成功!");
                            } else {
                                showToast("设置音量失败! " + djiError.getDescription());
                            }
                        }
                    });
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 监听探照灯亮度设置
        mSbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Spotlight spotlight = getSpotlight();
                if (spotlight != null) {
                    spotlight.setBrightness(i, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError == null) {
                                showToast("设置亮度成功!");
                            } else {
                                showToast("设置亮度失败! " + djiError.getDescription());
                            }
                        }
                    });
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    // 移除监听器
    private void removeListener() {

        // 移除附件集合回调
        AccessoryAggregation accessoryAggregation = getAccessoryAggregation();
        if (accessoryAggregation != null) {
            accessoryAggregation.setStateCallback(null);
        }
    }

    // region UI事件

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_record: record();break;   // 喊话器中【开始录音】按钮单击事件处理方法
            case R.id.btn_stop_playing: stopPlaying();break;   // 喊话器中【停止播放】按钮单击事件处理方法
            case R.id.btn_change_play_mode: changePlayMode();break;   // 喊话器中【切换播放模式】按钮单击事件处理方法
            case R.id.btn_enable_beacon: enableBeacon();break;   // 夜航灯中【打开/关闭】按钮单击事件处理方法
            case R.id.btn_enable_spotlight: enableSpotlight();break;   // 探照灯中【打开/关闭】按钮单击事件处理方法
            default: break;
        }
    }

    // 喊话器中【开始录音】按钮单击事件处理方法
    private void record() {
        if (mIsRecording) {
            // 停止录音
            stopRecording();
        } else {
            // 开始录音
            startRecording();
            mIsRecording = true;
            mBtnRecord.setText("停止录音并播放");
        }
    }

    // 开始录音
    private void startRecording() {

        // 获取喊话器对象，并判断是否为空
        Speaker speaker = getSpeaker();
        if (speaker == null) {
            showToast("喊话器未连接!");
            return;
        }
        // 设置存储文件信息
        // 设置当前时间为文件名
        String filename = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        // 设置文件为临时存储
        SettingsDefinitions.AudioStorageLocation location = SettingsDefinitions.AudioStorageLocation.TEMPORARY;
        AudioFileInfo info = new AudioFileInfo(filename, location);

        // 开始录制（同时传输）音频
        speaker.startTransmission(info, new TransmissionListener() {
            @Override
            public void onStart() {
                // 初始化音频录制工具
                if (mRecorderHandler == null){
                    mRecorderHandler = new AudioRecorderHandler(getApplicationContext());
                }

                // 开始录制
                mRecorderHandler.startRecord(new AudioRecorderHandler.AudioRecordingCallback() {
                    @Override
                    public void onRecording(byte[] data) {
                        Speaker speaker = getSpeaker();
                        if (speaker != null) {
                            // 传递音频数据流
                            speaker.paceData(data);
                        }
                    }

                    @Override
                    public void onStopRecord(String savedPath) {
                        Speaker speaker = getSpeaker();
                        if (speaker != null) {
                            // 标记音频数据流的结束
                            speaker.markEOF();
                        }
                        // 删除临时文件
                        mRecorderHandler.deleteLastRecordFile();
                    }
                });
            }

            @Override
            public void onProgress(int dataSize) {
                Log.v("数据传输进度:", "" + dataSize);
            }

            @Override
            public void onFinish(int index) {
                showToast("数据传输成功!");
                // 播放音频
                startPlayingAudio(index);
                mIsRecording = false;
                mBtnRecord.setText("开始录音");
            }

            @Override
            public void onFailure(DJIError djiError) {
                showToast("数据传输失败:" + djiError.getDescription());
                mIsRecording = false;
                mBtnRecord.setText("开始录音");
            }
        });
    }

    // 停止录音
    private void stopRecording() {

        // 获取喊话器对象，并判断是否为空
        Speaker speaker = getSpeaker();
        if (speaker == null) {
            showToast("喊话器未连接!");
            return;
        }

        // 停止录音
        if (mRecorderHandler != null) {
            mRecorderHandler.stopRecord();
        }
    }

    // 开始播放音频
    private void startPlayingAudio(final int index) {
        // 获取喊话器对象
        Speaker speaker = getSpeaker();
        if (speaker == null) {
            showToast("喊话器未连接!");
            return;
        }

        // 播放音频
        speaker.play(index, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    showToast("开始播放音频: " + index);
                } else {
                    showToast("开始播放音频失败! " + djiError.getDescription());
                }
            }
        });
    }

    // 喊话器中【停止播放】按钮单击事件处理方法
    private void stopPlaying() {
        Speaker speaker = getSpeaker();
        if (speaker != null) {
            speaker.stop(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        showToast("停止播放成功!");
                    } else {
                        showToast("停止播放失败:" + djiError.getDescription());
                    }
                }
            });
        }
    }

    // 喊话器中【切换播放模式】按钮单击事件处理方法
    private void changePlayMode() {
        Speaker speaker = getSpeaker();
        if (speaker != null) {
            DJIKey playModeKey = AccessoryAggregationKey.createSpeakerKey(AccessoryAggregationKey.PLAY_MODE);
            SettingsDefinitions.PlayMode playMode = (SettingsDefinitions.PlayMode) KeyManager.getInstance().getValue(playModeKey);
            if (playMode != null && playMode == SettingsDefinitions.PlayMode.REPEAT_SINGLE) {
                speaker.setPlayMode(SettingsDefinitions.PlayMode.SINGLE_ONCE, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            showToast("切换为单次播放成功!");
                        } else {
                            showToast("切换为单次播放失败:" + djiError.getDescription());
                        }
                    }
                });
            } else {
                speaker.setPlayMode(SettingsDefinitions.PlayMode.REPEAT_SINGLE, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            showToast("切换为循环播放成功!");
                        } else {
                            showToast("切换为循环播放失败:" + djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    // 夜航灯中【打开/关闭】按钮单击事件处理方法
    private void enableBeacon() {
        Beacon beacon = getBeacon();
        if (beacon != null) {
            DJIKey beaconEnabledKey = AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.BEACON_ENABLED);
            Boolean isBeaconEnabled = (Boolean) KeyManager.getInstance().getValue(beaconEnabledKey);
            if (isBeaconEnabled != null && isBeaconEnabled) {
                beacon.setEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            showToast("关闭夜航灯成功!");
                        } else {
                            showToast("关闭夜航灯失败:" + djiError.getDescription());
                        }
                    }
                });
            } else {
                beacon.setEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            showToast("打开夜航灯成功!");
                        } else {
                            showToast("打开夜航灯失败:" + djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    // 探照灯中【打开/关闭】按钮单击事件处理方法
    private void enableSpotlight() {
        Spotlight spotlight = getSpotlight();
        if (spotlight != null) {
            DJIKey spotlightEnabledKey = AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED);
            Boolean isSpotlightEnabled = (Boolean) KeyManager.getInstance().getValue(spotlightEnabledKey);
            if (isSpotlightEnabled != null && isSpotlightEnabled) {
                spotlight.setEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            showToast("关闭探照灯成功!");
                        } else {
                            showToast("关闭探照灯失败:" + djiError.getDescription());
                        }
                    }
                });
            } else {
                spotlight.setEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            showToast("打开探照灯成功!");
                        } else {
                            showToast("打开探照灯失败:" + djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    // endregion

    // region 获取附件对象

    // 获得无人机的附件对象
    private AccessoryAggregation getAccessoryAggregation() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            return product.getAccessoryAggregation();
        }
        return null;
    }

    // 获得喊话器对象
    private Speaker getSpeaker() {
        AccessoryAggregation accessoryAggregation = getAccessoryAggregation();
        if (accessoryAggregation != null && accessoryAggregation.isConnected()) {
            return accessoryAggregation.getSpeaker();
        }
        return null;
    }

    // 获得夜航灯对象
    private Beacon getBeacon() {
        AccessoryAggregation accessoryAggregation = getAccessoryAggregation();
        if (accessoryAggregation != null && accessoryAggregation.isConnected()) {
            return accessoryAggregation.getBeacon();
        }
        return null;
    }

    // 获得探照灯对象
    private Spotlight getSpotlight() {
        AccessoryAggregation accessoryAggregation = getAccessoryAggregation();
        if (accessoryAggregation != null && accessoryAggregation.isConnected()) {
            return accessoryAggregation.getSpotlight();
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
