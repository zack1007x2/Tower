package org.droidplanner.android.activities;

import android.annotation.SuppressLint;
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
import android.widget.Toast;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_set_mode;

import org.droidplanner.android.R;
import org.droidplanner.android.data.DroneModel;
import org.droidplanner.android.fragments.XmppControlFragment;
import org.droidplanner.android.utils.prefs.DRONE_MODE;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

import moremote.moapp.IPCam;
import moremote.moapp.MoApplication;
import moremote.moapp.activity.BaseActivity;
import moremote.moapp.asynctask.TCPConnectAsyncTask;
import moremote.p2p.TCPClient;
import moremote.relay.RelayClient;
import moremote.relay.RelayConnectionListener;
import moremote.surface.MyGLSurfaceView;


public class ControlActivity extends BaseActivity {

    private final static String TAG = AccountActivity.class.getSimpleName();


    private String connectTo;
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



    /*----------------------------------------decoder -------------------------------------------------*/
    static{
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
        DoAfterSuccess();
    }

    private void initView() {
        connectTo = getResources().getString(R.string.connect_to);

        mSpinnerMode = (Spinner)findViewById(R.id.spinnerMode);
        bt_streaming = (Button)findViewById(R.id.bt_streaming);
        String[] modes = {"Stabilize", "Acro", "Alt Hold", "Auto","Guided","Loiter","RTL",
                "Circle","Land","Drift","Sport","Flip","Autotune", "PosHold", "Brake"};
        ArrayAdapter<String> ModeItem = new ArrayAdapter<String>(this, android.R.layout
                .simple_spinner_item, modes);
        ModeItem.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerMode.setAdapter(ModeItem);

        mSpinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cur_mode = (int) DRONE_MODE.getDronePositionMap().getKey(position);
                String Msg = MsgTitle + msg_set_mode
                        .MAVLINK_MSG_ID_SET_MODE+"@"+ cur_mode;
                xmppConnection.sendMessage(connectTo, Msg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
        surfaceView = (MyGLSurfaceView)findViewById(R.id.GLSurfaceView);
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




    private void DoAfterSuccess(){
        createChat();
        DecoderInitial();

        mainCam = new IPCam(connectTo, surfaceView, xmppConnection, ipcamCount++);
        mainCam.setPlayButton(bt_streaming);
        mainCam.setToastHandler(toastHandler);
        connectedMap.put(connectTo, mainCam);
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

    private Handler toastHandler = new Handler(){

        public void handleMessage(android.os.Message msg){
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
        for(Object key : connectedMap.keySet()){

            IPCam ipcam = (IPCam)connectedMap.get(key);
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
            if(jid.contains("/")) {
                jid = jid.substring(0, jid.lastIndexOf("/"));
            }
            IPCam ipcam = (IPCam)connectedMap.get(jid);
            startTCPRelay(ipcam, uuid, auth);
        }
        else if (content.contains(MoApplication.XMPPCommand.TCP_CONNECTION)) {
            String jid = chat.getParticipant();
            if(jid.contains("/")) {
                jid = jid.substring(0, jid.lastIndexOf("/"));
            }
            IPCam ipcam = (IPCam)connectedMap.get(jid);
            int dashPosition = content.indexOf(":");
            ipcam.destinationPort = Integer.valueOf(content.substring(dashPosition + 1, content.length()));
            startP2POverTCP(ipcam);
        }
        else if(content.contains(MoApplication.XMPPCommand.ALARM_BABY)){
            Log.e("Ray", "baby alarm!!!");
            int dashPosition = content.indexOf(":");
            int Rnumber = Integer.valueOf(content.substring(dashPosition + 1, content.length()));
            android.os.Message msg = new android.os.Message();
            msg.what = Rnumber;
            alarmMsgHandler.sendMessage(msg);
        }
        else if (content.contains(MoApplication.XMPPCommand.ALARM_SOUND)){
            int dashPosition = content.indexOf(":");
            String soundCase = content.substring(dashPosition + 1, content.length());
            android.os.Message msg = new android.os.Message();
            msg.what = 0;
            alarmMsgHandler.sendMessage(msg);
        }
        else if(content.contains(MoApplication.XMPPCommand.CAMERA_TYPE)){
//            Log.e("Ray","@#getCameraType in MainActivity");
//            int cameraType = Integer.valueOf(content.split(":")[1]);
//            this.cameraType = cameraType;
        }else if(content.contains(MoApplication.XMPPCommand.DRONE)){
//            Toast.makeText(ControlActivity.this, "Receive Msg => "+content,Toast.LENGTH_SHORT).show();
            content = content.substring(17);
            if(content.contains("[")) {
                String[] byteValues = content.substring(1, content.length() - 1).split(",");
                byte[] bytes = new byte[byteValues.length];

                for (int i = 0, len = bytes.length; i < len; i++) {
                    bytes[i] = Byte.parseByte(byteValues[i].trim());
                }
                Parser drone_parser = new Parser();
                ByteBuffer tmpBtyrBuffer = ByteBuffer.allocate(bytes.length);
                tmpBtyrBuffer.put(bytes);
                for (int i = 0; i < tmpBtyrBuffer.limit(); i++) {
                    MAVLinkPacket pkt = drone_parser.mavlink_parse_char(tmpBtyrBuffer.get(i) &
                            0x00ff);
                    if(pkt!=null){
                        handlePacket(pkt);

                    }

                }
            }
        }
    }

    private void handlePacket(MAVLinkPacket pkt) {
        int msgId = pkt.unpack().msgid;
        DroneModel mDroneModel = DroneModel.getDroneModel();
        switch(msgId){

            case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                msg_attitude msg = (msg_attitude) pkt.unpack();
                mDroneModel.setRoll(msg.roll);
            case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                msg_heartbeat msgMode = (msg_heartbeat) pkt.unpack();
                mSpinnerMode.setSelection((Integer) DRONE_MODE.getDronePositionMap().getValue(msgMode
                        .custom_mode));
                break;

        }

    }

    private Handler alarmMsgHandler = new Handler(){
        public void handleMessage(android.os.Message msg){
            super.handleMessage(msg);
            int Rnumber = msg.what;
            if(Rnumber !=0) {
                String alarm = "Alarm: Baby appears at " + Rnumber + "!!!";
                Toast.makeText(ControlActivity.this, alarm, Toast.LENGTH_SHORT).show();
            }
            else {
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
            public void authed(String auth) {}
        });
        relayClient.setMessageListener(ipcam.getTcpMessageListener());
        ipcam.setRelayClient(relayClient);
        if(ipcam.jid == mainCam.jid){
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
        if(ipcam.jid == mainCam.jid){
            this.tcpClient = tcpClient;
        }
        TCPConnectAsyncTask connectionTask = new TCPConnectAsyncTask(this, tcpClient, xmppConnection, ipcam.jid);
        connectionTask.execute();
    }
//
//    @Override
//    protected void updateFriendStatus(final Presence presence) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                String name = presence.getFrom().split("/")[0];
////                String name = presence.getFrom();
//                Log.d(TAG, "Presence message :" + presence.toString() + "From" + presence.getFrom());
//                if (name.equals(connectTo) && !presence.isAvailable() && !ControlActivity.this
//                        .isFinishing()) {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(ControlActivity.this);
//                    builder.setMessage("cam_disconnection")
//                            .setPositiveButton("submit", new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int id) {
//                                    dialog.dismiss();
//                                    finish();
//                                }
//                            });
//
//                    // Create the AlertDialog object and display
//                    dialog = builder.create();
//                    dialog.show();
//                }
//            }
//        });
//    }

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

    public static ByteBuffer str_to_bb(String msg){
        Charset charset = Charset.forName("UTF-8");
        CharsetEncoder encoder = charset.newEncoder();
        try{
            return encoder.encode(CharBuffer.wrap(msg));
        }catch(Exception e){e.printStackTrace();}
        return null;
    }

}
