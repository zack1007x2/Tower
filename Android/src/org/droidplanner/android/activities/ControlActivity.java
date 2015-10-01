package org.droidplanner.android.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.droidplanner.android.R;
import org.droidplanner.android.fragments.XmppControlFragment;


public class ControlActivity extends DrawerNavigationUI {

    private final static String TAG = AccountActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        if (savedInstanceState == null) {
            Fragment droneShare;
            droneShare = new XmppControlFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.droneshare_control, droneShare).commit();
        }
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.droneshare_control);
    }

    @Override
    protected int getToolbarId() {
        return R.id.actionbar_toolbar;
    }

    @Override
    protected int getNavigationDrawerEntryId() {
        return R.id.navigation_account;
    }


}
