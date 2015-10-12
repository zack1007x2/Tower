package moremote.relay;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import moremote.p2p.TCPMessageListener;

public class RelayClient {

    public static final String TAG = "Relay";
    public static final int BUFFER_LENGTH = 1500;

    private SocketChannel socketChannel;
    private Selector selector;
    private boolean isStop;
    private String serverIP;
    private int serverPort;
    private int reconnectNum;
//    private RelayMessageListener messageListener;
    private TCPMessageListener messageListener;
    private RelayConnectionListener connectionListener;
    private String auth;
    private ByteBuffer byteBuffer;
    private byte[] authCommand;
    private Thread handleEventThread;

    private int decoderNumber = 0;

    public RelayClient(String serverIP, int serverPort, boolean isIPCam, String uuid, String secret, int decoderNumber) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        byteBuffer = ByteBuffer.allocate(BUFFER_LENGTH);
        this.authCommand = createAuthCommand(isIPCam, uuid, secret);
        this.decoderNumber = decoderNumber;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            Log.d(TAG, "socket channel open failed");
        }
    }

    public RelayClient(String serverIP, int serverPort, boolean isIPCam, String uuid, String secret) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        byteBuffer = ByteBuffer.allocate(BUFFER_LENGTH);
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
        if (socketChannel == null) {
            return -1;
        }

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
//        this.messageListener = messageListener;
    }
    public void setMessageListener(TCPMessageListener messageListener) {
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
                            readRTPPacket(socketChannel);
                        }

                        iterator.remove();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Event listener error");
                }
            }
        }

        private void readRTPPacket(SocketChannel channel) throws IOException {
            boolean status = false;
            int totalLength = channel.read(byteBuffer);

            // Log.d(TAG, "read length:" + totalLength);
            if (totalLength < 0) {
                Log.d(TAG, "read error@readRTPPacket");
                return;
            }

            byteBuffer.flip();

            // check the header format
            while (byteBuffer.position() + 1 < byteBuffer.limit()) {
                Log.d(TAG, "get rtp streaming header: " + byteBuffer.position() + "," + byteBuffer.limit());

                if (byteBuffer.get(byteBuffer.position()) == 0x24 && byteBuffer.get(byteBuffer.position() + 1) == 0x00) {
                    status = true;
                    break;
                }

                byteBuffer.position(byteBuffer.position() + 1);
            }
            if (!status) {
                byteBuffer.put(0, byteBuffer.get(byteBuffer.position() + 1));
                byteBuffer.position(0);
                byteBuffer.limit(byteBuffer.capacity());
                return;
            }

            // convert rtp length
            int rtpLength = byteBuffer.get(byteBuffer.position() + 2) << 8;
            rtpLength |= (byteBuffer.get(byteBuffer.position() + 3) & 0xFF);
            Log.d(TAG, rtpLength + "");
            if (rtpLength < 0) {
                return;
            }
            // Log.d(TAG, "process message " + rtpLength);

            // check bytebuffer length
            if (rtpLength > byteBuffer.limit() - 4) {
                // Log.d(TAG, "check bytebuffer length");
                byteBuffer.position(byteBuffer.limit());
                byteBuffer.limit(byteBuffer.capacity());
                return;
            }
            else if (byteBuffer.position() + rtpLength > byteBuffer.limit() - 4) {
                // Log.d(TAG, "check bytebuffer length");
                swapToHead(byteBuffer, byteBuffer.position(), byteBuffer.limit());
                byteBuffer.position(0);
                return;
            }

            // receive message callback
            byteBuffer.position(byteBuffer.position() + 4);
            Log.d(TAG, rtpLength + "");
            if (messageListener != null) {
                byte[] data = new byte[rtpLength];
                byteBuffer.get(data);
                messageListener.processMessage(data, rtpLength, decoderNumber);
            }

            // move tail to head
            swapToHead(byteBuffer, byteBuffer.position(), byteBuffer.limit());
            byteBuffer.position(byteBuffer.limit() - byteBuffer.position());
            byteBuffer.limit(byteBuffer.capacity());
        }

        private void swapToHead(ByteBuffer bb, int startPosition, int endPosition) {
            for (int i = startPosition; i < endPosition; i++) {
                bb.put(i - startPosition, bb.get(i));
                bb.put(i, (byte)0);
            }
        }

    }

}