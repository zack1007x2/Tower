package moremote.relay;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class RelayClientBACK {

	public static final String TAG = "RelayClient";
	public static final int BUFFER_LENGTH = 1024; 
	
	private Socket socket;
	private InputStream reader;
	private OutputStream writer;
	private Thread receiveThread;
	private boolean isStop;	
	private String serverIP;
	private int serverPort;
	private RelayMessageListener messageListener;
	private Thread receiveMessageThread;
	
	private boolean isConnection=false;
	
	public RelayClientBACK(String serverIP, int serverPort) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
	}
	
	public boolean connect() {
		try {
			socket = new Socket(serverIP, serverPort);
			reader = socket.getInputStream();
			writer = socket.getOutputStream();
			
			startReceiveMessageThread();
		} catch (UnknownHostException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		isConnection=true;
		return true;
	}
	
	public boolean isConnection() {
		return isConnection;
	}
	
	public void close() {
		try {
			isStop = true;
			messageListener = null;
			
			if (receiveThread != null) {
				receiveThread.interrupt();
				receiveThread = null;
			}
			
			if (reader != null) {
				reader.close();
				reader = null;
			}
			
			if (socket != null) {
				socket.close();
				socket = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean auth(boolean isIPCam, String uuid, String auth) {
		byte[] buffer = new byte[66];
		byte[] uuidBytes = uuid.getBytes();
		byte[] authBytes = auth.getBytes();
		
		buffer[0] = 0x31;
		buffer[1] = (byte) (isIPCam ? 0x2d : 0x31);
		for (int i = 0; i < uuidBytes.length; i++) {
			buffer[i+2] = uuidBytes[i];
		}
		for (int i = 0; i < authBytes.length; i++) {
			buffer[i+2+uuidBytes.length] = authBytes[i];
		}
		
		return send(buffer, 66);
	}
	
	public boolean send(byte[] buffer, int length) {
		try {			
			writer.write(buffer, 0, length);
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public void setMessageListener(RelayMessageListener messageListener) {
		this.messageListener = messageListener;  
	}
	
	public void startReceiveMessageThread() {
		Runnable receiver = new ReceiveMessageThread();
		receiveMessageThread = new Thread(receiver);
		receiveMessageThread.start();
	}
	
	class ReceiveMessageThread extends Thread {
		
		private byte[] lengthHeader;
		
		public ReceiveMessageThread() {
			lengthHeader = new byte[4];
		}
		
		@Override
		public void run() {
			int dataLength = 0, rtpLength = 0, tmpLength = 0;
			byte[] rtpData;
			
			try {
				while (!isStop) {	
					rtpLength = getRTPPacketLength();

//					Log.d("TCPClient", rtpLength + "");
					if (rtpLength < 0) {
						isStop = true;
						break;
					}
					
					// receive rtp data
					rtpData = new byte[rtpLength];
					dataLength = 0;
					do {
						tmpLength = reader.read(rtpData, dataLength, rtpLength - dataLength);
						if (tmpLength < 0) {
							isStop = true;
							break;
						}
						else {
							dataLength += tmpLength;
						}
					} while(dataLength != rtpLength || dataLength < 0);
					
					// message receive callback
					if (messageListener != null && !isStop) {
						messageListener.processMessage(rtpData, rtpLength);
					}
				}
			}
			catch (IOException e) {
				Log.d(TAG, "receive message error");
				isStop = true;
			}
		}
		
		private int getRTPPacketLength() throws IOException {
			int rtpLength = -1;

            do {
                lengthHeader[0] = (byte) reader.read();
                lengthHeader[1] = (byte) reader.read();

                if (lengthHeader[0] != 0x24 || lengthHeader[1] != 0x00) {
                    Log.d("Relay", Arrays.toString(lengthHeader));
                }

            } while(lengthHeader[0] != 0x24 || lengthHeader[1] != 0x00);

            lengthHeader[2] = (byte) reader.read();
            lengthHeader[3] = (byte) reader.read();
			
			rtpLength = (lengthHeader[2] & 0xFF) << 8;
			rtpLength |= lengthHeader[3] & 0xFF;
			
			return rtpLength;
		}
		
	}
	
}
