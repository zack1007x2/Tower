package moremote.moapp.asynctask;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import moremote.moapp.MoApplication;
import moremote.p2p.TCPClient;
import moremote.xmpp.XMPPConnector;

public class TCPConnectAsyncTask extends AsyncTask<String, Void, Boolean> {

	public static final String TAG = "TCPConnectAsyncTask";
	
	private TCPClient client;
	private Context context;
	private XMPPConnector xmppConnector;
	private String friendJID;
	
	public TCPConnectAsyncTask(Context context, TCPClient client, XMPPConnector xmppConnector, String friendName) {
		this.context = context;
		this.client = client;
		this.xmppConnector = xmppConnector;
		this.friendJID = friendName;
	}
	
	@Override
	protected Boolean doInBackground(String... params) {
		Log.d(TAG, "connecting...");
        Log.e("Ray", "connecting.. " + friendJID);
		return client.connect();
	}
		
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if (result) {
			Log.d(TAG, "connection success");
			
			xmppConnector.sendMessage(friendJID, MoApplication.XMPPCommand.STREAM_START);
			Toast.makeText(context, "connection_success",
					Toast.LENGTH_SHORT).show();
		}
		else {
			Log.d(TAG, "connection failed");
			Toast.makeText(context, "connection_failed",
					Toast.LENGTH_SHORT).show();
		}
	}

}