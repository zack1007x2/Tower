package moremote.p2p;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import moremote.moapp.IPCam;
import moremote.moapp.MoApplication;
import moremote.xmpp.XMPPConnector;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by lintzuhsiu on 14/11/10.
 */
public class StunAsyncTask extends Thread {
	
	public static final String TAG = "stun";
	public static final int SUCCESS = 1;
	public static final int FAILED = -1;
	public static class NetWorkType {
		public static final int WAN   = 0;
		public static final int LAN   = 1;
	};
	public static class DataField {
		public static final String IP   		   = "ip";
		public static final String PORT 		   = "port";
		public static final String CONNECTION_TYPE = "conn_type";
	}

    private String jid;
    private IPCam ipcam;
    private XMPPConnector xmppConnector;
    private MessageListener xmppMessageListener;
    private Handler stunMsgHandler;
    private Thread punchThread;
    private Thread receiveThread;
    private MulticastSocket socket;
    private String command;
    
	// LOCAL IP
	private String selfLocalIP;
	private int selfLocalPort;
	// WAN IP
    private String selfPublicIP;
    private int selfPublicPort;
    // DEST IP
    private String destinationIP;
    private int destinationPort;
    // STUN FLAG
    private boolean isLocalSuccess;
    private boolean isDestSuccess;
    private int connectionType;

    // NAT TYPE
    private MoApplication.NatType selfNatType;
    private MoApplication.NatType destNatType;
    private boolean receiveXmppMessage = false;

    public StunAsyncTask(XMPPConnector xmppConnector, String command, IPCam ipcam, Handler handler, String localIP) {
        this.xmppConnector = xmppConnector;
        this.ipcam = ipcam;
        this.jid = ipcam.jid;
        this.stunMsgHandler = handler;
        this.selfLocalIP = localIP;
        isLocalSuccess = false;
        isDestSuccess = false;
        this.command = command;
    }

    @Override
    public void run() {
        super.run();

        try {
        	socket = new MulticastSocket();
        	selfLocalPort = socket.getLocalPort();
        	Log.e("Ray", "local port = " + selfLocalPort);
            setXmppMsgListener();
			getPublicIPAndPort();
            checkNatType();

	        sendIPAndPort();
	        
	        // stop 5s and display message if stun failed
	        sleep(10000);
			if (!isLocalSuccess && !isDestSuccess) {
				Log.d(TAG, "stun failed");
				returnToMainThread(FAILED);				
			} 
		} catch (IOException e) {
            Log.d(TAG, "IO Exception");
		} catch (InterruptedException e) {
			Log.d(TAG, "InterruptedException");
		} finally {
	        interrupt();
		}
    }

    @Override
    public void interrupt() {
        if (receiveThread != null && !receiveThread.isInterrupted()) {
            receiveThread.interrupt();
        }
        if (punchThread != null && !punchThread.isInterrupted()) {
            punchThread.interrupt();
        }
        if (xmppMessageListener != null) {
        	xmppConnector.removeMessageListener(xmppMessageListener);
        }
        super.interrupt();
    }



    private void getPublicIPAndPort() {
		byte[] command = {0x01, 0x0e};
		byte[] receiveData = new byte[20];

		getReceiveData(command, receiveData, MoApplication.P2PSERVER_IP, MoApplication.P2PSERVER_PORT);
			
        StringBuffer ip = new StringBuffer();
        for (int i = 2; i < 18; i++) {
            if (receiveData[i] != 0x00) {
                ip.append((char)(receiveData[i] & 0xFF));
            }
        }
        selfPublicIP = ip.toString();

        selfPublicPort = (receiveData[18] & 0xFF) << 8;
        selfPublicPort |= receiveData[19] & 0xFF;

        Log.d(TAG, "got public ip and port: " + selfPublicIP + ", " + selfPublicPort);
    }

    private void setReceiveDatagram() {
        receiveThread = new ReceiveDatagramThread();
        receiveThread.setPriority(Thread.MAX_PRIORITY);
        receiveThread.start();
    }

