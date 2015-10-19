package org.droidplanner.android.fragments.actionbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Signal;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.gcs.returnToMe.ReturnToMeState;
import com.o3dr.services.android.lib.util.MathUtils;

import org.beyene.sius.unit.length.LengthUnit;
import org.droidplanner.android.R;
<<<<<<< HEAD
import org.droidplanner.android.activities.helpers.SuperUI;
import org.droidplanner.android.data.DroneModel;
import org.droidplanner.android.data.GpsModel;
import org.droidplanner.android.data.SignalModel;
import org.droidplanner.android.fragments.SettingsFragment;
import org.droidplanner.android.fragments.helpers.ApiListenerFragment;
import org.droidplanner.android.utils.collection.BroadCastIntent;
import org.droidplanner.android.utils.prefs.DRONE_MODE;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;
import org.droidplanner.android.widgets.spinners.ModeAdapter;
import org.droidplanner.android.widgets.spinners.SpinnerSelfSelect;
=======
import org.droidplanner.android.dialogs.SelectionListDialog;
import org.droidplanner.android.fragments.SettingsFragment;
import org.droidplanner.android.fragments.helpers.ApiListenerFragment;
import org.droidplanner.android.utils.Utils;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;
>>>>>>> DroidPlanner/develop

import java.util.Locale;

/**
 * Created by Fredia Huya-Kouadio on 1/14/15.
 */
public class ActionBarTelemFragment extends ApiListenerFragment {

    private boolean fregIsVisiable;
    private int curMode = -1;

    private final static IntentFilter eventFilter = new IntentFilter();

    static {
        eventFilter.addAction(AttributeEvent.BATTERY_UPDATED);
        eventFilter.addAction(AttributeEvent.STATE_CONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_DISCONNECTED);
        eventFilter.addAction(AttributeEvent.GPS_POSITION);
        eventFilter.addAction(AttributeEvent.GPS_COUNT);
        eventFilter.addAction(AttributeEvent.GPS_FIX);
        eventFilter.addAction(AttributeEvent.SIGNAL_UPDATED);
        eventFilter.addAction(AttributeEvent.STATE_VEHICLE_MODE);
        eventFilter.addAction(AttributeEvent.TYPE_UPDATED);
        eventFilter.addAction(AttributeEvent.ALTITUDE_UPDATED);

        eventFilter.addAction(SettingsFragment.ACTION_PREF_HDOP_UPDATE);
        eventFilter.addAction(SettingsFragment.ACTION_PREF_UNIT_SYSTEM_UPDATE);
<<<<<<< HEAD
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_BETTERY);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_SIGNAL);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_GPS);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_AVALIABLE);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_UNAVALIABLE);
=======

        eventFilter.addAction(DroidPlannerPrefs.ACTION_PREF_RETURN_TO_ME_UPDATED);
        eventFilter.addAction(AttributeEvent.RETURN_TO_ME_STATE_UPDATE);
        eventFilter.addAction(AttributeEvent.HOME_UPDATED);
>>>>>>> DroidPlanner/develop
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
<<<<<<< HEAD

            if(!fregIsVisiable){
                showTelemBar();
            }

            if(getActivity() == null)
=======
            if (getActivity() == null)
