package moremote.xmpp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import moremote.moapp.wrap.UserStatus;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by lintzuhsiu on 14/10/21.
 */
public class XMPPConnector {

	public static final String TAG = "xmpp";
    public static final int DEFAULT_PORT = 5222;

    private String ip;
    private String username;
    private String resource;
    private String password;
    private ExecutorService threadPool;
    private ConnectionConfiguration connectConfig;
    private XMPPTCPConnection connection;
    private Roster roster;
    private List<ConnectionListener> connectionListeners;
    private List<MessageListener> messageListeners;
    private List<RosterListener> rosterListeners;
    private HashMap<String, Chat> chats;

    public static class MessageType {
        public static int GET_ROSTER = 0;
    }

    public static class DataType {
        public static String FRIEND_LIST = "get_roster";
    }
    
    private MessageListener defaultMessageListener = new MessageListener() {
		
		@Override
		public void processMessage(Chat chat,
				org.jivesoftware.smack.packet.Message message) {
			for (MessageListener listener : messageListeners) {
                listener.processMessage(chat, message);
            }
		}
	};

    private ConnectionListener defaultConnectionListener = new ConnectionListener() {

        @Override
        public void connected(XMPPConnection c) {
            setRosterListener(c);
            try {
                connection.login(username, password, resource);
            } catch (XMPPException e) {
                Log.d(TAG, "XMPPException");
            } catch (SmackException e) {
                Log.d(TAG, "SmackException");
            } catch (IOException e) {
                Log.d(TAG, "IOException");
            }

            for (ConnectionListener listener : connectionListeners) {
                listener.connected(connection);
            }
        }

        @Override
        public void authenticated(XMPPConnection connection) {
        	setMessageListener(defaultMessageListener);
            for (ConnectionListener listener : connectionListeners) {
                listener.authenticated(connection);
            }
        }

        @Override
        public void connectionClosed() {
            for (ConnectionListener listener : connectionListeners) {
                listener.connectionClosed();
            }
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            for (ConnectionListener listener : connectionListeners) {
                listener.connectionClosedOnError(e);
            }
        }

        @Override
        public void reconnectingIn(int seconds) {
            for (ConnectionListener listener : connectionListeners) {
                listener.reconnectingIn(seconds);
            }
        }

        @Override
        public void reconnectionSuccessful() {
            for (ConnectionListener listener : connectionListeners) {
                listener.reconnectionSuccessful();
            }
        }

        @Override
        public void reconnectionFailed(Exception e) {
            for (ConnectionListener listener : connectionListeners) {
                listener.reconnectionFailed(e);
            }
        }
    };

    private RosterListener defaultRosterListener = new RosterListener() {
        @Override
        public void entriesAdded(Collection<String> addresses) {
            for (RosterListener listener : rosterListeners) {
                listener.entriesAdded(addresses);
            }
        }

        @Override
        public void entriesUpdated(Collection<String> addresses) {
            for (RosterListener listener : rosterListeners) {
                listener.entriesUpdated(addresses);
            }
        }

        @Override
        public void entriesDeleted(Collection<String> addresses) {
            for (RosterListener listener : rosterListeners) {
                listener.entriesDeleted(addresses);
            }
        }

        @Override
        public void presenceChanged(Presence presence) {
            for (RosterListener listener : rosterListeners) {
                listener.presenceChanged(presence);
            }
        }
    };

    private PacketListener defaultPresenceListener = new PacketListener() {
        @Override
        public void processPacket(Packet packet) throws SmackException.NotConnectedException {

            Presence presence = (Presence)packet;
            if(presence.getType() == Presence.Type.subscribe ){

                // Subscribe each other
                Log.d(TAG, "Subcribe request from : " + packet.getFrom());

                Presence subscribe = new Presence(Presence.Type.subscribe);
                subscribe.setTo(presence.getFrom());
                connection.sendPacket(subscribe);

            }else if(presence.getType() == Presence.Type.unsubscribe){
                // unfriend each other
                Log.d(TAG, "unSubcribe request from : " + packet.getFrom());

                try {
                    roster.removeEntry(roster.getEntry(packet.getFrom()));
                } catch (SmackException.NotLoggedInException e) {
                    e.printStackTrace();
                } catch (SmackException.NoResponseException e) {
                    e.printStackTrace();
                } catch (XMPPException.XMPPErrorException e) {
                    e.printStackTrace();
                }

            }else{

            }
        }
    };