    private void sendIPAndPort() {
    	Log.d(TAG, "sendIPAndPort:" + selfPublicIP + "," + selfPublicPort + "-" +
                selfLocalIP + "," + selfLocalPort + ":" + selfNatType.toInt());
    	
        xmppConnector.sendMessage(jid, command + selfPublicIP + "," + selfPublicPort + "-" + 
        		selfLocalIP + "," + selfLocalPort + ":" + selfNatType.toInt());
    }

    private void punchDatagram() {
    	punchThread = new PunchThread();
    	punchThread.start();
    }

    private void setXmppMsgListener() {
    	xmppMessageListener = new MessageListener() {
            @Override
            public void processMessage(Chat chat, Message message) {

                String sender = chat.getParticipant();
                if(sender.contains("/")) {
                    sender = sender.substring(0, sender.lastIndexOf("/"));
                }

                String tmp = message.getBody();
                if(sender.equals(jid)) {
                    Log.e("Ray", "receive right message@ " + sender + ", " + tmp);
                    receiveXmppMessage = true;
                }
                else {
//                    Log.e("Ray", "receive wrong message@ " + tmp);
                    return ;
                }
//                if(jid.equals(sender)){
//                    Log.e("Ray","equal");
//                }
                String msg = message.getBody();
                
                if (msg == null) {
                	return;
                }
                
                Log.d(TAG, "xmpp message received:" + msg);
                if (msg.contains(MoApplication.XMPPCommand.STUN) || msg.contains(MoApplication.XMPPCommand.AUDIO_BACK)) {
                	int dashPosition = msg.indexOf("-");
                    String desPublicIP = msg.substring(5, msg.indexOf(","));
                    int desPublicPort = Integer.valueOf(msg.substring(msg.indexOf(",") + 1, dashPosition));
                    String desLocalIP = msg.substring(dashPosition + 1, msg.indexOf(",", dashPosition));
                    int natPosition = msg.indexOf(":",dashPosition);
                    int desLocalPort = Integer.valueOf(msg.substring(msg.indexOf(",", dashPosition) + 1, natPosition));
                    destNatType = MoApplication.NatType.get( Integer.valueOf(msg.substring(natPosition + 1, msg.length())) );
                    
                    // Connection Type check
                    if (selfPublicIP.equals(desPublicIP)) {
                    	destinationIP = desLocalIP;
                    	destinationPort = desLocalPort;
                    	connectionType = NetWorkType.LAN;
                    }
                    else {
                    	destinationIP = desPublicIP;
                    	destinationPort = desPublicPort;
                    	connectionType = NetWorkType.WAN;
                    }

                    // After get destination IP & PORT , start receive and Punch Thread
                    setReceiveDatagram();
                    punchDatagram();
                }
                else if (msg.contains(MoApplication.XMPPCommand.STUN_SUCCESS)) {
                    destinationPort = Integer.valueOf(msg.substring(msg.indexOf(":") + 1, msg.length()));
            		isLocalSuccess = true;
                    isDestSuccess = true;
            		returnToMainThread(SUCCESS);
                }
                else if (msg.contains(MoApplication.XMPPCommand.CAMERA_TYPE)){
                    Log.e("Ray", "@# get CameraType in StunAsyncTask");
                    int cameraType = Integer.valueOf(msg.split(":")[1]);
                    ipcam.setCameraType(cameraType);
                }

            }
        };
        xmppConnector.addMessageListener(xmppMessageListener);
    }

    private synchronized void returnToMainThread(int result) {
        android.os.Message handleMsg = new android.os.Message();
		handleMsg.what = result;
        Bundle extras = new Bundle();
//        extras.putString("jid", jid);
        extras.putBoolean("receiveXmppMessage", receiveXmppMessage);
    	if (result == FAILED) {
            handleMsg.setData(extras);
            stunMsgHandler.sendMessage(handleMsg);
    	}
    	else if (isDestSuccess && isLocalSuccess) {

            extras.putString(DataField.IP, destinationIP);
            extras.putInt(DataField.PORT, destinationPort);
            extras.putInt(DataField.CONNECTION_TYPE, connectionType);

            handleMsg.obj = socket;
            handleMsg.setData(extras);
            stunMsgHandler.sendMessage(handleMsg);

        	Log.d(TAG, "return message to main thread");
    	}
    }
    