>>>>>>> DroidPlanner/develop
                return;

            switch (intent.getAction()) {
                case AttributeEvent.BATTERY_UPDATED:
                    updateBatteryTelem();
                    break;

                case AttributeEvent.STATE_CONNECTED:
                    showTelemBar();
                    updateAllTelem();
                    break;

                case AttributeEvent.STATE_DISCONNECTED:
                    hideTelemBar();
                    updateAllTelem();
                    break;

                case DroidPlannerPrefs.ACTION_PREF_RETURN_TO_ME_UPDATED:
                case AttributeEvent.RETURN_TO_ME_STATE_UPDATE:
                case AttributeEvent.GPS_POSITION:
                case AttributeEvent.HOME_UPDATED:
                    updateHomeTelem();
                    break;

                case AttributeEvent.GPS_COUNT:
                case AttributeEvent.GPS_FIX:
                    updateGpsTelem();
                    break;

                case AttributeEvent.SIGNAL_UPDATED:
                    updateSignalTelem();
                    break;

                case AttributeEvent.STATE_VEHICLE_MODE:
                case AttributeEvent.TYPE_UPDATED:
                    updateFlightModeTelem();
                    break;

                case SettingsFragment.ACTION_PREF_HDOP_UPDATE:
                    updateGpsTelem();
                    break;

                case SettingsFragment.ACTION_PREF_UNIT_SYSTEM_UPDATE:
                    updateHomeTelem();
                    break;
<<<<<<< HEAD
                case BroadCastIntent.PROPERTY_DRONE_BETTERY:
                    updateBatteryTelem(((SuperUI) getActivity()).mDroneModel);
                    break;
                case BroadCastIntent.PROPERTY_DRONE_SIGNAL:
                    updateSignalTelem(((SuperUI) getActivity()).mSignalModel);
                    break;
                case BroadCastIntent.PROPERTY_DRONE_GPS:
                    updateGpsTelem(((SuperUI) getActivity()).mGpsModel);
                    break;
                case BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE:
                    if(intent.getIntExtra("Mode",-2)!=curMode){
                        Log.d("Zack","Receive Mode change");
                        curMode = intent.getIntExtra("Mode", -2);
                        updateFlightModeTelem(curMode);
                    }
                    break;
                case BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_AVALIABLE:
                    showTelemBar();
                    break;
                case BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_UNAVALIABLE:
                    hideTelemBar();
                    break;
=======

                case AttributeEvent.ALTITUDE_UPDATED:
                    updateAltitudeTelem();
                    break;

>>>>>>> DroidPlanner/develop
                default:
                    break;
            }
        }
    };

    private DroidPlannerPrefs appPrefs;

    private TextView homeTelem;
    private TextView altitudeTelem;

    private TextView gpsTelem;
    private PopupWindow gpsPopup;

    private TextView batteryTelem;
    private PopupWindow batteryPopup;

    private TextView signalTelem;
    private PopupWindow signalPopup;

    private TextView flightModeTelem;

    private String emptyString;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_action_bar_telem, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyString = getString(R.string.empty_content);

        final Context context = getActivity().getApplicationContext();
        final LayoutInflater inflater = LayoutInflater.from(context);

        final int popupWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
        final int popupHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
        final Drawable popupBg = getResources().getDrawable(android.R.color.transparent);

        homeTelem = (TextView) view.findViewById(R.id.bar_home);
        homeTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Launch dialog to allow the user to select between rtl and rtm
                final SelectionListDialog selectionDialog = SelectionListDialog.newInstance(new ReturnToHomeAdapter(context, getDrone(), appPrefs));
                Utils.showDialog(selectionDialog, getChildFragmentManager(), "Return to home type", true);
            }
        });

        altitudeTelem = (TextView) view.findViewById(R.id.bar_altitude);

        gpsTelem = (TextView) view.findViewById(R.id.bar_gps);
        final View gpsPopupView = inflater.inflate(R.layout.popup_info_gps, (ViewGroup) view, false);
        gpsPopup = new PopupWindow(gpsPopupView, popupWidth, popupHeight, true);
        gpsPopup.setBackgroundDrawable(popupBg);
        gpsTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpsPopup.showAsDropDown(gpsTelem);
            }
        });

        batteryTelem = (TextView) view.findViewById(R.id.bar_battery);
        final View batteryPopupView = inflater.inflate(R.layout.popup_info_power, (ViewGroup) view, false);
        batteryPopup = new PopupWindow(batteryPopupView, popupWidth, popupHeight, true);
        batteryPopup.setBackgroundDrawable(popupBg);
        batteryTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                batteryPopup.showAsDropDown(batteryTelem);
            }
        });

        signalTelem = (TextView) view.findViewById(R.id.bar_signal);
        final View signalPopupView = inflater.inflate(R.layout.popup_info_signal, (ViewGroup) view, false);
        signalPopup = new PopupWindow(signalPopupView, popupWidth, popupHeight, true);
        signalPopup.setBackgroundDrawable(popupBg);
        signalTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signalPopup.showAsDropDown(signalTelem);
            }
        });

