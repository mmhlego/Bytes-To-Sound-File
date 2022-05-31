package lt.demo.stethoscope.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import androidx.annotation.NonNull;

public class AudioTrackPlayer {

//    /**
//     * Audio sample rate (HZ).
//     * 8000
//     * 11025
//     * 16000
//     * 22050
//     * 32000
//     * 44100
//     * 48000
//     * 88200
//     * 96000
//     * 176400
//     * 192000
//     * 352800
//     * 384000
//     */
//    private final static int SAMPLE_RATE_HZ = 8000;

    private final static String TAG = "AudioTrackPlayer";

    private final AudioTrack mAudioTrack;

    public AudioTrackPlayer() {
        this(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }

    public AudioTrackPlayer(int sampleRate, int channelConfig, int audioEncoding) {
        int streamType = AudioManager.STREAM_MUSIC;
        int mode = AudioTrack.MODE_STREAM;
//        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioEncoding);
        int bufferSizeInBytes = 256;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mAudioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(streamType)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(audioEncoding)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build())
                    .setTransferMode(mode)
                    .setBufferSizeInBytes(bufferSizeInBytes)
                    .build();
        } else {
            mAudioTrack = new AudioTrack(streamType
                    , sampleRate
                    , channelConfig
                    , audioEncoding
                    , bufferSizeInBytes
                    , mode);
        }
    }

    public void play() {
        mAudioTrack.play();
    }

    public void write(@NonNull short[] audioData) {
        mAudioTrack.write(audioData, 0, audioData.length);
    }

    public void cancel() {
        mAudioTrack.stop();
        mAudioTrack.release();
    }
}
