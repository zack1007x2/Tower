package org.droidplanner.android.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.ardupilotmega.msg_radio;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_gps_raw_int;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_set_mode;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.common.msg_vfr_hud;
import com.o3dr.services.android.lib.util.MathUtils;

import org.droidplanner.android.R;
import org.droidplanner.android.fragments.XmppControlFragment;
import org.droidplanner.android.utils.Utils;
import org.droidplanner.android.utils.collection.BroadCastIntent;
import org.droidplanner.android.widgets.joyStick.JoystickView;
import org.droidplanner.android.widgets.joyStick.LiftView;
import org.droidplanner.android.widgets.joyStick.RotateView;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;

import moremote.moapp.IPCam;
import moremote.moapp.MoApplication;
import moremote.moapp.asynctask.TCPConnectAsyncTask;
import moremote.moapp.wrap.UserStatus;
import moremote.p2p.TCPClient;
import moremote.relay.RelayClient;
import moremote.relay.RelayConnectionListener;
import moremote.surface.MyGLSurfaceView;


public class ControlActivity extends JoystickControlActivity implements ConnectionListener{

    private final static String TAG = ControlActivity.class.getSimpleName();


    private int cur_mode = -1;
    private int receive_mode = -1;
    private String MsgTitle = MoApplication.XMPPCommand.DRONE;

    private MessageListener xmppMessageListener;
    private HashMap connectedMap = new HashMap<String, IPCam>();

    private IPCam mainCam;
    private TCPClient tcpClient;
    private RelayClient relayClient;
    public MulticastSocket socket;
    public Button bt_streaming;
    private boolean remote_status;

    private HashMap<String, UserStatus> friends;


    private final static IntentFilter eventFilter = new IntentFilter();

