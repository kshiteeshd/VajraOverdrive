package vajra.world;

import vajra.enemy.Enemy;

/** THE MISSIONS — run-scoped side work. Wave 1 ships THE BOUNTY: the gold brokers pin a card
 *  when the Scout's beacon is raised, and a named rogue of the illusion wood becomes a person
 *  you can hunt for a purse. One row, three states, nothing carried out of the run. */
public final class Missions {
    public static final int NONE = 0, OPEN = 1, DONE = 2;

    public static final int SALVAGE_NEED = 6;      // Maya's retempering ask
    public static final float SALVAGE_TOUCH = 60;  // hull centre to wreck, px

    public int bounty = NONE;
    public int salvage = NONE;                // Maya's grove: the wrecks she wants home
    public int salvageTaken;
    private final World world;
    private Enemy rogue; // the named target, once the card goes up

    public Missions(World w) { world = w; }

    /** The card goes up (called from Chatter when the Scout's beacon answers, met only).
     *  The rogue is placed wherever open rock stands near the scout's post: elite, awake,
     *  and straight to hunting the way the cache-ambush law hunts — the drama starts fast. */
    public void openBounty() {
        if (bounty != NONE) return;
        bounty = OPEN;
        Enemy scout = world.voices.get(0);
        float rx = scout.x + 700, ry = scout.y + 900;
        for (int i = 0; i < 8 && !world.spotClear(rx, ry); i++) { // nudged until open rock holds him
            rx += 180;
            if (i % 4 == 3) { rx = scout.x - 700; ry += 400; }
        }
        rogue = new Enemy(Enemy.Kind.BHAVI, rx, ry);
        rogue.elite = true;
        rogue.rank = 1;
        rogue.hp = rogue.maxHp = rogue.maxHp * 4;
        rogue.hunt = true;
        rogue.active = true;
        rogue.wakeRange = 3000;
        world.add(rogue);
        world.toast("BOUNTY", "DAMBHA, A ROGUE OF THE WOOD, SPOILS THE SEAM. HIS PURSE IS YOURS.");
    }

    /** Maya's ask opens the salvage row: the grove's wrecks, six of them, to her hammer. */
    public void openSalvage() {
        if (salvage != NONE) return;
        salvage = OPEN;
        salvageTaken = 0;
        world.toast("SALVAGE", "SIX WRECKS OF MAYA VANA, HOME TO HER HAMMER.");
    }

    /** Run every frame from World.update: the bounty pays the frame the rogue is spent;
     *  the salvage takes wrecks as any hull moves over them (the co-op partner counts too). */
    public void update() {
        if (bounty == OPEN && rogue != null && rogue.dead) {
            bounty = DONE;
            world.hangar.coins += 250;
            world.lives++;
            world.toast("DAMBHA FALLS", "THE CARD IS PAID. +250c AND A LIFE.");
            Codex.unlock(world, Codex.ROGUE);
        }
        if (salvage != OPEN) return;
        java.util.ArrayList<vajra.ship.Ship> hulls = new java.util.ArrayList<>(2 + world.extras.size());
        hulls.add(world.ship);
        if (world.partnerAlive()) hulls.add(world.partner);
        for (World.SeatHull gh : world.extras)
            if (gh.ship != null && gh.ship.hp > 0) hulls.add(gh.ship);
        for (int i = 0; i < world.wrecks.length; i++) {
            if (world.wreckTaken[i]) continue;
            for (vajra.ship.Ship h : hulls) {
                if (h.hp <= 0) continue;
                float dx = h.x - world.wrecks[i][0], dy = h.y - world.wrecks[i][1];
                if (dx * dx + dy * dy > SALVAGE_TOUCH * SALVAGE_TOUCH) continue;
                world.wreckTaken[i] = true;
                salvageTaken++;
                world.addEffect(Effect.ring(world.wrecks[i][0], world.wrecks[i][1], 90, 0.55f, 0.95f, 1f));
                world.addEffect(Effect.ring(world.wrecks[i][0], world.wrecks[i][1], 44, 1f, 0.84f, 0.3f)); // the count is on the HUD tracker
                break;
            }
        }
        if (salvageTaken >= SALVAGE_NEED) {
            salvage = DONE;
            world.hangar.coins += 200;
            world.hangar.addShard(0); // Atala mints Akasha: the first fuel
            Codex.unlock(world, Codex.RETEMPER);
            Chatter.salvageDone(world);
        }
    }
}
