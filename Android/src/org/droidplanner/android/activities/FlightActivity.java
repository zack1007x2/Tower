package org.droidplanner.android.activities;

<<<<<<< HEAD
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
=======
>>>>>>> DroidPlanner/develop
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;
<<<<<<< HEAD
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.ardupilotmega.msg_radio;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_gps_raw_int;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_mission_item;
import com.MAVLink.common.msg_set_mode;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.common.msg_vfr_hud;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.error.ErrorType;
import com.o3dr.services.android.lib.util.MathUtils;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.droidplanner.android.R;
import org.droidplanner.android.data.UserPerference;
import org.droidplanner.android.fragments.DroneMap;
import org.droidplanner.android.fragments.FlightMapFragment;
import org.droidplanner.android.fragments.TelemetryFragment;
import org.droidplanner.android.fragments.control.FlightControlManagerFragment;
import org.droidplanner.android.fragments.mode.FlightModePanel;
import org.droidplanner.android.utils.Utils;
import org.droidplanner.android.utils.collection.BroadCastIntent;
import org.droidplanner.android.utils.prefs.AutoPanMode;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import moremote.moapp.MoApplication;
import moremote.moapp.activity.BaseActivity;
import moremote.moapp.wrap.UserStatus;

public class FlightActivity extends BaseActivity implements ConnectionListener{

    private static final String TAG = FlightActivity.class.getSimpleName();
    private static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 101;
=======

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.droidplanner.android.R;
import org.droidplanner.android.fragments.FlightDataFragment;
import org.droidplanner.android.fragments.WidgetsListFragment;
import org.droidplanner.android.fragments.actionbar.ActionBarTelemFragment;
import org.droidplanner.android.view.SlidingDrawer;

public class FlightActivity extends DrawerNavigationUI implements SlidingUpPanelLayout.PanelSlideListener {
>>>>>>> DroidPlanner/develop

    private static final String EXTRA_IS_ACTION_DRAWER_OPENED = "extra_is_action_drawer_opened";
    private static final boolean DEFAULT_IS_ACTION_DRAWER_OPENED = true;

<<<<<<< HEAD
    /**
     * Determines how long the failsafe view is visible for.
     */
    private static final long WARNING_VIEW_DISPLAY_TIMEOUT = 10000l; //ms

    private static final IntentFilter eventFilter = new IntentFilter();

    private String username;
    private String password;
    private SharedPreferences settings;
    private static final String JID_FIELD = "jid";
    private String resource;

    private MessageListener xmppMessageListener;
    private int curMode;

    private HashMap<String, UserStatus> friends;

    private boolean remote_status, receive_heartbeat;


