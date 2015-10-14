package org.droidplanner.android.data;

import com.o3dr.services.android.lib.coordinate.LatLong;

/**
 * Created by Zack on 15/10/7.
 */
public class GpsModel {
    private double mGpsEph;
    private int mSatCount;
    private int mFixType;
    private Coord2D mPosition;

    public static final String LOCK_2D = "2D";
    public static final String LOCK_3D = "3D";
    public static final String NO_FIX = "NoFix";

    private final static int LOCK_2D_TYPE = 2;
    private final static int LOCK_3D_TYPE = 3;

    private static GpsModel mGpsModel = new GpsModel();;


    private GpsModel() {
    }

    public static GpsModel getGpsModel() {
        return mGpsModel;
    }

    public Coord2D getCoord2DPosition() {
        return mPosition;
    }

    public void setPosition(double latitude, double longitude) {
        boolean positionUpdated = false;
        if(this.mPosition == null){
            this.mPosition = new Coord2D(latitude, longitude);
        }
        else if(this.mPosition.getLat() != latitude || this.mPosition.getLng() != longitude){
            this.mPosition.set(latitude, longitude);
        }
    }

    public int getFixType() {
        return mFixType;
    }

    public void setFixType(int mFixType) {
        this.mFixType = mFixType;
    }

    public double getGpsEph() {
        return mGpsEph;
    }

    public void setGpsEph(double mGpsEph) {
        this.mGpsEph = (mGpsEph/100.0);
    }

    public int getSatCount() {
        return mSatCount;
    }

    public void setSatCount(int mSatCount) {
        this.mSatCount = mSatCount;
    }

    public boolean isPositionValid() {
        return (mPosition != null);
    }

    public LatLong getPosition(){
        LatLong dronePosition = this.isPositionValid()
                ? new LatLong(this.getCoord2DPosition().getLat(), this.getCoord2DPosition().getLng())
                : null;
        return dronePosition;
    }

    public String getFixStatus(){
        switch (mFixType) {
            case LOCK_2D_TYPE:
                return LOCK_2D;

            case LOCK_3D_TYPE:
                return LOCK_3D;

            default:
                return NO_FIX;
        }
    }

}
