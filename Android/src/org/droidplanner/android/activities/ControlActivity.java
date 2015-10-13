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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_set_mode;

import org.droidplanner.android.R;
import org.droidplanner.android.data.DroneModel;
import org.droidplanner.android.fragments.XmppControlFragment;
import org.droidplanner.android.utils.collection.BroadCastIntent;
import org.droidplanner.android.utils.prefs.DRONE_MODE;
import org.droidplanner.android.widgets.CusJoystickView;
import org.droidplanner.android.widgets.JoystickView;
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

    private final static String TAG = AccountActivity.class.getSimpleName();


    private Spinner mSpinnerMode;
    private int cur_mode = 0;
    private String MsgTitle = MoApplication.XMPPCommand.DRONE;

    private MessageListener xmppMessageListener;
    private HashMap connectedMap = new HashMap<String, IPCam>();

    private IPCam mainCam;
    private TCPClient tcpClient;
    private RelayClient relayClient;
    public MulticastSocket socket;
    public Button bt_streaming;
    protected TextView tvRL, tvFB, tvPower, tvRotate;
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

                    cur_mode = (int) DRONE_MODE.getDronePositionMap().getValue(intent.getIntExtra
                            ("mode",0));
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
        joystick_left = (CusJoystickView) findViewById(R.id.joystick_left);
        joystick_right = (JoystickView) findViewById(R.id.joystick_right);

        tvRL = (TextView) findViewById(R.id.tvRL);
        tvFB = (TextView) findViewById(R.id.tvFB);
        tvPower = (TextView) findViewById(R.id.tvPower);
        tvRotate = (TextView) findViewById(R.id.tvRotate);

        joystick_left.setOnJoystickMoveListener(mLeftOnJoystickMoveListener, CusJoystickView
                .DEFAULT_LOOP_INTERVAL);

        joystick_right.setOnJoystickMoveListener(mRightOnJoystickMoveListener, JoystickView.DEFAULT_LOOP_INTERVAL);

        friends = new HashMap<String, UserStatus>();


        mSpinnerMode = (Spinner) findViewById(R.id.spinnerMode);
        bt_streaming = (Button) findViewById(R.id.bt_streaming);
        String[] modes = {"Stabilize", "Acro", "Alt Hold", "Auto", "Guided", "Loiter", "RTL",
                "Circle", "Land", "Drift", "Sport", "Flip", "Autotune", "PosHold", "Brake"};
        ArrayAdapter<String> ModeItem = new ArrayAdapter<String>(this, android.R.layout
                .simple_spinner_item, modes);
        ModeItem.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerMode.setAdapter(ModeItem);

        mSpinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cur_mode = (int) DRONE_MODE.getDronePositionMap().getKey(position);
                String Msg = MsgTitle + msg_set_mode
                        .MAVLINK_MSG_ID_SET_MODE + "@" + cur_mode;
                xmppConnection.sendMessage(MoApplication.CONNECT_TO, Msg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
        surfaceView = (MyGLSurfaceView) findViewById(R.id.GLSurfaceView);
        surfaceView.setBackgroundColor(Color.BLACK);
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.droneshare_control);
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

        Log.d("Zack","UserStatus: Status = "+item.getStatus()+"    Type = "+item.getType());

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

//        if (stopStreamAsyncTask != null) {
//            stopStreamAsyncTask.cancel(true);
//            stopStreamAsyncTask = null;
//        }

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

        Log.d(TAG, content);
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
        } else if (content.contains(MoApplication.XMPPCommand.CAMERA_TYPE)) {
//            Log.e("Ray","@#getCameraType in MainActivity");
//            int cameraType = Integer.valueOf(content.split(":")[1]);
//            this.cameraType = cameraType;
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
        switch (msgId) {
            case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                msg_attitude msg = (msg_attitude) pkt.unpack();
                DroneModel mDroneModel = DroneModel.getDroneModel();
                mDroneModel.setRoll(msg.roll);
                break;
            case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                msg_heartbeat msgHB = (msg_heartbeat) pkt.unpack();
                final int cusMode = (int)msgHB.custom_mode;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSpinnerMode.setSelection((int) DRONE_MODE.getDronePositionMap().getValue(cusMode));
                    }
                });
                break;

        }

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
