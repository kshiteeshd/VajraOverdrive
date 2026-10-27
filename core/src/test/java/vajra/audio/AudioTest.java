package vajra.audio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;
import vajra.weapon.WeaponDef;
import vajra.world.Pickup;
import vajra.world.World;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** The sound plan's contract: every sound the engine can ask for exists on disk, speech is sung by a fixed table, the world reports what happens, and with no device the engine is inert. */
class AudioTest {
    private static final ShipControls IDLE = new ShipControls() {
        public float thrustX() { return 0; }
        public float thrustY() { return 0; }
        public float rotate() { return 0; }
        public boolean fire() { return false; }
        public boolean boost() { return false; }
        public boolean shield() { return false; }
        public boolean mouseAim() { return false; }
        public float aimX() { return 0; }
        public float aimY() { return 0; }
    };

    private static File assets() { return new File("../assets/audio"); }

    @Test
    void everySoundTheEngineAsksForIsOnDisk() {
        List<String> missing = new ArrayList<>();
        for (String n : AudioEngine.sfxNames()) if (!new File(assets(), "sfx/" + n + ".ogg").exists()) missing.add("sfx/" + n);
        for (String n : AudioEngine.musicNames()) if (!new File(assets(), "music/" + n + ".ogg").exists()) missing.add("music/" + n);
        assertTrue(missing.isEmpty(), "missing: " + missing);
        assertTrue(AudioEngine.sfxNames().size() > 280);
    }

    @Test
    void everyFileIsOfPlausibleSize() {
        for (String n : AudioEngine.musicNames()) {
            File f = new File(assets(), "music/" + n + ".ogg");
            assertTrue(f.length() > 20_000, n + " is too small to be music: " + f.length());
        }
        for (String n : AudioEngine.sfxNames()) assertTrue(new File(assets(), "sfx/" + n + ".ogg").length() > 800, n);
    }

    @Test
    void speechIsSungByAFixedTable() {
        assertEquals(0, AudioEngine.degreeOf('A'));
        assertEquals(3, AudioEngine.degreeOf('O'));
        assertEquals(-1, AudioEngine.degreeOf('K'));
        assertEquals("bala", AudioEngine.speakerKey("BALA"));
        assertEquals("maya", AudioEngine.speakerKey("THE NINTH MAYA"));
        assertEquals("indra", AudioEngine.speakerKey("INDRA"));
        assertEquals("voice", AudioEngine.speakerKey(null));
    }

    @Test
    void withNoDeviceTheEngineIsInertAndNeverThrows() {
        AudioEngine a = new AudioEngine();
        assertFalse(a.enabled(), "tests run with no audio device");
        World w = new World();
        a.event(Snd.FIRE, 0, 0, 3, 1f);
        a.say("BALA", "THIS WORLD IS ONLY WHAT I PERMIT YOU TO SEE.");
        a.ui("ui_select");
        a.update(w, 0.016f, false, false, false);
        a.updateScreens(0.016f);
        a.setPaused(true);
        a.dispose();
    }

    @Test
    void theWorldReportsWhatHappens() {
        World w = new World();
        List<Integer> seen = new ArrayList<>();
        w.sink = new SoundSink() {
            public void event(int ev, float x, float y, int a, float b) { seen.add(ev); }
            public void say(String speaker, String text) { seen.add(-1); }
        };
        w.ship.weapon.def = WeaponDef.SINGLE;
        w.ship.weapon.held(0.016f, 0f, w.ship, w);
        assertTrue(seen.contains(Snd.FIRE), "a shot is heard");
        w.spawnPickup(Pickup.COIN, 2, w.ship.x, w.ship.y);
        for (int i = 0; i < 5; i++) w.update(0.016f, IDLE);
        assertTrue(seen.contains(Snd.PICKUP), "a coin is heard");
        Enemy e = new Enemy(Enemy.Kind.VAKRA, w.ship.x, w.ship.y + 300);
        w.enemies.add(e);
        e.hurt(99999f, w);
        assertTrue(seen.contains(Snd.FOE_DIE), "a foe's death is heard");
        w.ship.hp = 1f;
        w.ship.invuln = 0;
        w.ship.iFrames = 0;
        w.ship.damage(50f, w);
        assertTrue(seen.contains(Snd.DEATH), "the pilot's loss is heard");
        w.cue(vajra.world.Cue.ATALA_GATE);
        assertTrue(seen.contains(Snd.CUE));
    }

    @Test
    void aHostileShotNamesWhoseGunItIs() {
        World w = new World();
        int[] gun = {-2};
        w.sink = new SoundSink() {
            public void event(int ev, float x, float y, int a, float b) { if (ev == Snd.FOE_FIRE) gun[0] = a; }
            public void say(String speaker, String text) { }
        };
        w.spawnEnemyBullet(100, 100, 0f, Enemy.GUN[Enemy.Kind.VAKRA.ordinal()], 1f);
        assertEquals(Enemy.Kind.VAKRA.ordinal(), gun[0]);
    }
}