<<<<<<< HEAD
        flightModeTelem = (SpinnerSelfSelect) view.findViewById(R.id.bar_flight_mode);
        modeAdapter = new ModeAdapter(context, R.layout.spinner_drop_down_flight_mode);
        initList();
=======
        flightModeTelem = (TextView) view.findViewById(R.id.bar_flight_mode);
        flightModeTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Launch dialog to allow the user to select vehicle modes
                final Drone drone = getDrone();

                final SelectionListDialog selectionDialog = SelectionListDialog.newInstance(new FlightModeAdapter(context, drone));
                Utils.showDialog(selectionDialog, getChildFragmentManager(), "Flight modes selection", true);
            }
        });
>>>>>>> DroidPlanner/develop

        appPrefs = new DroidPlannerPrefs(context);
    }

<<<<<<< HEAD
    private void showTelemBar(){
        fregIsVisiable = true;
=======
    private void showTelemBar() {
>>>>>>> DroidPlanner/develop
        final View view = getView();
        if (view != null)
            view.setVisibility(View.VISIBLE);
    }

<<<<<<< HEAD
    private void hideTelemBar(){
        fregIsVisiable = false;
=======
    private void hideTelemBar() {
>>>>>>> DroidPlanner/develop
        final View view = getView();
        if (view != null)
            view.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        hideTelemBar();
        super.onStart();
    }

    @Override
    public void onApiConnected() {
        final Drone drone = getDrone();
        if (drone.isConnected())
            showTelemBar();
        else
            hideTelemBar();

<<<<<<< HEAD
        flightModeTelem.setAdapter(modeAdapter);
=======
>>>>>>> DroidPlanner/develop
        updateAllTelem();
        getBroadcastManager().registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onApiDisconnected() {
        getBroadcastManager().unregisterReceiver(eventReceiver);
    }

    private void updateAllTelem() {
        updateFlightModeTelem();
        updateSignalTelem();
        updateGpsTelem();
        updateHomeTelem();
        updateBatteryTelem();
        updateAltitudeTelem();
    }

    private void updateFlightModeTelem() {
        final Drone drone = getDrone();

        final boolean isDroneConnected = drone.isConnected();
        final State droneState = drone.getAttribute(AttributeType.STATE);
        if (isDroneConnected) {
            flightModeTelem.setText(droneState.getVehicleMode().getLabel());
            flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_light_blue_a400_18dp, 0, 0, 0);
        } else {
            flightModeTelem.setText(emptyString);
            flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_grey_700_18dp, 0, 0, 0);
        }
    }

    private void updateSignalTelem() {
        final Drone drone = getDrone();

        final View popupView = signalPopup.getContentView();
        TextView rssiView = (TextView) popupView.findViewById(R.id.bar_signal_rssi);
        TextView remRssiView = (TextView) popupView.findViewById(R.id.bar_signal_remrssi);
        TextView noiseView = (TextView) popupView.findViewById(R.id.bar_signal_noise);
        TextView remNoiseView = (TextView) popupView.findViewById(R.id.bar_signal_remnoise);
        TextView fadeView = (TextView) popupView.findViewById(R.id.bar_signal_fade);
        TextView remFadeView = (TextView) popupView.findViewById(R.id.bar_signal_remfade);

        final Signal droneSignal = drone.getAttribute(AttributeType.SIGNAL);
        if (!drone.isConnected() || !droneSignal.isValid()) {
            signalTelem.setText(emptyString);
            signalTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_signal_cellular_null_grey_700_18dp,
                    0, 0, 0);

            rssiView.setText("RSSI: " + emptyString);
            remRssiView.setText("RemRSSI: " + emptyString);
            noiseView.setText("Noise: " + emptyString);
            remNoiseView.setText("RemNoise: " + emptyString);
            fadeView.setText("Fade: " + emptyString);
            remFadeView.setText("RemFade: " + emptyString);
        } else {
            final int signalStrength = (int) droneSignal.getSignalStrength();
            final int signalIcon;
            if (signalStrength >= 100)
                signalIcon = R.drawable.ic_signal_cellular_4_bar_grey_700_18dp;
            else if (signalStrength >= 75)
                signalIcon = R.drawable.ic_signal_cellular_3_bar_grey_700_18dp;
            else if (signalStrength >= 50)
                signalIcon = R.drawable.ic_signal_cellular_2_bar_grey_700_18dp;
            else if (signalStrength >= 25)
                signalIcon = R.drawable.ic_signal_cellular_1_bar_grey_700_18dp;
            else
                signalIcon = R.drawable.ic_signal_cellular_0_bar_grey_700_18dp;

            signalTelem.setText(String.format(Locale.ENGLISH, "%d%%", signalStrength));
            signalTelem.setCompoundDrawablesWithIntrinsicBounds(signalIcon, 0, 0, 0);

            rssiView.setText(String.format("RSSI %2.0f dB", droneSignal.getRssi()));
            remRssiView.setText(String.format("RemRSSI %2.0f dB", droneSignal.getRemrssi()));
            noiseView.setText(String.format("Noise %2.0f dB", droneSignal.getNoise()));
            remNoiseView.setText(String.format("RemNoise %2.0f dB", droneSignal.getRemnoise()));
            fadeView.setText(String.format("Fade %2.0f dB", droneSignal.getFadeMargin()));
            remFadeView.setText(String.format("RemFade %2.0f dB", droneSignal.getRemFadeMargin()));
        }

        signalPopup.update();
    }

    private void updateGpsTelem() {
        final Drone drone = getDrone();
        final boolean displayHdop = appPrefs.shouldGpsHdopBeDisplayed();

        final View popupView = gpsPopup.getContentView();
        TextView satNoView = (TextView) popupView.findViewById(R.id.bar_gps_satno);
        TextView hdopStatusView = (TextView) popupView.findViewById(R.id.bar_gps_hdop_status);
        hdopStatusView.setVisibility(displayHdop ? View.GONE : View.VISIBLE);

        final String update;
        final int gpsIcon;
        if (!drone.isConnected()) {
            update = (displayHdop ? "hdop: " : "") + emptyString;
            gpsIcon = R.drawable.ic_gps_off_grey_700_18dp;
            satNoView.setText("S: " + emptyString);
            hdopStatusView.setText("hdop: " + emptyString);
        } else {
            Gps droneGps = drone.getAttribute(AttributeType.GPS);
            final String fixStatus = droneGps.getFixStatus();

            if (displayHdop) {
                update = String.format(Locale.ENGLISH, "hdop: %.1f", droneGps.getGpsEph());
            } else {
                update = String.format(Locale.ENGLISH, "%s", fixStatus);
            }

            switch (fixStatus) {
                case Gps.LOCK_3D:
//                case Gps.LOCK_3D_DGPS:
//                case Gps.LOCK_3D_RTK:
//                    gpsIcon = R.drawable.ic_gps_fixed_black_24dp;
//                    break;

                case Gps.LOCK_2D:
                case Gps.NO_FIX:
                default:
                    gpsIcon = R.drawable.ic_gps_not_fixed_grey_700_18dp;
                    break;
            }

            satNoView.setText(String.format(Locale.ENGLISH, "S: %d", droneGps.getSatellitesCount()));
            if (appPrefs.shouldGpsHdopBeDisplayed()) {
                hdopStatusView.setText(String.format(Locale.ENGLISH, "%s", fixStatus));
            } else {
                hdopStatusView.setText(String.format(Locale.ENGLISH, "hdop: %.1f", droneGps.getGpsEph()));
            }
        }

        gpsTelem.setText(update);
        gpsTelem.setCompoundDrawablesWithIntrinsicBounds(gpsIcon, 0, 0, 0);
        gpsPopup.update();
    }

    private void updateHomeTelem() {
        final Drone drone = getDrone();

        String update = getString(R.string.empty_content);
        int drawableResId = appPrefs.isReturnToMeEnabled()
                ? R.drawable.ic_person_grey_700_18dp
                : R.drawable.ic_home_grey_700_18dp;

        if (drone.isConnected()) {
            final Gps droneGps = drone.getAttribute(AttributeType.GPS);
            final Home droneHome = drone.getAttribute(AttributeType.HOME);
            if (droneGps.isValid() && droneHome.isValid()) {
                LengthUnit distanceToHome = getLengthUnitProvider().boxBaseValueToTarget
                        (MathUtils.getDistance2D(droneHome.getCoordinate(), droneGps.getPosition()));
                update = String.format("%s", distanceToHome);

                final ReturnToMeState returnToMe = drone.getAttribute(AttributeType.RETURN_TO_ME_STATE);
                switch (returnToMe.getState()) {

                    case ReturnToMeState.STATE_UPDATING_HOME:
                        //Change the home telemetry icon
                        drawableResId = R.drawable.ic_person_blue_a400_18dp;
                        break;

                    case ReturnToMeState.STATE_USER_LOCATION_INACCURATE:
                    case ReturnToMeState.STATE_USER_LOCATION_UNAVAILABLE:
                    case ReturnToMeState.STATE_WAITING_FOR_VEHICLE_GPS:
                    case ReturnToMeState.STATE_ERROR_UPDATING_HOME:
                        drawableResId = R.drawable.ic_person_red_500_18dp;
                        break;
                }
            }
        }

        homeTelem.setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, 0, 0);
        homeTelem.setText(update);
    }

    private void updateBatteryTelem() {
        final Drone drone = getDrone();

        final View batteryPopupView = batteryPopup.getContentView();
        final TextView dischargeView = (TextView) batteryPopupView.findViewById(R.id.bar_power_discharge);
        final TextView currentView = (TextView) batteryPopupView.findViewById(R.id.bar_power_current);
        final TextView remainView = (TextView) batteryPopupView.findViewById(R.id.bar_power_remain);

        String update;
        Battery droneBattery;
        final int batteryIcon;
        if (!drone.isConnected() || ((droneBattery = drone.getAttribute(AttributeType.BATTERY)) == null)) {
            update = emptyString;
            dischargeView.setText("D: " + emptyString);
            currentView.setText("C: " + emptyString);
            remainView.setText("R: " + emptyString);
            batteryIcon = R.drawable.ic_battery_circle_0_24dp;
        } else {
            Double discharge = droneBattery.getBatteryDischarge();
            String dischargeText;
            if (discharge == null) {
                dischargeText = "D: " + emptyString;
            } else {
                dischargeText = "D: " + electricChargeToString(discharge);
            }

            dischargeView.setText(dischargeText);

            final double battRemain = droneBattery.getBatteryRemain();
            remainView.setText(String.format(Locale.ENGLISH, "R: %2.0f %%", battRemain));
            currentView.setText(String.format("C: %2.1f A", droneBattery.getBatteryCurrent()));


            update = String.format(Locale.ENGLISH, "%2.1f V", droneBattery.getBatteryVoltage());

            if (battRemain >= 100) {
                batteryIcon = R.drawable.ic_battery_circle_8_24dp;
            } else if (battRemain >= 87.5) {
                batteryIcon = R.drawable.ic_battery_circle_7_24dp;
            } else if (battRemain >= 75) {
                batteryIcon = R.drawable.ic_battery_circle_6_24dp;
            } else if (battRemain >= 62.5) {
                batteryIcon = R.drawable.ic_battery_circle_5_24dp;
            } else if (battRemain >= 50) {
                batteryIcon = R.drawable.ic_battery_circle_4_24dp;
            } else if (battRemain >= 37.5) {
                batteryIcon = R.drawable.ic_battery_circle_3_24dp;
            } else if (battRemain >= 25) {
                batteryIcon = R.drawable.ic_battery_circle_2_24dp;
            } else if (battRemain >= 12.5) {
                batteryIcon = R.drawable.ic_battery_circle_1_24dp;
            } else {
                batteryIcon = R.drawable.ic_battery_circle_0_24dp;
            }
        }

        batteryPopup.update();
        batteryTelem.setText(update);
        batteryTelem.setCompoundDrawablesWithIntrinsicBounds(batteryIcon, 0, 0, 0);
    }

    private String electricChargeToString(double chargeInmAh) {
        double absCharge = Math.abs(chargeInmAh);
        if (absCharge >= 1000) {
            return String.format(Locale.US, "%2.1f Ah", chargeInmAh / 1000);
        } else {
            return String.format(Locale.ENGLISH, "%2.0f mAh", chargeInmAh);
        }
    }