    class PunchThread extends Thread {
    	
    	private boolean isStop = false;

    	@Override
		public void run() {
	        try {                    
                byte[] msg = {0x01, 0x02, 0, 0};
                boolean fChangePort = false;
                if( connectionType != NetWorkType.LAN && selfNatType.compare(MoApplication.NatType.NAT_NORMAL) && !destNatType.compare(MoApplication.NatType.NAT_NORMAL) ) {
                    if( !destNatType.compare(MoApplication.NatType.NAT3_UNUSUAL) )
                        fChangePort = true;
                    if(destNatType.compare(MoApplication.NatType.NAT4_RTOS)) {
                        destinationPort = destinationPort - computeRTOSPort(MoApplication.P2PSERVER_IP) + computeRTOSPort(selfPublicIP) + selfPublicPort - MoApplication.P2PSERVER_PORT;
                        if(destinationPort > 65536) destinationPort %= 65536;
                        else if( destinationPort < 0) destinationPort += 65536;
                    }
                    Thread.sleep(2000);
                }
                msg[2] = (byte)((destinationPort>>8) & 0xff);
                msg[3] = (byte)(destinationPort & 0xff);
                InetSocketAddress dstAddress = new InetSocketAddress(destinationIP, destinationPort);
                DatagramPacket sendPacket = new DatagramPacket(msg, msg.length, dstAddress);

                // Different Nat Type Setting
                int punchUnit = 30;
                int punchRetry = 3;
                int punchInterval = 500;
                if(destNatType.compare(MoApplication.NatType.NAT4_OTHER))
                {
                    destinationPort = 1024;
                    punchUnit = 1200;
                    punchInterval = 50;
                }

	            for (int i = 1+punchRetry; !isLocalSuccess && !isStop; i++) {

                    try{
                        socket.send(sendPacket);
                    }catch (SocketException e) {
                        Log.d(TAG, "punch: socket exception" + e.toString());
                    } catch (IOException e) {
                        Log.d(TAG, "punch: IO exception" + e.toString());
                    }

	                if (i % punchRetry == 0) {

                        if((i % punchUnit) == 0){
                            Log.d(TAG, "punch start:" + destinationIP + "," + destinationPort);
                            sleep(punchInterval);
                        }

                        if(fChangePort){
                            /** if dest is NAT4, need to change destinationPort
                             *** TODO if NAT TYPE == NAT4_OTHER, need change port by random
                             **/
                            if(destNatType.compare(MoApplication.NatType.NAT4_CGN) ){
                                destinationPort++;
                                destinationPort %= 65536;
                            }
                            else if(destNatType.compare(MoApplication.NatType.NAT4_LINUX)){
                                destinationPort++;
                                destinationPort %= 1024;
                            }
                            else if(destNatType.compare(MoApplication.NatType.NAT4_RTOS)){
                                if( i%(punchRetry*3) == 0)
                                    destinationPort += 2048;
                                else
                                    destinationPort -= 1024;
                                if( destinationPort < 0 ) destinationPort += 65536;
                                else destinationPort %= 65536;
                            }
                            else if( destNatType.compare(MoApplication.NatType.NAT4_OTHER) )
                            {
                                destinationPort++;
                                destinationPort %= 65536;
                            }
                            msg[2] = (byte)((destinationPort>>8) & 0xff);
                            msg[3] = (byte)(destinationPort & 0xff);
                            dstAddress = new InetSocketAddress(destinationIP, destinationPort);
                            sendPacket = new DatagramPacket(msg, msg.length, dstAddress);
                        }
	                }
	            }
	            
	            Log.d(TAG, "punch over:" + isLocalSuccess + "," + isStop);
	        } catch (SocketException e) {
	        	Log.d(TAG, "punch: socket exception");
	        } catch (IOException e) {
	        	Log.d(TAG, "punch: IO exception");
	        } catch (InterruptedException e) {
				Log.d(TAG, "punch: InterruptedException");
			} finally {
				interrupt();
			}
		}
    	
