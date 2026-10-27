package vajra.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import vajra.enemy.Enemy;
import vajra.weapon.Bullet;
import vajra.weapon.WeaponDef;
import vajra.world.Astra;
import vajra.world.AstraFlow;
import vajra.world.Cue;
import vajra.world.Pickup;
import vajra.world.World;

/** THE EARS OF THE GAME (docs/SOUND_PLAN.md). It listens to the world's events ({@link SoundSink}), reads a little of its state once a frame, and plays the written music and
 *  the rendered effects of assets/audio. With no audio device (the tests, a headless run) it does nothing at all; a missing file is silent, never a crash.
 *
 *  <p>Three buses sit under the pilot's volumes: MUSIC, SFX (everything of the world and the interface) and VOICE (the sung dialogue and the chanted mantras), each under MASTER. */
public final class AudioEngine implements SoundSink {
    private static final String SFX = "audio/sfx/", MUSIC = "audio/music/";
    private static final String[] KINDS = new String[Enemy.Kind.values().length];
    private static final String[] AREAS = {"atala", "vitala"};

    static {
        for (Enemy.Kind k : Enemy.Kind.values()) KINDS[k.ordinal()] = k.name().toLowerCase();
    }

    /** The material a pattern's round lands as. */
    private static final String[] HIT = {
            "hit_tick", "hit_tick", "hit_tick", "hit_tick", "hit_tick", "hit_thud", "hit_tick", "hit_bell", "hit_burn", "hit_thud", "hit_thud", "hit_thud", "hit_bell", "hit_tick", "hit_stone",
            "hit_glass", "hit_tick", "hit_glass", "hit_burn", "hit_tick", "hit_thud", "hit_wet", "hit_wet", "hit_bell", "hit_bell", "hit_glass", "hit_thud", "hit_tick", "hit_burn", "hit_wet",
            "hit_thud", "hit_stone", "hit_glass", "hit_bell", "hit_thud", "hit_bell"};

    private final boolean on;
    private final Map<String, Sound> cache = new HashMap<>();
    private final Set<String> missing = new HashSet<>();
    private final Map<String, Float> last = new HashMap<>();
    private final Map<String, long[]> loops = new HashMap<>();      // key -> {sound id}; the sound is held in loopSound
    private final Map<String, Sound> loopSound = new HashMap<>();
    private final ArrayList<float[]> sung = new ArrayList<>();       // {due time, speaker idx, degree, volume}
    private final ArrayList<String> sungName = new ArrayList<>();
    private float clock, lx, ly;
    private int coinChain;
    private float coinAt = -9f;
    private int shown = -1;

    // music
    private final Map<String, Track> tracks = new LinkedHashMap<>();
    private final Map<String, Float> want = new LinkedHashMap<>();
    private Music sting;
    private float stingDuck = 1f, stingT;
    private float duck = 1f;
    private String screenTrack;
    private boolean inGame;
    private int level, lastLevel;
    private float levelHold;
    private int lastLoka = -1;
    private boolean deathPlayed, yieldPlayed, mayaOn, ashOn, boostOn, openingOn;
    private int lastMayaStage;
    private float heartT;
    private int lastFlowKind;
    private boolean sceneOn, paused;
    private final Map<String, Float> lastWant = new LinkedHashMap<>();
    private float pausedDuck = 1f;

    private static final class Track {
        Music m;
        float vol, target;
        float idleT;
    }

    public AudioEngine() {
        on = Gdx.audio != null && Gdx.files != null;
    }

    public boolean enabled() { return on; }

