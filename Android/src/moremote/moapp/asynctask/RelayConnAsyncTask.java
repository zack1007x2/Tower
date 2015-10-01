package moremote.moapp.asynctask;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import moremote.moapp.MoApplication;
import moremote.relay.RelayClientBACK;
import moremote.xmpp.XMPPConnector;

public class RelayConnAsyncTask extends AsyncTask<Object, Void, Boolean> {

    public static final String TAG = "RelayConnAsyncTask";

    private Context context;
    private RelayClientBACK relayClient;
    private XMPPConnector xmppConnector;
    private String friendJID;

    public RelayConnAsyncTask(Context context, RelayClientBACK relayClient, XMPPConnector xmppConnector, String friendName) {
        this.context = context;
        this.relayClient = relayClient;
        this.xmppConnector = xmppConnector;
        this.friendJID = friendName;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Object... params) {
//		return relayClient.connect();
        if (relayClient.connect()) {
            return relayClient.auth(false, (String)params[1], (String)params[2]);
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (result) {
            Log.d(TAG, "connection success");

            xmppConnector.sendMessage(friendJID, MoApplication.XMPPCommand.STREAM_START);
            Toast.makeText(context, "Connect Success",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            Log.d(TAG, "connection failed");
            Toast.makeText(context, "connection failed",
                    Toast.LENGTH_SHORT).show();
        }
    }

}