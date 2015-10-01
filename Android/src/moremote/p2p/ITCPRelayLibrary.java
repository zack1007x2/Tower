package moremote.p2p;


public class ITCPRelayLibrary {
	
	static {
		System.loadLibrary("tcpRelay");
	}
	
	public interface OnMessageListener {
		public void receiveMessage(byte[] message);
	}
	
	private OnMessageListener messageListener;

	public native boolean connectToRelayServer(String serverIP, int serverPort);
	public native void close();
	public native boolean auth(boolean isIPCam, String uuid, String auth);
	public native boolean sendMessage(byte[] buff, int buffLen);

	public void setMessageListener(OnMessageListener messageListener) {
		this.messageListener = messageListener;
	}
	
	public void receiveMessage(byte[] message) {
		if (messageListener != null) {
			this.receiveMessage(message);
		}
	}
	
	static {
		System.loadLibrary("tcpRelay");
	}	
	
}
