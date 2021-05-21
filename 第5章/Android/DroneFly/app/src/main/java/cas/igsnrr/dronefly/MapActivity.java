package cas.igsnrr.dronefly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MapActivity extends AppCompatActivity {

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
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
        mWebView = findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true); // 允许弹窗
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.loadUrl("file:///android_asset/olmap.html");
    }

    private void initListener() {

        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState state) {
                // 获取无人机经度
                final double longitude = state.getAircraftLocation().getLongitude();
                // 获取无人机纬度
                final double latitude = state.getAircraftLocation().getLatitude();
                // 获取无人机航向
                final double yaw = state.getAttitude().yaw * Math.PI / 180;
                mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        String jsScript = "javascript:changeDroneLocation(" + longitude + "," + latitude + "," + yaw + ")";
                        mWebView.evaluateJavascript(jsScript, null);
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
