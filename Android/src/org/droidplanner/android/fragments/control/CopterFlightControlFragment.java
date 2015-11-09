package org.droidplanner.android.fragments.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.follow.FollowState;

import org.droidplanner.android.R;
import org.droidplanner.android.activities.FlightActivity;
import org.droidplanner.android.activities.helpers.SuperUI;
import org.droidplanner.android.dialogs.SlideToUnlockDialog;
import org.droidplanner.android.dialogs.SupportYesNoDialog;
import org.droidplanner.android.dialogs.SupportYesNoWithPrefsDialog;
import org.droidplanner.android.proxy.mission.MissionProxy;
import org.droidplanner.android.utils.analytics.GAUtils;
import org.droidplanner.android.utils.collection.BroadCastIntent;
import org.droidplanner.android.utils.prefs.DRONE_MODE;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;

/**
 * Provide functionality for flight action button specific to copters.
 */
public class CopterFlightControlFragment extends BaseFlightControlFragment {

    private static final String TAG = CopterFlightControlFragment.class.getSimpleName();

    private static final String ACTION_FLIGHT_ACTION_BUTTON = "Copter flight action button";
    private int curMode = -1;
    private boolean curArmed, curFlying;

    private boolean isConnected, isflaying, isArmed;
    private boolean waitForTakeOff, AutoTakeOff;

    private static final IntentFilter eventFilter = new IntentFilter();

