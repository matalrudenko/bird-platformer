import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import game2D.*;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. By default GameCore
// will handle the 'Escape' key to quit the game but you should
// override this with your own event handler.

/**
 * @author David Cairns
 *
 */
@SuppressWarnings("serial")

public class Game extends GameCore
{
	// Useful game constants
	static int screenWidth = 1056;
	static int screenHeight = 608;
	static int tileWidth = 0;
	static int tileHeight = 0;
	static int playerTileXL = 0;
    static int playerTileXR = 0;
    static int playerTileYT = 0;
	static int playerTileYB = 0;
	static int playerXL = 0;
    static int playerXR = 0;
    static int playerYT = 0;
    static int playerYB = 0;

    private enum Screen {
        START,
        FAIL,
        L1,
        L2,
        END
    }

    private Screen screen;

    private String directionString;
    private boolean showString = false;

    float 	lift = 0.005f;
    float	gravity = 0.0001f;
    
    // Game state flags
    boolean flap = false;
    boolean passedL1 = false;

    // Game resources
    Animation landing;
    Animation enemyRun;
    Animation dot;
    
    Sprite	player = null;
    ArrayList<Sprite> dots = new ArrayList<>();
    ArrayList<Sprite> enemies = new ArrayList<>();
    ArrayList<Sprite> clouds = new ArrayList<Sprite>();

    Sprite currentDot;
    int curDotIndex;

    TileMap tmap = new TileMap();	// Our tile map, note that we load it in init()
    
    long total;         			// The score will be the total time elapsed since a crash


    /**
	 * The obligatory main method that creates
     * an instance of our class and starts it running
     * 
     * @param args	The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) {

        Game gct = new Game();
        gct.init();
        // Start in windowed mode with the given screen height and width
        gct.run(false,screenWidth,screenHeight);
    }

    private void clearAll() {
        tmap = new TileMap();
        enemies.clear();
        dots.clear();
    }

    /**
     * Initialise the class, e.g. set up variables, load images,
     * create animations, register event handlers
     */
    public void init()
    {
        screen = Screen.START;
        initL1();
    }

    private void addEnemy(int x, int y) {
        Sprite s = new Sprite(enemyRun);
        s.setX(x * tileWidth);
        s.setY(y * tileHeight + 9);
        s.setVelocityX(0.04f);
        s.show();
        enemies.add(s);
    }

    private void addDot(int x, int y) {
        Sprite s = new Sprite(dot);
        s.setX(x * tileWidth);
        s.setY(y * tileHeight + 5);
        s.hide();
        dots.add(s);
    }

    private void initL1() {
        Sprite s;	// Temporary reference to a sprite

        // Load the tile map and print it out so we can check it is valid
        tmap.loadMap("maps", "map.txt");
        tileWidth = tmap.getTileWidth();
        tileHeight = tmap.getTileHeight();

        // Create a set of background sprites that we can
        // rearrange to give the illusion of motion

        landing = new Animation();
        landing.loadAnimationFromSheet("images/landbird.png", 4, 1, 60);

        // Initialise the player with an animation
        player = new Sprite(landing);

        // Load a single cloud animation
        Animation ca = new Animation();
        ca.addFrame(loadImage("images/cloud.png"), 1000);

        enemyRun = new Animation();
        enemyRun.addFrame(loadImage("images/e1.png"), 100);
        enemyRun.addFrame(loadImage("images/e2.png"), 100);
        enemyRun.addFrame(loadImage("images/e3.png"), 100);
        enemyRun.addFrame(loadImage("images/e4.png"), 100);
        enemyRun.addFrame(loadImage("images/e5.png"), 100);
        enemyRun.addFrame(loadImage("images/e6.png"), 100);
        enemyRun.addFrame(loadImage("images/e7.png"), 100);

        dot = new Animation();
        dot.addFrame(loadImage("images/dot.png"), 100);

        addEnemy(26, 6);
        addEnemy(26, 14);
        addEnemy(16, 8);
        addEnemy(10, 10);
        addEnemy(2, 14);
        addEnemy(14, 13);

        addDot(10, 4);
        addDot(11, 15);
        addDot(4, 15);
        addDot(27, 5);
        addDot(12, 10);

        directionString = "proceed here for level 2 ->";

        // Create 3 clouds at random positions off the screen
        // to the right
        for (int c=0; c<3; c++)
        {
            s = new Sprite(ca);
            s.setX(screenWidth + (int)(Math.random()*200.0f));
            s.setY(30 + (int)(Math.random()*150.0f));
            s.setVelocityX(-0.02f);
            clouds.add(s);
        }

        dots.get(0).show();
        currentDot = dots.get(0);
        curDotIndex = 0;

        initialiseGame();

        System.out.println(tmap);
    }

