package cas.igsnrr.dronefly;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecuteState;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;

public class WaypointMissionActivity extends AppCompatActivity implements View.OnClickListener {

    // UI视图
    private TextView mTvStatusWaypointMission, mTvStatusWaypointMissionExecute;
    private Button mBtnLoadWaypointMission, mBtnUploadWaypointMission, mBtnStartWaypointMission, mBtnPauseWaypointMission;

    private WaypointMissionOperator mWaypointMissionOperator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint_mission);
        initUI();
        initWaypointMissionOperator();
        /*
        if (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.DISCONNECTED) {
            getWaypointMissionOperator().downloadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showToast("Mission Download: " + (error == null ? "Successfully" : error.getDescription()));
                }
            });
        }*/
    }

    private void initWaypointMissionOperator() {
        getWaypointMissionOperator().addListener(new WaypointMissionOperatorListener() {
            @Override
            public void onDownloadUpdate(WaypointMissionDownloadEvent event) {
                /*
                // 当前航点任务执行状态
                String executeState = waypointMissionExecuteStateToString(event.getProgress().);
                // 目标航点序号
                int index = event.getProgress().targetWaypointIndex;
                // 总航点数
                int count = event.getProgress().totalWaypointCount;
                // 是否已经到达航点
                final String reached = event.getProgress().isWaypointReached ? "已到达" : "未到达";
                final String strState = String.format("航点:%d(%s) 总航点数:%d 状态:%s", index, reached, count, executeState);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvStatusWaypointMissionExecute.setText(strState);
                    }
                });
                 */
            }

            @Override
            public void onUploadUpdate(WaypointMissionUploadEvent event) {
                // 闪退
                /*
                // 当前航点任务状态
                WaypointMissionState state = event.getCurrentState();
                // 总航点数
                int count = event.getProgress().totalWaypointCount;
                final String strState = String.format("总航点数:%d", count);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvStatusWaypointMission.setText("航点任务状态:" + waypointMissionStateToString(state));
                        mTvStatusWaypointMissionExecute.setText(strState);
                    }
                });
                 */
            }

            @Override
            public void onExecutionUpdate(WaypointMissionExecutionEvent event) {

                // 当前航点任务状态
                WaypointMissionState state = event.getCurrentState();
                // 当前航点任务执行状态
                String executeState = waypointMissionExecuteStateToString(event.getProgress().executeState);
                // 目标航点序号
                int index = event.getProgress().targetWaypointIndex;
                // 总航点数
                int count = event.getProgress().totalWaypointCount;
                // 是否已经到达航点
                final String reached = event.getProgress().isWaypointReached ? "已到达" : "未到达";
                final String strState = String.format("航点:%d(%s) 总航点数:%d 状态:%s", index + 1, reached, count, executeState);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvStatusWaypointMission.setText("航点任务状态:" + waypointMissionStateToString(state));
                        mTvStatusWaypointMissionExecute.setText(strState);
                    }
                });
            }

            @Override
            public void onExecutionStart() {
                showToast("开始执行任务!");
            }

            @Override
            public void onExecutionFinish(DJIError djiError) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvStatusWaypointMission.setText("航点任务状态:已经结束");
                        mTvStatusWaypointMissionExecute.setText("已结束");
                    }
                });
                showToast("Execution finished: " + (djiError == null ? "Success!" : djiError.getDescription()));
            }
        });
    }

    private void initUI() {
        mTvStatusWaypointMission = findViewById(R.id.tv_status_waypoint_mission);
        mTvStatusWaypointMissionExecute = findViewById(R.id.tv_status_waypoint_mission_execute);

        mBtnLoadWaypointMission = findViewById(R.id.btn_load_waypoint_mission); // 【加载任务】按钮
        mBtnUploadWaypointMission = findViewById(R.id.btn_upload_waypoint_mission); // 【上传任务】按钮
        mBtnStartWaypointMission = findViewById(R.id.btn_start_waypoint_mission); // 【开始任务】按钮
        mBtnPauseWaypointMission = findViewById(R.id.btn_pause_waypoint_mission); // 【暂停任务】按钮
        mBtnLoadWaypointMission.setOnClickListener(this);
        mBtnUploadWaypointMission.setOnClickListener(this);
        mBtnStartWaypointMission.setOnClickListener(this);
        mBtnPauseWaypointMission.setOnClickListener(this);
    }

    private void test() {

        WaypointAction action = new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0);
        Waypoint waypoint1 = new Waypoint(43.513000, 125.647100, 80);
        /*
        125.7153 43.5294
        125.7181 43.5292
        125.7185 43.5266
        125.7157 43.5253
        */
        waypoint1.addAction(action);

        Waypoint waypoint2 = new Waypoint(43.512900, 125.647500, 80);
        waypoint2.addAction(action);


        WaypointMission.Builder builder = new WaypointMission.Builder().addWaypoint(waypoint1);
        builder.addWaypoint(waypoint2);
        // showToast("sdf" + builder.getWaypointCount());
        builder.autoFlightSpeed(5)
                .maxFlightSpeed(5)
                .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                .finishedAction(WaypointMissionFinishedAction.GO_HOME)
                .headingMode(WaypointMissionHeadingMode.USING_INITIAL_DIRECTION);

        WaypointMission mission = builder.build();
        DJIError error =  mission.checkParameters();
        if (error != null) {
            showToast(error.toString());
            return;
        }
            WaypointMissionOperator operator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        error = operator.loadMission(mission);
        if (error != null) {
            showToast(error.toString());
            return;
        }
        operator.uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    showToast(djiError.toString());
                }
                operator.startMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.toString());
                        }
                    }
                });
            }
        });
    }


    public WaypointMissionOperator getWaypointMissionOperator() {
        if (mWaypointMissionOperator == null) {
            mWaypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return mWaypointMissionOperator;
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


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_load_waypoint_mission: loadWaypointMission(); break;
            case R.id.btn_upload_waypoint_mission: uploadWaypointMission(); break;
            case R.id.btn_start_waypoint_mission: startWaypointMission(); break;
            case R.id.btn_pause_waypoint_mission: pauseWaypointMission(); break;
        }
    }

    // 加载航点任务
    private void loadWaypointMission() {

        // Curve就不执行了
        // WaypointMissionHeadingMode = Auto 此时指向下一个航点
        //

        WaypointAction actionStay = new WaypointAction(WaypointActionType.STAY, 2000);
        WaypointAction actionTakePhoto = new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0);
        WaypointAction actionStartRecord = new WaypointAction(WaypointActionType.START_RECORD, 0);
        WaypointAction actionStopRecord = new WaypointAction(WaypointActionType.STOP_RECORD, 0);
        WaypointAction actionAircraftToNorth = new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 0);
        WaypointAction actionGimbalStraightDown = new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90);
        WaypointAction actionGimbal45degree = new WaypointAction(WaypointActionType.GIMBAL_PITCH, -45);
        WaypointAction actionGimbalHorizontal = new WaypointAction(WaypointActionType.GIMBAL_PITCH, 0);

        Waypoint waypoint1 = new Waypoint(43.528712, 125.714001, 20);
        waypoint1.addAction(actionGimbalStraightDown);
        waypoint1.addAction(actionTakePhoto);
        waypoint1.addAction(actionStay);
        waypoint1.addAction(actionGimbalHorizontal);
        waypoint1.addAction(actionAircraftToNorth);
        waypoint1.addAction(actionTakePhoto);
        Waypoint waypoint2 = new Waypoint(43.528415, 125.714571, 40);
        waypoint2.addAction(actionStay);
        waypoint2.addAction(actionStartRecord);
        waypoint2.cornerRadiusInMeters = 10;
        Waypoint waypoint3 = new Waypoint(43.527944, 125.714470, 40);
        waypoint3.addAction(actionStopRecord);
        waypoint3.addAction(actionGimbal45degree);
        waypoint3.shootPhotoTimeInterval = 5;
        Waypoint waypoint4 = new Waypoint(43.527794, 125.713809, 30);

        WaypointMission.Builder builder = new WaypointMission.Builder();
        builder.addWaypoint(waypoint1);
        builder.addWaypoint(waypoint2);
        builder.addWaypoint(waypoint3);
        builder.addWaypoint(waypoint4);

        builder.autoFlightSpeed(5)
                .maxFlightSpeed(5)
                .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                .finishedAction(WaypointMissionFinishedAction.GO_HOME)
                .headingMode(WaypointMissionHeadingMode.AUTO);
        // builder.flightPathMode(WaypointMissionFlightPathMode.CURVED);
        WaypointMission mission = builder.build();

        DJIError error =  mission.checkParameters();
        if (error != null) {
            showToast(error.toString());
            return;
        }

        error = getWaypointMissionOperator().loadMission(mission);
        if (error == null) {
            showToast("加载航点模式成功!");
        } else {
            showToast("加载航点模式失败:" + error.getDescription());
        }
 }

    // 上传航点任务
    private void uploadWaypointMission() {
        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    showToast("上传航点模式成功!");
                } else {
                    showToast("上传航点模式失败:" + error.getDescription() + ". 正在重试上传...");
                    getWaypointMissionOperator().retryUploadMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (error == null) {
                                showToast("上传航点模式成功!");
                            } else {
                                showToast("上传航点模式失败:" + error.getDescription());
                            }
                        }
                    });
                }
            }
        });
    }

    // 开始(停止)航点任务
    private void startWaypointMission() {
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                showToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
    }

    // 暂停(继续)航点任务
    private void pauseWaypointMission() {

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                showToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

        /*
        getWaypointMissionOperator().pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });

        getWaypointMissionOperator().resumeMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });
        */
    }

    // region 枚举值与字符串的转换

    // 航点任务状态转字符串
    private String waypointMissionStateToString(WaypointMissionState state) {
        if (state.equals(WaypointMissionState.UNKNOWN)){
            return "UNKNOWN 未知";
        }else if (state.equals(WaypointMissionState.DISCONNECTED)){
            return "DISCONNECTED 断开连接";
        }else if (state.equals(WaypointMissionState.NOT_SUPPORTED)){
            return "NOT_SUPPORTED 不支持";
        }else if (state.equals(WaypointMissionState.RECOVERING)){
            return "RECOVERING 恢复连接中";
        }else if (state.equals(WaypointMissionState.READY_TO_UPLOAD)){
            return "READY_TO_UPLOAD 待上传";
        }else if (state.equals(WaypointMissionState.UPLOADING)){
            return "UPLOADING 上传中";
        }else if (state.equals(WaypointMissionState.READY_TO_EXECUTE)){
            return "READY_TO_EXECUTE 待执行";
        }else if (state.equals(WaypointMissionState.EXECUTING)){
            return "EXECUTING 执行中";
        }else if (state.equals(WaypointMissionState.EXECUTION_PAUSED)){
            return "EXECUTION_PAUSED 暂停中";
        }
        return "N/A";
    }

    // 航点任务执行枚举值转字符串
    private String waypointMissionExecuteStateToString(WaypointMissionExecuteState state) {

        switch (state)
        {
            case INITIALIZING:
                return "INITIALIZING 初始化";
            case MOVING:
                return "MOVING 移动中";
            case CURVE_MODE_MOVING:
                return "CURVE_MODE_MOVING 曲线模式移动中";
            case CURVE_MODE_TURNING:
                return "CURVE_MODE_TURNING 曲线模式拐弯中";
            case BEGIN_ACTION:
                return "BEGIN_ACTION 开始动作";
            case DOING_ACTION:
                return "DOING_ACTION 执行动作";
            case FINISHED_ACTION:
                return "FINISHED_ACTION 结束动作";
            case RETURN_TO_FIRST_WAYPOINT:
                return "RETURN_TO_FIRST_WAYPOINT 返回到第一个航点";
            case PAUSED:
                return "PAUSED 暂停中";
            default:
                return "N/A";
        }
    }

    // endregion
}
