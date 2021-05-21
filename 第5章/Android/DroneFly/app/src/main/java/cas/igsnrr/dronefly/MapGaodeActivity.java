package cas.igsnrr.dronefly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;

import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MapGaodeActivity extends AppCompatActivity {

    // 地图控件
    private MapView mMapView;
    // AMap对象
    private AMap mAMap;
    // 飞机标记
    private MarkerOptions droneMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_gaode);
        //获取地图对象
        mMapView = (MapView) findViewById(R.id.gaode_map);
        mMapView.onCreate(savedInstanceState);
        // 获取AMap对象
        if (mAMap == null)
            mAMap = mMapView.getMap();
        // 初始化飞机标记
        droneMarker = new MarkerOptions();
        droneMarker.anchor(0.5f, 0.5f);
        droneMarker.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                .decodeResource(getResources(),R.mipmap.aircraft)));
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        droneMarker.setFlat(true);//设置marker平贴地图效果
        // 初始化监听器
        initListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停地图的绘制
        mMapView.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // 重新绘制加载地图
        mMapView.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 销毁地图
        mMapView.onDestroy();
        // 移除监听器
        removeListener();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }


    private void initListener() {

        FlightController flightController = getFlightController();
        if (flightController != null) {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState state) {
                    // 获取无人机经度
                    double longitude = state.getAircraftLocation().getLongitude();
                    // 获取无人机纬度
                    double latitude = state.getAircraftLocation().getLatitude();
                    // 获取无人机航向
                    float yaw = (float)state.getAttitude().yaw;

                    // 初始化坐标转换类
                    CoordinateConverter converter = new CoordinateConverter(getApplicationContext());
                    converter.from(CoordinateConverter.CoordType.GPS);
                    // 设置需要转换的坐标
                    converter.coord(new LatLng(latitude,longitude));
                    // 转换成高德坐标
                    LatLng destPoint = converter.convert();

                    // 设置飞机标记的位置和方向
                    droneMarker.position(new LatLng(destPoint.latitude, destPoint.longitude));
                    droneMarker.rotateAngle(-yaw);

                    // 清除所有标记
                    mAMap.clear();
                    // 添加飞机标记
                    mAMap.addMarker(droneMarker);

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
