package moremote.p2p;

public interface TCPMessageListener {

	public void processMessage(byte[] data, int length, int decodeNumber);
	
}