    /**
     * You will probably want to put code to restart a game in
     * a separate method so that you can call it to restart
     * the game.
     */
    public void initialiseGame()
    {
    	total = 0;

    	if (!(screen == Screen.START)) {
            player.setX(64);
            player.setY(280);
            player.setVelocityX(0);
            player.setVelocityY(0);
            player.show();
        }
    }
    
    /**
     * Draw the current state of the game
     */
    public void draw(Graphics2D g)
    {    	
    	// Be careful about the order in which you draw objects - you
    	// should draw the background first, then work your way 'forward'

    	// First work out how much we need to shift the view 
    	// in order to see where the player is.
        int xo = 10;
        int yo = 10;
        
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (screen == Screen.START) {
            g.setColor(Color.black);
            g.drawString("Welcome to the CSCU9N6 demo!", 430, 230);
            g.drawString("Control the bird with arrow keys", 430, 250);
            g.drawString("Collect 5 dots", 430, 270);
            g.drawString("Click on the switch to open the road", 430, 290);
            g.drawString("Do not touch the aliens!", 430, 310);
            g.drawString("Press Esc to exit", 430, 330);
            g.drawString("Press Space to start the level", 430, 350);
        } else if (screen == Screen.FAIL) {
            g.setColor(Color.black);
            g.drawString("You died!", 490, 230);
            g.drawString("Press space bar to restart the level", 430, 250);
        } else if (screen == Screen.END) {
            g.setColor(Color.black);
            g.drawString("Demo over!", 490, 230);
            g.drawString("Thank you for playing!", 470, 250);
        } else {
            // Apply offsets to sprites then draw them
            for (Sprite s: clouds)
            {
                s.setOffsets(xo,yo);
                s.drawTransformed(g);
            }

            for (Sprite s: dots) {
                s.setOffsets(xo,yo);
                s.setScale(3.0f);
                s.drawTransformed(g);
//            g.setColor(Color.blue);
//            g.drawRect((int)s.getX() + 10,
//                    (int)s.getY() + 10,
//                    s.getImage().getWidth(null)*3,
//                    s.getImage().getHeight(null)*3);
            }

            for (Sprite s : enemies) {
                s.setOffsets(xo, yo);
                s.draw(g);
            }

            if (showString) {
                g.setColor(Color.black);
                g.drawString(directionString, 830, 380);
            }

            // Apply offsets to player and draw
            player.setOffsets(xo, yo);
            player.draw(g);

            // Apply offsets to tile map and draw  it
            tmap.draw(g,xo,yo);
        }
    }