    static {
        eventFilter.addAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION);
    }


    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                case BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE_ACTION:

                    cur_mode =intent.getIntExtra("mode",0);
                    String Msg_Mode = MsgTitle+ msg_set_mode
                            .MAVLINK_MSG_ID_SET_MODE + "@" + cur_mode;
                    xmppConnection.sendMessage(MoApplication.CONNECT_TO, Msg_Mode);
                    break;
            }
        }
    };

    /*----------------------------------------decoder -------------------------------------------------*/
    static {
        System.loadLibrary("UDTClientJni");
    }

    boolean isPlayMusic;

    private native void DecoderInitial();

    MyGLSurfaceView surfaceView;
    private int ipcamCount = 0;
    /*----------------------------------------decoder end -------------------------------------------------*/


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        if (savedInstanceState == null) {
            Fragment droneShare;
            droneShare = new XmppControlFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.droneshare_control, droneShare).commit();
        }
        initView();
        xmppInit();
    }

    private void initView() {
        friends = new HashMap<String, UserStatus>();
        bt_streaming = (Button) findViewById(R.id.bt_streaming);
        String[] modes = {"Stabilize", "Acro", "Alt Hold", "Auto", "Guided", "Loiter", "RTL",
                "Circle", "Land", "Drift", "Sport", "Flip", "Autotune", "PosHold", "Brake"};
        ArrayAdapter<String> ModeItem = new ArrayAdapter<String>(this, android.R.layout
                .simple_spinner_item, modes);
        ModeItem.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        surfaceView = (MyGLSurfaceView) findViewById(R.id.GLSurfaceView);
        surfaceView.setBackgroundColor(Color.BLACK);

        tvRL = (TextView) findViewById(R.id.tvRL);
        tvFB = (TextView) findViewById(R.id.tvFB);
        tvPower = (TextView) findViewById(R.id.tvPower);
        tvRotate = (TextView) findViewById(R.id.tvRotate);

        joystick_right = (JoystickView) findViewById(R.id.joystick_right);
        joystick_right.setOnJoystickMoveListener(mRightOnJoystickMoveListener, JoystickView.DEFAULT_LOOP_INTERVAL);
        joystick_lift = (LiftView)findViewById(R.id.joystick_lift);
        joystick_rotate = (RotateView)findViewById(R.id.joystick_rotate);
        joystick_lift.setOnLiftPowerMoveListener(mLiftPowerMoveListener, LiftView.DEFAULT_LOOP_INTERVAL);
        joystick_rotate.setOnRotateListener(mRotateListener, RotateView.DEFAULT_LOOP_INTERVAL);
        tbRC = (ToggleButton) findViewById(R.id.tbRC);
        tbRC.setOnCheckedChangeListener(mToggleChangedListener);
    }

    @Override
    protected int getToolbarId() {
        return R.id.actionbar_toolbar;
    }

    @Override
    protected int getNavigationDrawerEntryId() {
        return R.id.navigation_account;
    }


    private void xmppInit() {
        createChat();
        DecoderInitial();

        xmppConnection.getRoster(friends);

        UserStatus item = friends.get(MoApplication.CONNECT_TO);

        if(item.getType().equals("available")){
            remote_status = true;
            enableControlFrag(true);
        }

        mainCam = new IPCam(MoApplication.CONNECT_TO, surfaceView, xmppConnection, ipcamCount++);
        mainCam.setPlayButton(bt_streaming);
        mainCam.setToastHandler(toastHandler);
        connectedMap.put(MoApplication.CONNECT_TO, mainCam);
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

    private Handler toastHandler = new Handler() {

        public void handleMessage(android.os.Message msg) {
            Bundle bundle = msg.getData();
            String toToast = bundle.getString("toToast");
            Toast.makeText(ControlActivity.this, toToast, Toast.LENGTH_LONG).show();
        }
    };

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");

        if (relayClient != null) {
            relayClient.close();
        }

        if (socket != null) {
            socket.close();
        }

        if (tcpClient != null) {
            tcpClient.close();
            tcpClient = null;
        }

        if (xmppMessageListener != null) {
            xmppConnection.removeMessageListener(xmppMessageListener);
            xmppMessageListener = null;
        }
        for (Object key : connectedMap.keySet()) {

            IPCam ipcam = (IPCam) connectedMap.get(key);
            ipcam.stopStreaming();
            Log.e("Ray", "destroy: " + ipcam.jid);

        }


    }

    private void receiveXMPPMessage(Chat chat, final Message message) {
        String content = message.getBody();
        Log.e("Ray", "receive XmppMessage from " + chat.getParticipant());
        if (content == null) {
            return;
        }
        if (content.contains(MoApplication.XMPPCommand.RELAY) && content.length() < 64) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ControlActivity.this, "auth or secret error", Toast.LENGTH_LONG).show();