    public XMPPConnector(Context context) {
        connectionListeners = new ArrayList<ConnectionListener>();
        messageListeners = new ArrayList<MessageListener>();
        rosterListeners = new ArrayList<RosterListener>();
        chats = new HashMap<String, Chat>();
        threadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "Background executor service");
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setDaemon(true);
                return thread;
            }
        });
    }
    
    public void removeMessageListener(MessageListener listener) {
    	for (int i = 0; i < messageListeners.size(); i++) {
    		if (listener == messageListeners.get(i)) {
    			messageListeners.remove(i);
    			i--;
    		}
    	}
    }
    
    public void addMessageListener(MessageListener listener) {
    	messageListeners.add(listener);
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void addRosterListener(RosterListener listener) {
        rosterListeners.add(listener);
    }

    public boolean loginToXMPPServer(final String username, final String password,final String xmppDomain , final String resource) {
        final XMPPConnector self = this;
        String[] userData = null;

        if (!username.matches("^.*@.*$")) {
        	return false;
        }
        
    	userData = username.split("@");
        this.username = userData[0];

        this.ip = userData[1];

        this.resource = resource;
//        this.resource = userData[1].split("/").length==1 ?null :userData[1].split("/")[1];
        this.password = password;



        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    setConnectionConfig(ip, DEFAULT_PORT);
                    connection = new XMPPTCPConnection(connectConfig);
                    connection.addConnectionListener(defaultConnectionListener);
                    connection.addPacketListener(defaultPresenceListener, new PacketTypeFilter(Presence.class));
                    connection.connect();
                } catch (XMPPException e) {
                    Log.d(TAG, "XMPPException:" + e.toString());
                } catch (SmackException e) {
                    defaultConnectionListener.connectionClosedOnError(e);
                } catch (IOException e) {
                    Log.d(TAG, "IOException");
                }
            }
        });
        
        return true;
    }

    public void createChat(final String username, final MessageListener messageListener) {
        messageListeners.add(messageListener);

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                ChatManager chatManager = ChatManager.getInstanceFor(connection);
                Chat newChat = chatManager.createChat(username, messageListener);
                chats.put(username, newChat);
            }
        });
    }

    public void sendMessage(final String username, final String message) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                Chat chat = chats.get(username);

                if (chat == null) {
                    ChatManager chatManager = ChatManager.getInstanceFor(connection);
                    chat = chatManager.createChat(username, new MessageListener() {
                        @Override
                        public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {}
                    });
                    chats.put(username, chat);
                }

                try {
                    chat.sendMessage(message);
                } catch (XMPPException e) {
                    Log.d(TAG, "XMPP Exception");
                } catch (SmackException.NotConnectedException e) {
                    Log.d(TAG, "XMPP NotConnectedException");
                }
            }
        });
    }

    public void disconnect() {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                	if (connection != null) {
                		connection.disconnect();
                	}
                } catch (SmackException.NotConnectedException e) {
                	Log.d(TAG, "XMPP NotConnectedException");
                }
                connectionListeners.clear();
                messageListeners.clear();
                rosterListeners.clear();
                chats.clear();
            }
        });
    }

    public void changeStatus(final String status) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                Presence presence = new Presence(Presence.Type.available);
                presence.setStatus(status);
                try {
                    connection.sendPacket(presence);
                } catch (SmackException.NotConnectedException e) {
                	Log.d(TAG, "XMPP NotConnectedException");
                }
            }
        });
    }

    public void getRoster(HashMap<String, UserStatus> friends) {
    	if (connection == null) {
    		return;
    	}
    	
    	Roster roster = connection.getRoster();
    	Collection<RosterEntry> entries = roster.getEntries();
    	Presence presence;
    	
		for (RosterEntry entry : entries) {
			presence = roster.getPresence(entry.getUser());
            UserStatus item = friends.get(entry.getUser());
            if (item == null) {
                item = new UserStatus();
            }
            
            item.setType(presence.getType().name());
            item.setStatus(presence.getStatus());
            friends.put(entry.getUser(), item);
		}
    }
    
    public void getFriendRoster(final Handler messageHandler) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                Collection<RosterEntry> entries = roster.getEntries();
                ArrayList<String> friendList = new ArrayList<String>();
                Message msg = new Message();
                Bundle extras = new Bundle();

                for (RosterEntry entry : entries) {
                    friendList.add(entry.getUser());
                }

                extras.putStringArrayList(DataType.FRIEND_LIST, friendList);
                msg.setData(extras);
                msg.what = MessageType.GET_ROSTER;
                messageHandler.sendMessage(msg);
            }
        });
    }

    private void setMessageListener(final MessageListener messageListener){
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addChatListener(new ChatManagerListener() {
            @Override
            public void chatCreated(Chat chat, boolean createdLocally) {
//                if (!createdLocally) {
                chat.addMessageListener(messageListener);
            }
//            }
        });
    }

    public void addFriend(final String friendName, final String nickname) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    roster.createEntry(friendName, nickname, new String[]{});
                } catch (SmackException.NotLoggedInException e) {
                    Log.d(TAG, "XMPP NotLoggedInException");
                } catch (SmackException.NoResponseException e) {
                    Log.d(TAG, "XMPP NoResponseException");
                } catch (XMPPException.XMPPErrorException e) {
                    Log.d(TAG, "XMPPErrorException");
                } catch (SmackException.NotConnectedException e) {
                    Log.d(TAG, "XMPP NotConnectedException");
                }
            }
        });
    }

    public void deleteFriend(final String friendName) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    roster.removeEntry(roster.getEntry(friendName));
                } catch (SmackException.NotLoggedInException e) {
                    Log.d(TAG, "XMPP NotLoggedInException");
                } catch (SmackException.NoResponseException e) {
                    Log.d(TAG, "XMPP NoResponseException");
                } catch (XMPPException.XMPPErrorException e) {
                    Log.d(TAG, "XMPPErrorException");
                } catch (SmackException.NotConnectedException e) {
                    Log.d(TAG, "XMPP NotConnectedException");
                }
            }
        });
    }

    private void setConnectionConfig(String xmppServerIP, int port) {
        connectConfig = new ConnectionConfiguration(xmppServerIP, port,xmppServerIP);
        connectConfig.setReconnectionAllowed(true);
        connectConfig.setDebuggerEnabled(true);
        connectConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
//        connectConfig.setDebuggerEnabled(true);
    }

    private void setRosterListener(XMPPConnection connection) {
        roster = connection.getRoster();
        // manual to management roster
        // roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        roster.addRosterListener(defaultRosterListener);
    }

}