    /** Every effect this engine can ask for: the files assets/audio/sfx must hold (a test keeps the two in step). */
    public static java.util.List<String> sfxNames() {
        java.util.ArrayList<String> n = new java.util.ArrayList<>();
        for (int i = 0; i < WeaponDef.ALL.length; i++) { n.add("w" + i + "_a"); n.add("w" + i + "_b"); }
        for (String h : HIT) if (!n.contains(h)) n.add(h);
        for (String k : KINDS) { n.add("foe_" + k + "_fire"); n.add("foe_" + k + "_die"); n.add("foe_" + k + "_wake"); }
        for (int i = 0; i < 7; i++) { n.add("astra_" + i + "_invoke"); n.add("astra_" + i + "_loose"); n.add("astra_" + i + "_live"); n.add("astra_" + i + "_chant"); }
        for (int i = 0; i < 5; i++) { n.add("coin_" + i); n.add("shard_" + i); }
        for (String s : new String[] {"boom_small", "boom_big", "rock_tick", "hull_hit_light", "hull_hit_heavy", "shield_hit", "shield_loop", "death_burst", "astra_cancel", "astra_hit", "gate", "boss_wake", "lamp_bind",
                "bay_dock", "bay_close", "repair", "betray", "piece_hum", "seal_open", "foe_hurt_armour", "foe_mend", "foe_dodge", "foe_dive", "foe_surface", "chest", "execute_kill", "boss_telegraph", "ram",
                "health", "engine_loop", "mud_loop", "boost", "heartbeat", "ash_loop", "ash_start", "maya_forge", "mirror_shatter", "door_open", "amb_atala", "amb_vitala", "buy", "deny", "fit", "temper",
                "temper3", "sell", "ui_move", "ui_select", "ui_back", "ui_tour", "ui_page", "ui_wheel_open", "ui_wheel_tick", "ui_newgame", "ui_toggle", "ui_tick", "ui_popup", "ui_combo", "ui_error", "ui_codex"})
            n.add(s);
        for (String sp : new String[] {"indra", "bala", "bhairava", "scout", "maya", "kaliya", "seller", "maid", "dipaka", "vanija", "dvarapala", "voice"})
            for (int d = 0; d < 7; d++) n.add("v_" + sp + "_" + d);
        return n;
    }

    /** Every piece of music it can ask for. */
    public static java.util.List<String> musicNames() {
        java.util.ArrayList<String> n = new java.util.ArrayList<>(java.util.Arrays.asList("title", "menu", "opening", "bala_1", "bala_2", "bala_3", "bhairava", "death", "respawn", "descend_1", "yield_atala", "yield_vitala"));
        for (String a : AREAS) {
            n.add("rest_" + a);
            for (String s : new String[] {"drone", "drift", "combat", "crisis", "threat", "voice"}) n.add(a + "_" + s);
        }
        for (int i = 1; i <= 7; i++) n.add("piece_" + i);
        return n;
    }

    // ============================================================================================================================ the pilot's volumes
    private static vajra.Settings settings() { return vajra.profile.Profiles.active().settings; }

    private float sfxVol() { vajra.Settings s = settings(); return s.master * s.sfx; }

    private float voiceVol() { vajra.Settings s = settings(); return s.master * s.voice; }

    private float musicVol() { vajra.Settings s = settings(); return s.master * s.music; }

    // ============================================================================================================================ effects
    private Sound sound(String name) {
        Sound s = cache.get(name);
        if (s != null) return s;
        if (missing.contains(name)) return null;
        try {
            com.badlogic.gdx.files.FileHandle f = Gdx.files.internal(SFX + name + ".ogg");
            if (f.exists()) {
                s = Gdx.audio.newSound(f);
                cache.put(name, s);
                return s;
            }
        } catch (RuntimeException e) {
            // fall through: a sound that cannot load is a silent one
        }
        missing.add(name);
        return null;
    }

    /** True when the sound may play now (a minimum gap per name, so a stream of the same round does not become a buzz). */
    private boolean gate(String name, float gap) {
        Float t = last.get(name);
        if (t != null && clock - t < gap) return false;
        last.put(name, clock);
        return true;
    }

    private void play(String name, float vol, float pitch, float wx, float wy, float gap, boolean voiceBus) {
        if (!on || vol <= 0f) return;
        Sound s = sound(name);
        if (s == null || !gate(name, gap)) return;
        float pan = 0f, att = 1f;
        if (!Float.isNaN(wx)) {
            float dx = wx - lx, dy = wy - ly, d = (float) Math.sqrt(dx * dx + dy * dy);
            pan = Math.max(-0.9f, Math.min(0.9f, dx / 700f));
            att = Math.max(0.12f, Math.min(1f, 1f - (d - 450f) / 1500f));
        }
        float v = vol * att * (voiceBus ? voiceVol() : sfxVol());
        if (v > 0.002f) s.play(Math.min(1f, v), pitch, pan);
    }

    private void play(String name, float vol) { play(name, vol, 1f, Float.NaN, 0f, 0.02f, false); }