    static {
        eventFilter.addAction(AttributeEvent.AUTOPILOT_ERROR);
        eventFilter.addAction(AttributeEvent.STATE_ARMING);
        eventFilter.addAction(AttributeEvent.STATE_CONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_DISCONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_UPDATED);
        eventFilter.addAction(AttributeEvent.FOLLOW_START);
        eventFilter.addAction(AttributeEvent.MISSION_DRONIE_CREATED);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_ATTITUDE);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION);
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_ARM_STATE_CHANGE);
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case AttributeEvent.AUTOPILOT_ERROR:
                    String errorName = intent.getStringExtra(AttributeEventExtra.EXTRA_AUTOPILOT_ERROR_ID);
                    final ErrorType errorType = ErrorType.getErrorById(errorName);
                    onAutopilotError(errorType);
                    break;

                case AttributeEvent.STATE_ARMING:
                case AttributeEvent.STATE_CONNECTED:
                case AttributeEvent.STATE_DISCONNECTED:
                case AttributeEvent.STATE_UPDATED:
                    enableSlidingUpPanel(dpApp.getDrone());
                    break;

                case AttributeEvent.FOLLOW_START:
                    //Extend the sliding drawer if collapsed.
                    if (!mSlidingPanelCollapsing.get() && mSlidingPanel.isSlidingEnabled() &&
                            !mSlidingPanel.isPanelExpanded()) {
                        mSlidingPanel.expandPanel();
                    }
                    break;

                case AttributeEvent.MISSION_DRONIE_CREATED:
                    float dronieBearing = intent.getFloatExtra(AttributeEventExtra.EXTRA_MISSION_DRONIE_BEARING, -1);
                    if (dronieBearing != -1)
                        updateMapBearing(dronieBearing);
                    break;

                case BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION:

                    curMode = intent.getIntExtra("mode",0);
                    Log.d("Zack","Flight Receive Intent MODE = "+curMode);
                    String Msg = MoApplication.XMPPCommand.DRONE + msg_set_mode
                            .MAVLINK_MSG_ID_SET_MODE + "@" + curMode;
                    xmppConnection.sendMessage(MoApplication.CONNECT_TO, Msg);
                    break;
                case BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE:

                    break;
                case BroadCastIntent.PROPERTY_DRONE_ARM_STATE_CHANGE:

                    break;
            }
        }
    };

    private final AtomicBoolean mSlidingPanelCollapsing = new AtomicBoolean(false);

    private final SlidingUpPanelLayout.PanelSlideListener mDisablePanelSliding = new
            SlidingUpPanelLayout.PanelSlideListener() {
                @Override
                public void onPanelSlide(View view, float v) {
                }

                @Override
                public void onPanelCollapsed(View view) {
                    mSlidingPanel.setSlidingEnabled(false);
                    mSlidingPanel.setPanelHeight(mFlightActionsView.getHeight());
                    mSlidingPanelCollapsing.set(false);

                    //Remove the panel slide listener
                    mSlidingPanel.setPanelSlideListener(null);
                }

                @Override
                public void onPanelExpanded(View view) {
                }

                @Override
                public void onPanelAnchored(View view) {
                }

                @Override
                public void onPanelHidden(View view) {
                }
            };

    private final Runnable hideWarningView = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(this);

            if (warningView != null && warningView.getVisibility() != View.GONE)
                warningView.setVisibility(View.GONE);
        }
    };

    private final Handler handler = new Handler();

    private FragmentManager fragmentManager;

    private TextView warningView;

    private FlightMapFragment mapFragment;
    private FlightControlManagerFragment flightActions;
    private TelemetryFragment telemetryFragment;

    private SlidingUpPanelLayout mSlidingPanel;
    private View mFlightActionsView;

    private View mLocationButtonsContainer;
    private ImageButton mGoToMyLocation;
    private ImageButton mGoToDroneLocation;
    private ImageButton actionDrawerToggle;
=======
    private FlightDataFragment flightData;
>>>>>>> DroidPlanner/develop

    @Override
    public void onDrawerClosed() {
        super.onDrawerClosed();

<<<<<<< HEAD
        if (actionDrawerToggle != null)
            actionDrawerToggle.setActivated(false);

        if (telemetryFragment == null)
            return;
        final View telemetryView = telemetryFragment.getView();
        if (telemetryView != null) {
            final int slidingDrawerWidth = telemetryView.getWidth();
            final boolean isSlidingDrawerOpened = isActionDrawerOpened();
            updateLocationButtonsMargin(isSlidingDrawerOpened, slidingDrawerWidth);
        }
=======
        if (flightData != null)
            flightData.onDrawerClosed();
>>>>>>> DroidPlanner/develop
    }

    @Override
    public void onDrawerOpened() {
        super.onDrawerOpened();

<<<<<<< HEAD
        if (actionDrawerToggle != null)
            actionDrawerToggle.setActivated(true);

        if (telemetryFragment == null)
            return;

        final View telemetryView = telemetryFragment.getView();
        if (telemetryView != null) {
            final int slidingDrawerWidth = telemetryView.getWidth();
            final boolean isSlidingDrawerOpened = isActionDrawerOpened();
            updateLocationButtonsMargin(isSlidingDrawerOpened, slidingDrawerWidth);
        }
=======
        if (flightData != null)
            flightData.onDrawerOpened();
>>>>>>> DroidPlanner/develop
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight);

