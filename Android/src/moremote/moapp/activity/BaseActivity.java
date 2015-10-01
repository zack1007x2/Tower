package moremote.moapp.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import org.droidplanner.android.DroidPlannerApp;
import org.droidplanner.android.activities.DrawerNavigationUI;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import java.util.Collection;

import moremote.xmpp.XMPPConnector;

/**
 * Created by lintzuhsiu on 15/3/24.
 */
public class BaseActivity extends DrawerNavigationUI {

    protected XMPPConnector xmppConnection;
    protected AlertDialog dialog;

    protected RosterListener rosterListener = new RosterListener() {
        @Override
        public void entriesAdded(Collection<String> addresses) {updateFriendList(addresses);}

        @Override
        public void entriesUpdated(Collection<String> addresses) {
            updateFriendList(addresses);
        }

        @Override
        public void entriesDeleted(Collection<String> addresses) {
            // to do delete
             deleteFriendList(addresses);
        }

        @Override
        public void presenceChanged(final Presence presence) {
            updateFriendStatus(presence);
        }
    };

    protected ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connected(XMPPConnection connection) {}

        @Override
        public void authenticated(XMPPConnection connection) {
            xmppAuthenticated();
        }

        @Override
        public void connectionClosed() {}

        @Override
        public void connectionClosedOnError(Exception e) {
            xmppConnectionClosedOnError();
        }

        @Override
        public void reconnectingIn(int seconds) {}

        @Override
        public void reconnectionSuccessful() {}

        @Override
        public void reconnectionFailed(Exception e) {}
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DroidPlannerApp application = (DroidPlannerApp) getApplication();
        xmppConnection = application.getXmppConnector();

        xmppConnection.addRosterListener(rosterListener);
        xmppConnection.addConnectionListener(connectionListener);
    }

    @Override
    protected int getToolbarId() {
        return 0;
    }

    @Override
    protected int getNavigationDrawerEntryId() {
        return 0;
    }

    protected void showConnectionCloseMsg(final Context context) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("connection failed")
                        .setPositiveButton("submit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                android.os.Process.killProcess(android.os.Process.myPid());
                                finish();
                            }
                        });

                // Create the AlertDialog object and display
                dialog = builder.create();
                dialog.show();
            }
        });
    }

    // connection listener callback
    protected void xmppConnectionClosedOnError() {}
    protected void xmppAuthenticated() {}

    // roster listener callback
    protected void deleteFriendList(Collection<String> addresses) {}
    protected void updateFriendList(Collection<String> addresses) {}
    protected void updateFriendStatus(final Presence presence) {}

}
