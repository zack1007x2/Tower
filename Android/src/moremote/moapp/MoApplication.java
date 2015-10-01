package moremote.moapp;

import android.app.Application;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import moremote.p2p.ITCPRelayLibrary;
import moremote.xmpp.XMPPConnector;

/**
 * Created by lintzuhsiu on 14/11/6.
 */
public class MoApplication extends Application {

	public static final String TCP_RELAY_SERVER_IP = "128.199.141.188";
	public static final int TCP_RELAY_SERVER_PORT = 9010;
	public static final String UUID = "23454567890123456789012345678911";
	public static final String AUTH = "12345000000000000000000000000000";
	
    public static final String P2PSERVER_IP = "128.199.174.175";
    public static final int P2PSERVER_PORT = 9010;
    public static final String P2PSERVER_IP2 = "128.199.141.188";
    public static final int P2PSERVER_PORT2 = 9011;
	
	public static class XMPPCommand {
		public static final String STREAM_START 	= "streamstart";
		public static final String STREAM_STOP		= "streamstop";
        public static final String PLAY_MUSIC 		= "play_music";
        public static final String PREV_SONG		= "prev_song";
        public static final String NEXT_SONG 		= "next_song";
        public static final String STOP_MUSIC 		= "stop_music";
        public static final String OPEN_LED 		= "open_led";
        public static final String CLOSE_LED		= "close_led";
        public static final String ASK_RELAY		= "askrelay";
        public static final String RELAY 			= "relay:";
        public static final String STUN 			= "stun:";
        public static final String STUN_STOP 		= "stun_stop";
        public static final String STUN_SUCCESS 	= "stun_success:";
        public static final String BRIGHTNESS_UP 	= "brightnessup";
        public static final String BRIGHTNESS_DOWN  = "brightnessdown";
        public static final String ASK_TCP			= "ask_tcp";
        public static final String TCP_CONNECTION	= "tcp_connection:";
        public static final String FINISH 			= "finish";
        public static final String AUDIO_BACK		= "audioback:";
        public static final String ALARM_BABY       = "alarm_baby:";
        public static final String ALARM_SOUND      = "alarm_sound:";
        public static final String CAMERA_TYPE      = "camera_type:";
        public static final String DRONE            = "drone:";
    }
	
	public static class ConnectingType {
        public static final int NONE = -1;
		public static final int P2P_WAN_UDT = 0;
		public static final int P2P_LAN_TCP = 1;
		public static final int RELAY = 2;
	}

    public static class cameraType{
        public static final int AndroidCam = 0;
        public static final int LinuxCam = 1;
    }
    public static enum NatType {
        NAT_ERROR(-1),
        NAT_NORMAL(0),
        NAT3_UNUSUAL(1),
        NAT4_CGN(2),
        NAT4_LINUX(3),
        NAT4_RTOS(4),
        NAT4_OTHER(5);

        private int index;

        NatType(int idx){
            this.index = idx;
        }
        public int toInt(){
            return index;
        }

        public static NatType get( int idx ){
            switch (idx){
                case -1:    return NAT_ERROR;
                case 0:     return NAT_NORMAL;
                case 1:     return NAT3_UNUSUAL;
                case 2:     return NAT4_CGN;
                case 3:     return NAT4_LINUX;
                case 4:     return NAT4_RTOS;
                case 5:     return NAT4_OTHER;
            }
            return null;
        }
        public boolean compare(int i){
            return index == i;
        }
        public boolean compare(NatType type){
            return index == type.toInt();
        }
    }
	
    private XMPPConnector xmppConnector;
    private ITCPRelayLibrary tcpRelayLib;
    
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                
                if (intf.getDisplayName().contains("usbnet")) {
                	continue;
                }
                
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    @Override
	public void onCreate() {
		super.onCreate();
		xmppConnector = new XMPPConnector(this);
//		tcpRelayLib = new ITCPRelayLibrary();
	}

	public void setXMPPConnection(XMPPConnector connection) {
        xmppConnector = connection;
    }

    public XMPPConnector getXmppConnector() {
        return xmppConnector;
    }
    
    public ITCPRelayLibrary getTcpRelayLib() {
    	return tcpRelayLib;
    }
    
}
