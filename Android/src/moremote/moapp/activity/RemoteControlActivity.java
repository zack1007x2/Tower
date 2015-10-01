package moremote.moapp.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.droidplanner.android.R;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import java.net.MulticastSocket;
import java.util.HashMap;

import moremote.moapp.IPCam;
import moremote.moapp.MoApplication;
import moremote.moapp.asynctask.TCPConnectAsyncTask;
import moremote.p2p.TCPClient;
import moremote.relay.RelayClient;
import moremote.relay.RelayConnectionListener;
import moremote.surface.MyGLSurfaceView;


/**
 * Created by lintzuhsiu on 14/11/6.
 */
public class RemoteControlActivity extends BaseActivity implements View.OnClickListener {

    public static final String TAG = "remote";
    public MulticastSocket socket;

    private Context context;
    private MessageListener xmppMessageListener;
    private TCPClient tcpClient;
    private RelayClient relayClient;
    private IPCam mainCam;

    private String friendName;
    private HashMap connectedMap = new HashMap<String, IPCam>();
    private int[] btnRef = {
//    		R.id.send_message,
            R.id.prev_song,
            R.id.music,
            R.id.next_song,
            R.id.streaming,
            R.id.audioback,
            R.id.finish,
            R.id.btn_AddIPCam
    };
    /*----------------------------------------decoder -------------------------------------------------*/
    static{
        System.loadLibrary("UDTClientJni");
    }
    boolean isPlayMusic;
    private native void DecoderInitial();

    MyGLSurfaceView surfaceView;
    private int ipcamCount = 0;
    /*----------------------------------------decoder end -------------------------------------------------*/

    private Button btnAudioBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_ctrl_activity);

        context = this;

        getExtras();
        createChat();

        for (int i = 0; i < btnRef.length; i++) {
            Button btn = (Button) findViewById(btnRef[i]);
            btn.setOnClickListener(this);
        }

        // set UI
        setTitle(friendName);


        surfaceView = (MyGLSurfaceView)findViewById(R.id.GLSurfaceView);
        surfaceView.setBackgroundColor(Color.BLACK);

        btnAudioBack = (Button) findViewById(R.id.audioback);

        DecoderInitial();

        mainCam = new IPCam(friendName, surfaceView, xmppConnection, ipcamCount++);
        mainCam.setPlayButton((Button)findViewById(R.id.streaming));
        mainCam.setToastHandler(toastHandler);
        mainCam.setAudioBackButton(btnAudioBack);
        connectedMap.put(friendName, mainCam);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onClick(View v) {
        Button btn = (Button) findViewById(v.getId());

        switch (v.getId()) {
            case R.id.music:
                if (!isPlayMusic) {
                    isPlayMusic = true;
                    btn.setText("Stop Music");
                    xmppConnection.sendMessage(friendName, MoApplication.XMPPCommand.PLAY_MUSIC);
                }
                else {
                    isPlayMusic = false;
                    btn.setText("Play Music");
                    xmppConnection.sendMessage(friendName, MoApplication.XMPPCommand.STOP_MUSIC);
                }
                break;
            case R.id.prev_song:
                if (isPlayMusic) {
                    xmppConnection.sendMessage(friendName, MoApplication.XMPPCommand.PREV_SONG);
                }
                break;

            case R.id.next_song:
                if (isPlayMusic) {
                    xmppConnection.sendMessage(friendName, MoApplication.XMPPCommand.NEXT_SONG);
                }
                break;
            case R.id.streaming:
                break;
            case R.id.audioback:
//                audioBackState(!isRecording);
                break;
            case R.id.finish:
//            	Intent intent = new Intent(this, RemoteControlActivity.class);
//            	intent.putExtra(FriendListActivity.TransportData.friend, friendName);
//            	startActivity(intent);

                xmppConnection.sendMessage(friendName, MoApplication.XMPPCommand.FINISH);
                Toast.makeText(this, "finishing..", Toast.LENGTH_SHORT).show();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "InterruptedException");
                } finally {
                    setResult(RESULT_OK);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    finish();
                }
                break;
            case R.id.btn_AddIPCam:
                Button btn_play = new Button(this);
                btn_play.setText("Start Streaming");
                MyGLSurfaceView newSurfaceView = new MyGLSurfaceView(this);
                newSurfaceView.setBackgroundColor(Color.BLACK);
                LinearLayout layout = (LinearLayout)findViewById(R.id.layout);
                newSurfaceView.setLayoutParams(new LinearLayout.LayoutParams(surfaceView.getWidth(), surfaceView.getHeight()));
                layout.addView(btn_play);
                layout.addView(newSurfaceView);
                EditText ET = (EditText)findViewById(R.id.text_AddIPCam);

                IPCam ipcam;
                ipcam = new IPCam(ET.getText().toString(), newSurfaceView, xmppConnection, ipcamCount++);
                ipcam.setPlayButton(btn_play);
                ipcam.setToastHandler(toastHandler);
                connectedMap.put(ipcam.jid, ipcam);
                break;
        }
    }
    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
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

    private void getExtras() {
        Bundle extras = getIntent().getExtras();
        friendName = extras.getString(FriendListActivity.TransportData.friend);

        Log.d("Zack", "friendName = " + friendName);
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
            Toast.makeText(RemoteControlActivity.this, toToast, Toast.LENGTH_LONG).show();
        }
    };

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
                    Toast.makeText(RemoteControlActivity.this, "auth or secret error", Toast.LENGTH_LONG).show();
                    ((Button) findViewById(R.id.streaming)).setEnabled(true);
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
        }
    }
    private Handler alarmMsgHandler = new Handler(){
        public void handleMessage(android.os.Message msg){
            super.handleMessage(msg);
            int Rnumber = msg.what;
            if(Rnumber !=0) {
                String alarm = "Alarm: Baby appears at " + Rnumber + "!!!";
                Toast.makeText(RemoteControlActivity.this, alarm, Toast.LENGTH_SHORT).show();
            }
            else {
                String alarm = "Alarm: Sound is too loud!!!";
                Toast.makeText(RemoteControlActivity.this, alarm, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(RemoteControlActivity.this, "connection success", Toast.LENGTH_SHORT).show();
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

    protected void xmppConnectionClosedOnError() {
        super.showConnectionCloseMsg(this);
    }

    @Override
    protected void updateFriendStatus(final Presence presence) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String name = presence.getFrom().split("/")[0];
//                String name = presence.getFrom();
                Log.d(TAG, "Presence message :" + presence.toString() + "From" + presence.getFrom());
                if (name.equals(friendName) && !presence.isAvailable() && !((Activity) context).isFinishing()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("cam_disconnection")
                            .setPositiveButton("submit", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    finish();
                                }
                            });

                    // Create the AlertDialog object and display
                    dialog = builder.create();
                    dialog.show();
                }
            }
        });
    }

}