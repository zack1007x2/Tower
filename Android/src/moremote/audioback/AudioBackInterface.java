package moremote.audioback;

/**
 * Created by Ray on 2015/6/10.
 */
public interface AudioBackInterface {
    public boolean sendAudioBack(byte[] data, int length, int cameraType, int connectionType);
}
