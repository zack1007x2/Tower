package org.droidplanner.android.data;

/**
 * Created by Zack on 15/10/7.
 */
public class SignalModel {
    private int rxerrors;
    private int fixed;
    private int txbuf;
    private double rssi;
    private double remrssi;
    private double noise;
    private double remnoise;
    private double signalStrength;

    private static SignalModel mSignalModel = new SignalModel();;


    private SignalModel() {
    }

    public static SignalModel getSignalModel(){
        return mSignalModel;
    }

    public int getRxerrors() {
        return rxerrors;
    }

    public void setRxerrors(int rxerrors) {
        this.rxerrors = rxerrors;
    }

    public int getFixed() {
        return fixed;
    }

    public void setFixed(int fixed) {
        this.fixed = fixed;
    }

    public double getNoise() {
        return noise;
    }

    public void setNoise(double noise) {
        this.noise = noise;
    }

    public double getRemnoise() {
        return remnoise;
    }

    public void setRemnoise(double remnoise) {
        this.remnoise = remnoise;
    }

    public double getRemrssi() {
        return remrssi;
    }

    public void setRemrssi(double remrssi) {
        this.remrssi = remrssi;
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public double getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(double signalStrength) {
        this.signalStrength = signalStrength;
    }

    public int getTxbuf() {
        return txbuf;
    }

    public void setTxbuf(int txbuf) {
        this.txbuf = txbuf;
    }
    public double getFadeMargin() {
        return rssi - noise;
    }

    public double getRemFadeMargin() {
        return remrssi - remnoise;
    }
}
