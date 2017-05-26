package com.dji.sdk.sample.demo.test;

import android.app.Service;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.view.PresentableView;

import dji.common.error.DJIError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.activetrack.ActiveTrackMission;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.GetCallback;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.activetrack.ActiveTrackOperator;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.products.Aircraft;


public class TestView extends LinearLayout implements PresentableView {

    private Button takeOffBtn;
    private Button landBtn;
    private Button goHomeBtn;
    private ToggleButton gestBtn;
    private ToggleButton circleBtn;
    private TextView circleText;
    private SeekBar circleSeekBar;

    private final int circleRadius = 8;
    private int maxVelocity = (int) HotpointMissionOperator.maxAngularVelocityForRadius(circleRadius);

    private FlightController flightController;
    private HotpointMissionOperator hotpointMissionOperator;
    private ActiveTrackOperator activeTrackOperator;

    private CommonCallbacks.CompletionCallback logErrorCallback = new CommonCallbacks.CompletionCallback() {
        @Override
        public void onResult(DJIError djiError) {
            if(djiError != null) Log.e("TestView", djiError.toString());
        }
    };

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
        hotpointMissionOperator = MissionControl.getInstance().getHotpointMissionOperator();
        activeTrackOperator = MissionControl.getInstance().getActiveTrackOperator();
        setListeners();
        configureSettings();
    }

    private void configureSettings(){
        // ENABLE PRECISION LANDING FOR ACCURATE RETURN TO HOME
        flightController.getFlightAssistant().setPrecisionLandingEnabled(true, logErrorCallback);
        // ADD LISTENER TO AUTO-COMPLETE LANDING SEQUENCES
        FlightControllerKey landingConf = FlightControllerKey.create(FlightControllerKey.IS_LANDING_CONFIRMATION_NEEDED);
        KeyManager.getInstance().getValue(landingConf, new GetCallback() {
                    @Override
                    public void onSuccess(final @NonNull Object o) {
                        if (o instanceof Boolean && (Boolean) o) {
                            flightController.confirmLanding(logErrorCallback);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull DJIError djiError) {
                        // idk
                    }
                }
        );
        // EXPLICITLY DISABLE RETREAT FOR ACTIVE TRACK
//        activeTrackOperator.setRetreatEnabled(false, logErrorCallback);
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
                flightController.startTakeoff(logErrorCallback);
            }
        });
        // LANDING
        landBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.startLanding(logErrorCallback);
            }
        });
        // GO HOME
        goHomeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.setGoHomeHeightInMeters(20, logErrorCallback);
                flightController.startGoHome(logErrorCallback);
            }
        });
        // ENABLE/DISABLE GESTURES
        gestBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enableGest) {
                if (enableGest) {
                    activeTrackOperator.setGestureModeEnabled(true, logErrorCallback);
                }
                else { // disable and stop
                    activeTrackOperator.setGestureModeEnabled(false, logErrorCallback);
                    activeTrackOperator.stopTracking(logErrorCallback);
                }
            }
        });
        // ACTIVE TRACK LISTENERS
        /*activeTrackOperator.addListener(new ActiveTrackMissionOperatorListener() {
            @Override
            public void onUpdate(ActiveTrackMissionEvent activeTrackMissionEvent) {
                // AUTO CONFIRM ACTIVE TRACK MISSIONS
                if (activeTrackMissionEvent.getCurrentState() == ActiveTrackState.WAITING_FOR_CONFIRMATION) {
                    activeTrackOperator.acceptConfirmation(logErrorCallback);
                }
                // PREVENT AIRCRAFT FROM FOLLOWING ON ACTIVE TRACK MISSIONS
//                else if (activeTrackMissionEvent.getCurrentState() == ActiveTrackState.AIRCRAFT_FOLLOWING) {
//                    activeTrackOperator.stopAircraftFollowing(logErrorCallback);
//                }
            }
        });*/
        // ADJUST CIRCLE VELOCITY
        circleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if(fromUser){
                    // update the text
                    setSeekerBarText();

                    // and update the currentmission
                    hotpointMissionOperator.setAngularVelocity(-(circleSeekBar.getProgress()), logErrorCallback);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // START/STOP CIRCLING
        circleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean startCircling) {
                if (startCircling) { // we just tapped start circle
                    LocationCoordinate3D droneLocation = flightController.getState().getAircraftLocation();

                    HotpointMission mission = new HotpointMission(
                            new LocationCoordinate2D(droneLocation.getLatitude(), droneLocation.getLongitude()), // 2D point to circle around
                            5, // altitude in meters (~16ft)
                            circleRadius, // radius of circle in meters (~25ft)
                            circleSeekBar.getProgress(), // angular velocity in degrees per second (full rotation in ~20 seconds)
                            true, // move clockwise?
                            HotpointStartPoint.NORTH, // where should the drone start to traverse the circle?
                            HotpointHeading.TOWARDS_HOT_POINT // which way should the drone face while circling?
                    );
                    hotpointMissionOperator.startMission(mission, logErrorCallback);
                }
                else { // we just tapped stop circle
                    hotpointMissionOperator.stop(logErrorCallback);
                }
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
        gestBtn = (ToggleButton) findViewById(R.id.gestButton_title);
        circleBtn = (ToggleButton) findViewById(R.id.circleBtn_title);
        circleText = (TextView) findViewById(R.id.seekerText_title);

        circleSeekBar = (SeekBar) findViewById(R.id.seekBar);
        circleSeekBar.setMax(maxVelocity);
        circleSeekBar.setProgress(18);  // default angular velocity is 18 degrees per second (full rotation in ~20 seconds)
        setSeekerBarText();
    }

    private void setSeekerBarText(){
        if(circleSeekBar.getProgress() == 0){
            setText(circleText, "Circle Velocity: 0°/sec\n(infinity secs for full rotation)");
        }
        else {
            setText(circleText, "Circle Velocity: "+ (circleSeekBar.getProgress()) + "°/sec"
                    + "\n(" + 360/(circleSeekBar.getProgress()) + " secs for full rotation)");
        }
    }

    private void setText(final TextView tv, final String text) {
        tv.post(new Runnable() {
            @Override
            public void run() {

                tv.setText(text);
            }
        });
    }

    @Override
    public int getDescription() {
        return R.string.component_listview_test_keyed_interface;
    }

    //endregion
}

