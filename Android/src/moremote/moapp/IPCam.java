package moremote.moapp;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import moremote.audioback.AudioBack;
import moremote.audioback.AudioBackInterface;
import moremote.p2p.StunAsyncTask;
import moremote.p2p.TCPClient;
import moremote.p2p.TCPMessageListener;
import moremote.relay.RelayClient;
import moremote.surface.MyGLSurfaceView;
import moremote.xmpp.XMPPConnector;

import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Ray on 2015/5/18.
 */
public class IPCam {

    static{
        System.loadLibrary("UDTClientJni");
    }
    private String TAG= "ipcam";
    public String jid;
    public int connectionType=-1;
    public String destinationIP;
    public int destinationPort;
    public MulticastSocket socket;
    private int localPort;
    private int UDTClientFD = -1;

    boolean isInitial=false, isPlaying=false, isReceiveData = false;
    Thread surfaceViewInit, Audio;
    private native int getRtpData(byte[] data, int length, int decodeNumber);
    private native void getVideoFromC(int decodeNumber, ByteBuffer Y, ByteBuffer U, ByteBuffer V);
    private native int signDecoder(int decoderNumber);
    private native int getVideoWidth(int decodeNumber);
    private native int getVideoHeight(int decodeNumber);
    private native void getAudioFromC(ByteBuffer audioBuffer, int decoderNumber);
    private native int getAudioSize();
    private native void startupUDT();
    private native void cleanupUDT();
    private native void closeUDTSocket(int UDTFD);
    private native void initUDTPacket(int UDTSize, int decoderNumber);
    private native int createUDTClient(int port, String serverip, String serverport);
    private native int receiveUDTPacket(int UDTFD, int decoderNumber);
    private native int setAudioSampleRate(int audiobackSamplerate);
    private native boolean sendByUDT(int UDTFD, byte[] buff, int buffLen);

    public int cameraType;
    public int audiobackSamplerate;
    private int audioPlayerSamplerate;
    private int UDTSize = 0;
    private int WIDTH=0;
    private int HEIGHT=0;
    ByteBuffer buffer_Y, buffer_U, buffer_V;
    ByteBuffer audioBuffer;
    private boolean firstFrame = true;
    private int decoderNumber = -1;
    private boolean videoInitial = false;
    private boolean audioInitial = false;
    AudioTrack trackplayer;

    MyGLSurfaceView surfaceView;
    private TCPClient tcpClient = null;
    private RelayClient relayClient = null;
    private StopStreamWaitAsyncTask stopStreamAsyncTask;
    private Button btn_play;
    private Button btn_audioback;
    private StunAsyncTask stunAsyncTask;
    private XMPPConnector xmppConnector;
    private Handler toastHandler;
    private Thread ThreadUDT;
    private BlockingQueue<byte[]> audioQueue = new ArrayBlockingQueue<byte[]>(100);
    private BlockingQueue<byte[]> videoQueue_Y;
    private BlockingQueue<byte[]> videoQueue_U;
    private BlockingQueue<byte[]> videoQueue_V;
    public boolean isMainCam = false;
    private AudioBack audioBack;
    private boolean isRecording=false;
    private AudioBackInterface audioBackInterface;

    public IPCam(String jid, MyGLSurfaceView surfaceView, XMPPConnector xmppConnector, int Number){
        Log.e("Ray", "new IPCam " + Number);
        decoderNumber = Number;
        if(decoderNumber == 0){
            isMainCam = true;
        }
        this.jid = jid;
        this.TAG = jid;
        this.surfaceView = surfaceView;
        this.xmppConnector = xmppConnector;
        signDecoder(decoderNumber);
        setCameraType(MoApplication.cameraType.LinuxCam);
    }