    /**
     * Update any sprites and check for collisions
     * 
     * @param elapsed The elapsed time between this call and the previous call of elapsed
     */    
    public void update(long elapsed)
    {

        if (!(screen == Screen.L1 || screen == Screen.L2)) return;
    	
        // Make adjustments to the speed of the sprite due to gravity
        player.setVelocityY(player.getVelocityY()+(gravity*elapsed));
    	    	
       	player.setAnimationSpeed(1.0f);
       	
       	if (flap) 
       	{
       		player.setAnimationSpeed(1.8f);
       		player.setVelocityY(-0.08f);
       	}
                
       	for (Sprite s: clouds)
       		s.update(elapsed);

       	for (Sprite s: enemies)
       	    s.update(elapsed);

        for (Sprite s: dots)
            s.update(elapsed);
       	
        // Now update the sprites animation and position
        player.update(elapsed);
        updatePlayerCoords();
       
        // Then check for any collisions that may have occurred
        handlePlayerCollisions(elapsed);
        handleEnemyTileEdges();
    }
    
    
    /**
     * Checks and handles collisions with the tile map for the
     * given sprite 's'. Initial functionality is limited...
     *
     * @param elapsed	How time has gone by
     */
    public void handlePlayerCollisions(long elapsed)
    {
    	// This method should check actual tile map collisions. For
    	// now it just checks if the player has gone off the bottom
    	// of the tile map.

        if (screen == Screen.FAIL || screen == Screen.END) return;
    	
        if (player.getX()  > tmap.getPixelWidth()) {
            if (passedL1) {
                clearAll();
                screen = Screen.END;
            } else {
                showString = false;

                passedL1 = true;
                screen = Screen.L2;
                clearAll();
                initL2();
            }
        }

        if (tmap.valid(playerTileXL, playerTileYT) && tmap.valid(playerTileXL, playerTileYB)) {
            if (tmap.getTileChar(playerTileXL, playerTileYT) != '.') {
                player.setX((playerTileXL + 1) * tileWidth - 13);
                updatePlayerCoords();
                player.setVelocityX(0.0f);
            }
        }

        if (tmap.valid(playerTileXR, playerTileYT) && tmap.valid(playerTileXR, playerTileYB)) {
            if (tmap.getTileChar(playerTileXR, playerTileYT) != '.') {
                player.setX((playerTileXR * tileWidth - (playerXR - playerXL) - 18));
                updatePlayerCoords();
                player.setVelocityX(0.0f);
            }
        }

        if (tmap.valid(playerTileXL, playerTileYB) && tmap.valid(playerTileXR, playerTileYB)) {
            if (tmap.getTileChar(playerTileXL, playerTileYB) != '.' ||
                    tmap.getTileChar(playerTileXR, playerTileYB) != '.') {
                player.setY(playerTileYB * tileHeight - (playerYB - playerYT) - 5);
                updatePlayerCoords();
                player.setVelocityY(-player.getVelocityY() * (0.03f * elapsed));
            }
        }

        if (tmap.valid(playerTileXL, playerTileYT) && tmap.valid(playerTileXR, playerTileYT)) {
            if (tmap.getTileChar(playerTileXL, playerTileYT) != '.' ||
                    tmap.getTileChar(playerTileXR, playerTileYT) != '.') {
                player.setY((playerTileYT + 1) * tileHeight + 5);
                updatePlayerCoords();
                player.setVelocityY(-player.getVelocityY() * (0.03f * elapsed));
            }
        }

        for (Sprite s : enemies) {
            if (playerXR >= s.getX() + 15 && playerXL <= s.getX() + s.getWidth() - 15 &&
                playerYB >= s.getY() + 15 && playerYT <= s.getY() + s.getHeight() - 10) {

                Sound caw = new Sound("sounds/caw.wav");
                caw.start();
                screen = Screen.FAIL;
                clearAll();
                return;
            }
        }

        if (playerXR >= currentDot.getX() + 10 &&
                playerXL <= currentDot.getX() + (currentDot.getWidth() * 3) &&
                playerYB >= currentDot.getY() + 10 &&
                playerYT <= currentDot.getY() + (currentDot.getHeight() * 3)) {
            currentDot.hide();
            curDotIndex++;
            if (curDotIndex < dots.size()) {
                Sound boop = new Sound("sounds/boop.wav");
                boop.start();
                currentDot = dots.get(curDotIndex);
                currentDot.show();
            } else {
                Sound groovey = new Sound("sounds/groovey.wav");
                groovey.start();
                if (screen == Screen.L1) {
                    tmap.setTileChar('b', 31, 9);
                    tmap.setTileChar('.', 31, 10);
                    tmap.setTileChar('.', 31, 11);
                    tmap.setTileChar('t', 31, 12);
                } else if (screen == Screen.L2) {
                    tmap.setTileChar('b', 31, 10);
                    tmap.setTileChar('.', 31, 11);
                    tmap.setTileChar('.', 31, 12);
                    tmap.setTileChar('t', 31, 13);
                }
                showString = true;
            }
        }
    }
    
