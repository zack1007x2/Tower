package org.droidplanner.android.graphic.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.o3dr.services.android.lib.coordinate.LatLong;

import org.droidplanner.android.R;
import org.droidplanner.android.activities.helpers.SuperUI;
import org.droidplanner.android.data.DroneModel;
import org.droidplanner.android.data.GpsModel;
import org.droidplanner.android.data.UserPerference;
import org.droidplanner.android.maps.MarkerInfo;

/**
 * Created by Zack on 15/10/15.
 */



public class GraphicXmppDrone extends MarkerInfo.SimpleMarkerInfo{

    private SuperUI mActivity;

    public GraphicXmppDrone(SuperUI activity) {
        mActivity = activity;
    }

    @Override
    public float getAnchorU() {
        return 0.5f;
    }

    @Override
    public float getAnchorV() {
        return 0.5f;
    }

    public LatLong getPosition() {
        GpsModel droneGps = mActivity.mGpsModel;
        return droneGps.getPosition()!=null ? droneGps.getPosition() :  null;
    }

    public Bitmap getIcon(Resources res) {
        if (UserPerference.getUserPerference(mActivity).getIsDroneConnected()) {
            return BitmapFactory.decodeResource(res, R.drawable.quad);
        }
        return BitmapFactory.decodeResource(res, R.drawable.quad_disconnect);

    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public boolean isFlat() {
        return true;
    }

    public float getRotation() {
        DroneModel droneModel = mActivity.mDroneModel;
        return droneModel == null ? 0 : (float) droneModel.getYaw();
    }

}
