package vajra.world;

import vajra.enemy.Enemy;

/** THE MISSIONS — run-scoped side work. Wave 1 ships THE BOUNTY: the gold brokers pin a card
 *  when the Scout's beacon is raised, and a named rogue of the illusion wood becomes a person
 *  you can hunt for a purse. One row, three states, nothing carried out of the run. */
public final class Missions {
    public static final int NONE = 0, OPEN = 1, DONE = 2;

    public int bounty = NONE;
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
        world.popups.push(new Popups.Spec()
                .text("THE BROKERS PIN A CARD: DAMBHA, A ROGUE OF THE WOOD, SPOILS THE SEAM. HIS PURSE PAID ON HIS FALL.")
                .at(world.ship.x, world.ship.y - 60).life(3f).rise(8)
                .color(1f, 0.8f, 0.35f).scale(1.2f));
    }

    /** Run every frame from World.update: the purse pays the frame the rogue is spent. */
    public void update() {
        if (bounty != OPEN) return;
        if (rogue == null || !rogue.dead) return;
        bounty = DONE;
        world.hangar.coins += 250;
        world.lives++;
        world.popups.push(new Popups.Spec()
                .text("DAMBHA IS SPENT - THE BROKERS PAY THE CARD, AND A LIFE SAILS FOR IT.")
                .at(world.ship.x, world.ship.y - 60).life(3f).rise(8)
                .color(1f, 0.9f, 0.5f).scale(1.2f));
    }
}