    private void at(String name, float vol, float x, float y, float gap) { play(name, vol, 1f, x, y, gap, false); }

    /** An interface sound (a menu, a page, a purchase): no place in the world. */
    public void ui(String name) {
        play(name, name.equals("ui_tick") || name.equals("ui_move") ? 0.5f : 0.8f, 1f, Float.NaN, 0f, 0.05f, false);
    }

    public void ui(String name, float pitch) { play(name, 0.8f, pitch, Float.NaN, 0f, 0.05f, false); }

    // loops ----------------------------------------------------------------------------------------------------------------------
    private void loop(String key, String name, float vol, float pitch, boolean voiceBus) {
        if (!on) return;
        float v = vol * (voiceBus ? voiceVol() : sfxVol());
        long[] id = loops.get(key);
        Sound s = loopSound.get(key);
        if (id == null) {
            s = sound(name);
            if (s == null || v < 0.003f) return;
            id = new long[] {s.loop(Math.min(1f, v), pitch, 0f)};
            loops.put(key, id);
            loopSound.put(key, s);
        } else {
            s.setVolume(id[0], Math.min(1f, v));
            s.setPitch(id[0], pitch);
        }
    }

    private void loopOff(String key) {
        long[] id = loops.remove(key);
        Sound s = loopSound.remove(key);
        if (id != null && s != null) s.stop(id[0]);
    }

    // ============================================================================================================================ the world's events
    @Override
    public void event(int ev, float x, float y, int a, float b) {
        if (!on) return;
        switch (ev) {
            case Snd.FIRE: {
                int i = Math.max(0, Math.min(WeaponDef.ALL.length - 1, a));
                WeaponDef d = WeaponDef.ALL[i];
                boolean rapid = d.cooldown < 0.15f;
                play("w" + i + (((int) (clock * 50f) & 1) == 0 ? "_a" : "_b"), rapid ? 0.45f : 0.8f, 1f, Float.NaN, 0f, rapid ? 0.045f : 0.018f, false);
                break;
            }
            case Snd.HIT: {
                int i = Math.max(0, Math.min(HIT.length - 1, a));
                at(HIT[i], 0.5f, x, y, 0.045f);
                break;
            }
            case Snd.BOOM: at(a == 0 ? "boom_small" : "boom_big", a == 0 ? 0.7f : 1f, x, y, 0.08f); break;
            case Snd.ROCK: at("rock_tick", 0.3f, x, y, 0.07f); break;
            case Snd.FOE_FIRE: at("foe_" + kindName(a) + "_fire", 0.55f, x, y, 0.05f); break;
            case Snd.FOE_DIE: at("foe_" + kindName(a) + "_die", 0.9f, x, y, 0.04f); break;
            case Snd.FOE_WAKE: at("foe_" + kindName(a) + "_wake", 0.5f, x, y, 0.15f); break;
            case Snd.HULL_HIT: play(b >= 18f ? "hull_hit_heavy" : "hull_hit_light", 0.9f); break;
            case Snd.SHIELD_HIT: play("shield_hit", 0.8f); break;
            case Snd.DEATH: play("death_burst", 1f); break;
            case Snd.PICKUP: pickup(a, (int) b); break;
            case Snd.ASTRA_INVOKE: play("astra_" + a + "_invoke", 0.8f); break;
            case Snd.ASTRA_LOOSE: play("astra_" + a + "_loose", 1f); break;
            case Snd.ASTRA_CANCEL: play("astra_cancel", 0.7f); break;
            case Snd.ASTRA_HIT: at("astra_hit", 0.5f, x, y, 0.06f); break;
            case Snd.CUE: cue(a); break;
            case Snd.LAMP: play("lamp_bind", 0.9f); break;
            case Snd.DOCK: play("bay_dock", 0.9f); break;
            case Snd.BAY_CLOSE: play("bay_close", 0.7f); break;
            case Snd.MEDKIT: play("repair", 0.9f); break;
            case Snd.BETRAY: at("betray", 0.7f, x, y, 0.3f); break;
            case Snd.PIECE_LIFT: play("piece_hum", 0.8f); break;
            case Snd.SEAL: play("seal_open", 1f); break;
            case Snd.FOE_HURT_ARMOUR: at("foe_hurt_armour", 0.5f, x, y, 0.05f); break;
            case Snd.FOE_MEND: at("foe_mend", 0.5f, x, y, 0.2f); break;
            case Snd.FOE_DODGE: at("foe_dodge", 0.35f, x, y, 0.15f); break;
            case Snd.FOE_DIVE: at(b > 0.5f ? "foe_surface" : "foe_dive", 0.4f, x, y, 0.2f); break;
            case Snd.CHEST: play("chest", 0.9f); break;
            case Snd.EXECUTE: at("execute_kill", 0.8f, x, y, 0.1f); break;
            case Snd.BOSS_TELEGRAPH: play("boss_telegraph", 0.8f); break;
            case Snd.RAM: at("ram", 0.5f, x, y, 0.1f); break;
            default: break;
        }
    }

