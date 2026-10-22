package vajra.world;

import vajra.enemy.Enemy;
import vajra.ship.ShipControls;
import vajra.weapon.WeaponDef;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** THE VOICES: a person on the map is never a target. The flag's law — never hurt, never
 *  firing, never woken by the fight, speech fired once, and the roster's order of battle
 *  unchanged — pinned here, headless. */
class NpcTest {
    private static ShipControls controls(final boolean shield) {
        return new ShipControls() {
            public float thrustX() { return 0; }
            public float thrustY() { return 0; }
            public float rotate() { return 0; }
            public boolean fire() { return false; }
            public boolean boost() { return false; }
            public boolean dash() { return false; }
            public boolean shield() { return shield; }
            public boolean mouseAim() { return false; }
            public float aimX() { return 0; }
            public float aimY() { return 0; }
        };
    }

    private static final ShipControls IDLE = controls(false);

    /** Uparichara's Scout at his post, awake, parked on its home. */
    private Enemy voice(World w, float x, float y) {
        Enemy e = new Enemy(Enemy.Kind.LANCE, x, y);
        e.voice = true;
        e.active = true;
        e.homeX = x;
        e.homeY = y;
        w.voices.add(e);
        return e;
    }

    @Test
    void theScoutStandsAtHisBandTwoPost() {
        World w = new World();
        assertEquals(1, w.voices.size(), "one voice in Atala wave 1");
        Enemy scout = w.voices.get(0);
        assertTrue(scout.voice, "voiced");
        assertTrue(scout.homeY > w.regionStart[2] && scout.homeY < w.regionEnd[2],
                "the Scout holds the band-2 post");
        assertTrue(scout.hp > 0, "alive");
        // and the roster never changed: the order of battle does not count a person
        assertEquals(227, w.enemies.size(), "Atala's placement is untouched by voices");
        int lance = 0;
        for (Enemy e : w.enemies)
            if (e.kind == Enemy.Kind.LANCE && e.voice) lance++;
        assertEquals(0, lance, "no voice hides inside the roster either");
    }

    @Test
    void aVoiceNeverHurts() {
        World w = new World();
        w.enemies.clear(); // a bare arena: nothing but the person and the hull
        w.voices.clear();
        Enemy scout = voice(w, 2600, 19100);
        float hp = scout.hp;
        assertFalse(scout.hurt(999, w), "hurt refuses outright");
        assertEquals(hp, scout.hp, "and nothing was spent");
        assertFalse(scout.dead, "immortal against every path that calls hurt");
    }

    @Test
    void aVoiceNeverFires() {
        World w = new World();
        w.enemies.clear();
        w.voices.clear();
        Enemy scout = voice(w, 2600, 19100);
        w.ship.x = 2800; // the hull sits inside any gun's range window
        w.ship.y = 19100;
        for (int i = 0; i < 240; i++) scout.update(0.016f, w, i * 0.016f);
        for (var b : w.bullets) assertFalse(b.hostile, "a voice owns a gun that never speaks");
        scout.update(1f, w, 999f);
        for (var b : w.bullets) assertFalse(b.hostile, "still never, a whole second on");
    }

    @Test
    void theFightNeverWakesAPerson() {
        World w = new World();
        w.voices.clear(); // keep the gen roster: the call-to-arms wake is what's under test,
        w.ship.y = w.regionStart[3] + 100; // and it only carries in the top three bands
        // clear the roster's sleepers out of hearing range, then place one near the ship
        w.enemies.clear();
        Enemy sleeper = new Enemy(Enemy.Kind.AURIS, w.ship.x + 100, w.ship.y);
        sleeper.active = false;
        w.enemies.add(sleeper);
        Enemy person = voice(w, w.ship.x + 90, w.ship.y);
        person.active = false;
        sleeper.hurt(1, w); // a fight starts beside them both
        assertTrue(sleeper.active, "the roster hears the call to arms");
        assertFalse(person.active, "and the voice hears nothing: not a soldier");
    }

    @Test
    void speechFiresOncePerRun() {
        World w = new World();
        w.voices.clear();
        Enemy scout = voice(w, 2600, 19100);
        w.ship.x = 2620; // at speaking distance
        w.ship.y = 19100;
        Chatter.update(w);
        assertTrue(scout.voiceMet, "first sight: the line fired");
        Chatter.update(w);
        Chatter.update(w);
        assertTrue(scout.voiceMet, "and only once: the flag holds");
        // the beacon line answers only to a met voice
        Chatter.beaconRaised(w);
        assertTrue(scout.voiceMet, "the payout speaks through the person met");
    }

    @Test
    void aVoiceTakesNoBulletsAndTheShotFliesOn() {
        World w = new World();
        w.enemies.clear();
        w.voices.clear();
        Enemy scout = voice(w, 3000, 19100);
        w.ship.x = 2600;
        w.ship.y = 19100;
        w.spawnBullet(2612, 19100, 0, WeaponDef.SINGLE, 1f, 1f); // a clean player round
        float hp = scout.hp;
        for (int i = 0; i < 210 && !w.bullets.isEmpty(); i++) w.update(0.016f, IDLE);
        assertEquals(hp, scout.hp, "the round passed through the person");
        assertTrue(scout.hp > 0, "and nothing spent");
    }
}
