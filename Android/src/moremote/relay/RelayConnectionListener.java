package moremote.relay;

public interface RelayConnectionListener {

	public void connected();
	public void authed(String auth);
	
}
