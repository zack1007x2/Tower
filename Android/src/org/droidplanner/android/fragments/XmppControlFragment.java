package org.droidplanner.android.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.droidplanner.android.R;
import org.droidplanner.android.fragments.helpers.ApiListenerFragment;

/**
 * Created by Zack on 15/10/1.
 */
public class XmppControlFragment extends ApiListenerFragment {


    private static final IntentFilter eventFilter = new IntentFilter();
    static {
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {

            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flight_actions_bar, container, false);
    }

    @Override
    public void onApiConnected() {
    }

    @Override
    public void onApiDisconnected() {
    }


}
