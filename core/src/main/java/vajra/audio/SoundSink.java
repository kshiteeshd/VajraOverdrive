package vajra.audio;

/** What the simulation tells the ears. World only NAMES things that happened (an id from {@link Snd}, where, and two numbers); it never imports sound,
 *  never waits for it, and runs the same with no sink at all (the tests, the headless game). */
public interface SoundSink {
    void event(int ev, float x, float y, int a, float b);

    /** Someone speaks in a bubble: the sung line (docs/SOUND_PLAN.md 1.6). */
    void say(String speaker, String text);
}