    public void setAudioBackButton(Button btn_audioback){
//        this.btn_audioback = btn_audioback;

        defineAudioBackInterface();

        btn_audioback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioBackState(!isRecording);
            }
        });
    }

    private void defineAudioBackInterface(){
        audioBackInterface = new AudioBackInterface() {
            @Override
            public boolean sendAudioBack(byte[] data, int length, int cameraType, int connectionType) {
                byte[] len = null;
                if(cameraType == MoApplication.cameraType.AndroidCam){
                    len = new byte[2];
                    int nLen = length;
                    len[0] = (byte) (nLen >> 8);
                    len[1] = (byte) (nLen & 0xFF);
                }
                switch (connectionType){

                    case MoApplication.ConnectingType.P2P_LAN_TCP:
                        if (tcpClient != null && tcpClient.isConnection()) {
                            if(cameraType == MoApplication.cameraType.AndroidCam) {
                                tcpClient.send(len, 2);
                            }
                            tcpClient.send(data, length);
                            Log.e("audioback", "TCPclient send:" + data.length);
                        }
                        return true;
//                        break;
                    case MoApplication.ConnectingType.P2P_WAN_UDT:
                        if (UDTClientFD != -1) {
                            if(cameraType == MoApplication.cameraType.AndroidCam) {
                                byte[] data2 = new byte[500];
                                // receive  by c on UDT
                                byte[] dataLen = ByteBuffer.allocate(4).putInt(length).array();

                                for (int i = 0; i < dataLen.length / 2; i++) {
                                    byte tmp = dataLen[i];
                                    dataLen[i] = dataLen[dataLen.length - i - 1];
                                    dataLen[dataLen.length - i - 1] = tmp;
                                }
                                System.arraycopy(dataLen, 0, data2, 0, dataLen.length);
                                System.arraycopy(data, 0, data2, dataLen.length, length);
                                sendByUDT(UDTClientFD, data2, data2.length);
                            }
                            else{
                                sendByUDT(UDTClientFD, data, length);
                            }
                        }
                        return true;
//                        break;
                    case MoApplication.ConnectingType.RELAY:
                        if (relayClient != null) {
                            if(cameraType == MoApplication.cameraType.AndroidCam) {
                                relayClient.send(len, 2);
                            }
                            relayClient.send(data, length);
                            Log.e(TAG, "audioback2");
                        }
                        return true;
//                        break;
                    case MoApplication.ConnectingType.NONE:
                        Log.e("audioBack", "not Connection");
                        return false;
//                        break;
                    default:
                        return false;
                }
            }
        };
    }

    private void audioBackState(Boolean recording){
        if(recording){
            audioBack = new AudioBack(this, audioBackInterface);
            audioBack.start();
//            btn_audioback.setText("StopAudioBack");
        }
        else if(audioBack != null){
            audioBack.interrupt();
            audioBack.release();
            audioBack = null;
//            btn_audioback.setText("StartAudioBack");
        }
        isRecording = recording;

    }

    public int getDecoderNumber(){
        return decoderNumber;
    }

    public void (){
        Log.e("Ray", "startStun");
        stunAsyncTask = new StunAsyncTask(xmppConnector, MoApplication.XMPPCommand.STUN, this, stunMsgHandler, MoApplication.getLocalIpAddress());
        stunAsyncTask.start();
    }


    public void startThreads(){
        if(isInitial == false) {
            isInitial=true;
            isPlaying = true;
            surfaceViewInit = new surfaceViewInit();
            surfaceViewInit.start();
        }

        Audio = new Audio();
        Audio.start();

    }
    public void setCameraType(int cameraType) {
        this.cameraType = cameraType;
        if(cameraType == MoApplication.cameraType.AndroidCam){
            audioPlayerSamplerate = 16000;
            audiobackSamplerate = 44100;
            UDTSize = 1000;
        }
        else{
            audioPlayerSamplerate = 48000;
            audiobackSamplerate = 8000;
            UDTSize = 1496;
        }
        if(isMainCam)
            setAudioSampleRate(audioPlayerSamplerate);
    }

    public void setTcpClient(TCPClient tcpClient){
        this.tcpClient = tcpClient;
    }

    public void setRelayClient(RelayClient relayClient){
        this.relayClient = relayClient;
    }

    public TCPClient getTcpClient(){
        return tcpClient;
    }

    public RelayClient getRelayClient(){
        return relayClient;
    }

    public void setToastHandler(Handler toastHandler){
        this.toastHandler = toastHandler;
    }

    private Handler stunMsgHandler = new Handler() {
        @SuppressLint("NewApi")
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);

            Bundle extras = msg.getData();
            startThreads();

            if (msg.what == StunAsyncTask.SUCCESS) {
//                connectionType= extras.getInt(StunAsyncTask.DataField.CONNECTION_TYPE);
                connectionType = extras.getInt(StunAsyncTask.DataField.CONNECTION_TYPE);
                destinationIP = extras.getString(StunAsyncTask.DataField.IP);
                destinationPort = extras.getInt(StunAsyncTask.DataField.PORT);
                socket = (MulticastSocket) msg.obj;


                if (connectionType == StunAsyncTask.NetWorkType.LAN) {
                    connectionType = MoApplication.ConnectingType.P2P_LAN_TCP;
                    Log.e("Ray", "connectionType: localTCP");
                    Log.e(TAG, "@@ stunMsgHandler " + destinationIP + ":" + destinationPort);
                    xmppConnector.sendMessage(jid, MoApplication.XMPPCommand.ASK_TCP);
                }
                else {
                    connectionType = MoApplication.ConnectingType.P2P_WAN_UDT;
                    Log.e("Ray", "connectionType: UDT");
                    toast("WAN");
                    Log.e("", "stun for streaming success!! go UDT + " + socket.getLocalPort());
                    if (socket.getLocalPort() < 0) {
                        socket.close();
                        return;
                    }
                    localPort = socket.getLocalPort();


                    xmppConnector.sendMessage(jid, MoApplication.XMPPCommand.STREAM_START);
                    stunAsyncTask.interrupt();
                    ThreadUDT = new ThreadUDT();
                    ThreadUDT.start();
                }
                socket.close();
            }
            else if (msg.what == StunAsyncTask.FAILED) {
                boolean receiveXmppMessage = extras.getBoolean("receiveXmppMessage");
                if(receiveXmppMessage) {
                    toast("Relay");
                    connectionType = MoApplication.ConnectingType.RELAY;
                    xmppConnector.sendMessage(jid, MoApplication.XMPPCommand.ASK_RELAY);
                }
                else{
                    connectionType = MoApplication.ConnectingType.NONE;
                    toast("Camera not exist: "+jid);
                }
            }

            getStunAsyncTask().interrupt();
        }
    };

    private Handler firstDataHandler = new Handler(){

        public void handleMessage(Message msg){
            btn_play.setText("Stop Streaming");
            btn_play.setEnabled(true);

//            if(isMainCam)
//                btn_audioback.setEnabled(true);
        }
    };

    private void toast(String toToast){

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("toToast",toToast);
        message.setData(bundle);
        toastHandler.sendMessage(message);
    }
    class ThreadUDT extends Thread {
        public void run(){
            startupUDT();

            UDTClientFD = createUDTClient(localPort, destinationIP, String.valueOf(destinationPort));
            Log.e("Ray", "get UDTClientFD " + UDTClientFD);
            if( UDTClientFD == -1 ) {
                Log.e(TAG, "UDT client connect fail");
            }
            else {
                Log.e(TAG, "UDT client connect success " + UDTClientFD);
                toast("Connection Success");
                initUDTPacket(UDTSize+4, decoderNumber);
                while(!isInterrupted()) {
                    Log.e("Ray", "decoderNumber: " + decoderNumber);
                    int type = receiveUDTPacket(UDTClientFD, decoderNumber);
                    if(type!=0 && !isReceiveData){
                        firstDataHandler.sendEmptyMessage(0);
                        isReceiveData = true;
                    }
                    if(type != 0) {
                        afterDecode(type);
                    }
                }
                closeUDTSocket(UDTClientFD);
                cleanupUDT();
            }
        }
    }
    public void setPlayButton(Button button){
        this.btn_play = button;
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn_play = (Button)v;
                if (!isPlaying) {
                    btn_play.setText("Stop Streaming");
                    btn_play.setEnabled(false);
                    isPlaying = true;
                    firstFrame = true;
                    startStun();
                }
                else {
                    if(isMainCam) {
                        Log.e("Ray", "audiobackStop!!");
//                        btn_audioback.setEnabled(false);
                        audioBackState(false);
//                        audioBackStopHandler.sendEmptyMessage(0);
                    }
                    stopStreaming();
                    isPlaying = false;

//                    RemoteControlActivity.removeIPCam(jid);
                }
            }
        });
    }



    public StunAsyncTask getStunAsyncTask(){
        return stunAsyncTask;
    }

    class StopStreamWaitAsyncTask extends AsyncTask {

        private Button btn_play;

        public StopStreamWaitAsyncTask(Button btn_play) {
            this.btn_play = btn_play;
            surfaceView.setBackgroundColor(Color.BLACK);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isPlaying = false;
            btn_play.setEnabled(false);
        }

        private void stopStun() {
            if (stunAsyncTask != null) {
                stunAsyncTask.interrupt();
                stunAsyncTask = null;
            }
        }

        @Override
        protected Object doInBackground(Object... params) {
//            cleanVideoQ();
            stopStun();
            xmppConnector.sendMessage(jid, MoApplication.XMPPCommand.STREAM_STOP);
            if (socket != null) {
                socket.close();
                socket = null;
            }
            if (tcpClient != null) {
                tcpClient.close();
                tcpClient = null;
            }
            if (relayClient != null) {
                relayClient.close();
                relayClient = null;
            }
            if(surfaceViewInit != null){
                surfaceViewInit.interrupt();
                surfaceViewInit = null;
            }
            if(Audio != null){
                Audio.interrupt();
                Audio = null;
            }
            if(ThreadUDT != null){
                ThreadUDT.interrupt();
                ThreadUDT = null;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.d(TAG, "StopStreamWaitAsyncTask InterruptedException");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            btn_play.setText("Start Streaming");
            btn_play.setEnabled(true);
            isReceiveData = false;
        }
    }

    public void stopStreaming(){
        audioBackState(false);
        stopStreamAsyncTask = new StopStreamWaitAsyncTask(btn_play);
        stopStreamAsyncTask.execute();
    }

    class surfaceViewInit extends Thread {
        @Override
        public void run() {
            //super.run();
            /** Wait For get video width and height , then initial surfaceView*/
            try {
                sleep(500);
                int getVideoWidth=0, getVideoHeight=0;
                while((getVideoWidth = getVideoWidth(decoderNumber)) ==0 && !isInterrupted()) {
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e("Ray", "###getvideoWidth exception");
                        currentThread().interrupt();
                    }
                }
                getVideoHeight = getVideoHeight(decoderNumber);
                Log.e("Ray", "getVideoWidth = " + getVideoWidth + ", getVideoHeight = " + getVideoHeight);

                WIDTH = getVideoWidth;
                HEIGHT = getVideoHeight;
                buffer_Y = ByteBuffer.allocateDirect(WIDTH * HEIGHT);
                buffer_U = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);
                buffer_V = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);

                surfaceView.mRenderer.update(WIDTH, HEIGHT);
                adjustSurfaceView.sendEmptyMessage(0);
                videoInitial = true;
                Log.e("Ray", "@# before while");
//                while(!isInterrupted()){
//                    if(100-videoQueue_V.remainingCapacity() > 0) {
//                        byte[] tmp, tmp2, tmp3;
//                        tmp = videoQueue_Y.poll();
//                        tmp2 = videoQueue_U.poll();
//                        tmp3 = videoQueue_V.poll();
//                        surfaceView.mRenderer.update(tmp, tmp2, tmp3);
//                    }
//                }
//                Log.e("Ray","@# end");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private Handler Show = new Handler() {
        public void handleMessage(android.os.Message msg) {
            surfaceView.setBackgroundColor(Color.TRANSPARENT);
        }
    };
    private Handler adjustSurfaceView = new Handler(){
        public void handleMessage(android.os.Message msg){
            if(WIDTH!=0){
                int ViewWidth = surfaceView.getWidth();
                int ViewHeight = surfaceView.getWidth() * HEIGHT / WIDTH;
                surfaceView.setLayoutParams(new RelativeLayout.LayoutParams(ViewWidth, ViewHeight));
                surfaceView.getWidth();
            }
        }
    };

    class Audio extends Thread {
        @Override
        public void run() {
            int bufsize = AudioTrack.getMinBufferSize(audioPlayerSamplerate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            trackplayer = new AudioTrack(AudioManager.STREAM_MUSIC, audioPlayerSamplerate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufsize,
                    AudioTrack.MODE_STREAM);
            trackplayer.play();
            Log.e(TAG, "@#@ audio start");
            int getAudioSize;
            while((getAudioSize = getAudioSize()) ==0 && !isInterrupted()) {
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "in sleep exception");
                    currentThread().interrupt();
                }
            }
            audioBuffer = ByteBuffer.allocateDirect(getAudioSize);
            audioInitial = true;
            while(!isInterrupted()) {
                if(!audioQueue.isEmpty()){
//                if(100-audioQueue.remainingCapacity() > 0){
                    byte[] tmp = audioQueue.remove();
                    trackplayer.write(tmp, 0, tmp.length);
                }
//                ret = getAudioFromQueue2(audioBuffer);
//                if (ret > 0) {
//                    trackplayer.write(audioBuffer.array(), 0, audioBuffer.limit());
//                }
            }
            Log.e(TAG, "@#@ audio end");
            trackplayer.flush();
            trackplayer.stop();
            trackplayer.release();
        }
    }

    void afterDecode(int type){
        switch (type){
            case 1:
                if(videoInitial) {
//                    if(videoQueue_Y.remainingCapacity()>1) {
//                        Log.e("Ray","@#remaining: "+videoQueue_Y.remainingCapacity());
//                        byte[] tmp = buffer_Y.array().clone();
//                        videoQueue_Y.offer(tmp);
//                        byte[] tmp2 = buffer_U.array().clone();
//                        videoQueue_U.offer(tmp2);
//                        byte[] tmp3 = buffer_V.array().clone();
//                        videoQueue_V.offer(tmp3);
//                        if (firstFrame) {
//                            Show.sendEmptyMessage(0);
//                            firstFrame = false;
//                        }
//                    }
                    getVideoFromC(decoderNumber, buffer_Y, buffer_U, buffer_V);
                    surfaceView.mRenderer.update(buffer_Y.array(), buffer_U.array(), buffer_V.array());
                    if(firstFrame){
                        Show.sendEmptyMessage(0);
                        firstFrame = false;
                    }
                }
                break;
            case 2:
                if(audioInitial && isMainCam) {
//                    Log.e("Ray","before             }\n");
                    getAudioFromC(audioBuffer, decoderNumber);
//                    Log.e("Ray","after getAudioFromC");
                    byte[] audioData = audioBuffer.array().clone();
                    audioQueue.offer(audioData);
                }
                break;
            default:
                break;
        }
    }

    public TCPMessageListener getTcpMessageListener(){
        return tcpMessageListener;
    }
    TCPMessageListener tcpMessageListener = new TCPMessageListener() {
        @Override
        public void processMessage(byte[] data, int length, int decoderNumber) {
            if(!isReceiveData){
                firstDataHandler.sendEmptyMessage(0);
                isReceiveData = true;
            }
//            Log.e(TAG, "getRtpdata:"+decoderNumber);
//            if(data[12] == 0x00)
//                Log.e("Ray","get audio Len: "+length);
            int type = getRtpData(data, length, decoderNumber);
//            Log.e(TAG,"getType: "+type);
            if(type != 0)
                afterDecode(type);
//            Log.e(TAG,"tcpMessageListener last line");
        }
    };
}
