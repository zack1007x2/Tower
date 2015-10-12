package org.droidplanner.android.activities;

import android.util.Log;
import android.widget.TextView;

import com.MAVLink.common.msg_rc_channels_override;

import org.droidplanner.android.widgets.CusJoystickView;
import org.droidplanner.android.widgets.JoystickView;

import moremote.moapp.MoApplication;
import moremote.moapp.activity.BaseActivity;

/**
 * Created by Zack on 15/10/12.
 */
public class JoystickControlActivity extends BaseActivity {

    protected JoystickView joystick_right;
    protected CusJoystickView joystick_left;
    protected short cur_lift, cur_rotate, ch1, ch2;
    protected Thread defaultLoop;
    protected boolean isTouching_left, isTouching_right;
    protected TextView tvRL, tvFB, tvPower, tvRotate;
    protected boolean startUp, isLock;

    private String MsgTitle = MoApplication.XMPPCommand.DRONE;

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
            sentRCcmd(ch1,ch2,cur_lift,cur_rotate);
        }

        @Override
        public void onNotTouch() {
            isTouching_right = false;
        }
    };

    protected CusJoystickView.OnJoystickMoveListener mLeftOnJoystickMoveListener = new CusJoystickView.OnJoystickMoveListener() {

        @Override
        public void onValueChanged(int angle, int power, int direction) {
            isTouching_left = true;
            if (power >= 99) {
                power = 100;
            }
            switch (direction) {
                case JoystickView.FRONT:
                    cur_lift = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    cur_rotate = 1500;
                    break;
                case JoystickView.FRONT_RIGHT:
                    cur_lift = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    cur_rotate = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.RIGHT:
                    cur_lift = 1500;
                    cur_rotate = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.RIGHT_BOTTOM:
                    cur_lift = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    cur_rotate = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    if (cur_lift == 1000 && cur_rotate == 2000) {
                        isLock = false;
                    }
                    break;
                case JoystickView.BOTTOM:
                    cur_lift = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    cur_rotate = 1500;
                    break;
                case JoystickView.BOTTOM_LEFT:
                    cur_lift = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    cur_rotate = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    if (cur_lift == 1000 && cur_rotate == 1000) {
                        isLock = true;
                    }
                    break;
                case JoystickView.LEFT:
                    cur_lift = 1500;
                    cur_rotate = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    break;
                case JoystickView.LEFT_FRONT:
                    cur_lift = (short) (1500 + (Integer.valueOf(5 * power).shortValue()));
                    cur_rotate = (short) (1500 - (Integer.valueOf(5 * power).shortValue()));
                    break;
                default:
            }
            if (!isTouching_right) {
                ch1 = 1500;
                ch2 = 1500;
            }
            sentRCcmd(ch1,ch2,cur_lift,cur_rotate);

        }

        @Override
        public void onNotTouch() {
            isTouching_left = false;
        }
    };


    @Override
    public void onResume() {
        super.onResume();
        isLock = true;
        startUp = true;
        defaultLoop = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    if(!isLock){
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (!isTouching_left && !isTouching_right) {
                            ch1 = 1500;
                            ch2 = 1500;
                            cur_rotate = 1500;

                            if (startUp) {
                                cur_lift = 1000;
                                startUp = false;
                            }
                            sentRCcmd(ch1, ch2, cur_lift, cur_rotate);
                        }
                    }
                }
            }
        });
        defaultLoop.start();
    }

    private void sentRCcmd(short ch1,short ch2,short ch3,short ch4){
        //drone:MSG_ID@.....
        String Msg_RC = MsgTitle+msg_rc_channels_override
                .MAVLINK_MSG_ID_RC_CHANNELS_OVERRIDE+ "@"+ch1+
                "@"+ch2+ "@"+ch3+ "@"+ch4;
        Log.d("Zack", Msg_RC);
        xmppConnection.sendMessage(MoApplication.CONNECT_TO, Msg_RC);
//        Log.d("Zack", "前後 = " + ch2 + " 左右 = " + ch1 + " 升力 = " + ch3 + " 自轉 = " + ch4);
    }
}