    private void updatePlayerCoords() {
        playerXL = (int)player.getX() + 15;
        playerXR = playerXL + player.getImage().getWidth(null) - 25;
        playerYT = (int)player.getY() + 5;
        playerYB = playerYT + player.getImage().getHeight(null) - 10;
        playerTileXL = playerXL / tileWidth;
        playerTileXR = playerXR / tileWidth;
        playerTileYT = playerYT / tileHeight;
        playerTileYB = playerYB / tileHeight;
    }

    private void handleEnemyTileEdges() {
        for (Sprite s : enemies) {
            int spriteTileR = (int)(s.getX() + s.getImage().getWidth(null) - 10) / tileWidth;
            int spriteTileL = (int) (s.getX() + 15) / tileWidth;
            int spriteTileB = (int) (s.getY() + s.getImage().getWidth(null) + 20) / tileHeight;
            if (tmap.getTileChar(spriteTileR, spriteTileB) != 'g') {
                s.setX(s.getX() - 3);
                s.flip();
                s.setVelocityX(-s.getVelocityX());
            } else if (tmap.getTileChar(spriteTileL, spriteTileB) != 'g') {
                s.setX(s.getX() + 3);
                s.flip();
                s.setVelocityX(-s.getVelocityX());
            }
        }
    }
     
    /**
     * Override of the keyPressed event defined in GameCore to catch our
     * own events
     * 
     *  @param e The event that has been generated
     */
    public void keyPressed(KeyEvent e) 
    { 
    	int key = e.getKeyCode();
    	
    	if (key == KeyEvent.VK_ESCAPE) stop();
    	
    	if (key == KeyEvent.VK_UP) flap = true;

    	if (key == KeyEvent.VK_RIGHT) player.setVelocityX(0.14f);

        if (key == KeyEvent.VK_LEFT) player.setVelocityX(-0.14f);
    }

    public void mouseClicked(MouseEvent e) {
        int x = e.getX() / tileWidth;
        int y = e.getY() / tileHeight;
        if (screen == screen.L1) {
            if (x >= 18 && x <= 19 && y >= 15 && y <= 16) {
                Sound lever = new Sound("sounds/lever.wav");
                lever.start();
                if (tmap.getTileChar(18, 15) == 'l') {
                    tmap.setTileChar('r', 18, 15);
                    tmap.setTileChar('p', 5, 6);
                    tmap.setTileChar('b', 5, 7);
                    tmap.setTileChar('t', 5, 8);
                    tmap.setTileChar('p', 5, 9);

                    tmap.setTileChar('b', 20, 7);
                    tmap.setTileChar('.', 20, 8);
                    tmap.setTileChar('.', 20, 9);
                    tmap.setTileChar('t', 20, 10);
                } else {
                    tmap.setTileChar('l', 18, 15);
                    tmap.setTileChar('b', 5, 6);
                    tmap.setTileChar('.', 5, 7);
                    tmap.setTileChar('.', 5, 8);
                    tmap.setTileChar('t', 5, 9);

                    tmap.setTileChar('p', 20, 7);
                    tmap.setTileChar('b', 20, 8);
                    tmap.setTileChar('t', 20, 9);
                    tmap.setTileChar('p', 20, 10);
                }
            }
        } else if (screen == Screen.L2) {
            if (x >= 15 && x <= 16 && y >= 6 && y <= 7) {
                Sound lever = new Sound("sounds/lever.wav");
                lever.start();
                if (tmap.getTileChar(15, 6) == 'l') {
                    tmap.setTileChar('r', 15, 6);
                    tmap.setTileChar('p', 10, 2);
                    tmap.setTileChar('b', 10, 3);
                    tmap.setTileChar('t', 10, 4);
                    tmap.setTileChar('p', 10, 5);

                    tmap.setTileChar('x', 4, 10);
                    tmap.setTileChar('c', 5, 10);
                    tmap.setTileChar('z', 6, 10);
                    tmap.setTileChar('x', 7, 10);
//
                    tmap.setTileChar('b', 20, 2);
                    tmap.setTileChar('.', 20, 3);
                    tmap.setTileChar('.', 20, 4);
                    tmap.setTileChar('t', 20, 5);

                    tmap.setTileChar('c', 24, 10);
                    tmap.setTileChar('.', 25, 10);
                    tmap.setTileChar('.', 26, 10);
                    tmap.setTileChar('z', 27, 10);
                } else {
                    tmap.setTileChar('l', 15, 6);
                    tmap.setTileChar('b', 10, 2);
                    tmap.setTileChar('.', 10, 3);
                    tmap.setTileChar('.', 10, 4);
                    tmap.setTileChar('t', 10, 5);

                    tmap.setTileChar('c', 4, 10);
                    tmap.setTileChar('.', 5, 10);
                    tmap.setTileChar('.', 6, 10);
                    tmap.setTileChar('z', 7, 10);
//
                    tmap.setTileChar('p', 20, 2);
                    tmap.setTileChar('b', 20, 3);
                    tmap.setTileChar('t', 20, 4);
                    tmap.setTileChar('p', 20, 5);

                    tmap.setTileChar('x', 24, 10);
                    tmap.setTileChar('c', 25, 10);
                    tmap.setTileChar('z', 26, 10);
                    tmap.setTileChar('x', 27, 10);
                }
            }
        }
    }

