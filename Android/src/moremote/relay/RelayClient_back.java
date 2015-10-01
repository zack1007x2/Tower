package moremote.relay;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class RelayClient_back {

	public static final String TAG = "Relay";
	public static final int BUFFER_LENGTH = 1024; 
	
	private SocketChannel socketChannel;
	private Selector selector;
	private boolean isStop;	
	private String serverIP;
	private int serverPort;
	private int reconnectNum;
	private RelayMessageListener messageListener;
	private RelayConnectionListener connectionListener;
	private byte[] rtpHeader;
	private String auth;
	private ByteBuffer headerBuffer;
	private ByteBuffer[] rtpBuffer;
	private byte[] authCommand;
	private Thread handleEventThread;
	
	public RelayClient_back(String serverIP, int serverPort, boolean isIPCam, String uuid, String secret) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		headerBuffer = ByteBuffer.allocate(4);
		rtpBuffer = new ByteBuffer[1024];
		rtpHeader = new byte[4];
		this.authCommand = createAuthCommand(isIPCam, uuid, secret);
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);

			selector = Selector.open();
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		} catch (IOException e) {
			Log.d(TAG, "socket channel open failed");
		}
	}
	
	public boolean connect() {
		try {
			Log.d(TAG, "connecting..");
			socketChannel.connect(new InetSocketAddress(serverIP, serverPort));
			handleEventThread = new HandleEventThread();
			handleEventThread.start();
		} catch (IOException e) {
			if (reconnectNum < 3) {
				Log.d(TAG, "reconnecting..");
				connect();
			}
			else {
				Log.d(TAG, "connect to server failed");
				return false;
			}
		}
		return true;
	}
	
	public void close() {
		try {
			isStop = true;
			messageListener = null;
			handleEventThread.interrupt();
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int send(byte[] buffer, int length) {
		try {
			Log.d(TAG, new String(buffer));
			return socketChannel.write(ByteBuffer.wrap(buffer, 0, length));
		} catch (NotYetConnectedException e) {
			Log.d(TAG, "NotYetConnectedException");
			return -1;
		} catch (ClosedChannelException e) {
			Log.d(TAG, "channel is closed");
			return -1;
		} catch (IOException e) {
			Log.d(TAG, "IOException");
//			connect();
			return -1;
		}
	}
	
	public void setMessageListener(RelayMessageListener messageListener) {
		this.messageListener = messageListener;  
	}
	
	public void setConnectionListener(RelayConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
	}
	
	private byte[] createAuthCommand(boolean isIPCam, String uuid, String secret) {
		byte[] buffer = new byte[66];
		byte[] uuidBytes = uuid.getBytes();
		byte[] authBytes = secret.getBytes();
		
		buffer[0] = 0x31;
		buffer[1] = (byte) (isIPCam ? 0x2d : 0x31);
		for (int i = 0; i < uuidBytes.length; i++) {
			buffer[i+2] = uuidBytes[i];
		}
		for (int i = 0; i < authBytes.length; i++) {
			buffer[i+2+uuidBytes.length] = authBytes[i];
		}
		
		return buffer;
	}
	
	class HandleEventThread extends Thread {
		
		@Override
		public void run() {
			while (!isStop) {
				try {
					selector.select();

					Set keys = selector.selectedKeys();
					for (Iterator iterator = keys.iterator(); iterator.hasNext(); ) {
						SelectionKey key = (SelectionKey) iterator.next();
						SocketChannel socketChannel = (SocketChannel) key.channel();
						
						if (key.isConnectable()) {
							Log.d(TAG, "Found Server");
							
							if (socketChannel.isConnectionPending()) {
								socketChannel.finishConnect();
							}
							
							socketChannel.register(selector, SelectionKey.OP_READ);
							
							Log.d(TAG, "auth");
							send(authCommand, authCommand.length);
							if (connectionListener != null) {
								connectionListener.connected();
							}
						}
						else if (key.isReadable()) {
							Log.d(TAG, "reading..");
							readRTPPacket(socketChannel.socket());
						}
						
						iterator.remove();
					}
				} catch (IOException e) {
					Log.d(TAG, "Event listener error");
				}
			}
		}
		
		private void read(SocketChannel channel) throws IOException {
//			if (channel.read(byteBuffer) > 0) {
//				byte[] authBytes = new byte[32];
//				
//				byteBuffer.flip();
//				if (byteBuffer.get(0) == 0x31 && byteBuffer.get(1) == 0x2e) {
//					byteBuffer.position(34);
//					byteBuffer.get(authBytes);
//					
//					auth = new String(authBytes, "UTF-8");
//					Log.d(TAG, "secret:" + auth);
//					
//					if (connectionListener != null) {
//						connectionListener.authed(auth);
//					}
//				}
//			}
		}
		
		private void readRTPPacket(Socket socket) throws IOException {
			int dataLength = 0, rtpLength = 0;
			byte[] lengthHeader = new byte[4];
			byte[] rtpData;
			InputStream reader = socket.getInputStream();
			
			try {
				while (!isStop) {						
					// receive rtp lenth
					if (reader.read(lengthHeader, 0, lengthHeader.length) <= 0) {
						isStop = true;
						break;
					}

					if (lengthHeader[0] == 0x24 && lengthHeader[1] == 0x00) {
						rtpLength = (short) ((lengthHeader[2] & 0xFF) << 8);
						rtpLength |= (short) (lengthHeader[3] & 0xFF);
					}
					
					// receive rtp data
					dataLength = 0;
					rtpData = new byte[rtpLength];
					do {
						dataLength += reader.read(rtpData, dataLength, rtpLength - dataLength);
					} while(dataLength != rtpLength);
					
					// message receive callback
					if (messageListener != null) {
						messageListener.processMessage(rtpData, rtpLength);
					}
				}
			}
			catch (IOException e) {
				isStop = true;
			}
			
			
//			int totalLength = channel.read(byteBuffer);
//			if (totalLength <= 0) {
//				Log.d(TAG, "read error@readRTPPacket");
//				return;
//			}
//
//			byteBuffer.position(0);
//			byteBuffer.limit(1024);
//			
//			// check the header format 	
//			for (int i = 0; i < byteBuffer.limit() - rtpHeader.length; i++) {
//				Log.d(TAG, byteBuffer.position() + "");
//				byteBuffer.get(rtpHeader);
//				byteBuffer.position(i + rtpHeader.length);
//				if (rtpHeader[0] == 0x24 && rtpHeader[1] == 0x00) {
//					break;
//				}
//			}
//			
//			// convert rtp length
//			int rtpLength = (rtpHeader[2] & 0xFF) << 8;
//			rtpLength |= (rtpHeader[3] & 0xFF);
//				
//			// check rtp packet length
//			if (rtpLength > byteBuffer.remaining()) {
//				byteBuffer.position(byteBuffer.position() - rtpHeader.length);
//				swapToHead(byteBuffer, byteBuffer.position(), byteBuffer.limit());
//				byteBuffer.position(byteBuffer.limit() - byteBuffer.position());
//				return;
//			}
//				
//			// receive message callback
//			if (messageListener != null) {
//				byte[] data = new byte[rtpLength];
//				byteBuffer.get(data);
//				messageListener.processMessage(data, rtpLength);
//			}
//			byteBuffer.position(rtpLength + rtpHeader.length);
//			
//			// keep the remaining data or clean byte buffer 
//			if (byteBuffer.remaining() > 0) {
//				swapToHead(byteBuffer, byteBuffer.position(), byteBuffer.limit());
//				byteBuffer.position(byteBuffer.limit() - byteBuffer.position());
//			}
//			else {
//				byteBuffer.clear();
//			}
		}
		
		private void swapToHead(ByteBuffer bb, int startPosition, int endPosition) {
			for (int i = startPosition; i < endPosition; i++) {
				bb.put(i - startPosition, bb.get(i));
			}
		}
		
	}
	
}
