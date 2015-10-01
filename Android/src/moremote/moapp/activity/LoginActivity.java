package moremote.moapp.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends BaseActivity {

    private static final String APP_NAME = "moapp";
    private static final String JID_FIELD = "jid";
    public static class UserInfo {
        public static String username = "username";
    }

    private EditText usernameET;
    private EditText passwordET;
    private Button loginBtn;
    private String username;
    private String resource;
    private String password;
    private SharedPreferences settings;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.login_activity);
//        usernameET = (EditText) findViewById(R.id.username);
//        passwordET = (EditText) findViewById(R.id.password);
//        loginBtn = (Button) findViewById(R.id.login);
//
//        loginBtn.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				login();
//			}
//		});
//
//        settings = getSharedPreferences(APP_NAME, 0);
//        usernameET.setText(settings.getString(JID_FIELD, getResources().getString(R.string.test_username)));
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
////        MenuInflater inflater = getMenuInflater();
////        inflater.inflate(R.menu.login, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.submit:
//                login();
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    public static String getMacAddress(Context context) {
//        WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInf = wifiMan.getConnectionInfo();
//        return wifiInf.getMacAddress();
//    }
//
//    private void login() {
//        username = usernameET.getText().toString();
//        password = passwordET.getText().toString();
//
//        if (username.length() == 0 || password.length() == 0) {
//            Toast.makeText(this, getResources().getString(R.string.parameter_incomplete), Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        settings.edit().putString(JID_FIELD, username).commit();
//
//        // login to xmpp server
//        resource = getMacAddress(this)+"AppName";
//        String xmppDomain = "xmpp01.moremote.com";
//        // xmppDomain set null if want use account domain
//        xmppConnection.loginToXMPPServer(username, password,xmppDomain, resource);
//    }
//
//    private void toCtrlActivity(String username, String password) {
//        Intent intent = new Intent(this, FriendListActivity.class);
//        startActivity(intent);
//        finish();
//    }
//
//    @Override
//    protected void xmppConnectionClosedOnError() {
//        super.showConnectionCloseMsg(this);
//    }
//
//    @Override
//    protected void xmppAuthenticated() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(LoginActivity.this, getResources().getString(R.string.login_success),
//                        Toast.LENGTH_SHORT).show();
//                toCtrlActivity(username, password);
//            }
//        });
//    }

}



