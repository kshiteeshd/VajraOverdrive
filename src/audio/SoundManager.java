package audio;

import save.SettingsManager;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SoundManager — singleton audio controller.
 *
 * SFX  : loaded once into Clip objects, replayed on demand.
 *        Missing files are silently skipped — no crash.
 * Music: streamed on a background thread so the game loop never blocks.
 *        Loops until stopMusic() or a new playMusic() call.
 *
 * Volume is read from SettingsManager on every play() call so
 * in-game settings changes take effect immediately.
 */
public class SoundManager {

    // ── Singleton ─────────────────────────────────────────────────────
    private static SoundManager instance;
    public static SoundManager get() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    // ── SFX cache ─────────────────────────────────────────────────────
    private final Map<String, Clip> sfxCache = new HashMap<>();

    // ── Music state ───────────────────────────────────────────────────
    private Thread          musicThread;
    private volatile String pendingMusic  = null;
    private volatile boolean stopMusic    = false;
    private volatile boolean musicRunning = false;
    private String          currentMusic  = null;

    // ── SFX root ──────────────────────────────────────────────────────
    private static final String SFX_DIR   = "resources/audio/sfx/";
    private static final String MUSIC_DIR = "resources/audio/music/";

    // ── Private constructor ───────────────────────────────────────────
    private SoundManager() {}

    // ─────────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────────

    /** Play a short SFX clip by name (no extension). */
    public void play(String name) {
        if (!SettingsManager.get().soundEnabled) return;

        Clip clip = getClip(name);
        if (clip == null) return;

        float vol = SettingsManager.get().soundVolume / 100f;
        setClipVolume(clip, vol);

        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    /** Start looping background music by name (no extension). */
    public void playMusic(String name) {
        if (!SettingsManager.get().musicEnabled) return;
        if (name.equals(currentMusic) && musicRunning) return;

        pendingMusic = name;
        stopMusic    = true;   // signal current thread to stop

        if (musicThread == null || !musicThread.isAlive()) {
            startMusicThread();
        }
    }

    /** Stop background music immediately. */
    public void stopMusic() {
        stopMusic    = true;
        pendingMusic = null;
        currentMusic = null;
    }

    // ─────────────────────────────────────────────────────────────────
    //  INTERNAL — SFX
    // ─────────────────────────────────────────────────────────────────

    private Clip getClip(String name) {
        if (sfxCache.containsKey(name)) return sfxCache.get(name);

        File f = new File(SFX_DIR + name + ".wav");
        if (!f.exists()) {
            sfxCache.put(name, null);   // cache miss so we don't keep retrying
            return null;
        }

        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            sfxCache.put(name, clip);
            return clip;
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            System.err.println("SoundManager: could not load sfx/" + name + " — " + e.getMessage());
            sfxCache.put(name, null);
            return null;
        }
    }

    private static void setClipVolume(Clip clip, float vol) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        // vol is 0.0–1.0; map to decibels
        float dB = vol <= 0f ? gain.getMinimum()
                             : 20f * (float) Math.log10(vol);
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
    }

    // ─────────────────────────────────────────────────────────────────
    //  INTERNAL — Music thread
    // ─────────────────────────────────────────────────────────────────

    private void startMusicThread() {
        musicThread = new Thread(() -> {
            while (true) {
                String toPlay = pendingMusic;
                if (toPlay == null) { musicRunning = false; return; }

                pendingMusic  = null;
                stopMusic     = false;
                currentMusic  = toPlay;
                musicRunning  = true;

                File f = new File(MUSIC_DIR + toPlay + ".wav");
                if (!f.exists()) {
                    musicRunning = false;
                    return;
                }

                try (AudioInputStream raw = AudioSystem.getAudioInputStream(f)) {
                    AudioFormat    fmt  = raw.getFormat();
                    DataLine.Info  info = new DataLine.Info(SourceDataLine.class, fmt);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(fmt);

                    float vol = SettingsManager.get().musicVolume / 100f;
                    if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        FloatControl gain = (FloatControl)
                                line.getControl(FloatControl.Type.MASTER_GAIN);
                        float dB = vol <= 0f ? gain.getMinimum()
                                             : 20f * (float) Math.log10(vol);
                        gain.setValue(Math.max(gain.getMinimum(),
                                Math.min(gain.getMaximum(), dB)));
                    }

                    line.start();
                    byte[] buf = new byte[4096];
                    int    n;

                    // Stream loop — restart file when it ends unless interrupted
                    outer:
                    while (true) {
                        AudioInputStream loop = AudioSystem.getAudioInputStream(f);
                        while ((n = loop.read(buf)) != -1) {
                            if (stopMusic || pendingMusic != null) break outer;
                            line.write(buf, 0, n);
                        }
                        loop.close();
                        if (stopMusic || pendingMusic != null) break;
                    }

                    line.drain();
                    line.close();

                } catch (Exception e) {
                    System.err.println("SoundManager: music error — " + e.getMessage());
                }

                // If a new track was queued, loop back and play it
                if (pendingMusic == null) { musicRunning = false; return; }
            }
        }, "MusicThread");

        musicThread.setDaemon(true);
        musicThread.start();
    }
}