    static {
        eventFilter.addAction(AttributeEvent.STATE_ARMING);
        eventFilter.addAction(AttributeEvent.STATE_CONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_DISCONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_UPDATED);
        eventFilter.addAction(AttributeEvent.STATE_VEHICLE_MODE);
        eventFilter.addAction(AttributeEvent.FOLLOW_START);
        eventFilter.addAction(AttributeEvent.FOLLOW_STOP);
        eventFilter.addAction(AttributeEvent.FOLLOW_UPDATE);
        eventFilter.addAction(AttributeEvent.MISSION_DRONIE_CREATED);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_AVALIABLE);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_UNAVALIABLE);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE);
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case AttributeEvent.STATE_ARMING:
                case AttributeEvent.STATE_CONNECTED:
                case AttributeEvent.STATE_DISCONNECTED:
                case AttributeEvent.STATE_UPDATED:
                    setupButtonsByFlightState();
                    break;

                case AttributeEvent.STATE_VEHICLE_MODE:
                    updateFlightModeButtons();
                    break;

                case AttributeEvent.FOLLOW_START:
                case AttributeEvent.FOLLOW_STOP:
                    final FollowState followState = getDrone().getAttribute(AttributeType.FOLLOW_STATE);
                    if (followState != null) {
                        String eventLabel = null;
                        switch (followState.getState()) {
                            case FollowState.STATE_START:
                                eventLabel = "FollowMe enabled";
                                break;

                            case FollowState.STATE_RUNNING:
                                eventLabel = "FollowMe running";
                                break;

                            case FollowState.STATE_END:
                                eventLabel = "FollowMe disabled";
                                break;

                            case FollowState.STATE_INVALID:
                                eventLabel = "FollowMe error: invalid state";
                                break;

                            case FollowState.STATE_DRONE_DISCONNECTED:
                                eventLabel = "FollowMe error: drone not connected";
                                break;

                            case FollowState.STATE_DRONE_NOT_ARMED:
                                eventLabel = "FollowMe error: drone not armed";
                                break;
                        }

                        if (eventLabel != null) {
                            HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                                    .setCategory(GAUtils.Category.FLIGHT)
                                    .setAction(ACTION_FLIGHT_ACTION_BUTTON)
                                    .setLabel(eventLabel);
                            GAUtils.sendEvent(eventBuilder);

                            Toast.makeText(getActivity(), eventLabel, Toast.LENGTH_SHORT).show();
                        }
                    }

                    /* FALL - THROUGH */
                case AttributeEvent.FOLLOW_UPDATE:
                    updateFlightModeButtons();
                    updateFollowButton();
                    break;

                case AttributeEvent.MISSION_DRONIE_CREATED:
                    //Get the bearing of the dronie mission.
                    float bearing = intent.getFloatExtra(AttributeEventExtra.EXTRA_MISSION_DRONIE_BEARING, -1);
                    if (bearing >= 0) {
                        final FlightActivity flightActivity = (FlightActivity) getActivity();
                        if (flightActivity != null) {
                            flightActivity.updateMapBearing(bearing);
                        }
                    }
                    break;
                case BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE:
                    Log.d("Zack", "MAXXX" + curArmed+"@"+curFlying+"@"+curMode);
                    if(curArmed!=intent.getBooleanExtra("Arm",false)) {
                        isArmed = intent.getBooleanExtra("Arm", false);
                        curArmed =isArmed;
                        UpdateXmppControlButton();
                    }
                    if(curFlying!=intent.getBooleanExtra("Fly", false)){
                        isflaying =intent.getBooleanExtra("Fly", false);
                        curFlying = isflaying;
                        UpdateXmppControlButton();
                    }
                    if(intent.getIntExtra("Mode",-2)!=curMode){
                        curMode = intent.getIntExtra("Mode", -2);
                        if(curMode== DRONE_MODE.MODE_GUIDED){
                            if(waitForTakeOff){
                                Intent i = new Intent();
                                i.setAction(BroadCastIntent.COMMAND_DRONE_TAKE_OFF);
                                getActivity().sendBroadcast(i);
                                waitForTakeOff =false;
                                if(AutoTakeOff){
                                    Intent i2 = new Intent();
                                    i2.setAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION);
                                    i2.putExtra("mode", DRONE_MODE.MODE_AUTO);
                                    getActivity().sendBroadcast(i);
                                    AutoTakeOff = false;
                                }
                            }
                        }
                        isConnected=true;
                        UpdateXmppControlButton();
                    }
                    break;
                case BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_AVALIABLE:
                    isConnected =true;
                    UpdateXmppControlButton();
                    break;
                case BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_UNAVALIABLE:
                    isConnected = false;
                    UpdateXmppControlButton();
                    break;
            }
        }
    };

    private MissionProxy missionProxy;

    private View mDisconnectedButtons;
    private View mDisarmedButtons;
    private View mArmedButtons;
    private View mInFlightButtons;

    private Button followBtn;
    private Button homeBtn;
    private Button landBtn;
    private Button pauseBtn;
    private Button autoBtn;

    private int orangeColor;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_copter_mission_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        orangeColor = getResources().getColor(R.color.orange);

        mDisconnectedButtons = view.findViewById(R.id.mc_disconnected_buttons);
        mDisarmedButtons = view.findViewById(R.id.mc_disarmed_buttons);
        mArmedButtons = view.findViewById(R.id.mc_armed_buttons);
        mInFlightButtons = view.findViewById(R.id.mc_in_flight_buttons);

        final Button connectBtn = (Button) view.findViewById(R.id.mc_connectBtn);
        connectBtn.setOnClickListener(this);

        homeBtn = (Button) view.findViewById(R.id.mc_homeBtn);
        homeBtn.setOnClickListener(this);

        final Button armBtn = (Button) view.findViewById(R.id.mc_armBtn);
        armBtn.setOnClickListener(this);

        final Button disarmBtn = (Button) view.findViewById(R.id.mc_disarmBtn);
        disarmBtn.setOnClickListener(this);

        landBtn = (Button) view.findViewById(R.id.mc_land);
        landBtn.setOnClickListener(this);

        final Button takeoffBtn = (Button) view.findViewById(R.id.mc_takeoff);
        takeoffBtn.setOnClickListener(this);

        pauseBtn = (Button) view.findViewById(R.id.mc_pause);
        pauseBtn.setOnClickListener(this);

        autoBtn = (Button) view.findViewById(R.id.mc_autoBtn);
        autoBtn.setOnClickListener(this);

        final Button takeoffInAuto = (Button) view.findViewById(R.id.mc_TakeoffInAutoBtn);
        takeoffInAuto.setOnClickListener(this);

        followBtn = (Button) view.findViewById(R.id.mc_follow);
        followBtn.setOnClickListener(this);

        final Button dronieBtn = (Button) view.findViewById(R.id.mc_dronieBtn);
        dronieBtn.setOnClickListener(this);
    }

    @Override
    public void onApiConnected() {
        super.onApiConnected();
        missionProxy = getMissionProxy();

        setupButtonsByFlightState();
        updateFlightModeButtons();
        updateFollowButton();

        getBroadcastManager().registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onApiDisconnected() {
        super.onApiDisconnected();
        getBroadcastManager().unregisterReceiver(eventReceiver);
    }

    @Override
    public void onClick(View v) {
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(GAUtils.Category.FLIGHT);
        if(!isConnected){
            final Drone drone = getDrone();
            switch (v.getId()) {
                case R.id.mc_connectBtn:
                    ((SuperUI) getActivity()).toggleDroneConnection();
                    break;

                case R.id.mc_armBtn:
                    getArmingConfirmation();
                    eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel("Arm");
                    break;

                case R.id.mc_disarmBtn:
                    getDrone().arm(false);
                    eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel("Disarm");
                    break;

                case R.id.mc_land:
                    getDrone().changeVehicleMode(VehicleMode.COPTER_LAND);
                    eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel(VehicleMode
                            .COPTER_LAND.getLabel());
                    break;

                case R.id.mc_takeoff:
                    getTakeOffConfirmation();
                    eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel("Takeoff");
                    break;

                case R.id.mc_homeBtn:
                    getDrone().changeVehicleMode(VehicleMode.COPTER_RTL);
                    eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel(VehicleMode.COPTER_RTL
                            .getLabel());
                    break;

                case R.id.mc_pause: {
                    final FollowState followState = drone.getAttribute(AttributeType.FOLLOW_STATE);
                    if (followState.isEnabled()) {
                        drone.disableFollowMe();
                    }

                    drone.pauseAtCurrentLocation();
                    eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel("Pause");
                    break;
                }

                case R.id.mc_autoBtn:
                    getDrone().changeVehicleMode(VehicleMode.COPTER_AUTO);
                    eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel(VehicleMode.COPTER_AUTO.getLabel());
                    break;

                case R.id.mc_TakeoffInAutoBtn:
                    getTakeOffInAutoConfirmation();
                    eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel(VehicleMode.COPTER_AUTO.getLabel());
                    break;

                case R.id.mc_follow:
                    toggleFollowMe();
                    break;

                case R.id.mc_dronieBtn:
                    getDronieConfirmation();
                    eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel("Dronie uploaded");
                    break;

                default:
                    eventBuilder = null;
                    break;
            }

            if (eventBuilder != null) {
                GAUtils.sendEvent(eventBuilder);
            }
        }else{
            Intent i = new Intent();
            switch (v.getId()) {
                case R.id.mc_armBtn:
                    i.setAction(BroadCastIntent.PROPERTY_DRONE_ARM_STATE_CHANGE);
                    i.putExtra("arm", true);
                    getActivity().sendBroadcast(i);

                    break;

                case R.id.mc_disarmBtn:
                    i.setAction(BroadCastIntent.PROPERTY_DRONE_ARM_STATE_CHANGE);
                    i.putExtra("arm", false);
                    getActivity().sendBroadcast(i);

                    break;

                case R.id.mc_land:

                    i.putExtra("mode", DRONE_MODE.MODE_LAND);
                    getActivity().sendBroadcast(i);

                    break;

                case R.id.mc_takeoff:
                    if(curMode ==DRONE_MODE.MODE_GUIDED){
                        i.setAction(BroadCastIntent.COMMAND_DRONE_TAKE_OFF);
                        getActivity().sendBroadcast(i);
                    }else{
                        i.setAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION);
                        i.putExtra("mode", DRONE_MODE.MODE_GUIDED);
                        getActivity().sendBroadcast(i);
                        waitForTakeOff = true;
                    }
                    break;

                case R.id.mc_homeBtn:
                    i.setAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION);
                    i.putExtra("mode", DRONE_MODE.MODE_RTL);
                    getActivity().sendBroadcast(i);
                    break;

                case R.id.mc_pause: {
                    break;
                }

                case R.id.mc_autoBtn:
                    i.setAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION);
                    i.putExtra("mode", DRONE_MODE.MODE_AUTO);
                    getActivity().sendBroadcast(i);
                    break;

                case R.id.mc_TakeoffInAutoBtn:
                    if(curMode ==DRONE_MODE.MODE_GUIDED){
                        i.setAction(BroadCastIntent.COMMAND_DRONE_TAKE_OFF);
                        getActivity().sendBroadcast(i);
                    }else{
                        i.setAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION);
                        i.putExtra("mode", DRONE_MODE.MODE_GUIDED);
                        getActivity().sendBroadcast(i);
                        waitForTakeOff = true;
                        AutoTakeOff = true;
                    }
                    break;

                case R.id.mc_follow:
                    break;

                case R.id.mc_dronieBtn:
                    break;

                default:
                    break;
            }

        }


    }

    private void getDronieConfirmation() {
        SupportYesNoWithPrefsDialog ynd = SupportYesNoWithPrefsDialog.newInstance(getActivity()
                        .getApplicationContext(), getString(R.string.pref_dronie_creation_title),
                getString(R.string.pref_dronie_creation_message), new SupportYesNoDialog.Listener() {
                    @Override
                    public void onYes() {
                        missionProxy.makeAndUploadDronie(getDrone());
                    }

                    @Override
                    public void onNo() {
                    }
                }, DroidPlannerPrefs.PREF_WARN_ON_DRONIE_CREATION);

        if (ynd != null) {
            ynd.show(getChildFragmentManager(), "Confirm dronie creation");
        }
    }

    private void getTakeOffConfirmation(){
        final SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("take off", new Runnable() {
            @Override
            public void run() {
                final int takeOffAltitude = getAppPrefs().getDefaultAltitude();
                getDrone().doGuidedTakeoff(takeOffAltitude);
            }
        });
        unlockDialog.show(getChildFragmentManager(), "Slide to take off");
    }

    private void getTakeOffInAutoConfirmation() {
        final SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("take off in auto", new Runnable() {
            @Override
            public void run() {

                final int takeOffAltitude = getAppPrefs().getDefaultAltitude();

                Drone drone = getDrone();
                drone.doGuidedTakeoff(takeOffAltitude);
                drone.changeVehicleMode(VehicleMode.COPTER_AUTO);
            }
        });
        unlockDialog.show(getChildFragmentManager(), "Slide to take off in auto");
    }

    private void getArmingConfirmation() {
        SlideToUnlockDialog unlockDialog = SlideToUnlockDialog.newInstance("arm", new Runnable() {
            @Override
            public void run() {
                getDrone().arm(true);
            }
        }) ;
        unlockDialog.show(getChildFragmentManager(), "Slide To Arm");
    }

    private void updateFlightModeButtons() {
        resetFlightModeButtons();

        State droneState = getDrone().getAttribute(AttributeType.STATE);
        if (droneState == null)
            return;

        final VehicleMode flightMode = droneState.getVehicleMode();
        if (flightMode == null)
            return;

        switch (flightMode) {
            case COPTER_AUTO:
                autoBtn.setActivated(true);
                break;

            case COPTER_GUIDED:
                final Drone drone = getDrone();
                final GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
                final FollowState followState = drone.getAttribute(AttributeType.FOLLOW_STATE);
                if (guidedState.isInitialized() && !followState.isEnabled()) {
                    pauseBtn.setActivated(true);
                }
                break;

            case COPTER_RTL:
                homeBtn.setActivated(true);
                break;

            case COPTER_LAND:
                landBtn.setActivated(true);
                break;
            default:
                break;
        }
    }

    private void resetFlightModeButtons() {
        homeBtn.setActivated(false);
        landBtn.setActivated(false);
        pauseBtn.setActivated(false);
        autoBtn.setActivated(false);
    }

    private void updateFollowButton() {
        FollowState followState = getDrone().getAttribute(AttributeType.FOLLOW_STATE);
        if (followState == null)
            return;

        switch (followState.getState()) {
            case FollowState.STATE_START:
                followBtn.setBackgroundColor(orangeColor);
                break;

            case FollowState.STATE_RUNNING:
                followBtn.setActivated(true);
                followBtn.setBackgroundResource(R.drawable.flight_action_row_bg_selector);
                break;

            default:
                followBtn.setActivated(false);
                followBtn.setBackgroundResource(R.drawable.flight_action_row_bg_selector);
                break;
        }
    }

    private void resetButtonsContainerVisibility() {
        Log.d(TAG,"resetButtonsContainerVisibility");
        mDisconnectedButtons.setVisibility(View.GONE);
        mDisarmedButtons.setVisibility(View.GONE);
        mArmedButtons.setVisibility(View.GONE);
        mInFlightButtons.setVisibility(View.GONE);
    }

    private void setupButtonsByFlightState() {
        final State droneState = getDrone().getAttribute(AttributeType.STATE);
        if (droneState != null && droneState.isConnected()) {
            if (droneState.isArmed()) {
                if (droneState.isFlying()) {
                    setupButtonsForFlying();
                } else {
                    setupButtonsForArmed();
                }
            } else {
                setupButtonsForDisarmed();
            }
        } else {
            setupButtonsForDisconnected();
        }
    }

    private void setupButtonsForDisconnected() {
        resetButtonsContainerVisibility();
        mDisconnectedButtons.setVisibility(View.VISIBLE);
        Log.d(TAG, "setupButtonsForDisconnected");
    }

    private void setupButtonsForDisarmed() {
        resetButtonsContainerVisibility();
        mDisarmedButtons.setVisibility(View.VISIBLE);
        Log.d(TAG, "setupButtonsForDisarmed");
    }

    private void setupButtonsForArmed() {
        resetButtonsContainerVisibility();
        mArmedButtons.setVisibility(View.VISIBLE);
        Log.d(TAG, "setupButtonsForArmed");
    }

    private void setupButtonsForFlying() {
        resetButtonsContainerVisibility();
        mInFlightButtons.setVisibility(View.VISIBLE);
        Log.d(TAG, "setupButtonsForFlying");
    }

    @Override
    public boolean isSlidingUpPanelEnabled(Drone drone) {
        if (!drone.isConnected())
            return false;

        final State droneState = drone.getAttribute(AttributeType.STATE);
        return droneState.isArmed() && droneState.isFlying();
    }


    private void UpdateXmppControlButton(){

        if (isConnected) {
            if (isArmed) {
                if (isflaying) {
                        setupButtonsForFlying();

                } else {
                        setupButtonsForArmed();
                }
            } else {
                    setupButtonsForDisarmed();
            }
        } else {
                setupButtonsForDisconnected();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(eventReceiver);
    }
}