<<<<<<< HEAD
    private void updateBatteryTelem(DroneModel mDroneModel) {
        final View batteryPopupView = batteryPopup.getContentView();
        final TextView dischargeView = (TextView) batteryPopupView.findViewById(R.id.bar_power_discharge);
        final TextView currentView = (TextView) batteryPopupView.findViewById(R.id.bar_power_current);
        final TextView mAhView = (TextView) batteryPopupView.findViewById(R.id.bar_power_mAh);

        String update;
        final int batteryIcon;
        dischargeView.setVisibility(View.GONE);
        if (mDroneModel==null) {
            update = emptyString;
            currentView.setText("C: " + emptyString);
            mAhView.setText("R: " + emptyString);
            batteryIcon = R.drawable.ic_battery_unknown_black_24dp;
        } else {
            mAhView.setText(String.format(Locale.ENGLISH, "R: %2.0f %%", mDroneModel.getBatteryRemain()));
            currentView.setText(String.format("C: %2.1f A", mDroneModel.getBatteryCurrent()));
            update = String.format(Locale.ENGLISH, "%2.1f V", mDroneModel.getBatteryVoltage());
            batteryIcon = R.drawable.ic_battery_std_black_24dp;
        }

        batteryPopup.update();
        batteryTelem.setText(update);
        batteryTelem.setCompoundDrawablesWithIntrinsicBounds(batteryIcon, 0, 0, 0);
    }




    private void updateSignalTelem(SignalModel signal) {

        final View popupView = signalPopup.getContentView();
        TextView rssiView = (TextView) popupView.findViewById(R.id.bar_signal_rssi);
        TextView remRssiView = (TextView) popupView.findViewById(R.id.bar_signal_remrssi);
        TextView noiseView = (TextView) popupView.findViewById(R.id.bar_signal_noise);
        TextView remNoiseView = (TextView) popupView.findViewById(R.id.bar_signal_remnoise);
        TextView fadeView = (TextView) popupView.findViewById(R.id.bar_signal_fade);
        TextView remFadeView = (TextView) popupView.findViewById(R.id.bar_signal_remfade);

        if(signal==null){
            signalTelem.setText(emptyString);
            signalTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_signal_wifi_statusbar_null_black_24dp,
                    0, 0, 0);

            rssiView.setText("RSSI: " + emptyString);
            remRssiView.setText("RemRSSI: " + emptyString);
            noiseView.setText("Noise: " + emptyString);
            remNoiseView.setText("RemNoise: " + emptyString);
            fadeView.setText("Fade: "  + emptyString);
            remFadeView.setText("RemFade: " + emptyString);
        }
        else{
            final int signalStrength = MathUtils.getSignalStrength(signal.getFadeMargin(),
                    signal.getRemFadeMargin());
            final int signalIcon;
            if (signalStrength >= 100)
                signalIcon = R.drawable.ic_signal_wifi_4_bar_black_24dp;
            else if (signalStrength >= 75)
                signalIcon = R.drawable.ic_signal_wifi_3_bar_black_24dp;
            else if (signalStrength >= 50)
                signalIcon = R.drawable.ic_signal_wifi_2_bar_black_24dp;
            else if (signalStrength >= 25)
                signalIcon = R.drawable.ic_signal_wifi_1_bar_black_24dp;
            else
                signalIcon = R.drawable.ic_signal_wifi_0_bar_black_24dp;

            signalTelem.setText(String.format(Locale.ENGLISH, "%d%%", signalStrength));
            signalTelem.setCompoundDrawablesWithIntrinsicBounds(signalIcon, 0, 0, 0);

            rssiView.setText(String.format("RSSI %2.0f dB", signal.getRssi()));
            remRssiView.setText(String.format("RemRSSI %2.0f dB", signal.getRemrssi()));
            noiseView.setText(String.format("Noise %2.0f dB", signal.getNoise()));
            remNoiseView.setText(String.format("RemNoise %2.0f dB", signal.getRemnoise()));
            fadeView.setText(String.format("Fade %2.0f dB", signal.getFadeMargin()));
            remFadeView.setText(String.format("RemFade %2.0f dB", signal.getRemFadeMargin()));
        }

        signalPopup.update();
    }


    private void updateGpsTelem(GpsModel mGpsModel) {
        final boolean displayHdop = appPrefs.shouldGpsHdopBeDisplayed();

        final View popupView = gpsPopup.getContentView();
        TextView satNoView = (TextView) popupView.findViewById(R.id.bar_gps_satno);
        TextView hdopStatusView = (TextView) popupView.findViewById(R.id.bar_gps_hdop_status);
        hdopStatusView.setVisibility(displayHdop ? View.GONE : View.VISIBLE);

        final String update;
        final int gpsIcon;
        if (mGpsModel==null) {
            update = (displayHdop ? "HDOP: " : "") + emptyString;
            gpsIcon = R.drawable.ic_gps_off_black_24dp;
            satNoView.setText("S: " + emptyString);
            hdopStatusView.setText("HDOP: " + emptyString);
        } else {
            final String fixStatus = mGpsModel.getFixStatus();

            if (displayHdop) {
                update = String.format(Locale.ENGLISH, "HDOP: %.1f", mGpsModel.getGpsEph());
            } else {
                update = String.format(Locale.ENGLISH, "%s", fixStatus);
            }

            switch(fixStatus){
                case Gps.LOCK_3D:
//                case Gps.LOCK_3D_DGPS:
//                case Gps.LOCK_3D_RTK:
//                    gpsIcon = R.drawable.ic_gps_fixed_black_24dp;
//                    break;

                case Gps.LOCK_2D:
                case Gps.NO_FIX:
                default:
                    gpsIcon = R.drawable.ic_gps_not_fixed_black_24dp;
                    break;
            }

            satNoView.setText(String.format(Locale.ENGLISH, "S: %d", mGpsModel.getSatCount()));
            if (appPrefs.shouldGpsHdopBeDisplayed()) {
                hdopStatusView.setText(String.format(Locale.ENGLISH, "%s", fixStatus));
            } else {
                hdopStatusView.setText(String.format(Locale.ENGLISH, "HDOP: %.1f", mGpsModel.getGpsEph()));
            }
        }

        gpsTelem.setText(update);
        gpsTelem.setCompoundDrawablesWithIntrinsicBounds(gpsIcon, 0, 0, 0);
        gpsPopup.update();
    }



    private void updateFlightModeTelem(int mode) {
        mode = (int)DRONE_MODE.getInstance().getDronePositionMap().getValue(mode);
        flightModeTelem.forcedSetSelection(mode);
        flightModeTelem.setOnSpinnerItemSelectedListener(new SpinnerSelfSelect.OnSpinnerItemSelectedListener() {
            @Override
            public void onSpinnerItemSelected(Spinner spinner, int position) {
                int cur_mode = (int) DRONE_MODE.getInstance().getDronePositionMap().getKey
                        (position);
                Intent mode_intent = new Intent();
                mode_intent.setAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION);
                mode_intent.putExtra("mode", cur_mode);
                getActivity().sendBroadcast(mode_intent);
            }
        });

    }

    private void initList() {
        final List<VehicleMode> flightModes = VehicleMode.getVehicleModePerDroneType(Type.TYPE_COPTER);

        modeAdapter.clear();
        modeAdapter.addAll(flightModes);
        modeAdapter.notifyDataSetChanged();
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
=======
    private void updateAltitudeTelem() {
        final Drone drone = getDrone();
        final Altitude altitude = drone.getAttribute(AttributeType.ALTITUDE);
        if (altitude != null) {
            double alt = altitude.getAltitude();
            LengthUnit altUnit = getLengthUnitProvider().boxBaseValueToTarget(alt);

            this.altitudeTelem.setText(altUnit.toString());
        }
    }

>>>>>>> DroidPlanner/develop
}
