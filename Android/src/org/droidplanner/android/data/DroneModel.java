package org.droidplanner.android.data;

/**
 * Created by Zack on 15/10/5.
 */
public class DroneModel {
    private long time_boot_ms;

    private float roll;

    private float pitch;

    private float yaw;

    private float rollspeed;

    private float pitchspeed;

    private float yawspeed;

    private double verticalSpeed; // m/s
    private double groundSpeed; // m/s
    private double airSpeed; // m/s


    private double batteryVoltage;
    private double batteryRemain;
    private double batteryCurrent;
    private Double batteryDischarge;

    private static DroneModel mDroneModel = new DroneModel();;


    private DroneModel() {
    }

    public static DroneModel getDroneModel() {
        return mDroneModel;
    }

    public double getAirSpeed() {
        return airSpeed;
    }

    public void setAirSpeed(double airSpeed) {
        this.airSpeed = airSpeed;
    }

    public double getBatteryCurrent() {
        return batteryCurrent;
    }

    public void setBatteryCurrent(double batteryCurrent) {
        this.batteryCurrent = batteryCurrent;
    }

    public Double getBatteryDischarge() {
        return batteryDischarge;
    }

    public void setBatteryDischarge(Double batteryDischarge) {
        this.batteryDischarge = batteryDischarge;
    }

    public double getBatteryRemain() {
        return batteryRemain;
    }

    public void setBatteryRemain(double batteryRemain) {
        this.batteryRemain = batteryRemain;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public double getGroundSpeed() {
        return groundSpeed;
    }

    public void setGroundSpeed(double groundSpeed) {
        this.groundSpeed = groundSpeed;
    }

    public DroneModel getmDroneModel() {
        return mDroneModel;
    }

    public void setmDroneModel(DroneModel mDroneModel) {
        this.mDroneModel = mDroneModel;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getPitchspeed() {
        return pitchspeed;
    }

    public void setPitchspeed(float pitchspeed) {
        this.pitchspeed = pitchspeed;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getRollspeed() {
        return rollspeed;
    }

    public void setRollspeed(float rollspeed) {
        this.rollspeed = rollspeed;
    }

    public long getTime_boot_ms() {
        return time_boot_ms;
    }

    public void setTime_boot_ms(long time_boot_ms) {
        this.time_boot_ms = time_boot_ms;
    }

    public double getVerticalSpeed() {
        return verticalSpeed;
    }

    public void setVerticalSpeed(double verticalSpeed) {
        this.verticalSpeed = verticalSpeed;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getYawspeed() {
        return yawspeed;
    }

    public void setYawspeed(float yawspeed) {
        this.yawspeed = yawspeed;
    }
}