    private static String kindName(int k) { return k >= 0 && k < KINDS.length ? KINDS[k] : "bala"; }

    private void pickup(int type, int variant) {
        if (type == Pickup.COIN) {
            coinChain = clock - coinAt < 0.9f ? Math.min(4, coinChain + 1) : 0;
            coinAt = clock;
            play("coin_" + coinChain, 0.55f, 1f, Float.NaN, 0f, 0.025f, false);
        } else if (type == Pickup.SHARD) {
            play("shard_" + Math.max(0, Math.min(4, variant)), 0.9f);
        } else if (type == Pickup.HEALTH) {
            play("health", 0.9f);
        } else if (type == Pickup.PIECE) {
            stingMusic("piece_" + (Math.max(0, Math.min(6, variant)) + 1), 1f, 0.45f);
        }
    }

    private void cue(int ordinal) {
        Cue[] cs = Cue.values();
        if (ordinal < 0 || ordinal >= cs.length) return;
        switch (cs[ordinal]) {
            case ATALA_GATE: play("gate", 1f); break;
            case BALA_WAKES:
            case BHAIRAVA_WAKES: play("boss_wake", 1f); break;
            default: break;
        }
    }

    @Override
    public void say(String speaker, String text) {
        sing(speaker, text, 30f);
    }

    // ============================================================================================================================ sung speech
    /** Speech is sung (docs/SOUND_PLAN.md 1.6): the vowels of a line are notes of the speaker's instrument, by a fixed table (A E I O U -> degrees 0..4; the rest of the letters fill the
     *  degrees 5 and 6 when a word's last letter is a stop), so the same sentence always sings the same tune. */
    public void sing(String speaker, String text, float cps) {
        if (!on || text == null || text.isEmpty()) return;
        String who = speakerKey(speaker);
        float t = clock;
        int vowels = 0;
        for (int i = 0; i < text.length() && vowels < 70; i++) {
            char c = Character.toUpperCase(text.charAt(i));
            int d = degreeOf(c);
            if (d >= 0) {
                sung.add(new float[] {clock + i / cps, d, vowels % 5 == 0 ? 0.9f : 0.6f, 0f});
                sungName.add(who);
                vowels++;
            }
        }
    }

    static int degreeOf(char c) {
        switch (c) {
            case 'A': return 0;
            case 'E': return 1;
            case 'I': return 2;
            case 'O': return 3;
            case 'U': return 4;
            case 'Y': return 5;
            default: return -1;
        }
    }

    static String speakerKey(String s) {
        if (s == null) return "voice";
        String u = s.toUpperCase();
        if (u.contains("INDRA")) return "indra";
        if (u.contains("BALA")) return "bala";
        if (u.contains("BHAIRAVA")) return "bhairava";
        if (u.contains("SCOUT")) return "scout";
        if (u.contains("MAYA")) return "maya";
        if (u.contains("KALIYA")) return "kaliya";
        if (u.contains("SELLER") || u.contains("GOLD")) return "seller";
        if (u.contains("MAID") || u.contains("GAURI")) return "maid";
        if (u.contains("DIPAKA") || u.contains("LAMP")) return "dipaka";
        if (u.contains("VANIJA") || u.contains("BAY")) return "vanija";
        if (u.contains("DVARAPALA") || u.contains("GATE") || u.contains("WARD")) return "dvarapala";
        return "voice";
    }

    // ============================================================================================================================ music
    public void screen(String track) {
        screenTrack = track;
        inGame = false;
    }

    /** Silence the world's music (leaving a flight for a menu). */
    public void leaveGame() {
        inGame = false;
        stopLoops();
        sceneOn = false;
        openingOn = false;
    }

