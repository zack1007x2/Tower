package moremote.moapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import org.jivesoftware.smack.packet.Presence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import moremote.moapp.adapter.FriendListAdapter;
import moremote.moapp.wrap.UserStatus;


public class FriendListActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener,AdapterView.OnItemLongClickListener {

    public static class TransportData {
        public static String friend = "friend";
    }

    private HashMap<String, UserStatus> friends;
    private Button addFriendBtn;
    private ListView friendListView;
    private FriendListAdapter friendListAdapter;
    private String deleteFriendName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.friend_list_activity);
//
//        addFriendBtn = (Button) findViewById(R.id.add_friend);
//        friendListView = (ListView) findViewById(R.id.friend_list);
//
//        // init object
//        friends = new HashMap<String, UserStatus>();
//
//        setFriendList();
//        addFriendBtn.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        xmppConnection.disconnect();
    }

    @Override
    public void onClick(View v) {
        // create a new friend
        if (v == addFriendBtn) {
            addFriend();
        }
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

//        Log.v("long clicked", "pos: " + position);
//        Iterator it = friends.entrySet().iterator();
//        for (int i = 0; i <= position && it.hasNext(); i++) {
//            Map.Entry<String, UserStatus> pairs = (Map.Entry) it.next();
//            if (i == position) {
//                deleteFriendName = pairs.getKey();
//            }
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setMessage(R.string.delete_friend)
//                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.dismiss();
//                        xmppConnection.deleteFriend(deleteFriendName);
//                    }
//                });
//
//        // Create the AlertDialog object and display
//        dialog = builder.create();
//        dialog.show();

        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Iterator it = friends.entrySet().iterator();
        for (int i = 0; i <= position && it.hasNext(); i++) {
            Map.Entry<String, UserStatus> pairs = (Map.Entry) it.next();
            if (i == position) {
                String name = pairs.getKey();
                toRemoteCtrl(name);
            }
        }
    }



    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}

    private void setFriendList() {
    	xmppConnection.getRoster(friends);
    	friendListAdapter = new FriendListAdapter(this, friends);
    	friendListView.setAdapter(friendListAdapter);
    	friendListView.setOnItemClickListener(this);
        friendListView.setOnItemLongClickListener(this);
    }
    
    private void addFriend() {
//        String friendName = ((EditText) findViewById(R.id.add_friend_name)).getText().toString();
//        xmppConnection.addFriend(friendName, friendName);
    }

    private void toRemoteCtrl(String friendName) {
        Intent intent = new Intent(this, RemoteControlActivity.class);
        intent.putExtra(TransportData.friend, friendName);
        startActivity(intent);
    }

    private void refreshFriendList(final Collection<String> addresses) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (String friendName : addresses) {
                    UserStatus item = friends.get(friendName);
                    if (item == null) {
                        friends.put(friendName, new UserStatus());
                    }
                }

                friendListAdapter.notifyDataSetInvalidated();
            }
        });
    }

    protected void xmppConnectionClosedOnError() {
        super.showConnectionCloseMsg(this);
    }

    @Override
    protected void deleteFriendList(final Collection<String> addresses) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (String friendName : addresses) {
                    UserStatus item = friends.get(friendName);
                    if (item != null) {
                        friends.remove(friendName);
                    }
                }

                friendListAdapter.notifyDataSetInvalidated();
            }
        });
    }

    @Override
    protected void updateFriendList(Collection<String> addresses) {
        refreshFriendList(addresses);
    }

    @Override
    protected void updateFriendStatus(final Presence presence) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String friendName = presence.getFrom().split("/")[0];
//                String friendName = presence.getFrom();

                UserStatus item = friends.get(friendName);
                if (item == null) {
                    item = new UserStatus();
                }

                item.setType(presence.getType().name());
                item.setStatus(presence.getStatus());
                friends.put(friendName, item);

                friendListAdapter.notifyDataSetInvalidated();
            }
        });
    }

}
