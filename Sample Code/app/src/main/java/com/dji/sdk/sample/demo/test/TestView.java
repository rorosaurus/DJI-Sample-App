package com.dji.sdk.sample.demo.test;

import android.app.Service;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.BatteryKey;
import dji.keysdk.CameraKey;
import dji.keysdk.GimbalKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.GetCallback;
import dji.keysdk.callback.KeyListener;
import dji.keysdk.callback.SetCallback;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;


public class TestView extends LinearLayout implements PresentableView {

    private Button takeOffBtn;
    private Button landBtn;
    private Button goHomeBtn;
    private Button circleBtn;

    FlightController flightController;

    public TestView(Context context) {
        super(context);
        initUI(context);
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
        useDJIKeyedInterface();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void useDJIKeyedInterface() {
        takeOffBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        // something bad happened :(
                    }
                });
            }
        });
        landBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        // uh-oh!
                    }
                });
            }
        });
        goHomeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.setGoHomeHeightInMeters(20, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        // blah
                    }
                });
                flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        // hope it works! ;)
                    }
                });
            }
        });
        circleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                HotpointMissionOperator missionOperator = MissionControl.getInstance().getHotpointMissionOperator();

                LocationCoordinate3D droneLocation = flightController.getState().getAircraftLocation();

                HotpointMission mission = new HotpointMission(
                        new LocationCoordinate2D(droneLocation.getLatitude(), droneLocation.getLongitude()), // 2D point to circle around
                        5, // altitude in meters (~16ft)
                        8, // radius of circle in meters (~25ft)
                        18, // angular velocity in degrees per second (full rotation in ~20 seconds)
                        true, // move clockwise?
                        HotpointStartPoint.NORTH, // where should the drone start to traverse the circle?
                        HotpointHeading.TOWARDS_HOT_POINT // which way should the drone face while circling?
                );
                missionOperator.startMission(mission, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        // uhhh... hope for the best? :D
                    }
                });
            }
        });
    }

    //region Helper Method
    private void initUI(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        layoutInflater.inflate(R.layout.test_keyed_interface, this, true);

        takeOffBtn = (Button) findViewById(R.id.takeOffBtn_title);
        landBtn = (Button) findViewById(R.id.landBtn_title);
        goHomeBtn = (Button) findViewById(R.id.goHomeBtn_title);
        circleBtn = (Button) findViewById(R.id.circleBtn_title);
    }

    @Override
    public int getDescription() {
        return R.string.component_listview_test_keyed_interface;
    }

    //endregion
}

