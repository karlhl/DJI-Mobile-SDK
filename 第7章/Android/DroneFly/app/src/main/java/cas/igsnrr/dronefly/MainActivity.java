package cas.igsnrr.dronefly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.realname.AircraftBindingState;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends AppCompatActivity {

    // 显示应用程序激活状态的文本视图
    private TextView tvAppActivation;
    // 显示无人机绑定状态的文本视图
    private TextView tvAircraftBinding;
    // 应用程序激活状态监听器
    private AppActivationState.AppActivationStateListener activationStateListener;
    // 无人机绑定状态监听器
    private AircraftBindingState.AircraftBindingStateListener bindingStateListener;
    // 需要申请的用户权限
    private static final String[] PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE, // 程序震动
            Manifest.permission.INTERNET, // 访问互联网(可能产生网络流量)
            Manifest.permission.ACCESS_WIFI_STATE, // 获取WiFi状态以及WiFi接入点信息
            Manifest.permission.WAKE_LOCK, // 关闭屏幕时后台进程仍然执行
            Manifest.permission.ACCESS_COARSE_LOCATION, // 获得模糊定位信息(通过基站或者WiFi信息)
            Manifest.permission.ACCESS_NETWORK_STATE, // 获取网络状态信息
            Manifest.permission.ACCESS_FINE_LOCATION, // 获得精准定位信息(通过定位卫星信号)
            Manifest.permission.CHANGE_WIFI_STATE, // 改变WiFi连接状态
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写入外部存储
            Manifest.permission.BLUETOOTH, // 配对蓝牙设备
            Manifest.permission.BLUETOOTH_ADMIN, // 配对蓝牙设备(不通知用户)
            Manifest.permission.READ_EXTERNAL_STORAGE, // 读取外部存储
            Manifest.permission.READ_PHONE_STATE, // 访问电话状态
    };
    // 缺失的用户权限
    private List<String> missingPermission = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 检查应用程序权限
        if (!checkPermissions()) {
            // 存在缺失权限，调用requestPermissions申请应用程序权限
            requestPermissions();
        }
        // 再次检查应用程序权限
        if (checkPermissions()) {
            // 不存在缺失权限，开始注册应用程序
            registerApplication();
        }
        // 初始化监听器
        initListener();
        // 初始化UI
        initUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除应用程序激活监听器
        AppActivationManager.getInstance()
                .removeAppActivationStateListener(activationStateListener);
        // 移除无人机绑定监听器
        AppActivationManager.getInstance()
                .removeAircraftBindingStateListener(bindingStateListener);
    }

    private void initUI(){
        // 获得“登陆DJI账号”按钮实例对象
        Button btnLogin = (Button) findViewById(R.id.btn_login);
        // 对“登陆DJI账号”按钮增加监听器
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserAccountManager.getInstance().logIntoDJIUserAccount(MainActivity.this,
                        new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                            @Override
                            public void onSuccess(UserAccountState userAccountState) {
                                showToast("登陆成功!");
                            }
                            @Override
                            public void onFailure(DJIError djiError) {
                                showToast("登陆失败!" + djiError.getDescription());
                            }
                        });
            }
        });

        // 获得“退出DJI账号”按钮实例对象
        Button btnLogout = (Button) findViewById(R.id.btn_logout);
        // 对“退出DJI账号”按钮增加监听器
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserAccountManager.getInstance().logoutOfDJIUserAccount(
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError == null) {
                                    showToast("退出成功!");
                                } else {
                                    showToast("退出失败!");
                                }
                            }
                        });
            }
        });

        // 获得“获取应用激活状态”按钮实例对象
        Button btnAppActivationStatus = (Button) findViewById(R.id.btn_status_appactivation);
        // 对“获取应用激活状态”按钮增加监听器
        btnAppActivationStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppActivationManager mgrActivation =
                        DJISDKManager.getInstance().getAppActivationManager();
                showToast("激活状态:" + mgrActivation.getAppActivationState());
            }
        });

        // 获得“获取无人机绑定状态”按钮实例对象
        Button btnAircraftBindingStatus = (Button) findViewById(R.id.btn_status_aircraftbinding);
        // 对“获取无人机绑定状态”按钮增加监听器
        btnAircraftBindingStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppActivationManager mgrActivation =
                        DJISDKManager.getInstance().getAppActivationManager();
                showToast("绑定状态:" + mgrActivation.getAircraftBindingState());
            }
        });
        // 应用程序激活状态文本视图
        tvAppActivation = findViewById(R.id.tv_status_appactivation);
        // 无人机绑定状态文本视图
        tvAircraftBinding = findViewById(R.id.tv_status_aircraftbinding);

        // 获得“飞行控制器”按钮实例对象
        Button btnFlightController = findViewById(R.id.btn_flight_controller);
        // 对“飞行控制器”按钮增加监听器
        btnFlightController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkDroneConnection() == false) {
                    return;
                }
                // 弹出FlightActivity
                Intent i = new Intent(MainActivity.this, FlightActivity.class);
                startActivity(i);
            }
        });

        // 获得“飞行控制器(键值管理器)”按钮实例对象
        Button btnKeyedFlightController = findViewById(R.id.btn_keyed_flight_controller);
        // 对“飞行控制器(键值管理器)”按钮增加监听器
        btnKeyedFlightController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkDroneConnection() == false) {
                    return;
                }
                // 弹出KeyedFlightActivity
                Intent i = new Intent(MainActivity.this, KeyedFlightActivity.class);
                startActivity(i);
            }
        });

        // 获得“在地图中显示无人机位置”按钮实例对象
        Button btnMap = findViewById(R.id.btn_map);
        // 对“在地图中显示无人机位置”按钮增加监听器
        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkDroneConnection() == false) {
                    return;
                }
                // 弹出MapActivity
                Intent i = new Intent(MainActivity.this, MapActivity.class);
                startActivity(i);
            }
        });

        // 获得“相机与云台”按钮实例对象
        Button btnCameraGimbal = findViewById(R.id.btn_camera_gimbal);
        // 对“相机与云台”按钮增加监听器
        btnCameraGimbal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkDroneConnection() == false) {
                    return;
                }
                // 弹出CameraGimbalActivity
                Intent i = new Intent(MainActivity.this, CameraGimbalActivity.class);
                startActivity(i);
            }
        });
    }

    private boolean checkDroneConnection() {
        // 应用程序激活管理器
        AppActivationManager mgrActivation =
                DJISDKManager.getInstance().getAppActivationManager();
        // 判断应用程序是否注册
        if (!DJISDKManager.getInstance().hasSDKRegistered()) {
            showToast("应用程序未注册!");
            return false;
        }
        // 判断应用程序是否激活
        if (mgrActivation.getAppActivationState() != AppActivationState.ACTIVATED) {
            showToast("应用程序未激活!");
            return false;
        }
        // 判断无人机是否绑定
        if (mgrActivation.getAircraftBindingState() != AircraftBindingState.BOUND) {
            showToast("无人机未绑定!");
            return false;
        }
        // 判断无人机连接是否正常
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product == null || !product.isConnected()) {
            showToast("无人机连接失败!");
            return false;
        }
        return true;
    }

    // 检查应用程序权限
    private boolean checkPermissions(){
        // 判断应用程序编译的SDK版本是否大于22 (Android 5.1),否则不需要申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 遍历所有Mobile SDK需要的权限
            for (String permission : PERMISSION_LIST) {
                // 判断该权限是否已经被赋予
                if (ContextCompat.checkSelfPermission(this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    // 没有赋予的权限放入到missingPermission列表对象中
                    missingPermission.add(permission);
                }
            }
            // 如果不存在缺失权限，则返回真；否则返回假
            return missingPermission.isEmpty();
        }
        else return true;
    }

    // 申请应用程序权限
    private void requestPermissions() {
        // 申请所有没有被赋予的权限
        ActivityCompat.requestPermissions(this,
                missingPermission.toArray(new String[missingPermission.size()]),
                1009);
    }

    // 注册应用程序
    private void registerApplication() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(),
                        new DJISDKManager.SDKManagerCallback() {
                            @Override
                            public void onRegister(DJIError djiError) {
                                if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                    showToast("应用程序注册成功!" + djiError.getDescription());
                                    DJISDKManager.getInstance().startConnectionToProduct();
                                } else {
                                    showToast("应用程序注册失败!" + djiError.getDescription());
                                }
                            }
                            @Override
                            public void onProductDisconnect() {
                                showToast("设备断开连接!");
                            }

                            @Override
                            public void onProductConnect(BaseProduct baseProduct) {
                                showToast("设备连接:" + baseProduct.getModel().getDisplayName());
                            }

                            @Override
                            public void onComponentChange(BaseProduct.ComponentKey componentKey,
                                                          BaseComponent oldComponent,
                                                          BaseComponent newComponent) {
                                String strInfo = String.format("组件变化 键:%s, 旧组件:%s,"
                                        + "新组件:%s", componentKey, oldComponent, newComponent);
                                showToast(strInfo);
                            }


                            @Override
                            public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int process) {
                                showToast(djisdkInitEvent.getInitializationState().toString()
                                        + "进度: " + process + "%");
                            }

                            @Override
                            public void onDatabaseDownloadProgress(long process, long sum) {
                                Log.v("限飞数据库下载", "已下载" + process + "字节, 总共: " + sum + "字节");
                            }
                        });
            }
        });
    }

    // 初始化监听器
    private void initListener() {

        activationStateListener = new AppActivationState.AppActivationStateListener() {
            @Override
            public void onUpdate(final AppActivationState state) {
                // 对回调的应用程序激活状态对象进行操作
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvAppActivation.setText("当前应用激活状态:" + state.name());
                    }
                });
            }
        };

        bindingStateListener = new AircraftBindingState.AircraftBindingStateListener() {
            @Override
            public void onUpdate(final AircraftBindingState state) {
                // 对回调的无人机绑定状态对象进行操作
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvAircraftBinding.setText("当前无人机绑定状态:" + state.name());
                    }
                });
            }
        };

        // 添加应用程序激活监听器
        AppActivationManager.getInstance()
                .addAppActivationStateListener(activationStateListener);
        // 添加无人机绑定监听器
        AppActivationManager.getInstance()
                .addAircraftBindingStateListener(bindingStateListener);

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
