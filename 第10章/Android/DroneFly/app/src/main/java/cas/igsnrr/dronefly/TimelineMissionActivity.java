package cas.igsnrr.dronefly;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.Rotation;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.TimelineMission;
import dji.sdk.mission.timeline.actions.GimbalAttitudeAction;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.timeline.actions.HotpointAction;
import dji.sdk.mission.timeline.actions.LandAction;
import dji.sdk.mission.timeline.actions.RecordVideoAction;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.mission.timeline.triggers.AircraftLandedTrigger;
import dji.sdk.mission.timeline.triggers.Trigger;
import dji.sdk.mission.timeline.triggers.TriggerEvent;

public class TimelineMissionActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mBtnLoadTimelineMission, mBtnStartTimelineMission;
    private Button mBtnPauseTimelineMission, mBtnResumeTimelineMission, mBtnStopTimelineMission;

    private TextView mTvStatusTimelineMission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline_mission);

        // 初始化UI界面
        initUI();
        // 初始化监听器
        initListener();
    }

    // 初始化UI界面
    private void initUI() {

        mTvStatusTimelineMission = findViewById(R.id.tv_status_timeline_mission);

        mBtnLoadTimelineMission = findViewById(R.id.btn_load_timeline_mission);
        mBtnStartTimelineMission = findViewById(R.id.btn_start_timeline_mission);
        mBtnPauseTimelineMission = findViewById(R.id.btn_pause_timeline_mission);
        mBtnResumeTimelineMission = findViewById(R.id.btn_resume_timeline_mission);
        mBtnStopTimelineMission = findViewById(R.id.btn_stop_timeline_mission);

        mBtnLoadTimelineMission.setOnClickListener(this);
        mBtnStartTimelineMission.setOnClickListener(this);
        mBtnPauseTimelineMission.setOnClickListener(this);
        mBtnResumeTimelineMission.setOnClickListener(this);
        mBtnStopTimelineMission.setOnClickListener(this);
    }

    // 初始化监听器
    private void initListener() {

        // 添加任务控制器的监听器
        MissionControl.getInstance().addListener(new MissionControl.Listener() {
            @Override
            public void onEvent(TimelineElement timelineElement, TimelineEvent timelineEvent, DJIError djiError) {

                // 当无时间线元素时，此时为整个时间线任务的状态更新
                if (timelineElement == null) {
                    mTvStatusTimelineMission.setText(timelineEvent.toString());
                    return;
                }
                // 当时间线元素不为空时，此时为该时间线元素的状态更新
                if (timelineElement instanceof TimelineMission) {
                    String elementName = ((TimelineMission) timelineElement).getMissionObject().getClass().getSimpleName();
                    String eventName = timelineEvent.toString();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvStatusTimelineMission.setText(elementName + " " + eventName + "!");
                        }
                    });
                } else {
                    String elementName = timelineElement.getClass().getSimpleName();
                    String eventName = timelineEvent.toString();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvStatusTimelineMission.setText(elementName + " " + eventName);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除任务控制器的监听器
        MissionControl.getInstance().removeAllListeners();
    }

    // region UI事件

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_load_timeline_mission: loadTimelineMission(); break; // 加载时间线任务
            case R.id.btn_start_timeline_mission: startTimelineMission(); break; // 开始时间线任务
            case R.id.btn_pause_timeline_mission: pauseTimelineMission(); break; // 暂停时间线任务
            case R.id.btn_resume_timeline_mission: resumeTimelineMission(); break; // 继续时间线任务
            case R.id.btn_stop_timeline_mission: stopTimelineMission(); break; // 停止时间线任务
        }
    }

    // 加载时间线任务
    private void loadTimelineMission() {

        // 如果任务控制器存在时间线元素，则清除所有的元素
        if (MissionControl.getInstance().scheduledCount() > 0) {
            MissionControl.getInstance().unscheduleEverything();
        }

        // 时间线元素列表
        List<TimelineElement> elements = new ArrayList<>();

        // 1. 起飞
        elements.add(new TakeOffAction());

        // 2. 云台朝下45°
        Attitude attitude = new Attitude(-45, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        GimbalAttitudeAction gimbalAction = new GimbalAttitudeAction(attitude);
        gimbalAction.setCompletionTime(2);
        elements.add(gimbalAction);

        // 3. 开始录像
        elements.add(RecordVideoAction.newStartRecordVideoAction());

        // 4. 去往经度：125.714001 纬度：43.528712 高度：10米
        elements.add(new GoToAction(new LocationCoordinate2D(43.528712, 125.714001), 10));

        // 5. 停止录像
        elements.add(RecordVideoAction.newStopRecordVideoAction());

        // 6. 拍摄一张相片
        elements.add(ShootPhotoAction.newShootSinglePhotoAction());

        // 7. 执行兴趣点环绕任务
        HotpointMission hotpointMission = new HotpointMission();
        hotpointMission.setHotpoint(new LocationCoordinate2D(43.528419, 125.713440)); // 兴趣点
        hotpointMission.setAltitude(10); // 飞行高度
        hotpointMission.setRadius(10); // 环绕半径
        hotpointMission.setAngularVelocity(10); // 角速度 单位：度/秒
        hotpointMission.setStartPoint(HotpointStartPoint.NEAREST);  // 环绕开始位置
        hotpointMission.setHeading(HotpointHeading.TOWARDS_HOT_POINT); // 航向：对准兴趣点
        // 通过兴趣点环绕任务对象，创建兴趣点环绕动作对象
        elements.add(new HotpointAction(hotpointMission, 360));

        // 8. 降落
        elements.add(new LandAction());

        // 9. 执行航点飞行任务
        elements.add(TimelineMission.elementFromWaypointMission(initWaypointMission()));

        // 任务控制器加载时间线元素
        MissionControl.getInstance().scheduleElements(elements);

        // 添加飞机降落触发器。降落后触发动作，弹出“飞行降落触发Action!"提示框
        AircraftLandedTrigger trigger = new AircraftLandedTrigger();
        trigger.setAction(new Trigger.Action() {
            @Override
            public void onCall() {
                showToast("飞机降落触发Action!");
            }
        });
        // 任务控制器加载触发器
        List<Trigger> triggers = MissionControl.getInstance().getTriggers();
        if (triggers == null) {
            triggers = new ArrayList<>();
        }
        triggers.add(trigger);
        MissionControl.getInstance().setTriggers(triggers);

        showToast("加载任务成功!");
    }

    // 创建航点飞行任务
    private WaypointMission initWaypointMission() {

        // 创建航点动作
        WaypointAction actionStay = new WaypointAction(WaypointActionType.STAY, 2000);
        WaypointAction actionTakePhoto = new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0);
        WaypointAction actionStartRecord = new WaypointAction(WaypointActionType.START_RECORD, 0);
        WaypointAction actionStopRecord = new WaypointAction(WaypointActionType.STOP_RECORD, 0);
        WaypointAction actionAircraftToNorth = new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 0);
        WaypointAction actionGimbalStraightDown = new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90);
        WaypointAction actionGimbal45degree = new WaypointAction(WaypointActionType.GIMBAL_PITCH, -45);
        WaypointAction actionGimbalHorizontal = new WaypointAction(WaypointActionType.GIMBAL_PITCH, 0);

        // 创建航点 1
        Waypoint waypoint1 = new Waypoint(43.528712, 125.714001, 20);
        waypoint1.addAction(actionGimbalStraightDown);
        waypoint1.addAction(actionTakePhoto);
        waypoint1.addAction(actionStay);
        waypoint1.addAction(actionGimbalHorizontal);
        waypoint1.addAction(actionAircraftToNorth);
        waypoint1.addAction(actionTakePhoto);
        // 创建航点 2
        Waypoint waypoint2 = new Waypoint(43.528415, 125.714571, 40);
        waypoint2.addAction(actionStay);
        waypoint2.addAction(actionStartRecord);
        // 创建航点 3
        Waypoint waypoint3 = new Waypoint(43.527944, 125.714470, 40);
        waypoint3.addAction(actionStopRecord);
        waypoint3.addAction(actionGimbal45degree);
        waypoint3.shootPhotoTimeInterval = 2;
        // 创建航点 4
        Waypoint waypoint4 = new Waypoint(43.527794, 125.713809, 30);

        // 构建航点任务
        WaypointMission.Builder builder = new WaypointMission.Builder();
        builder.addWaypoint(waypoint1);
        builder.addWaypoint(waypoint2);
        builder.addWaypoint(waypoint3);
        builder.addWaypoint(waypoint4);

        // 航点任务的飞行速度为5m/s，常规飞行路径，自动航向，结束后返航。
        builder.autoFlightSpeed(5)
                .maxFlightSpeed(5)
                .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                .finishedAction(WaypointMissionFinishedAction.GO_HOME)
                .headingMode(WaypointMissionHeadingMode.AUTO);

        // 创建航点任务
        WaypointMission mission = builder.build();

        // 航点任务检查
        DJIError error =  mission.checkParameters();
        if (error != null) {
            return null;
        }
        return mission;
    }

    // 开始时间线任务
    private void startTimelineMission() {
        if (MissionControl.getInstance().scheduledCount() > 0) {
            MissionControl.getInstance().startTimeline();
            showToast("开始任务成功!");
        } else {
            showToast("无时间线元素，请加载任务!");
        }

    }

    // 暂停时间线任务
    private void pauseTimelineMission() {
        MissionControl.getInstance().pauseTimeline();
        showToast("暂停任务成功!");
    }

    // 继续时间线任务
    private void resumeTimelineMission() {
        MissionControl.getInstance().resumeTimeline();
        showToast("继续任务成功!");
    }

    // 停止时间线任务
    private void stopTimelineMission() {
        MissionControl.getInstance().stopTimeline();
        showToast("停止任务成功!");
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

