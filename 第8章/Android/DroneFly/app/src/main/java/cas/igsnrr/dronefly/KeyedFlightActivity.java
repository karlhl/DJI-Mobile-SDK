package cas.igsnrr.dronefly;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.GetCallback;
import dji.keysdk.callback.KeyListener;
import dji.keysdk.callback.SetCallback;

public class KeyedFlightActivity extends AppCompatActivity implements View.OnClickListener  {

    private Button mBtnTakeoff, mBtnCancelTakeoff, mBtnLanding, mBtnCancelLanding;
    private Button mBtnSetGoHomeHeight, mBtnGetGoHomeHeight, mBtnGoHome, mBtnCancelGoHome;
    private TextView mTvMode, mTvLocation, mTvVelocity, mTvSatelliteCount;

    private String mFlightMode;
    private double mAltitude, mLongitude, mLatitude, mVelocityX, mVelocityY, mVelocityZ;
    private int mSatelliteCount;

    private KeyListener mFlightModeListener, mAltitudeListener, mLongitudeListener, mLatitudeListener;
    private KeyListener mVelocityXListener, mVelocityYListener, mVelocityZListener, mSatelliteCountListener;

    private DJIKey mKeyFlightMode, mKeyAltitude, mKeyLongitude, mKeyLatitude;
    private DJIKey mKeyVelocityX, mKeyVelocityY, mKeyVelocityZ, mKeySatelliteCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 由于界面相同，与FLightActivity共用同一个layout文件
        setContentView(R.layout.activity_flight);

