package moremote.p2p;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClient {

    private String TAG = "TCPClient";
    private String ip;
    private int port;
    private boolean isStop;
    private Socket socket;
    private InputStream reader;
    private OutputStream writer;
    private TCPMessageListener messageListener;
    private Thread receiveThread;
    private boolean isConnection=false;

    private int decodeNumber = -1;

    public TCPClient(String ip, int port, int decodeNumber) {
        this.ip = ip;
        this.port = port;
        this.decodeNumber = decodeNumber;
    }

    public boolean connect() {
        try {
            Log.e("Ray", "connect to " + ip + ":" + port);
            socket = new Socket(ip, port);
            reader = socket.getInputStream();
            writer = socket.getOutputStream();

            startReceiveMessage();
            isConnection = true;
        } catch (UnknownHostException e) {
            Log.e("Ray", "TCPClient exception:" + e);
            return false;
        } catch (IOException e) {
            Log.e("Ray", "TCPClietn IOException:" + e);
            return false;
        }
        return true;
    }

    public boolean isConnection() {
        return isConnection;
    }

    public void setMessageListener(TCPMessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public boolean send(byte[] buffer, int length) {
        try {
            writer.write(buffer, 0, length);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean close() {
        isStop = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                return false;
            }
            socket = null;
        }
        return true;
    }

    private void startReceiveMessage() {
        Runnable receiver = new MessageReceiver();
        receiveThread = new Thread(receiver);
        receiveThread.start();
    }

    class MessageReceiver implements Runnable {

        private byte[] lengthHeader;

        public MessageReceiver() {
            lengthHeader = new byte[4];
        }

        @Override
        public void run() {
            int dataLength = 0, rtpLength = 0, tmpLength = 0;

            try {
                while (!isStop) {
                    rtpLength = getRTPPacketLength();

                    Log.d("TCPClient", rtpLength + "");
                    if (rtpLength < 0) {
                        isStop = true;
                        break;
                    }

                    // receive rtp data
                    byte[] rtpData = new byte[rtpLength];
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
                    /** for test packet delay time*/
//                    long timestamp = 0;
//                    int i=0;
//                    while( i < 4 ){
//                        timestamp <<= 8;
//                        timestamp |= rtpData[4+i]&0xff;
//                        i++;
//                    }
//                    Log.d("TCPClient", "timestamp: " + timestamp);
                    // message receive callback
                    if (messageListener != null && !isStop) {
                        messageListener.processMessage(rtpData, rtpLength, decodeNumber);
                    }
                }
            }
            catch (IOException e) {
                isStop = true;
                Log.d(TAG, "Receive IO Exception:" + e.toString());
            }
            catch (IndexOutOfBoundsException e){
                isStop = true;
                Log.d(TAG, "Receive Index Out Of Bounds Exception:" + e.toString());
            }
        }

        private int getRTPPacketLength() throws IOException,IndexOutOfBoundsException {
            int rtpLength = 0;
            int len = 0;
            do{
                len = reader.read(lengthHeader, rtpLength, lengthHeader.length - rtpLength);
                if (len < 0) {
                    return -1;
                }
                rtpLength += len;
            }while( rtpLength < 4);

//            if (lengthHeader[0] != 0x24 || lengthHeader[1] != 0x00) {
//                while (reader.read() != 0x24 || reader.read() != 0x00) {
//					Log.d("TCPClient", Arrays.toString(lengthHeader));
//                }
//
//                lengthHeader[2] = (byte) reader.read();
//                lengthHeader[3] = (byte) reader.read();
//            }

            rtpLength = (lengthHeader[2] & 0xFF) << 8;
            rtpLength |= lengthHeader[3] & 0xFF;

            return rtpLength;
        }

    }

}