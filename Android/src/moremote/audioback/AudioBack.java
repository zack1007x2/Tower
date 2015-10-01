package moremote.audioback;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import moremote.moapp.IPCam;
import moremote.moapp.MoApplication;

/**
* Created by Ray on 2015/5/8.
*/
public class AudioBack extends Thread {

    private MediaCodec encoder;
    private AudioRecord recorder;
    private int sampleRate = 0;
    private int bufferSize = 0;
    private int cameraType = -1;
    private short audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private short channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private boolean stop = false;
    private boolean init = false;
    private int connectionType = -1;
    private AudioBackInterface audioBackInterface;

    public AudioBack(IPCam ipCam, AudioBackInterface audioBackInterface){

        this.sampleRate = ipCam.audiobackSamplerate;
        this.cameraType = ipCam.cameraType;
        this.connectionType = ipCam.connectionType;
        this.audioBackInterface = audioBackInterface;

        if(setRecorder()) {
            init = setEncoder();
        }
    }



    private boolean setEncoder() {
        try {
            encoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64 * 1024);//AAC-LC 64kbps
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        return true;
    }
    private boolean setRecorder(){
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        if (bufferSize != AudioRecord.ERROR_BAD_VALUE){
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);
        }
        return true;
    }

    public void encode(byte[] input, int length){
        ByteBuffer[] inputBuffers;
        ByteBuffer[] outputBuffers;

        ByteBuffer inputBuffer;
        ByteBuffer outputBuffer;

        MediaCodec.BufferInfo bufferInfo;
        int inputBufferIndex;
        int outputBufferIndex;

        byte[] outData;

        inputBuffers = encoder.getInputBuffers();
        outputBuffers = encoder.getOutputBuffers();
        inputBufferIndex = encoder.dequeueInputBuffer(0);
        if (inputBufferIndex >= 0) {
            inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(input, 0, length);
            encoder.queueInputBuffer(inputBufferIndex, 0, length, System.nanoTime() / 1000, 0);
        }

        bufferInfo = new MediaCodec.BufferInfo();
        outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);

        byte[] data = null;

        while (outputBufferIndex >= 0) {
            outputBuffer = outputBuffers[outputBufferIndex];

            data = new byte[bufferInfo.size];
            outputBuffer.get(data, 0, bufferInfo.size);
            audioBackInterface.sendAudioBack(data, data.length, cameraType, connectionType);
//            outputBuffer.position(bufferInfo.offset);
//            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
//            outputBuffer.get(output, 0, bufferInfo.size);
            encoder.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
        }

    }

    public void release(){
        if(stop == true) {
            recorder.release();
            encoder.release();
        }
        else {
        }
    }


    public void run()
    {
        recorder.startRecording();
        encoder.start();
        int read;
        byte[] buffer1 = new byte[bufferSize];
        byte[] afterEncode = new byte[400];
        boolean isRecording = true;
        while (!interrupted()) {
            read = recorder.read(buffer1, 0, bufferSize);
            if(cameraType == MoApplication.cameraType.LinuxCam) {
                audioBackInterface.sendAudioBack(buffer1, read, cameraType, connectionType);
            }
            else if(cameraType == MoApplication.cameraType.AndroidCam) {
                encode(buffer1, read);
            }
        }
        Log.e("Ray", "after interrupt");
        recorder.stop();
        encoder.stop();
        stop = true;
        Log.e("Ray", "run last line");
    }
}
