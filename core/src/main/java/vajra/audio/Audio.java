package vajra.audio;

/** The one engine every screen shares (a headless run builds one that does nothing). */
public final class Audio {
    private static AudioEngine engine;

    public static synchronized AudioEngine get() {
        if (engine == null) engine = new AudioEngine();
        return engine;
    }

    public static synchronized void dispose() {
        if (engine != null) engine.dispose();
        engine = null;
    }

    private Audio() {}
}
