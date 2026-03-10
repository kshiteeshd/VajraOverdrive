package spawn;

import config.CombatArea;
import entity.EntityManager;
import player.PlayerShip;

public class SpawnManager {

    private EntityManager entityManager;

    public SpawnManager(EntityManager entityManager){
        this.entityManager = entityManager;
    }

    public void spawnPlayer(){

        int scale = 2;
        int shipSize = 16 * scale;

        int spawnX = CombatArea.LEFT_BOUND +
                (CombatArea.WIDTH / 2) - (shipSize / 2);

        int spawnY = CombatArea.TOP_BOUND +
                (int)(CombatArea.HEIGHT * 0.7);

        PlayerShip player = new PlayerShip(spawnX, spawnY, scale);

        entityManager.addEntities(player);
    }

}