//                    ((Button) findViewById(R.id.streaming)).setEnabled(true);
                }
            });
            return;
        }

        if (content.contains(MoApplication.XMPPCommand.RELAY)) {
            int startPosition = content.indexOf(":") + 1;
            String uuid = content.substring(startPosition, startPosition + 32);
            String auth = content.substring(startPosition + 32, content.length());

            String jid = chat.getParticipant();
            if (jid.contains("/")) {
                jid = jid.substring(0, jid.lastIndexOf("/"));
            }
            IPCam ipcam = (IPCam) connectedMap.get(jid);
            startTCPRelay(ipcam, uuid, auth);
        } else if (content.contains(MoApplication.XMPPCommand.TCP_CONNECTION)) {
            String jid = chat.getParticipant();
            if (jid.contains("/")) {
                jid = jid.substring(0, jid.lastIndexOf("/"));
            }
            IPCam ipcam = (IPCam) connectedMap.get(jid);
            int dashPosition = content.indexOf(":");
            ipcam.destinationPort = Integer.valueOf(content.substring(dashPosition + 1, content.length()));
            startP2POverTCP(ipcam);
        } else if (content.contains(MoApplication.XMPPCommand.ALARM_BABY)) {
            Log.e("Ray", "baby alarm!!!");
            int dashPosition = content.indexOf(":");
            int Rnumber = Integer.valueOf(content.substring(dashPosition + 1, content.length()));
            android.os.Message msg = new android.os.Message();
            msg.what = Rnumber;
            alarmMsgHandler.sendMessage(msg);
        } else if (content.contains(MoApplication.XMPPCommand.ALARM_SOUND)) {
            int dashPosition = content.indexOf(":");
            String soundCase = content.substring(dashPosition + 1, content.length());
            android.os.Message msg = new android.os.Message();
            msg.what = 0;
            alarmMsgHandler.sendMessage(msg);
        } else if (content.contains(MoApplication.XMPPCommand.DRONE)) {
            Parser drone_parser = new Parser();
            String[] pktArr = content.split(MoApplication.XMPPCommand.DRONE + "@FROM_DRONE");
            for (int i = 0; i < pktArr.length; i++) {
                if (pktArr[i].contains("[")) {
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
                i.setAction(BroadCastIntent.PROPERTY_DRONE_GPS);
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
                msg_heartbeat mMode = (msg_heartbeat)pkt.unpack();
                receive_mode = (int)mMode.custom_mode;
                Log.d("Zack", "receive_mode = "+receive_mode);
                i.setAction(BroadCastIntent.PROPERTY_DRONE_MODE_CHANGE);
                i.putExtra("Mode",receive_mode);
                break;
        }
        sendBroadcast(i);

    }

    private Handler alarmMsgHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            int Rnumber = msg.what;
            if (Rnumber != 0) {
                String alarm = "Alarm: Baby appears at " + Rnumber + "!!!";
                Toast.makeText(ControlActivity.this, alarm, Toast.LENGTH_SHORT).show();
            } else {
                String alarm = "Alarm: Sound is too loud!!!";
                Toast.makeText(ControlActivity.this, alarm, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void startTCPRelay(IPCam ipcam, String uuid, String auth) {
        Log.d(TAG, "tcp relay start");

        final RelayClient relayClient = new RelayClient(MoApplication.TCP_RELAY_SERVER_IP, MoApplication.TCP_RELAY_SERVER_PORT, false, uuid, auth, ipcam.getDecoderNumber());
        relayClient.setConnectionListener(new RelayConnectionListener() {
            @Override
            public void connected() {
                Log.d(TAG, "connection success");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ControlActivity.this, "connection success", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void authed(String auth) {
            }
        });
        relayClient.setMessageListener(ipcam.getTcpMessageListener());
        ipcam.setRelayClient(relayClient);
        if (ipcam.jid == mainCam.jid) {
            this.relayClient = relayClient;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                relayClient.connect();
            }
        }).start();
    }

    private void startP2POverTCP(IPCam ipcam) {
//        Log.d(TAG, "tcp connection: " + destinationIP + "," + destinationPort);

        TCPClient tcpClient = new TCPClient(ipcam.destinationIP, ipcam.destinationPort, ipcam.getDecoderNumber());
        Log.e("Ray", "connecting..! " + ipcam.jid + ", " + ipcam.destinationIP + ", " + ipcam.destinationPort);
        tcpClient.setMessageListener(ipcam.getTcpMessageListener());
        ipcam.setTcpClient(tcpClient);
        if (ipcam.jid == mainCam.jid) {
            this.tcpClient = tcpClient;
        }
        TCPConnectAsyncTask connectionTask = new TCPConnectAsyncTask(this, tcpClient, xmppConnection, ipcam.jid);
        connectionTask.execute();
    }
    @Override
    protected void updateFriendStatus(final Presence presence) {
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
            enableControlFrag(true);
        }else{
            remote_status = false;
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.startstream:

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static ByteBuffer str_to_bb(String msg) {
        Charset charset = Charset.forName("UTF-8");
        CharsetEncoder encoder = charset.newEncoder();
        try {
            return encoder.encode(CharBuffer.wrap(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(eventReceiver,eventFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(eventReceiver);
    }

    @Override
    public void connected(XMPPConnection xmppConnection) {

    }

    @Override
    public void authenticated(XMPPConnection xmppConnection) {

    }

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectionClosedOnError(Exception e) {

    }

    @Override
    public void reconnectingIn(int i) {

    }

    @Override
    public void reconnectionSuccessful() {

    }

    @Override
    public void reconnectionFailed(Exception e) {

    }
}