    public boolean boundingBoxCollision(Sprite s1, Sprite s2)
    {
    	return false;   	
    }


	public void keyReleased(KeyEvent e) { 

		int key = e.getKeyCode();

		// Switch statement instead of lots of ifs...
		// Need to use break to prevent fall through.
		switch (key)
		{
			case KeyEvent.VK_ESCAPE : stop(); break;
			case KeyEvent.VK_UP     : flap = false; break;
			case KeyEvent.VK_RIGHT: player.setVelocityX(0.0f); break;
            case KeyEvent.VK_LEFT: player.setVelocityX(0.0f); break;
            case KeyEvent.VK_SPACE:
                if (screen == Screen.START || screen == Screen.FAIL) {
                    if (passedL1) {
                        screen = Screen.L2;
                        clearAll();
                        initL2();
                    } else {
                        screen = Screen.L1;
                        clearAll();
                        initL1();
                    }
                }
			default :  break;
		}
	}

	private void initL2() {
        Sprite s;	// Temporary reference to a sprite

        // Load the tile map and print it out so we can check it is valid
        tmap.loadMap("maps", "map2.txt");
        tileWidth = tmap.getTileWidth();
        tileHeight = tmap.getTileHeight();

        // Create a set of background sprites that we can
        // rearrange to give the illusion of motion

        landing = new Animation();
        landing.loadAnimationFromSheet("images/landbird.png", 4, 1, 60);

        // Initialise the player with an animation
        player = new Sprite(landing);

        // Load a single cloud animation
        Animation ca = new Animation();
        ca.addFrame(loadImage("images/cloud.png"), 1000);

        enemyRun = new Animation();
        enemyRun.addFrame(loadImage("images/e1.png"), 100);
        enemyRun.addFrame(loadImage("images/e2.png"), 100);
        enemyRun.addFrame(loadImage("images/e3.png"), 100);
        enemyRun.addFrame(loadImage("images/e4.png"), 100);
        enemyRun.addFrame(loadImage("images/e5.png"), 100);
        enemyRun.addFrame(loadImage("images/e6.png"), 100);
        enemyRun.addFrame(loadImage("images/e7.png"), 100);

        dot = new Animation();
        dot.addFrame(loadImage("images/dot.png"), 100);

        addEnemy(4, 14);
        addEnemy(23, 11);
        addEnemy(16, 8);
        addEnemy(25, 4);
        addEnemy(2, 4);
//
        addDot(2, 12);
        addDot(20, 12);
        addDot(15, 5);
        addDot(7, 4);
        addDot(21, 5);

        directionString = "proceed here to end demo ->";

        // Create 3 clouds at random positions off the screen
        // to the right
        for (int c=0; c<3; c++)
        {
            s = new Sprite(ca);
            s.setX(screenWidth + (int)(Math.random()*200.0f));
            s.setY(30 + (int)(Math.random()*150.0f));
            s.setVelocityX(-0.02f);
            clouds.add(s);
        }

        dots.get(0).show();
        currentDot = dots.get(0);
        curDotIndex = 0;

        initialiseGame();
    }
}