<<<<<<< HEAD

        friends = new HashMap<String, UserStatus>();

        fragmentManager = getSupportFragmentManager();

        mSlidingPanel = (SlidingUpPanelLayout) findViewById(R.id.slidingPanelContainer);
        warningView = (TextView) findViewById(R.id.failsafeTextView);

        setupMapFragment();

        mLocationButtonsContainer = findViewById(R.id.location_button_container);
        mGoToMyLocation = (ImageButton) findViewById(R.id.my_location_button);
        mGoToDroneLocation = (ImageButton) findViewById(R.id.drone_location_button);
        actionDrawerToggle = (ImageButton) findViewById(R.id.toggle_action_drawer);
        actionDrawerToggle.setVisibility(View.VISIBLE);

        actionDrawerToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isActionDrawerOpened())
                    closeActionDrawer();
                else
                    openActionDrawer();
            }
        });

        mGoToMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToMyLocation();
                    updateMapLocationButtons(AutoPanMode.DISABLED);
                }
            }
        });
        mGoToMyLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToMyLocation();
                    updateMapLocationButtons(AutoPanMode.USER);
                    return true;
                }
                return false;
            }
        });

        mGoToDroneLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToDroneLocation();
                    updateMapLocationButtons(AutoPanMode.DISABLED);
                }
            }
        });
        mGoToDroneLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToDroneLocation();
                    updateMapLocationButtons(AutoPanMode.DRONE);
                    return true;
                }
                return false;
            }
        });

        flightActions = (FlightControlManagerFragment) fragmentManager.findFragmentById(R.id.flightActionsFragment);
        if (flightActions == null) {
            flightActions = new FlightControlManagerFragment();
            fragmentManager.beginTransaction().add(R.id.flightActionsFragment, flightActions).commit();
        }
=======
        final FragmentManager fm = getSupportFragmentManager();
