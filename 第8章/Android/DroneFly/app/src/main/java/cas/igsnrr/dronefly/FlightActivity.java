package cas.igsnrr.dronefly;

import androidx.annotation.NonNull;
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
import dji.common.flightcontroller.FlightControllerState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;


public class FlightActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mBtnTakeoff, mBtnCancelTakeoff, mBtnLanding, mBtnCancelLanding;
    private Button mBtnSetGoHomeHeight, mBtnGetGoHomeHeight, mBtnGoHome, mBtnCancelGoHome;
    private TextView mTvMode, mTvLocation, mTvVelocity, mTvSatelliteCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight);
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

        FlightController flightController = getFlightController();
        if (flightController != null) {
            // 设置飞行控制器的FlightControllerState回调
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState state) {
                    // 获取飞行模式
                    final String flightMode= state.getFlightModeString();
                    // 获取无人机高度
                    final double altitude=state.getAircraftLocation().getAltitude();
                    // 获取无人机经度
                    final double longitude=state.getAircraftLocation().getLongitude();
                    // 获取无人机纬度
                    final double latitude=state.getAircraftLocation().getLatitude();
                    // 获取无人机X方向移动速度
                    final double velocityX = state.getVelocityX();
                    // 获取无人机Y方向移动速度
                    final double velocityY = state.getVelocityY();
                    // 获取无人机Z方向移动速度
                    final double velocityZ = state.getVelocityZ();
                    // 获取卫星连接数量
                    final double satelliteCount = state.getSatelliteCount();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvMode.setText("飞行模式:" + flightMode);
                            mTvLocation.setText(String.format("经度: %.4f°, 纬度:%.4f°, 高度:%.2f米",
                                    longitude, latitude, altitude));
                            mTvVelocity.setText(String.format(
                                    "X方向速度: %.2f米/秒, Y方向速度: %.2f米/秒, 垂直速度:%.2f米/秒",
                                    velocityX, velocityY, velocityZ));
                            mTvSatelliteCount.setText("连接卫星数量: " + satelliteCount + "个");
                        }
                    });
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接是否正常!");
        }
    }

    private void removeListener() {
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.setStateCallback(null);
        }
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

    // 获取无人机的飞行控制器
    private FlightController getFlightController() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                return ((Aircraft) product).getFlightController();
            }
        }
        return null;
    }

    // 起飞
    private void takeoff(){
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                if (djiError != null)
                    showToast(djiError.toString());
                else showToast("开始起飞!");
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接是否正常!");
        }
    }

    // 取消起飞
    private void cancelTakeoff(){
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.cancelTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                if (djiError != null)
                    showToast(djiError.toString());
                else showToast("取消起飞成功!");
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接是否正常!");
        }
    }

    // 降落
    private void landing(){
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                if (djiError != null)
                    showToast(djiError.toString());
                else showToast("开始降落!");
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接是否正常!");
        }
    }

    // 取消降落
    private void cancelLanding(){
        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.cancelLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                if (djiError != null)
                    showToast(djiError.toString());
                else showToast("取消降落成功!");
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接是否正常!");
        }
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
                final FlightController flightController = getFlightController();
                if (flightController != null) {
                    // 设置返航高度
                    flightController.setGoHomeHeightInMeters(height,
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null)
                                    showToast(djiError.toString());
                                else showToast("返航高度设置成功!");
                            }
                        });
                } else {
                    showToast("飞行控制器获取失败，请检查飞行器连接是否正常!");
                }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // 获取返航高度
    private void getGohomeHeight(){
        final FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.getGoHomeHeightInMeters(
                new CommonCallbacks.CompletionCallbackWith<Integer>() {
                    @Override
                    public void onSuccess(Integer height) {
                        showToast("返航高度为: " + height + "米.");
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        showToast("获取返航高度失败: " + djiError.toString());
                    }
                });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接是否正常!");
        }
    }

    // 返航
    private void gohome(){
        final FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                if (djiError != null)
                    showToast(djiError.toString());
                else showToast("开始返航!");
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接是否正常!");
        }
    }

    // 取消返航
    private void cancelGohome(){
        final FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.cancelGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                if (djiError != null)
                    showToast(djiError.toString());
                else showToast("取消返航成功!");
                }
            });
        } else {
            showToast("飞行控制器获取失败，请检查飞行器连接是否正常!");
        }
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
