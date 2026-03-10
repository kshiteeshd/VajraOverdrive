package core;

import entity.EntityManager;
import input.InputManager;
import player.PlayerShip;
import ui.GameUILayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import spawn.SpawnManager;

public class GameCanvas extends JPanel {

    private InputManager input;
    private int fps;
    private GameUILayout uiLayout;
    private SpawnManager spawnManager;
    private EntityManager entityManager;



    public GameCanvas(){
        //Basic Settings
        setBackground(Color.black);
        input = new InputManager();
        setFocusable(true);
        addKeyListener(input);
        entityManager = new EntityManager();
        spawnManager = new SpawnManager(entityManager);
        uiLayout = new GameUILayout();

        //Spawning Elements
        spawnManager.spawnPlayer();


    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;


        uiLayout.render(g2);
        entityManager.render(g);

        g.setColor(Color.white);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        g.drawString(""+fps,10,20);//Explicit Conversion


    }

    public void update(){
        entityManager.update();
    }

    public void setFPS(int fps){
        this.fps=fps;
    }
}
