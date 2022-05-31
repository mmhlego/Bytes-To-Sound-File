package lt.demo.stethoscope.utils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;

public class PlayRecordThread extends Thread {

    private static final String TAG = "PlayRecordThread";
    private static final int LEN = 512;
    private final File mFile;
    private AudioTrackPlayer mAudioTrackPlayer;
    private final Callback mCallback;
    private boolean isPlaying;

    public PlayRecordThread(File file, Callback callback) {
        mFile = file;
        this.mCallback = callback;
        mAudioTrackPlayer = new AudioTrackPlayer();
    }

    public PlayRecordThread(String filePath, Callback callback) {
        mFile = new File(filePath);
        this.mCallback = callback;
    }

    @Override
    public void run() {
        super.run();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mFile);
            byte[] bytes = new byte[LEN];
            int len = 0;
            if (mAudioTrackPlayer != null) {
                mAudioTrackPlayer.play();
            }
            while (isPlaying && len > -1) {
                len = fis.read(bytes, 0, LEN);
                if (len > 0) {
                    if (len < LEN) {
                        byte[] old = bytes.clone();
                        bytes = new byte[len];
                        System.arraycopy(old, 0, bytes, 0, len);
                    }
                    short[] buffer = Utils.bytesToShort(bytes);
                    if (mAudioTrackPlayer != null) {
                        mAudioTrackPlayer.write(buffer);
                    }
                    if (mCallback != null) {
                        mCallback.onOutputBuffer(buffer);
                    }
                }
            }
            isPlaying = false;
        } catch (Exception e) {
            Log.e(TAG, "Exception0", e);
        }
        try {
            if (fis != null) {
                fis.close();
            }
            if (mAudioTrackPlayer != null) {
                mAudioTrackPlayer.cancel();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception2", e);
        }
        if (mCallback != null) {
            mCallback.onCompleted();
        }
    }

    @Override
    public synchronized void start() {
        isPlaying = true;
        super.start();
    }

    @Override
    public void interrupt() {
        isPlaying = false;
        super.interrupt();
    }

    public interface Callback {

        void onOutputBuffer(short[] buffer);

        void onCompleted();
    }

}
