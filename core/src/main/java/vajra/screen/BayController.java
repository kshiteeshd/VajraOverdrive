package vajra.screen;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import vajra.ship.ShipDef;
import vajra.weapon.WeaponDef;
import vajra.world.LokaTable;
import vajra.world.World;

/** The bay's purchase logic: what a pressed key or click actually DOES. The layout, hit boxes
 *  and every label live in {@link BayUi}; the rules about what may be bought live in
 *  {@link vajra.ship.Hangar}. A run owns nothing on exit, so nothing is persisted here.
 *
 *  <p>Keys: TAB or click changes the rail; A/D (or the arrows) move along the strip, the wheel or
 *  the cards; W/S move between the hull strip and its tier cards; ENTER buys or fits; 1-7 jump
 *  to a hull; X sells every shard; ESC (the run's own key) leaves. */
public final class BayController {
    private final World world;
    private final BayUi ui;
    private boolean open;

    public BayController(World world, BayUi ui) {
        this.world = world;
        this.ui = ui;
    }

    public void open() {
        open = true;
        ui.syncTo(world);
    }

    public void close() {
        open = false;
        world.closeDocked();
    }

    public boolean isOpen() { return open; }
    public BayUi ui() { return ui; }

    public boolean keyDown(int k) {
        if (k == Input.Keys.TAB) { ui.setRail(ui.rail() + 1); return true; }
        if (k == Input.Keys.X) { world.hangar.sellShards(); return true; }
        if (k >= Input.Keys.NUM_1 && k <= Input.Keys.NUM_7) {
            ui.setRail(BayUi.HULLS);
            ui.setHull(k - Input.Keys.NUM_1);
            ui.setTierFocus(-1);
            if (world.hangar.ownsShip(ui.hull())) world.selectShip(ui.hull()); // a hull you own is fitted at once; one you do not is only focused
            return true;
        }
        int dx = (k == Input.Keys.D || k == Input.Keys.RIGHT ? 1 : 0) - (k == Input.Keys.A || k == Input.Keys.LEFT ? 1 : 0);
        int dy = (k == Input.Keys.S || k == Input.Keys.DOWN ? 1 : 0) - (k == Input.Keys.W || k == Input.Keys.UP ? 1 : 0);
        if (dx != 0 || dy != 0) {
            move(dx, dy);
            return true;
        }
        if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) return act();
        return false;
    }

    /** One step of focus. Pure state: the tests drive it without a keyboard. */
    public void move(int dx, int dy) {
        switch (ui.rail()) {
            case BayUi.HULLS:
                if (dy > 0 && ui.tierFocus() < 0) ui.setTierFocus(0);
                else if (dy < 0 && ui.tierFocus() >= 0) ui.setTierFocus(-1);
                else if (ui.tierFocus() >= 0) ui.setTierFocus(ui.tierFocus() + dx);
                else ui.setHull(Math.max(0, Math.min(ShipDef.ALL.length - 1, ui.hull() + dx)));
                break;
            case BayUi.ARMS:
                ui.setArm(ui.arm() + (dx != 0 ? dx : dy));
                break;
            default:
                ui.setAstra(ui.astra() + dx);
        }
    }

    /** ENTER: buy or fit what holds the focus. */
    public boolean act() {
        switch (ui.rail()) {
            case BayUi.HULLS:
                return ui.tierFocus() <= 0 ? actHull() : actTier(ui.tierFocus());
            case BayUi.ARMS:
                return actWeapon(ui.arm());
            default:
                return actAstra(ui.astra());
        }
    }

    public boolean click(Vector2 m) {
        if (!ui.click(m)) return false;
        int i = ui.hitIndex();
        switch (ui.hitRegion()) {
            case BayUi.RAIL: ui.setRail(i); return true;
            case BayUi.SELL: world.hangar.sellShards(); return true;
            case BayUi.HULL:
                if (ui.hull() == i && ui.tierFocus() < 0) return actHull(); // a second click acts
                ui.setHull(i);
                ui.setTierFocus(-1);
                if (world.hangar.ownsShip(i)) world.selectShip(i);          // a hull you own is fitted by the first click; a purchase waits for the second
                return true;
            case BayUi.TIER:
                ui.setTierFocus(i);
                return i == 0 ? actHull() : actTier(i);
            case BayUi.ARM:
                if (ui.arm() == i) return actWeapon(i);
                ui.setArm(i);
                return true;
            case BayUi.ASTRA_CARD:
                if (ui.astra() == i) return actAstra(i);
                ui.setAstra(i);
                return true;
            default:
                return true;
        }
    }

    public void hover(Vector2 m) {
        ui.hover(m);
    }

    /** The strip card (and tier 0): an owned hull is fitted, an unowned one bought then fitted. */
    private boolean actHull() {
        int i = ui.hull();
        if (!world.hangar.ownsShip(i)) {
            if (world.hangar.buyShip(i, world.lokaIndex) <= 0) return true; // short, or the loka has not opened it
        }
        world.selectShip(i);
        if (ui.tierFocus() == 0) world.hangar.look[i] = 0;   // the x.1 card wears the first colourway
        return true;
    }

    /** A tier card: an unowned tier is bought (the chain rules apply); an OWNED one is WORN - the hull takes that colourway (the stats follow
     *  the tiers owned, the look follows the card). */
    private boolean actTier(int t) {
        ShipDef d = ShipDef.ALL[ui.hull()];
        if (world.hangar.owned[d.id][t]) {
            if (world.hangar.ownsShip(d.id)) {
                world.hangar.look[d.id] = t;
                world.selectShip(d.id);
            }
            return true;
        }
        world.hangar.buy(d, t);
        return true;
    }

    /** The wheel's one key: an unowned pattern is bought and fitted, an owned one is fitted, and the
     *  one already in the guns is tempered to its next rank. */
    private boolean actWeapon(int i) {
        if (!world.hangar.weaponOwned[i]) {
            if (world.hangar.buyWeapon(i)) world.selectWeapon(i);
            return true;
        }
        if (world.hangar.weaponIndex != i) {
            world.selectWeapon(i);
            return true;
        }
        if (world.hangar.buyRank(i)) world.selectWeapon(i); // refit: the guns now fire the tempered pattern
        return true;
    }

    /** An astra card: it becomes the one F invokes. */
    private boolean actAstra(int kind) {
        if (kind >= 0 && kind < vajra.world.Astra.KINDS) world.astraSel = kind;
        return true;
    }
}
