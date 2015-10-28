package org.droidplanner.android.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.MAVLink.common.msg_rc_channels_override;

import org.droidplanner.android.R;
import org.droidplanner.android.widgets.joyStick.JoystickView;
import org.droidplanner.android.widgets.joyStick.LiftView;
import org.droidplanner.android.widgets.joyStick.RotateView;

import moremote.moapp.MoApplication;
import moremote.moapp.activity.BaseActivity;

/**
 * Created by Zack on 15/10/12.
 */
public class JoystickControlActivity extends BaseActivity {

    protected JoystickView joystick_right;
    protected RotateView joystick_rotate;
    protected LiftView joystick_lift;



    private static final int DEFAULT_PACKET = 1;
    private static final int DIRECTION_PACKET = 2;
    private static final int PRINT_PARAM = 3;
    private static final int DISABLE_CONTROL = 4;
    private static final int MODE_CHANGE = 5;
    protected int cur_lift, cur_rotate, ch1, ch2;
    protected TextView tvRL, tvFB, tvPower, tvRotate;
    protected Thread defaultLoop;
    protected boolean isTouching_left, isTouching_right;
    protected boolean startUp, isLock;
    protected boolean keeploopong;
    protected ToggleButton tbRC;

    private String MsgTitle = MoApplication.XMPPCommand.DRONE;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DEFAULT_PACKET:
                    Log.d("Zack", "前後 = " + ch2 + " 左右 = " + ch1 + " 升力 = " + cur_lift + " 自轉 = "+ cur_rotate);
                    sentRCcmd(ch1, ch2, cur_lift, cur_rotate);
                    this.sendEmptyMessage(PRINT_PARAM);
                    break;
                case DIRECTION_PACKET:
                    Log.d("Zack", "前後 = " + ch2 + " 左右 = " + ch1 + " 升力 = " + cur_lift + " 自轉 = "+ cur_rotate);
                    sentRCcmd(ch1, ch2, cur_lift, cur_rotate);
                    this.sendEmptyMessage(PRINT_PARAM);
                    break;
                case PRINT_PARAM:
                    tvRL.setText("Roll\n" + String.valueOf(ch1));
                    tvFB.setText("Pitch\n" + String.valueOf(ch2));
                    tvPower.setText("Throttle\n" + String.valueOf(cur_lift));
                    tvRotate.setText("Yaw\n" + String.valueOf(cur_rotate));
                    break;
                case DISABLE_CONTROL:
                    Log.d("Zack", "前後 = " + 0 + " 左右 = " + 0 + " 升力 = " + 0 + " 自轉 = "+ 0);
                    sentRCcmd(0,0,0,0);
                    tvRL.setText("");
                    tvFB.setText("");
                    tvPower.setText("");
                    tvRotate.setText("");
                    break;
            }
        }
    };


    protected JoystickView.OnJoystickMoveListener mRightOnJoystickMoveListener = new JoystickView.OnJoystickMoveListener() {
        @Override
        public void onValueChanged(int angle, int power, int direction) {
            isTouching_right = true;
            if (power >= 99) {
                power = 100;
            }
            switch (direction) {
                case JoystickView.FRONT:
                    ch1 = 1500;
                    ch2 = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.FRONT_RIGHT:
                    ch1 = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    ch2 = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.RIGHT:
                    ch1 = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    ch2 = 1500;
                    break;
                case JoystickView.RIGHT_BOTTOM:
                    ch1 = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    ch2 = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.BOTTOM:
                    ch1 = 1500;
                    ch2 = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.BOTTOM_LEFT:
                    ch1 = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    ch2 = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.LEFT:
                    ch1 = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    ch2 = 1500;
                    break;
                case JoystickView.LEFT_FRONT:
                    ch1 = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    ch2 = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    break;
                default:
                    ch1 = 1500;
                    ch2 = 1500;
            }

            if (!isTouching_left) {
                cur_rotate = 1500;
            }
            if (tbRC.isChecked()) mHandler.sendEmptyMessage(DIRECTION_PACKET);
        }

        @Override
        public void onNotTouch() {
            isTouching_right = false;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        keeploopong = true;
        isLock = true;
        startUp = true;
        defaultLoop = new Thread(new Runnable() {
            public void run() {
                while (keeploopong) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!isTouching_left && !isTouching_right && tbRC.isChecked()) {
                        ch1 = 1500;
                        ch2 = 1500;
                        cur_rotate = 1500;

                        if (startUp) {
                            cur_lift = 1000;
                            startUp= false;
                        }
                        mHandler.sendEmptyMessage(DEFAULT_PACKET);

                    }
                }

            }
        });
        defaultLoop.start();
    }



    LiftView.OnLiftPowerMoveListener mLiftPowerMoveListener = new LiftView.OnLiftPowerMoveListener(){
        @Override
        public void onValueChanged(int angle, int power, int direction) {
            isTouching_left = true;
            if (power >= 99) {
                power = 100;
            }
            switch (direction) {
                case JoystickView.FRONT:
                    cur_lift = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.FRONT_RIGHT:
                    cur_lift = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.RIGHT:
                    cur_lift = 1500;
                    break;
                case JoystickView.RIGHT_BOTTOM:
                    cur_lift = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    if (cur_lift == 1000 && cur_rotate == 2000) {
                        isLock = false;
                    }
                    break;
                case JoystickView.BOTTOM:
                    cur_lift = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.BOTTOM_LEFT:
                    cur_lift = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    if (cur_lift == 1000 && cur_rotate == 1000) {
                        isLock = true;
                    }
                    break;
                case JoystickView.LEFT:
                    cur_lift = 1500;
                    break;
                case JoystickView.LEFT_FRONT:
                    cur_lift = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                default:
            }
            if (!isTouching_right) {
                ch1 = 1500;
                ch2 = 1500;
            }
            if (tbRC.isChecked()) mHandler.sendEmptyMessage(DIRECTION_PACKET);

        }

        @Override
        public void onNotTouch() {
            isTouching_left = false;
        }
    };

    RotateView.OnRotateListener mRotateListener = new RotateView.OnRotateListener() {
        @Override
        public void onValueChanged(int angle, int power, int direction) {
            isTouching_left = true;
            if (power >= 99) {
                power = 100;
            }
            switch (direction) {
                case JoystickView.FRONT:
                    cur_rotate = 1500;
                    break;
                case JoystickView.FRONT_RIGHT:
                    cur_rotate = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.RIGHT:
                    cur_rotate = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.RIGHT_BOTTOM:
                    cur_rotate = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    if (cur_lift == 1000 && cur_rotate == 2000) {
                        isLock = false;
                    }
                    break;
                case JoystickView.BOTTOM:
                    cur_rotate = 1500;
                    break;
                case JoystickView.BOTTOM_LEFT:
                    cur_rotate = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    if (cur_lift == 1000 && cur_rotate == 1000) {
                        isLock = true;
                    }
                    break;
                case JoystickView.LEFT:
                    cur_rotate = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.LEFT_FRONT:
                    cur_rotate = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    break;
                default:
            }
            if (!isTouching_right) {
                ch1 = 1500;
                ch2 = 1500;
            }
            if (tbRC.isChecked()) mHandler.sendEmptyMessage(DIRECTION_PACKET);

        }

        @Override
        public void onNotTouch() {
            isTouching_left = false;
        }
    };

    private void sentRCcmd(int ch1,int ch2,int ch3,int ch4){
        //drone:MSG_ID@.....
        String Msg_RC = MsgTitle+msg_rc_channels_override
                .MAVLINK_MSG_ID_RC_CHANNELS_OVERRIDE+ "@"+ch1+
                "@"+ch2+ "@"+ch3+ "@"+ch4;
        Log.d("Zack", Msg_RC);
        xmppConnection.sendMessage(MoApplication.CONNECT_TO, Msg_RC);
//        Log.d("Zack", "前後 = " + ch2 + " 左右 = " + ch1 + " 升力 = " + ch3 + " 自轉 = " + ch4);
    }

    @Override
    public void onPause() {
        super.onPause();
        keeploopong = false;
    }
    CompoundButton.OnCheckedChangeListener mToggleChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                mHandler.sendEmptyMessage(DISABLE_CONTROL);
            }
            cur_lift = 1000;
        }
    };
}