    private void stopLoops() {
        for (String k : new ArrayList<>(loops.keySet())) loopOff(k);
    }

    public void stingMusic(String name, float vol, float duckTo) {
        if (!on) return;
        try {
            com.badlogic.gdx.files.FileHandle f = Gdx.files.internal(MUSIC + name + ".ogg");
            if (!f.exists()) return;
            if (sting != null) { sting.stop(); sting.dispose(); }
            sting = Gdx.audio.newMusic(f);
            sting.setLooping(false);
            sting.setVolume(Math.min(1f, vol * musicVol()));
            sting.play();
            stingDuck = duckTo;
            stingT = 0f;
        } catch (RuntimeException e) {
            sting = null;
        }
    }

    /** A cutscene has begun or ended: the opening has its own score, any other scene lowers the music under its words. */
    public void scene(Cue cue, boolean begins) {
        sceneOn = begins;
        openingOn = begins && cue == Cue.OPENING;
    }

    private void wantTrack(String name, float vol) { want.put(name, vol); }

    private void updateMusic(float dt) {
        float mv = musicVol();
        float fade = dt * 0.8f;
        // the stingers duck the rest while they play
        float duckTarget = 1f;
        if (sting != null) {
            stingT += dt;
            if (!sting.isPlaying() && stingT > 0.3f) { sting.dispose(); sting = null; }
            else duckTarget = stingDuck;
        }
        duckTarget *= paused ? 0.3f : 1f;
        duck += (duckTarget - duck) * Math.min(1f, dt * 3f);
        if (sting != null) sting.setVolume(Math.min(1f, mv));
        // create what is wanted, together
        for (Map.Entry<String, Float> e : want.entrySet()) {
            Track t = tracks.get(e.getKey());
            if (t == null) {
                try {
                    com.badlogic.gdx.files.FileHandle f = Gdx.files.internal(MUSIC + e.getKey() + ".ogg");
                    if (!f.exists()) continue;
                    t = new Track();
                    t.m = Gdx.audio.newMusic(f);
                    t.m.setLooping(true);
                    t.m.setVolume(0f);
                    tracks.put(e.getKey(), t);
                } catch (RuntimeException ex) {
                    continue;
                }
            }
            t.target = e.getValue();
            t.idleT = 0f;
        }
        // start every stem of a group on the same frame
        boolean any = false;
        for (Track t : tracks.values()) if (t.m != null && !t.m.isPlaying()) { any = true; break; }
        if (any) for (Track t : tracks.values()) if (t.m != null && !t.m.isPlaying()) t.m.play();
        for (Iterator<Map.Entry<String, Track>> it = tracks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Track> e = it.next();
            Track t = e.getValue();
            if (!want.containsKey(e.getKey())) t.target = 0f;
            float rate = t.target > t.vol ? fade : dt * 1.0f;
            t.vol += Math.max(-rate, Math.min(rate, t.target - t.vol));
            t.m.setVolume(Math.min(1f, t.vol * mv * duck));
            if (t.target <= 0f && t.vol <= 0.002f && !want.containsKey(e.getKey())) {
                t.m.stop();
                t.m.dispose();
                it.remove();
            }
        }
        lastWant.clear();
        lastWant.putAll(want);
        want.clear();
    }

    /** A pause menu over a flight: the loops stop, the music lowers (and comes back when the flight does). */
    public void setPaused(boolean p) {
        paused = p;
        if (p) stopLoops();
    }

