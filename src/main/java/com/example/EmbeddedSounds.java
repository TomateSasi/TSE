package com.example;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class EmbeddedSounds {

    public static final java.util.LinkedHashMap<String, String> SOUNDS =
            new java.util.LinkedHashMap<>();

    static {
        SOUNDS.put("yippee", "/assets/tse/sounds/yippee.wav");
        SOUNDS.put("Anvil",  "/assets/tse/sounds/Anvil.wav");
    }

    private static final String SECRET_PATH = "/assets/tse/sounds/ngg.ogg";
    private static final String APRIL_PATH  = "/assets/tse/sounds/fart.wav";

    public static void play(String name, int volume) {
        String path = SOUNDS.get(name);
        if (path == null) return;
        playResource(path, volume);
    }

    public static void playSecret() { playResource(SECRET_PATH, 100); }

    public static boolean secretExists() {
        return EmbeddedSounds.class.getResourceAsStream(SECRET_PATH) != null;
    }

    static void playAprilFools(int volume) { playResource(APRIL_PATH, volume); }

    static boolean aprilFoolsExists() {
        return EmbeddedSounds.class.getResourceAsStream(APRIL_PATH) != null;
    }

    public static void playResource(String resourcePath, int volume) {
        InputStream raw = EmbeddedSounds.class.getResourceAsStream(resourcePath);
        if (raw == null) return;
        final float gainFactor = volume / 100.0f;
        new Thread(() -> {
            try {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                        new BufferedInputStream(raw, 65536));
                AudioFormat srcFmt = audioIn.getFormat();
                AudioInputStream pcmIn;

                if (srcFmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                        || srcFmt.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
                    pcmIn = audioIn;
                } else {
                    int ch = srcFmt.getChannels() > 0 ? srcFmt.getChannels() : 2;
                    float sr = srcFmt.getSampleRate() > 0 ? srcFmt.getSampleRate() : 44100f;
                    AudioFormat pcmFmt = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            sr, AudioSystem.NOT_SPECIFIED,
                            ch, AudioSystem.NOT_SPECIFIED,
                            sr, false);
                    pcmIn = AudioSystem.getAudioInputStream(pcmFmt, audioIn);
                }

                AudioFormat decoded = pcmIn.getFormat();
                int ch  = decoded.getChannels()   > 0 ? decoded.getChannels()   : 2;
                float sr = decoded.getSampleRate() > 0 ? decoded.getSampleRate() : 44100f;
                AudioFormat playFmt = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED, sr, 16, ch, ch * 2, sr, false);
                AudioInputStream playIn = new AudioInputStream(pcmIn, playFmt, pcmIn.getFrameLength());

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, playFmt);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(playFmt);
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = gainFactor > 0 ? (float)(20.0 * Math.log10(gainFactor)) : gain.getMinimum();
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                }
                line.start();
                byte[] buf = new byte[4096]; int read;
                while ((read = playIn.read(buf)) != -1) line.write(buf, 0, read);
                line.drain(); line.close(); playIn.close();
            } catch (Exception ignored) {}
        }, "TSE-Embedded-Player").start();
    }
}
