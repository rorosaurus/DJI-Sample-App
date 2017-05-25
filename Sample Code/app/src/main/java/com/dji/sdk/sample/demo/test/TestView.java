package com.dji.sdk.sample.demo.test;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.view.PresentableView;

import dji.common.error.DJIError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointMissionState;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.GetCallback;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.products.Aircraft;


public class TestView extends LinearLayout implements PresentableView {

    private Button takeOffBtn;
    private Button landBtn;
    private Button goHomeBtn;
    private Button circleBtn;

    private TextView circleText;
    private SeekBar circleSeekBar;
    private final int circleRadius = 8;
    HotpointMissionOperator missionOperator;

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
        missionOperator = MissionControl.getInstance().getHotpointMissionOperator();
        setListeners();
        configureSettings();
    }

    private void configureSettings(){
        // ENABLE PRECISION LANDING FOR ACCURATE RETURN TO HOME
        flightController.getFlightAssistant().setPrecisionLandingEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                // :D
            }
        });
        // ADD LISTENER TO AUTO-COMPLETE LANDING SEQUENCES
        FlightControllerKey landingConf = FlightControllerKey.create(FlightControllerKey.IS_LANDING_CONFIRMATION_NEEDED);
        KeyManager.getInstance().getValue(landingConf, new GetCallback() {
                    @Override
                    public void onSuccess(final @NonNull Object o) {
                        if (o instanceof Boolean && o == true) {
                            flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    // already? I was just getting started!
                                }
                            });
                        }
                    }
                    @Override
                    public void onFailure(@NonNull DJIError djiError) {
                        // idk
                    }
                }
        );
        // todo: enable obstacle avoidance?
        // maybe if enabled, take off for easy debugging?
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void setListeners() {
        // TAKE OFF
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
        // LANDING
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
        // GO HOME
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
        // ADJUST CIRCLE VELOCITY
        circleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (missionOperator.getCurrentState() == HotpointMissionState.EXECUTING ||
                        missionOperator.getCurrentState() == HotpointMissionState.INITIAL_PHASE ||
                        missionOperator.getCurrentState() == HotpointMissionState.EXECUTION_PAUSED) {
                    missionOperator.setAngularVelocity((circleSeekBar.getProgress()+1), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            // la di da
                        }
                    });
                }
                circleSeekBar.post(new Runnable() {
                    @Override
                    public void run() {
                        circleText.setText("Circle Velocity: "+ (circleSeekBar.getProgress()+1) + "°/sec"
                                + "\n(" + 360/(circleSeekBar.getProgress()+1) + " secs for full rotation)");
                    }
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // START CIRCLING
        circleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationCoordinate3D droneLocation = flightController.getState().getAircraftLocation();

                HotpointMission mission = new HotpointMission(
                        new LocationCoordinate2D(droneLocation.getLatitude(), droneLocation.getLongitude()), // 2D point to circle around
                        5, // altitude in meters (~16ft)
                        circleRadius, // radius of circle in meters (~25ft)
                        circleSeekBar.getProgress()+1, // angular velocity in degrees per second (full rotation in ~20 seconds)
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
        circleText = (TextView) findViewById(R.id.textView4);

        circleSeekBar = (SeekBar) findViewById(R.id.seekBar);
        double maxVelocity = HotpointMissionOperator.maxAngularVelocityForRadius(circleRadius);
        circleSeekBar.setMax(((int)maxVelocity)-1);
        circleSeekBar.setProgress(17);
    }

    @Override
    public int getDescription() {
        return R.string.component_listview_test_keyed_interface;
    }

    //endregion
}