    // ============================================================================================================================ the frame
    /** Once a frame in a flight: the loops that follow the hull, the music that follows the fight. */
    public void update(World w, float dt, boolean cutscene, boolean shopOpen, boolean tourOn) {
        if (!on) return;
        clock += dt;
        inGame = true;
        paused = false;
        lx = w.ship.x;
        ly = w.ship.y;
        vajra.ship.Ship s = w.ship;
        boolean alive = !w.death.hidden();
        // the hull
        float sp = (float) Math.sqrt(s.vx * s.vx + s.vy * s.vy), spN = Math.min(1.4f, sp / Math.max(1f, s.maxSpeed()));
        if (alive && !w.runEnded()) loop("engine", "engine_loop", 0.1f + 0.22f * Math.min(1f, spN), 0.78f + 0.45f * spN + (s.boosting ? 0.2f : 0f), false);
        else loopOff("engine");
        if (s.boosting && !boostOn) play("boost", 0.8f);
        boostOn = s.boosting;
        if (s.shieldOn && s.shieldEnergy > 0 && alive) loop("shield", "shield_loop", 0.12f, 1f, false); else loopOff("shield");
        if (s.mud < 1f && alive) loop("mud", "mud_loop", 0.35f, 1f, false); else loopOff("mud");
        float hpF = s.hp / Math.max(1f, s.maxHp());
        if (alive && hpF < 0.3f && !w.runEnded()) {
            heartT -= dt;
            if (heartT <= 0f) { play("heartbeat", 0.6f); heartT = 0.45f + 0.7f * (hpF / 0.3f); }
        }
        // the astra
        if (w.astraInvoking) {
            float k = Astra.k(w.astraCharge);
            loop("chant", "astra_" + w.astraKind + "_chant", 0.35f + 0.55f * k, 0.9f + 0.35f * k, true);
        } else loopOff("chant");
        int fk = w.flow.kind;
        if (fk != AstraFlow.NONE) {
            int idx = fk == AstraFlow.AGNI ? 2 : fk == AstraFlow.VAYU ? 1 : fk == AstraFlow.LIGHTNING ? 0 : fk == AstraFlow.BOULDER ? 4 : fk == AstraFlow.LOTUS ? 5 : 3;
            loop("live", "astra_" + idx + "_live", 0.6f, 1f, false);
        } else if (w.gazeT > 0f) loop("live", "astra_6_live", 0.7f, 1f, false);
        else loopOff("live");
        // the rulers' set pieces
        if (w.maya.active) {
            int stage = w.maya.t >= vajra.world.Maya.OPEN_AT && w.maya.mode == vajra.world.Maya.DOORS ? 3 : w.maya.t >= vajra.world.Maya.CRACK_AT ? 2 : 1;
            if (!mayaOn) { play("maya_forge", 0.9f); lastMayaStage = 1; }
            if (stage >= 2 && lastMayaStage < 2) play("mirror_shatter", 1f);
            if (stage >= 3 && lastMayaStage < 3) play("door_open", 1f);
            lastMayaStage = stage;
        }
        mayaOn = w.maya.active;
        if (w.ash.active) {
            if (!ashOn) play("ash_start", 0.9f);
            loop("ash", "ash_loop", 0.4f, 1f, false);
        } else loopOff("ash");
        ashOn = w.ash.active;
        // ambience of the place
        int loka = Math.max(0, Math.min(AREAS.length - 1, w.lokaAt(s.y)));
        if (!cutscene && !shopOpen) loop("amb", "amb_" + AREAS[loka], 0.22f, 1f, false); else loopOff("amb");
        // the sung lines that are due
        for (int i = sung.size() - 1; i >= 0; i--) {
            float[] n = sung.get(i);
            if (clock >= n[0]) {
                String who = sungName.get(i);
                if (clock - n[0] < 0.25f) play("v_" + who + "_" + (int) n[1], 0.55f * n[2], 1f, Float.NaN, 0f, 0.05f, true);
                sung.remove(i);
                sungName.remove(i);
            }
        }
        musicPolicy(w, dt, loka, cutscene, shopOpen);
        updateMusic(dt);
    }