        @Override
        public void interrupt() {
            super.interrupt();
            isStop = true;
        }
    }
    
    class ReceiveDatagramThread extends Thread {

        @Override
        public void run() {
            try {
                byte[] buf = new byte[4];
                DatagramPacket receiveDatagramPacket = new DatagramPacket(buf, buf.length);
                
                Log.d(TAG, "start receive data");
                while(!isDestSuccess) {
                    socket.receive(receiveDatagramPacket);
                    Log.d(TAG, "received punch data");
                    if (buf[0] == 0x01 && buf[1] == 0x02 && !isDestSuccess) {
                        int selfPort;
                        selfPort = (buf[2] & 0xff) <<8;
                        selfPort |= buf[3] & 0xff;
                        Log.d(TAG, "send success message over xmpp " + MoApplication.XMPPCommand.STUN_SUCCESS + selfPort);
                        xmppConnector.sendMessage(jid, MoApplication.XMPPCommand.STUN_SUCCESS + selfPort);
                        isDestSuccess = true;
                        isLocalSuccess = true;
                        destinationPort = Integer.valueOf(receiveDatagramPacket.getPort());

                        returnToMainThread(SUCCESS);
                    }
                }
	        } catch (IOException e) {
            	Log.d(TAG, "Receive punch data exception");
	            interrupt();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
        }
    }

    private void checkNat3()
    {
        byte[] command = {0x01, 0x1f};
        byte[] receiveData = new byte[20];
        getReceiveData(command, receiveData, MoApplication.P2PSERVER_IP, MoApplication.P2PSERVER_PORT);
        try {
            sleep(2);
        }catch (InterruptedException e) {
            Log.e(TAG, "checkNat3 1st: InterruptedException");
            selfNatType = MoApplication.NatType.NAT_ERROR;
            return;
        }

        command[1] = 0x0e;
        getReceiveData(command, receiveData, MoApplication.P2PSERVER_IP, MoApplication.P2PSERVER_PORT2);

        int portNAT3;
        portNAT3 = (receiveData[18] & 0xFF) <<8;
        portNAT3 |= receiveData[19] & 0xFF;
        Log.i(TAG, "checkNat3 get port2 " + portNAT3);
        if(portNAT3 != selfPublicPort)
            selfNatType = MoApplication.NatType.NAT3_UNUSUAL;
        else
            selfNatType = MoApplication.NatType.NAT_NORMAL;
    }

    private int computeRTOSPort(String ip)
    {
        int ipv4[] = new int[4], i;
        String[] s = ip.split("\\.");
        for( i=0 ; i<4 ; i++){
            //Log.d(TAG,"computeRTOSPort "+s[i]);
            ipv4[i] = Integer.valueOf(s[i]);
        }
        int port =
                //((((((ipv4[0]-1)<<2) + ipv4[1])<<2) + ipv4[2])<<8) + ipv4[3];
                4096*(ipv4[0]-1)+1024*ipv4[1]+256*ipv4[2]+ipv4[3];
        if(ipv4[2] >= 19) port += 1024;
        if(ipv4[1] >=5 ) {
            port += (((ipv4[1]-5)/63)+1)*1024;
        }
        int[] excA = new int[]{3,18,34,50,64,71,85,101,117,133,148,164,180,196,211,227,243};
        for( i=0 ; i<17 ; i++){
            if(ipv4[0] < excA[i]) break;
        }
        port += i*1024;
        if(ipv4[0] >= 64) port+=48128;
        return port%65536;
    }