        // 初始化数据
        initData();
        // 初始化UI界面
        initUI();
        // 初始化监听器
        initListener();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除监听器
        removeListener();
    }

    private void initData() {

        mKeyFlightMode = FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE_STRING);
        mKeyAltitude = FlightControllerKey.create(FlightControllerKey.ALTITUDE);
        mKeyLongitude = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE);
        mKeyLatitude = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE);
        mKeyVelocityX = FlightControllerKey.create(FlightControllerKey.VELOCITY_X);
        mKeyVelocityY = FlightControllerKey.create(FlightControllerKey.VELOCITY_Y);
        mKeyVelocityZ = FlightControllerKey.create(FlightControllerKey.VELOCITY_Z);
        mKeySatelliteCount = FlightControllerKey.create(FlightControllerKey.SATELLITE_COUNT);

        KeyManager.getInstance().getValue(mKeyFlightMode, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object o) {
                if (o instanceof String) {
                    mFlightMode = (String) o;
                }
            }

            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });

        KeyManager.getInstance().getValue(mKeyLongitude, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object o) {
                if (o instanceof Double) {
                    mLongitude = (Double) o;
                }
            }

            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });

        KeyManager.getInstance().getValue(mKeyLatitude, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object o) {
                if (o instanceof Double) {
                    mLatitude = (Double) o;
                }
            }

            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
        KeyManager.getInstance().getValue(mKeyAltitude, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object o) {
                if (o instanceof Float) {
                    mAltitude = (Float) o;
                }
            }

            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
        KeyManager.getInstance().getValue(mKeyVelocityX, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object o) {
                if (o instanceof Float) {
                    mVelocityX = (Float) o;
                }
            }

            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
        KeyManager.getInstance().getValue(mKeyVelocityY, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object o) {
                if (o instanceof Float) {
                    mVelocityY = (Float) o;
                }
            }

            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
        KeyManager.getInstance().getValue(mKeyVelocityZ, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object o) {
                if (o instanceof Float) {
                    mVelocityZ = (Float) o;
                }
            }

            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
        KeyManager.getInstance().getValue(mKeySatelliteCount, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object o) {
                if (o instanceof Integer) {
                    mSatelliteCount = (Integer) o;
                }
            }
            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });

    }

    private void initUI() {

        mBtnTakeoff = findViewById(R.id.btn_takeoff);
        mBtnCancelTakeoff = findViewById(R.id.btn_takeoff_cancel);
        mBtnLanding = findViewById(R.id.btn_landing);
        mBtnCancelLanding = findViewById(R.id.btn_landing_cancel);
        mBtnSetGoHomeHeight = findViewById(R.id.btn_set_gohome_height);
        mBtnGetGoHomeHeight = findViewById(R.id.btn_get_gohome_height);
        mBtnGoHome = findViewById(R.id.btn_gohome);
        mBtnCancelGoHome = findViewById(R.id.btn_gohome_cancel);

        mTvMode = findViewById(R.id.tv_mode);
        mTvLocation = findViewById(R.id.tv_location);
        mTvVelocity = findViewById(R.id.tv_velocity);
        mTvSatelliteCount = findViewById(R.id.tv_satellite_count);

        updateUI();
    }

    private void initListener() {
        mBtnTakeoff.setOnClickListener(this);
        mBtnCancelTakeoff.setOnClickListener(this);
        mBtnLanding.setOnClickListener(this);
        mBtnCancelLanding.setOnClickListener(this);
        mBtnSetGoHomeHeight.setOnClickListener(this);
        mBtnGetGoHomeHeight.setOnClickListener(this);
        mBtnGoHome.setOnClickListener(this);
        mBtnCancelGoHome.setOnClickListener(this);

        mFlightModeListener = new KeyListener() {
            @Override
            public void onValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
                if (newValue instanceof String) {
                    mFlightMode = (String)newValue;
                    updateUI();
                }
            }
        };

        mLongitudeListener = new KeyListener() {
            @Override
            public void onValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
                if (newValue instanceof Double) {
                    mLongitude = (Double)newValue;
                    updateUI();
                }
            }
        };

        mLatitudeListener = new KeyListener() {
            @Override
            public void onValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
                if (newValue instanceof Double) {
                    mLatitude = (Double)newValue;
                    updateUI();
                }
            }
        };

        mAltitudeListener = new KeyListener() {
            @Override
            public void onValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
                if (newValue instanceof Float) {
                    mAltitude = (Float)newValue;
                    updateUI();
                }
            }
        };

        mVelocityXListener = new KeyListener() {
            @Override
            public void onValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
                if (newValue instanceof Float) {
                    mVelocityX = (Float)newValue;
                    updateUI();
                }
            }
        };
        mVelocityYListener = new KeyListener() {
            @Override
            public void onValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
                if (newValue instanceof Float) {
                    mVelocityY = (Float)newValue;
                    updateUI();
                }
            }
        };
        mVelocityZListener = new KeyListener() {
            @Override
            public void onValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
                if (newValue instanceof Float) {
                    mVelocityZ = (Float)newValue;
                    updateUI();
                }
            }
        };

        mSatelliteCountListener = new KeyListener() {
            @Override
            public void onValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
                if (newValue instanceof Integer) {
                    mSatelliteCount = (Integer)newValue;
                    updateUI();
                }
            }
        };
        KeyManager.getInstance().addListener(mKeyFlightMode, mFlightModeListener);
        KeyManager.getInstance().addListener(mKeyLongitude, mLongitudeListener);
        KeyManager.getInstance().addListener(mKeyLatitude, mLatitudeListener);
        KeyManager.getInstance().addListener(mKeyAltitude, mAltitudeListener);
        KeyManager.getInstance().addListener(mKeyVelocityX, mVelocityXListener);
        KeyManager.getInstance().addListener(mKeyVelocityY, mVelocityYListener);
        KeyManager.getInstance().addListener(mKeyVelocityZ, mVelocityZListener);
        KeyManager.getInstance().addListener(mKeySatelliteCount, mSatelliteCountListener);

    }

    private void removeListener() {
        KeyManager.getInstance().removeListener(mFlightModeListener);
        KeyManager.getInstance().removeListener(mLongitudeListener);
        KeyManager.getInstance().removeListener(mLatitudeListener);
        KeyManager.getInstance().removeListener(mAltitudeListener);
        KeyManager.getInstance().removeListener(mVelocityXListener);
        KeyManager.getInstance().removeListener(mVelocityYListener);
        KeyManager.getInstance().removeListener(mVelocityZListener);
        KeyManager.getInstance().removeListener(mSatelliteCountListener);
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvMode.setText("飞行模式:" + mFlightMode);
                mTvLocation.setText(String.format("经度: %.4f°, 纬度:%.4f°, 高度:%.2f米",
                        mLongitude, mLatitude, mAltitude));
                mTvVelocity.setText(String.format(
                        "X方向速度: %.2f米/秒, Y方向速度: %.2f米/秒, 垂直速度:%.2f米/秒",
                        mVelocityX, mVelocityY, mVelocityZ));
                mTvSatelliteCount.setText("连接卫星数量: " + mSatelliteCount + "个");
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_takeoff: takeoff();break;
            case R.id.btn_takeoff_cancel: cancelTakeoff();break;
            case R.id.btn_landing: landing();break;
            case R.id.btn_landing_cancel: cancelLanding();break;
            case R.id.btn_set_gohome_height: setGohomeHeight();break;
            case R.id.btn_get_gohome_height: getGohomeHeight();break;
            case R.id.btn_gohome: gohome();break;
            case R.id.btn_gohome_cancel: cancelGohome();break;
            default: break;
        }
    }

    // 起飞
    private void takeoff(){
        DJIKey key = FlightControllerKey.create(FlightControllerKey.TAKE_OFF);
        KeyManager.getInstance().performAction(key, new ActionCallback() {
            @Override
            public void onSuccess() {
                showToast("开始起飞!");
            }
            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
    }

    // 取消起飞
    private void cancelTakeoff(){
        DJIKey key = FlightControllerKey.create(FlightControllerKey.CANCEL_TAKE_OFF);
        KeyManager.getInstance().performAction(key, new ActionCallback() {
            @Override
            public void onSuccess() {
                showToast("取消起飞成功!");
            }
            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
    }

    // 降落
    private void landing(){
        DJIKey key = FlightControllerKey.create(FlightControllerKey.START_LANDING);
        KeyManager.getInstance().performAction(key, new ActionCallback() {
            @Override
            public void onSuccess() {
                showToast("开始降落!");
            }
            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
    }

    // 取消降落
    private void cancelLanding(){
        DJIKey key = FlightControllerKey.create(FlightControllerKey.CANCEL_LANDING);
        KeyManager.getInstance().performAction(key, new ActionCallback() {
            @Override
            public void onSuccess() {
                showToast("取消降落成功!");
            }
            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
    }

    // 设置返航高度
    private void setGohomeHeight(){
        // 返航高度设置文本框
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);  // 只能输入纯数字
        // 弹出设置返航高度对话框
        new AlertDialog.Builder(this)
            .setTitle("请输入返航高度 (m)")
            .setView(editText)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 获取返航高度设置
                    int height = Integer.parseInt(editText.getText().toString());
                    DJIKey key = FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS);
                    KeyManager.getInstance().setValue(key, height, new SetCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("返航高度设置成功!");
                        }

                        @Override
                        public void onFailure(@NonNull DJIError djiError) {
                            showToast("设置高度设置失败!" + djiError.toString());
                        }
                    });
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // 获取返航高度
    private void getGohomeHeight(){

        DJIKey key = FlightControllerKey.create(FlightControllerKey.GO_HOME_HEIGHT_IN_METERS);
        KeyManager.getInstance().getValue(key, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object o) {
                if (o instanceof Integer){
                    showToast("返航高度为: " + (int)o + "米.");
                }
            }
            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast("获取返航高度失败: " + djiError.toString());
            }
        });
    }

    // 返航
    private void gohome(){

        DJIKey key = FlightControllerKey.create(FlightControllerKey.START_GO_HOME);
        KeyManager.getInstance().performAction(key, new ActionCallback() {
            @Override
            public void onSuccess() {
                showToast("开始返航!");
            }
            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
    }

    // 取消返航
    private void cancelGohome(){
        DJIKey key = FlightControllerKey.create(FlightControllerKey.CANCEL_GO_HOME);
        KeyManager.getInstance().performAction(key, new ActionCallback() {
            @Override
            public void onSuccess() {
                showToast("取消返航成功!");
            }
            @Override
            public void onFailure(@NonNull DJIError djiError) {
                showToast(djiError.toString());
            }
        });
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