    private void musicPolicy(World w, float dt, int loka, boolean cutscene, boolean shopOpen) {
        String area = AREAS[loka];
        if (lastLoka >= 0 && loka != lastLoka && loka == 1) stingMusic("descend_1", 0.9f, 0.3f);
        lastLoka = loka;
        // the pilot's death and return
        if (w.death.active && !deathPlayed) { stingMusic("death", 0.9f, 0.1f); deathPlayed = true; }
        if (!w.death.active && deathPlayed) { deathPlayed = false; stingMusic("respawn", 0.9f, 0.5f); }
        if (w.death.active) { wantTrack(area + "_drone", 0.2f); return; }
        // a ruler yields
        if (w.yielder != null && !yieldPlayed) { stingMusic(loka == 0 ? "yield_atala" : "yield_vitala", 0.9f, 0.1f); yieldPlayed = true; }
        if (w.yielder == null) yieldPlayed = false;
        if (w.yielder != null) { wantTrack(area + "_drone", 0.25f); return; }
        if (openingOn) { wantTrack("opening", 1f); return; }
        if (shopOpen) { wantTrack("rest_" + area, 0.9f); return; }
        // a ruler fights
        Enemy b = w.boss;
        if (b != null && b.active && !b.dead && !b.yielding) {
            if (w.stage == 0) {
                float f = b.hp / Math.max(1f, b.maxHp);
                wantTrack(f > 0.66f ? "bala_1" : f > 0.33f ? "bala_2" : "bala_3", 0.95f);
                wantTrack("atala_threat", 0.12f);
            } else {
                wantTrack("bhairava", 0.95f);
                wantTrack("vitala_threat", 0.12f);
            }
            return;
        }
        // the land: three paces of the same composition, by how near the danger is
        int raw = assess(w);
        if (raw > level) { level = raw; levelHold = 6f; }
        else if (raw < level) { levelHold -= dt; if (levelHold <= 0f) { level--; levelHold = 4f; } }
        else levelHold = Math.max(levelHold, 3f);
        float d = sceneOn ? 0.6f : 1f;
        switch (level) {
            case 0:
                wantTrack(area + "_drone", 0.6f * d); wantTrack(area + "_drift", 0.9f * d);
                wantTrack(area + "_combat", 0f); wantTrack(area + "_crisis", 0f); wantTrack(area + "_threat", 0f); wantTrack(area + "_voice", 0f);
                break;
            case 1:
                wantTrack(area + "_drone", 0.5f * d); wantTrack(area + "_drift", 0.6f * d); wantTrack(area + "_combat", 0.4f * d);
                wantTrack(area + "_crisis", 0f); wantTrack(area + "_threat", 0.1f * d); wantTrack(area + "_voice", 0f);
                break;
            case 2:
                wantTrack(area + "_drone", 0.35f * d); wantTrack(area + "_drift", 0f); wantTrack(area + "_combat", 0.9f * d);
                wantTrack(area + "_crisis", 0f); wantTrack(area + "_threat", 0.25f * d); wantTrack(area + "_voice", 0f);
                break;
            default:
                wantTrack(area + "_drone", 0.25f * d); wantTrack(area + "_drift", 0f); wantTrack(area + "_combat", 0f);
                wantTrack(area + "_crisis", 0.95f * d); wantTrack(area + "_threat", 0.45f * d); wantTrack(area + "_voice", 0.5f * d);
                break;
        }
    }

    /** 0 drift, 1 alert, 2 combat, 3 crisis: how near the awake foes are, how hurt the hull is, what is being loosed. */
    private int assess(World w) {
        int near = 0, wide = 0;
        float px = w.ship.x, py = w.ship.y;
        for (Enemy e : w.enemies) {
            if (e.dead || !e.active || e.dummy || e.voice || e.boss) continue;
            float dx = e.x - px, dy = e.y - py, d2 = dx * dx + dy * dy;
            if (d2 < 700f * 700f) near++;
            else if (d2 < 1500f * 1500f) wide++;
        }
        for (Bullet bl : w.bullets) {
            if (!bl.hostile) continue;
            float dx = bl.x - px, dy = bl.y - py;
            if (dx * dx + dy * dy < 500f * 500f) { near++; break; }
        }
        float hpF = w.ship.hp / Math.max(1f, w.ship.maxHp());
        if (near >= 9 || hpF < 0.22f || w.maya.active || (w.flow.kind != AstraFlow.NONE && near >= 2)) return 3;
        if (near >= 1) return 2;
        if (wide >= 1) return 1;
        return 0;
    }

    // ============================================================================================================================ menus
    /** The music of a screen outside a flight (the title, the menus): called every frame by the screens that want one, cheap. */
    public void updateScreens(float dt) {
        if (!on) return;
        clock += dt;
        if (paused && inGame) {
            want.putAll(lastWant);
            updateMusic(dt);
        } else if (!inGame) {
            stopLoops();
            if (screenTrack != null) wantTrack(screenTrack, 0.9f);
            updateMusic(dt);
        }
    }

    public void dispose() {
        for (Sound s : cache.values()) s.dispose();
        cache.clear();
        for (Track t : tracks.values()) { t.m.stop(); t.m.dispose(); }
        tracks.clear();
        if (sting != null) { sting.dispose(); sting = null; }
        loops.clear();
        loopSound.clear();
    }
}
