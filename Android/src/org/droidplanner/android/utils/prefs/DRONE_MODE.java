package org.droidplanner.android.utils.prefs;

import org.droidplanner.android.utils.collection.HashBiMap;

/**
 * Created by Zack on 15/10/1.
 */
public class DRONE_MODE {
    public final static int MODE_STABLE = 0;
    public final static int MODE_ACRO = 1;
    public final static int MODE_ALT_HOLD = 2;
    public final static int MODE_AUTO = 3;
    public final static int MODE_GUIDED = 4;
    public final static int MODE_LOITER = 5;
    public final static int MODE_RTL = 6;
    public final static int MODE_CIRCLE = 7;
    public final static int MODE_LAND = 9;
    public final static int MODE_DRIFT = 11;
    public final static int MODE_SPORT = 13;
    public final static int MODE_FLIP = 14;
    public final static int MODE_AUTOTUNE = 15;
    public final static int MODE_POSHOLD = 16;
    public final static int MODE_BRAKE = 17;

    private static HashBiMap mDronePositionMap;
    private static DRONE_MODE instance = null;

    public static DRONE_MODE getInstance(){
        if(instance == null || mDronePositionMap == null) {
            instance = new DRONE_MODE();
            mDronePositionMap = new HashBiMap();
            mDronePositionMap.put(MODE_STABLE,0);
            mDronePositionMap.put(MODE_ACRO,1);
            mDronePositionMap.put(MODE_ALT_HOLD,2);
            mDronePositionMap.put(MODE_AUTO,3);
            mDronePositionMap.put(MODE_GUIDED,4);
            mDronePositionMap.put(MODE_LOITER,5);
            mDronePositionMap.put(MODE_RTL,6);
            mDronePositionMap.put(MODE_CIRCLE,7);
            mDronePositionMap.put(MODE_LAND,8);
            mDronePositionMap.put(MODE_DRIFT,9);
            mDronePositionMap.put(MODE_SPORT,10);
            mDronePositionMap.put(MODE_FLIP,11);
            mDronePositionMap.put(MODE_AUTOTUNE,12);
            mDronePositionMap.put(MODE_POSHOLD,13);
            mDronePositionMap.put(MODE_BRAKE,14);
        }
        return instance;
    }

    private DRONE_MODE() {

    }


    public static HashBiMap getDronePositionMap(){
//        if(mDronePositionMap==null||mDronePositionMap.keySet().size()!=15){
//            mDronePositionMap = new HashBiMap();
//            mDronePositionMap.put(MODE_STABLE,0);
//            mDronePositionMap.put(MODE_ACRO,1);
//            mDronePositionMap.put(MODE_ALT_HOLD,2);
//            mDronePositionMap.put(MODE_AUTO,3);
//            mDronePositionMap.put(MODE_GUIDED,4);
//            mDronePositionMap.put(MODE_LOITER,5);
//            mDronePositionMap.put(MODE_RTL,6);
//            mDronePositionMap.put(MODE_CIRCLE,7);
//            mDronePositionMap.put(MODE_LAND,8);
//            mDronePositionMap.put(MODE_DRIFT,9);
//            mDronePositionMap.put(MODE_SPORT,10);
//            mDronePositionMap.put(MODE_FLIP,11);
//            mDronePositionMap.put(MODE_AUTOTUNE,12);
//            mDronePositionMap.put(MODE_POSHOLD,13);
//            mDronePositionMap.put(MODE_BRAKE,14);
//        }
        return mDronePositionMap;
    }
}