package moremote.relay;

public interface RelayMessageListener {

	public void processMessage(byte[] data, int length);
	
}