    private void checkNat4()
    {
        byte[] command = {0x01, 0x0e};
        byte[] receiveData = new byte[20];

        getReceiveData(command, receiveData, MoApplication.P2PSERVER_IP2, MoApplication.P2PSERVER_PORT);

        int portNAT4;
        portNAT4 = (receiveData[18] & 0xFF) << 8;
        portNAT4 |= receiveData[19] & 0xFF;
        Log.i(TAG, "checkNat4 1st got port: " + portNAT4);

        if(portNAT4 == selfPublicPort) {
            selfNatType = MoApplication.NatType.NAT_NORMAL;
            return;
        }

        getReceiveData(command, receiveData, MoApplication.P2PSERVER_IP2, MoApplication.P2PSERVER_PORT2);

        int port2NAT4;
        port2NAT4 = (receiveData[18] & 0xFF) << 8;
        port2NAT4 |= receiveData[19] & 0xFF;
        Log.i(TAG, "checkNat4 2th got port2: " + port2NAT4);

        /** after got 2 port, start check NAT4 TYPE*/
        //check CGN
        int dif[] = new int[2];
        dif[0] = portNAT4 - selfPublicPort;
        dif[1] = port2NAT4 - selfPublicPort;
        if( dif[0] < 0 ) dif[0] += 65536;
        if( dif[1] < 0 ) dif[1] += 65536;
        if( dif[0] < 30 && dif[1] < 30 ){
            selfNatType = MoApplication.NatType.NAT4_CGN;
            return;
        }

        //check linux base
        if(selfLocalPort <=1024 && portNAT4 <= 1024 && port2NAT4 <= 1024){
            selfNatType = MoApplication.NatType.NAT4_LINUX;
            return;
        }

        //check RTOS
        int ip1PortAddNum = computeRTOSPort(MoApplication.P2PSERVER_IP);
        int ip2PortAddNum = computeRTOSPort(MoApplication.P2PSERVER_IP2);

        if( portNAT4 == selfPublicPort - ip1PortAddNum + ip2PortAddNum){
            //selfPublicPort = selfPublicPort - ip1PortAddNum + computeRTOSPort(destinationIP);
            selfNatType = MoApplication.NatType.NAT4_RTOS;
            return;
        }
        selfNatType = MoApplication.NatType.NAT4_OTHER;
    }


    private void checkNatType()
    {
        Log.i(TAG, "check self NAT Type");
        checkNat4();
        if( selfNatType.toInt() == MoApplication.NatType.NAT_NORMAL.toInt())
            checkNat3();
        Log.i(TAG, "self NAT TYPE = " + selfNatType);
    }

    private void getReceiveData(byte[] sendData, byte[] receiveData, final String dstIP, final int dstPort) {
        InetSocketAddress dstAddress = new InetSocketAddress(dstIP, dstPort);

        try {
            final DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dstAddress);

            final Semaphore lock = new Semaphore(0);
            Thread sendDataThread = new Thread(new Runnable(){
                @Override
                public void run(){
                    //int i=0;
                    while(true) try {
                        //Log.d(TAG, "send to P2Pserver " + i + " times");
                        //i++;
                        socket.send(sendPacket);
                        if (lock.tryAcquire(500, TimeUnit.MILLISECONDS))
                            break;
                    } catch (IOException e) {
                        Log.e(TAG, "getReceiveData send: IO Exception");
                    } catch (InterruptedException e) {
                        Log.e(TAG, "getReceiveData send: Interrupted Exception");
                    }
                }
            });
            sendDataThread.setPriority(Thread.MIN_PRIORITY);
            sendDataThread.start();

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            do{
                socket.receive(receivePacket);

                if( receivePacket.getAddress().getHostAddress().compareTo( dstIP )!=0 ||
                        receivePacket.getPort() != dstPort ){
                    Log.d(TAG, receivePacket.getAddress().getHostAddress() + " vs " + dstIP);
                    Log.d(TAG, receivePacket.getPort() + " vs " + dstPort);
                    receiveData[1] = 0x00;
                }
            }while(receiveData[0] != 0x00 || receiveData[1] != 0x0E);
            lock.release();

        } catch (SocketException e) {
            Log.e(TAG, "getReceiveData: Socket Exception");
        } catch (IOException e) {
            Log.e(TAG, "getReceiveData recv: IO Exception");
        }
    }
}