>>>>>>> DroidPlanner/develop

        //Add the flight data fragment
        flightData = (FlightDataFragment) fm.findFragmentById(R.id.flight_data_container);
        if(flightData == null){
            Bundle args = new Bundle();
            args.putBoolean(FlightDataFragment.EXTRA_SHOW_ACTION_DRAWER_TOGGLE, true);

            flightData = new FlightDataFragment();
            flightData.setArguments(args);
            fm.beginTransaction().add(R.id.flight_data_container, flightData).commit();
        }

        // Add the telemetry fragment
        final int actionDrawerId = getActionDrawerId();
        WidgetsListFragment widgetsListFragment = (WidgetsListFragment) fm.findFragmentById(actionDrawerId);
        if (widgetsListFragment == null) {
            widgetsListFragment = new WidgetsListFragment();
            fm.beginTransaction()
                    .add(actionDrawerId, widgetsListFragment)
                    .commit();
        }

        boolean isActionDrawerOpened = DEFAULT_IS_ACTION_DRAWER_OPENED;
        if (savedInstanceState != null) {
            isActionDrawerOpened = savedInstanceState.getBoolean(EXTRA_IS_ACTION_DRAWER_OPENED, isActionDrawerOpened);
        }

        if (isActionDrawerOpened)
            openActionDrawer();

        settings = getSharedPreferences(getResources().getString(R.string.app_title), 0);
        login();
    }

    @Override
    protected void onToolbarLayoutChange(int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom){
        if(flightData != null)
            flightData.updateActionbarShadow(bottom);
    }

    @Override
    protected void addToolbarFragment() {
        final int toolbarId = getToolbarId();
        final FragmentManager fm = getSupportFragmentManager();
        Fragment actionBarTelem = fm.findFragmentById(toolbarId);
        if (actionBarTelem == null) {
            actionBarTelem = new ActionBarTelemFragment();
            fm.beginTransaction().add(toolbarId, actionBarTelem).commit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_IS_ACTION_DRAWER_OPENED, isActionDrawerOpened());
    }

    @Override
    protected int getToolbarId() {
        return R.id.actionbar_toolbar;
    }

    @Override
    protected int getNavigationDrawerMenuItemId() {
        return R.id.navigation_flight_data;
    }

    @Override
    protected boolean enableMissionMenus() {
        return true;
    }

    @Override
    public void onPanelSlide(View view, float v) {
        final int bottomMargin = (int) getResources().getDimension(R.dimen.action_drawer_margin_bottom);

<<<<<<< HEAD
    public void setGuidedClickListener(FlightMapFragment.OnGuidedClickListener listener) {
        mapFragment.setGuidedClickListener(listener);
    }

    public void addMapMarkerProvider(DroneMap.MapMarkerProvider provider) {
        mapFragment.addMapMarkerProvider(provider);
    }

    public void removeMapMarkerProvider(DroneMap.MapMarkerProvider provider) {
        mapFragment.removeMapMarkerProvider(provider);
=======
        //Update the bottom margin for the action drawer
        final View flightActionBar = ((ViewGroup)view).getChildAt(0);
        final int[] viewLocs = new int[2];
        flightActionBar.getLocationInWindow(viewLocs);
        updateActionDrawerBottomMargin(viewLocs[0] + flightActionBar.getWidth(), Math.max((int) (view.getHeight() * v), bottomMargin));
    }

    @Override
    public void onPanelCollapsed(View view) {
        final int bottomMargin = (int) getResources().getDimension(R.dimen.action_drawer_margin_bottom);

        //Reset the bottom margin for the action drawer
        final View flightActionBar = ((ViewGroup)view).getChildAt(0);
        final int[] viewLocs = new int[2];
        flightActionBar.getLocationInWindow(viewLocs);
        updateActionDrawerBottomMargin(viewLocs[0] + flightActionBar.getWidth(), bottomMargin);
>>>>>>> DroidPlanner/develop
    }

    @Override
    public void onPanelExpanded(View view) {
        //Update the bottom margin for the action drawer
        final View flightActionBar = ((ViewGroup)view).getChildAt(0);
        final int[] viewLocs = new int[2];
        flightActionBar.getLocationInWindow(viewLocs);
        updateActionDrawerBottomMargin(viewLocs[0] + flightActionBar.getWidth(), view.getHeight());
    }

    @Override
    public void onPanelAnchored(View view) {

    }

    @Override
    public void onPanelHidden(View view) {
        final int bottomMargin = (int) getResources().getDimension(R.dimen.action_drawer_margin_bottom);

        final View flightActionBar = ((ViewGroup)view).getChildAt(0);
        final int[] viewLocs = new int[2];
        flightActionBar.getLocationInWindow(viewLocs);
        updateActionDrawerBottomMargin(viewLocs[0] + flightActionBar.getWidth(), bottomMargin);
    }

<<<<<<< HEAD
    private void onAutopilotError(ErrorType errorType) {
        if (errorType == null)
            return;

        final CharSequence errorLabel;
        switch (errorType) {
            case NO_ERROR:
                errorLabel = null;
                break;

            default:
                errorLabel = errorType.getLabel(getApplicationContext());
                break;
        }

        if (!TextUtils.isEmpty(errorLabel)) {
            handler.removeCallbacks(hideWarningView);
=======
    private void updateActionDrawerBottomMargin(int rightEdge, int bottomMargin){
        final ViewGroup actionDrawerParent = (ViewGroup) getActionDrawer();
        final View actionDrawer = ((ViewGroup)actionDrawerParent.getChildAt(1)).getChildAt(0);

        final int[] actionDrawerLocs = new int[2];
        actionDrawer.getLocationInWindow(actionDrawerLocs);
>>>>>>> DroidPlanner/develop

        if(actionDrawerLocs[0] <= rightEdge) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) actionDrawerParent.getLayoutParams();
            lp.bottomMargin = bottomMargin;
            actionDrawerParent.requestLayout();
        }
    }

    private void login() {
        username = MoApplication.USER_ID;
        password = MoApplication.USER_PWD;

        settings.edit().putString(JID_FIELD, username).commit();

        // login to xmpp server
        resource = getMacAddress(this) + "AppName";
        String xmppDomain = "xmpp01.moremote.com";
        // xmppDomain set null if want use account domain
        xmppConnection.loginToXMPPServer(username, password, xmppDomain, resource);
    }

    @Override
    protected void xmppConnectionClosedOnError() {
//        super.showConnectionCloseMsg(this);
    }

    @Override
    protected void xmppAuthenticated() {
        createChat();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FlightActivity.this, "XMPP LOGIN SUCCESS",
                        Toast.LENGTH_SHORT).show();
            }
        });
        initFriendMap();
    }

    private void initFriendMap() {
        xmppConnection.getRoster(friends);

        UserStatus item = friends.get(MoApplication.CONNECT_TO);

        if(item!=null){
            if(item.getType().equals("available")){
                remote_status = true;
                onDroneConnectionUpdate();
            }
        }
    }

    public static String getMacAddress(Context context) {
        WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        return wifiInf.getMacAddress();
    }

    private void createChat() {
        xmppMessageListener = new MessageListener() {
            @Override
            public void processMessage(Chat chat, final Message message) {
                receiveXMPPMessage(chat, message);
            }
        };
        xmppConnection.addMessageListener(xmppMessageListener);
    }


    private void receiveXMPPMessage(Chat chat, final Message message) {
        String content = message.getBody();
//        Log.e("Ray", "receive XmppMessage from " + chat.getParticipant());
        if (content == null) {
            return;
        }

        if (content.contains(MoApplication.XMPPCommand.RELAY) && content.length() < 64) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(FlightActivity.this, "auth or secret error", Toast.LENGTH_LONG).show();
//                    ((Button) findViewById(R.id.streaming)).setEnabled(true);
                }
            });
            return;
        } else if (content.contains(MoApplication.XMPPCommand.DRONE)) {
            if(fragmentManager.findFragmentById(R.id.flightActionsFragment)!=null){
                if(!fragmentManager.findFragmentById(R.id.flightActionsFragment).isHidden()){
                    fragmentManager.beginTransaction().hide(flightActions).commit();
                }
            }
            if(content.contains("@FROM_DRONE")){
                Parser drone_parser = new Parser();
                String[] pktArr = content.split(MoApplication.XMPPCommand.DRONE + "@FROM_DRONE");
                for (int i = 0; i < pktArr.length; i++) {
                    if (pktArr[i].contains("[")) {
//                    Log.d("Zack","pktArr[i] = "+pktArr[i]);
                        ByteBuffer pktBuffer = StringToByteBuffer(pktArr[i]);
                        for (int j = 0; j < pktBuffer.limit(); j++) {
                            MAVLinkPacket pkt = drone_parser.mavlink_parse_char(pktBuffer.get(j) &
                                    0x00ff);
                            if (pkt != null) {
                                handlePacket(pkt);
                            }
                        }
                    }
                }

            }else if(content.contains(MoApplication.droneConnectionState.Disconnected)){
                receive_heartbeat = false;
                onDroneConnectionUpdate();
            }

        }
    }

    private ByteBuffer StringToByteBuffer(String content) {
        String[] byteValues = content.substring(1, content.length() - 1).split(",");
        byte[] bytes = new byte[byteValues.length];

        for (int j = 0, len = bytes.length; j < len; j++) {
            bytes[j] = Byte.parseByte(byteValues[j].trim());
        }

        ByteBuffer tmpBtyrBuffer = ByteBuffer.allocate(bytes.length);
        tmpBtyrBuffer.put(bytes);
        return tmpBtyrBuffer;
    }

    private void handlePacket(MAVLinkPacket pkt) {
        int msgId = pkt.unpack().msgid;
        Intent i = new Intent();
        switch (msgId) {
            case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                msg_attitude msg_atti = (msg_attitude) pkt.unpack();
                mDroneModel.setRoll(Utils.fromRadToDeg(msg_atti.roll));
                mDroneModel.setRollspeed(Utils.fromRadToDeg(msg_atti.rollspeed));
                mDroneModel.setPitch(Utils.fromRadToDeg(msg_atti.pitch));
                mDroneModel.setPitchspeed(Utils.fromRadToDeg(msg_atti.pitchspeed));
                mDroneModel.setYaw(Utils.fromRadToDeg(msg_atti.yaw));
                mDroneModel.setYawspeed(Utils.fromRadToDeg(msg_atti.yawspeed));
                mDroneModel.setTime_boot_ms(msg_atti.time_boot_ms);
                i.setAction(BroadCastIntent.PROPERTY_DRONE_ATTITUDE);
                break;
            case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:
                msg_vfr_hud msg_speed = (msg_vfr_hud) pkt.unpack();
                mDroneModel.setAirSpeed(msg_speed.airspeed);
                mDroneModel.setGroundSpeed(msg_speed.groundspeed);
                mDroneModel.setVerticalSpeed(msg_speed.climb);
                mDroneModel.setAlt(msg_speed.alt);
                i.setAction(BroadCastIntent.PROPERTY_DRONE_SPEED);
                break;
            case msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS:
                msg_sys_status mSystemStatus = (msg_sys_status)pkt.unpack();
                mDroneModel.setBatteryCurrent(mSystemStatus.current_battery / 100.0);
                mDroneModel.setBatteryRemain(mSystemStatus.battery_remaining);
                mDroneModel.setBatteryVoltage(mSystemStatus.voltage_battery / 1000.0);
                i.setAction(BroadCastIntent.PROPERTY_DRONE_BETTERY);
                break;
            case msg_gps_raw_int.MAVLINK_MSG_ID_GPS_RAW_INT:
                msg_gps_raw_int mGpsRaw = (msg_gps_raw_int)pkt.unpack();
                mGpsModel.setGpsEph(mGpsRaw.eph);
                mGpsModel.setFixType(mGpsRaw.fix_type);
                mGpsModel.setSatCount(mGpsRaw.satellites_visible);
                i.setAction(BroadCastIntent.PROPERTY_DRONE_GPS);
                break;
            case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                msg_global_position_int mGpsPosition = (msg_global_position_int)pkt.unpack();
                mGpsModel.setPosition(mGpsPosition.lat / 1E7, mGpsPosition.lon / 1E7);
                i.setAction(BroadCastIntent.PROPERTY_DRONE_GPS_POSITION);
                break;
            case msg_radio.MAVLINK_MSG_ID_RADIO:
                msg_radio mRadio = (msg_radio)pkt.unpack();
                mSignalModel.setFixed(mRadio.fixed);
                mSignalModel.setNoise(mRadio.noise);
                mSignalModel.setRemnoise(mRadio.remnoise);
                mSignalModel.setRemrssi(mRadio.remrssi);
                mSignalModel.setRssi(mRadio.rssi);
                mSignalModel.setRxerrors(mRadio.rxerrors);
                mSignalModel.setTxbuf(mRadio.txbuf);
                mSignalModel.setSignalStrength(MathUtils.getSignalStrength(mSignalModel.getFadeMargin(), mSignalModel.getRemFadeMargin()));
                i.setAction(BroadCastIntent.PROPERTY_DRONE_SIGNAL);
                break;
            case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                receive_heartbeat = true;
                onDroneConnectionUpdate();
                msg_heartbeat mMode = (msg_heartbeat)pkt.unpack();
                curMode = (int)mMode.custom_mode;
                MoApplication.CUR_MODE = curMode;
                i.setAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE);
                i.putExtra("Mode",curMode);
                break;
            case msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM:
                //setHome



                break;
        }
        sendBroadcast(i);
    }

    public int getDroneMode(){
        return curMode;
    }

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        if (xmppMessageListener != null) {
            xmppConnection.removeMessageListener(xmppMessageListener);
            xmppMessageListener = null;
        }

    }

    @Override
    public void connected(XMPPConnection xmppConnection) {
        remote_status = true;
        onDroneConnectionUpdate();
    }

    @Override
    public void authenticated(XMPPConnection xmppConnection) {

    }

    @Override
    public void connectionClosed() {
        remote_status = false;
        onDroneConnectionUpdate();
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        remote_status = false;
        onDroneConnectionUpdate();
    }

    @Override
    public void reconnectingIn(int i) {

    }

    @Override
    public void reconnectionSuccessful() {
        remote_status = true;
        onDroneConnectionUpdate();
    }

    @Override
    public void reconnectionFailed(Exception e) {
//        remote_status = false;
        onDroneConnectionUpdate();
    }

    @Override
    protected void updateFriendStatus(Presence presence) {
        super.updateFriendStatus(presence);


        String friendName = presence.getFrom().split("/")[0];
        UserStatus item = friends.get(friendName);
        if (item == null) {
            item = new UserStatus();
        }
        item.setType(presence.getType().name());
        item.setStatus(presence.getStatus());
        friends.put(friendName, item);


        if(friendName.equals(MoApplication.CONNECT_TO) && presence.getType().name().equals
                (MoApplication.friendType.available)){
            remote_status = true;
            onDroneConnectionUpdate();

        }else{
            remote_status = false;
            onDroneConnectionUpdate();
        }

    }

    private void onDroneConnectionUpdate(){
        boolean isConnect = remote_status && receive_heartbeat;
        if(isConnect != UserPerference.getUserPerference(this).getIsDroneConnected()){
            UserPerference.getUserPerference(this).setIsDroneConnected(isConnect);
            if(isConnect){
                Log.d("Zack","DRONE CONNECTED!!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableControlFrag(true);
                    }
                });
                Intent i = new Intent();
                i.setAction(BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_AVALIABLE);
                sendBroadcast(i);
            }else{
                Log.d("Zack","DRONE DISCONNECTED!!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableControlFrag(false);
                    }
                });
                Intent i = new Intent();
                i.setAction(BroadCastIntent.PROPERTY_DRONE_XMPP_COPILOTE_UNAVALIABLE);
                sendBroadcast(i);
            }
        }



    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(eventReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(eventReceiver, eventFilter);
    }
}
