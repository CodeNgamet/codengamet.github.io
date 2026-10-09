import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class StarHopper extends JPanel implements ActionListener, KeyListener {

    static final int WIDTH = 800;
    static final int HEIGHT = 600;
    static final int HUD_HEIGHT = 55;

    private final Timer timer = new Timer(16, this);
    private final Random random = new Random();

    private final ArrayList<Bullet> bullets = new ArrayList<>();
    private final ArrayList<EnemyBullet> enemyBullets = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Star> stars = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final ArrayList<PowerUp> powerUps = new ArrayList<>();

    private Player player1;
    private Player player2;

    private BufferedImage playerOneImage;
    private BufferedImage playerTwoImage;
    private BufferedImage basicEnemyImage;
    private BufferedImage shooterEnemyImage;
    private BufferedImage fastEnemyImage;

    private int mode = 0;
    private int selectedMode = 1;

    private int score1 = 0;
    private int score2 = 0;
    private int level = 1;
    private int wave = 1;

    private boolean started = false;
    private boolean gameOver = false;
    private boolean paused = false;

    private int spawnCooldown = 35;
    private int enemyShotCooldown = 70;

    private boolean musicStarted = false;
    private Process musicProcess;

    private Font pixelFont;

    public StarHopper() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        loadPixelFont();
        loadImages();

        player1 = new Player(
                1,
                new Color(70, 190, 255),
                WIDTH / 2 - 80,
                HEIGHT - 90,
                playerOneImage
        );

        player2 = new Player(
                2,
                new Color(255, 170, 70),
                WIDTH / 2 + 30,
                HEIGHT - 90,
                playerTwoImage
        );

        for (int i = 0; i < 100; i++) {
            stars.add(new Star(
                    random.nextInt(WIDTH),
                    random.nextInt(HEIGHT),
                    1 + random.nextInt(3),
                    1 + random.nextInt(3)
            ));
        }

        timer.start();
    }

    private void loadPixelFont() {
        try {
            File fontFile = new File("PressStart2P-Regular.ttf");
            pixelFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);

            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(pixelFont);

        } catch (Exception e) {
            pixelFont = new Font("Monospaced", Font.BOLD, 16);
        }
    }

    private void loadImages() {
        playerOneImage = loadImage("player_one.png");
        playerTwoImage = loadImage("player_two.png");
        basicEnemyImage = loadImage("BASIC.png");
        shooterEnemyImage = loadImage("SHOOTER.png");
        fastEnemyImage = loadImage("FAST.png");

        System.out.println("Image loading finished.");
        System.out.println("Player 1: " + (playerOneImage != null));
        System.out.println("Player 2: " + (playerTwoImage != null));
        System.out.println("BASIC enemy: " + (basicEnemyImage != null));
        System.out.println("SHOOTER enemy: " + (shooterEnemyImage != null));
        System.out.println("FAST enemy: " + (fastEnemyImage != null));
    }

    private BufferedImage loadImage(String filename) {
        try {
            BufferedImage image = ImageIO.read(new File(filename));

            if (image == null) {
                System.out.println("Could not read " + filename);
            }

            return image;

        } catch (IOException e) {
            System.out.println(
                    "Could not load " + filename +
                    ". Make sure it is in the same folder as StarHopper.java."
            );
            return null;
        }
    }

    private Font pixel(float size) {
        return pixelFont.deriveFont(size);
    }

    private void startMusicOnce() {
        if (musicStarted) {
            return;
        }

        musicStarted = true;

        try {
            File music = new File("retro.mp3").getAbsoluteFile();

            if (!music.isFile()) {
                return;
            }

            musicProcess = new ProcessBuilder(
                    "mpv",
                    "--no-video",
                    "--really-quiet",
                    "--loop-file=inf",
                    music.getAbsolutePath()
            ).redirectErrorStream(true).start();

        } catch (IOException ignored) {
            musicProcess = null;
        }
    }

    private void stopMusic() {
        if (musicProcess != null) {
            musicProcess.destroy();
            musicProcess = null;
        }
    }

    private void resetGame() {
        score1 = 0;
        score2 = 0;
        level = 1;
        wave = 1;

        spawnCooldown = 30;
        enemyShotCooldown = 65;

        gameOver = false;
        paused = false;
        started = true;

        bullets.clear();
        enemyBullets.clear();
        enemies.clear();
        particles.clear();
        powerUps.clear();

        player1.reset(WIDTH / 2 - 80, HEIGHT - 90);

        if (mode == 2) {
            player2.reset(WIDTH / 2 + 30, HEIGHT - 90);
        } else {
            player2.alive = false;
            player2.lives = 0;
        }

        startMusicOnce();
        requestFocusInWindow();
    }

    private void returnToMenu() {
        started = false;
        gameOver = false;
        paused = false;

        bullets.clear();
        enemyBullets.clear();
        enemies.clear();
        particles.clear();
        powerUps.clear();

        player1.alive = true;
        player2.alive = true;

        requestFocusInWindow();
    }

    private boolean anyPlayerAlive() {
        if (player1.alive) {
            return true;
        }

        return mode == 2 && player2.alive;
    }

    private Player randomLivingPlayer() {
        boolean p1Alive = player1.alive;
        boolean p2Alive = mode == 2 && player2.alive;

        if (p1Alive && p2Alive) {
            return random.nextBoolean() ? player1 : player2;
        }

        if (p1Alive) {
            return player1;
        }

        if (p2Alive) {
            return player2;
        }

        return null;
    }

    private void shoot(Player player) {
        if (!player.alive || player.shootCooldown > 0) {
            return;
        }

        int centerX = player.x + player.width / 2;
        int startY = player.y - 8;

        bullets.add(new Bullet(
                centerX,
                startY,
                0,
                -10,
                player.id
        ));

        player.shootCooldown = player.rapidFire ? 5 : 10;
    }

    private void spawnEnemy() {
        int roll = random.nextInt(100);
        Enemy.Type type;

        if (level >= 3 && roll < 20) {
            type = Enemy.Type.SHOOTER;
        } else if (level >= 2 && roll < 38) {
            type = Enemy.Type.FAST;
        } else {
            type = Enemy.Type.BASIC;
        }

        int size;
        double speed;

        if (type == Enemy.Type.FAST) {
            size = 23 + random.nextInt(12);
            speed = 2.8 + level * 0.20 + random.nextDouble();

        } else if (type == Enemy.Type.SHOOTER) {
            size = 45 + random.nextInt(10);
            speed = 1.2 + level * 0.15 + random.nextDouble() * 0.7;

        } else {
            size = 48 + random.nextInt(16);
            speed = 1.5 + level * 0.25 + random.nextDouble() * 1.5;
        }

        int x = random.nextInt(Math.max(1, WIDTH - size));

        enemies.add(new Enemy(
                x,
                -size,
                size,
                speed,
                type
        ));
    }

    private void enemyShoot(Enemy enemy) {
        Player target = randomLivingPlayer();

        if (target == null) {
            return;
        }

        double startX = enemy.x + enemy.size / 2.0;
        double startY = enemy.y + enemy.size;

        double targetX = target.x + target.width / 2.0;
        double targetY = target.y + target.height / 2.0;

        double dx = targetX - startX;
        double dy = targetY - startY;

        double distance = Math.max(
                1.0,
                Math.sqrt(dx * dx + dy * dy)
        );

        double speed = 4.0 + Math.min(2.0, level * 0.12);

        enemyBullets.add(new EnemyBullet(
                startX,
                startY,
                dx / distance * speed,
                dy / distance * speed
        ));
    }

    private void damagePlayer(Player player) {
        if (!player.alive || player.invincibility > 0) {
            return;
        }

        player.lives--;
        player.invincibility = 90;

        createExplosion(
                player.x + player.width / 2,
                player.y + player.height / 2,
                30
        );

        if (player.lives <= 0) {
            player.lives = 0;
            player.alive = false;

            createExplosion(
                    player.x + player.width / 2,
                    player.y + player.height / 2,
                    50
            );

            if (!anyPlayerAlive()) {
                gameOver = true;
            }
        }
    }

    private void createExplosion(int x, int y, int amount) {
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1 + random.nextDouble() * 4;

            particles.add(new Particle(
                    x,
                    y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    25 + random.nextInt(30)
            ));
        }
    }

    private void updateGame() {
        updateStars();
        updateParticles();

        if (!started || gameOver || paused) {
            return;
        }

        player1.update();

        if (mode == 2) {
            player2.update();
        }

        if (player1.shooting) {
            shoot(player1);
        }

        if (mode == 2 && player2.shooting) {
            shoot(player2);
        }

        level = 1 + (score1 + score2) / 100;
        wave = 1 + (score1 + score2) / 250;

        spawnCooldown--;

        if (spawnCooldown <= 0) {
            spawnEnemy();

            spawnCooldown = Math.max(
                    12,
                    55 - level * 3
            );
        }

        enemyShotCooldown--;

        if (enemyShotCooldown <= 0
                && !enemies.isEmpty()
                && anyPlayerAlive()) {

            int shots = level >= 5 ? 2 : 1;

            for (int i = 0; i < shots; i++) {
                Enemy enemy = enemies.get(
                        random.nextInt(enemies.size())
                );

                if (enemy.type == Enemy.Type.SHOOTER) {
                    enemyShoot(enemy);
                } else if (random.nextInt(100) < 30) {
                    enemyShoot(enemy);
                }
            }

            enemyShotCooldown = Math.max(
                    25,
                    75 - level * 4
            );
        }

        updateBullets();
        updateEnemies();
        updateEnemyBullets();
        updatePowerUps();
    }

    private void updateBullets() {
        for (Bullet bullet : bullets) {
            bullet.x += bullet.vx;
            bullet.y += bullet.vy;
        }

        bullets.removeIf(
                bullet ->
                        bullet.y < -30
                                || bullet.x < -30
                                || bullet.x > WIDTH + 30
        );

        Iterator<Bullet> bulletIterator =
                bullets.iterator();

        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();

            Iterator<Enemy> enemyIterator =
                    enemies.iterator();

            boolean hit = false;

            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();

                if (!bullet.getBounds().intersects(
                        enemy.getBounds())) {
                    continue;
                }

                int points;

                if (enemy.type == Enemy.Type.FAST) {
                    points = 15;
                } else if (enemy.type == Enemy.Type.SHOOTER) {
                    points = 20;
                } else {
                    points = 10;
                }

                if (bullet.owner == 1) {
                    score1 += points;
                } else {
                    score2 += points;
                }

                createExplosion(
                        enemy.x + enemy.size / 2,
                        enemy.y + enemy.size / 2,
                        18
                );

                if (random.nextInt(100) < 15) {
                    powerUps.add(new PowerUp(
                            enemy.x + enemy.size / 2,
                            enemy.y + enemy.size / 2
                    ));
                }

                enemyIterator.remove();
                hit = true;
                break;
            }

            if (hit) {
                bulletIterator.remove();
            }
        }
    }

    private void updateEnemies() {
        Iterator<Enemy> iterator = enemies.iterator();

        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();

            enemy.update();

            if (enemy.y > HEIGHT + 30) {
                iterator.remove();
                continue;
            }

            if (player1.alive
                    && enemy.getBounds().intersects(
                            player1.getBounds())) {

                damagePlayer(player1);
                iterator.remove();
                continue;
            }

            if (mode == 2
                    && player2.alive
                    && enemy.getBounds().intersects(
                            player2.getBounds())) {

                damagePlayer(player2);
                iterator.remove();
            }
        }
    }

    private void updateEnemyBullets() {
        for (EnemyBullet bullet : enemyBullets) {
            bullet.x += bullet.vx;
            bullet.y += bullet.vy;
        }

        enemyBullets.removeIf(
                bullet ->
                        bullet.x < -40
                                || bullet.x > WIDTH + 40
                                || bullet.y < -40
                                || bullet.y > HEIGHT + 40
        );

        Iterator<EnemyBullet> iterator =
                enemyBullets.iterator();

        while (iterator.hasNext()) {
            EnemyBullet bullet = iterator.next();

            if (player1.alive
                    && bullet.getBounds().intersects(
                            player1.getBounds())) {

                iterator.remove();
                damagePlayer(player1);
                continue;
            }

            if (mode == 2
                    && player2.alive
                    && bullet.getBounds().intersects(
                            player2.getBounds())) {

                iterator.remove();
                damagePlayer(player2);
            }
        }
    }

    private void updatePowerUps() {
        Iterator<PowerUp> iterator =
                powerUps.iterator();

        while (iterator.hasNext()) {
            PowerUp powerUp = iterator.next();

            powerUp.y += 2;

            if (powerUp.y > HEIGHT + 30) {
                iterator.remove();
                continue;
            }

            if (player1.alive
                    && powerUp.getBounds().intersects(
                            player1.getBounds())) {

                powerUp.apply(player1);
                iterator.remove();
                continue;
            }

            if (mode == 2
                    && player2.alive
                    && powerUp.getBounds().intersects(
                            player2.getBounds())) {

                powerUp.apply(player2);
                iterator.remove();
            }
        }
    }

    private void updateStars() {
        for (Star star : stars) {
            star.y += star.speed;

            if (star.y > HEIGHT) {
                star.y = 0;
                star.x = random.nextInt(WIDTH);
            }
        }
    }

    private void updateParticles() {
        Iterator<Particle> iterator =
                particles.iterator();

        while (iterator.hasNext()) {
            Particle p = iterator.next();

            p.x += p.vx;
            p.y += p.vy;
            p.life--;

            if (p.life <= 0) {
                iterator.remove();
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        drawBackground(g2);

        if (!started) {
            drawModeScreen(g2);
        } else {
            drawGame(g2);
        }

        g2.dispose();
    }

    private void drawBackground(Graphics2D g) {
        g.setColor(new Color(10, 15, 35));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (Star star : stars) {
            int brightness =
                    Math.min(255, 100 + star.size * 45);

            g.setColor(new Color(
                    brightness,
                    brightness,
                    brightness
            ));

            g.fillOval(
                    star.x,
                    star.y,
                    star.size,
                    star.size
            );
        }
    }

    private void drawModeScreen(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(pixel(38));

        drawCentered(g, "STAR HOPPER", 120);

        g.setColor(new Color(70, 190, 255));
        g.setFont(pixel(15));

        drawCentered(
                g,
                "ARCADE SPACE SHOOTER",
                160
        );

        g.setColor(Color.WHITE);
        g.setFont(pixel(20));

        drawCentered(g, "CHOOSE MODE", 220);

        g.setColor(
                selectedMode == 1
                        ? new Color(255, 230, 70)
                        : new Color(100, 100, 120)
        );

        g.setFont(pixel(28));
        drawCentered(g, "1 PLAYER", 285);

        g.setColor(
                selectedMode == 2
                        ? new Color(255, 230, 70)
                        : new Color(100, 100, 120)
        );

        g.setFont(pixel(28));
        drawCentered(g, "2 PLAYER", 350);

        g.setColor(Color.WHITE);
        g.setFont(pixel(12));

        drawCentered(g, "1 OR 2 TO SELECT", 420);
        drawCentered(g, "ENTER TO START", 450);

        g.setColor(new Color(160, 160, 180));
        g.setFont(pixel(10));

        drawCentered(g, "P1 ARROWS + SPACE", 495);
        drawCentered(g, "P2 WASD + SHIFT", 525);
    }

    private void drawGame(Graphics2D g) {
        drawEnemies(g);
        drawPowerUps(g);
        drawBullets(g);
        drawEnemyBullets(g);

        player1.draw(g);

        if (mode == 2) {
            player2.draw(g);
        }

        drawParticles(g);
        drawHud(g);

        if (paused) {
            g.setColor(new Color(0, 0, 0, 190));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(pixel(35));

            drawCentered(g, "PAUSED", 280);

            g.setFont(pixel(12));
            drawCentered(g, "P TO CONTINUE", 330);
            drawCentered(g, "M FOR MENU", 365);
        }

        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(new Color(255, 70, 90));
            g.setFont(pixel(32));

            drawCentered(g, "GAME OVER", 220);

            g.setColor(Color.WHITE);
            g.setFont(pixel(13));

            if (mode == 2) {
                drawCentered(g, "P1 " + score1, 285);
                drawCentered(g, "P2 " + score2, 320);
            } else {
                drawCentered(g, "SCORE " + score1, 300);
            }

            g.setFont(pixel(10));

            drawCentered(
                    g,
                    "ENTER TO PLAY AGAIN",
                    375
            );

            drawCentered(
                    g,
                    "M FOR MAIN MENU",
                    410
            );
        }
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(15, 20, 40));
        g.fillRect(0, 0, WIDTH, HUD_HEIGHT);

        g.setFont(pixel(10));

        g.setColor(player1.color);

        g.drawString(
                "P1 " + score1 +
                        "  LIVES " + player1.lives,
                10,
                32
        );

        if (mode == 2) {
            g.setColor(player2.color);

            g.drawString(
                    "P2 " + score2 +
                            "  LIVES " + player2.lives,
                    560,
                    32
            );
        }

        g.setColor(Color.WHITE);

        g.drawString(
                "LV " + level +
                        "  WAVE " + wave,
                325,
                32
        );
    }

    private void drawEnemies(Graphics2D g) {
        for (Enemy enemy : enemies) {
            if (enemy.type == Enemy.Type.BASIC) {
                drawEnemyImage(
                        g,
                        basicEnemyImage,
                        enemy.x,
                        enemy.y,
                        enemy.size,
                        enemy.size
                );

            } else if (enemy.type == Enemy.Type.SHOOTER) {
                drawEnemyImage(
                        g,
                        shooterEnemyImage,
                        enemy.x,
                        enemy.y,
                        enemy.size,
                        enemy.size
                );

            } else {
                drawFastEnemy(g, enemy);
            }
        }
    }

    private void drawEnemyImage(
            Graphics2D g,
            BufferedImage image,
            int x,
            int y,
            int maxWidth,
            int maxHeight
    ) {
        if (image == null) {
            g.setColor(Color.RED);
            g.fillRect(x, y, maxWidth, maxHeight);
            return;
        }

        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        double scale = Math.min(
                (double) maxWidth / originalWidth,
                (double) maxHeight / originalHeight
        );

        int drawWidth =
                Math.max(1, (int) (originalWidth * scale));

        int drawHeight =
                Math.max(1, (int) (originalHeight * scale));

        int drawX =
                x + (maxWidth - drawWidth) / 2;

        int drawY =
                y + (maxHeight - drawHeight) / 2;

        g.drawImage(
                image,
                drawX,
                drawY,
                drawWidth,
                drawHeight,
                null
        );
    }

    private void drawFastEnemy(Graphics2D g, Enemy enemy) {
        drawEnemyImage(
                g,
                fastEnemyImage,
                enemy.x,
                enemy.y,
                enemy.size,
                enemy.size
        );
    }

    private void drawPowerUps(Graphics2D g) {
        for (PowerUp powerUp : powerUps) {
            g.setColor(new Color(80, 255, 120));

            g.fillOval(
                    (int) powerUp.x - 10,
                    (int) powerUp.y - 10,
                    20,
                    20
            );

            g.setColor(Color.BLACK);
            g.setFont(pixel(9));

            String text = "P";
            FontMetrics fm = g.getFontMetrics();

            g.drawString(
                    text,
                    (int) powerUp.x -
                            fm.stringWidth(text) / 2,
                    (int) powerUp.y + 4
            );
        }
    }

    private void drawBullets(Graphics2D g) {
        for (Bullet bullet : bullets) {
            if (bullet.owner == 1) {
                g.setColor(new Color(255, 240, 100));
            } else {
                g.setColor(new Color(100, 255, 255));
            }

            g.fillRoundRect(
                    (int) bullet.x,
                    (int) bullet.y,
                    5,
                    14,
                    5,
                    5
            );
        }
    }

    private void drawEnemyBullets(Graphics2D g) {
        g.setColor(new Color(255, 80, 80));

        for (EnemyBullet bullet : enemyBullets) {
            g.fillOval(
                    (int) bullet.x - 4,
                    (int) bullet.y - 4,
                    8,
                    8
            );
        }
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            int alpha = Math.max(
                    0,
                    Math.min(255, p.life * 8)
            );

            g.setColor(new Color(
                    255,
                    170,
                    40,
                    alpha
            ));

            g.fillOval(
                    (int) p.x,
                    (int) p.y,
                    5,
                    5
            );
        }
    }

    private void drawCentered(
            Graphics2D g,
            String text,
            int y
    ) {
        FontMetrics fm = g.getFontMetrics();

        int x =
                (WIDTH - fm.stringWidth(text)) / 2;

        g.drawString(text, x, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (!started) {
            if (key == KeyEvent.VK_1 ||
                    key == KeyEvent.VK_NUMPAD1) {

                selectedMode = 1;

            } else if (
                    key == KeyEvent.VK_2 ||
                    key == KeyEvent.VK_NUMPAD2) {

                selectedMode = 2;

            } else if (key == KeyEvent.VK_ENTER) {

                mode = selectedMode;
                resetGame();
            }

            return;
        }

        if (key == KeyEvent.VK_M) {
            returnToMenu();
            return;
        }

        if (key == KeyEvent.VK_P) {
            if (!gameOver) {
                paused = !paused;
            }
            return;
        }

        if (key == KeyEvent.VK_ENTER) {
            if (gameOver) {
                resetGame();
            }
            return;
        }

        if (paused || gameOver) {
            return;
        }

        // PLAYER 1: ARROWS + SPACE

        if (key == KeyEvent.VK_LEFT) {
            player1.left = true;
        }

        if (key == KeyEvent.VK_RIGHT) {
            player1.right = true;
        }

        if (key == KeyEvent.VK_UP) {
            player1.up = true;
        }

        if (key == KeyEvent.VK_DOWN) {
            player1.down = true;
        }

        if (key == KeyEvent.VK_SPACE) {
            player1.shooting = true;
        }

        // PLAYER 2: WASD + SHIFT

        if (mode == 2) {
            if (key == KeyEvent.VK_A) {
                player2.left = true;
            }

            if (key == KeyEvent.VK_D) {
                player2.right = true;
            }

            if (key == KeyEvent.VK_W) {
                player2.up = true;
            }

            if (key == KeyEvent.VK_S) {
                player2.down = true;
            }

            if (key == KeyEvent.VK_SHIFT) {
                player2.shooting = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        // PLAYER 1

        if (key == KeyEvent.VK_LEFT) {
            player1.left = false;
        }

        if (key == KeyEvent.VK_RIGHT) {
            player1.right = false;
        }

        if (key == KeyEvent.VK_UP) {
            player1.up = false;
        }

        if (key == KeyEvent.VK_DOWN) {
            player1.down = false;
        }

        if (key == KeyEvent.VK_SPACE) {
            player1.shooting = false;
        }

        // PLAYER 2

        if (mode == 2) {
            if (key == KeyEvent.VK_A) {
                player2.left = false;
            }

            if (key == KeyEvent.VK_D) {
                player2.right = false;
            }

            if (key == KeyEvent.VK_W) {
                player2.up = false;
            }

            if (key == KeyEvent.VK_S) {
                player2.down = false;
            }

            if (key == KeyEvent.VK_SHIFT) {
                player2.shooting = false;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private class Player {
        int id;
        int x;
        int y;

        int width = 50;
        int height = 40;

        int lives = 3;

        boolean alive = true;

        boolean left;
        boolean right;
        boolean up;
        boolean down;
        boolean shooting;

        boolean rapidFire = false;

        int shootCooldown = 0;
        int invincibility = 0;

        Color color;
        BufferedImage image;

        Player(
                int id,
                Color color,
                int x,
                int y,
                BufferedImage image
        ) {
            this.id = id;
            this.color = color;
            this.x = x;
            this.y = y;
            this.image = image;
        }

        void reset(int x, int y) {
            this.x = x;
            this.y = y;

            lives = 3;
            alive = true;

            left = false;
            right = false;
            up = false;
            down = false;
            shooting = false;

            rapidFire = false;

            shootCooldown = 0;
            invincibility = 0;
        }

        void update() {
            if (!alive) {
                return;
            }

            if (left) {
                x -= 6;
            }

            if (right) {
                x += 6;
            }

            if (up) {
                y -= 6;
            }

            if (down) {
                y += 6;
            }

            x = Math.max(
                    5,
                    Math.min(
                            WIDTH - width - 5,
                            x
                    )
            );

            y = Math.max(
                    HUD_HEIGHT + 8,
                    Math.min(
                            HEIGHT - height - 5,
                            y
                    )
            );

            if (shootCooldown > 0) {
                shootCooldown--;
            }

            if (invincibility > 0) {
                invincibility--;
            }
        }

        Rectangle getBounds() {
            return new Rectangle(
                    x + 5,
                    y,
                    width - 10,
                    height - 5
            );
        }

        void draw(Graphics2D g) {
            if (!alive) {
                return;
            }

            if (invincibility > 0 &&
                    (invincibility / 5) % 2 == 1) {
                return;
            }

            if (image != null) {
                g.drawImage(
                        image,
                        x,
                        y,
                        width,
                        height,
                        null
                );
            } else {
                Polygon ship = new Polygon();

                ship.addPoint(
                        x + width / 2,
                        y
                );

                ship.addPoint(
                        x + width - 2,
                        y + 34
                );

                ship.addPoint(
                        x + width / 2,
                        y + 27
                );

                ship.addPoint(
                        x + 2,
                        y + 34
                );

                g.setColor(color);
                g.fillPolygon(ship);

                g.setColor(Color.WHITE);

                g.fillOval(
                        x + 19,
                        y + 8,
                        12,
                        12
                );
            }
        }
    }

    private static class Bullet {
        double x;
        double y;
        double vx;
        double vy;
        int owner;

        Bullet(
                double x,
                double y,
                double vx,
                double vy,
                int owner
        ) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.owner = owner;
        }

        Rectangle getBounds() {
            return new Rectangle(
                    (int) x,
                    (int) y,
                    5,
                    14
            );
        }
    }

    private static class EnemyBullet {
        double x;
        double y;
        double vx;
        double vy;

        EnemyBullet(
                double x,
                double y,
                double vx,
                double vy
        ) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        Rectangle getBounds() {
            return new Rectangle(
                    (int) x - 4,
                    (int) y - 4,
                    8,
                    8
            );
        }
    }

    private static class Enemy {
        enum Type {
            BASIC,
            FAST,
            SHOOTER
        }

        int x;
        int y;
        int size;

        double speed;
        double drift;

        Type type;

        Enemy(
                int x,
                int y,
                int size,
                double speed,
                Type type
        ) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.type = type;

            drift =
                    Math.random() < 0.5
                            ? -0.35
                            : 0.35;
        }

        void update() {
            y += speed;
            x += drift;

            if (x <= 4 ||
                    x + size >= WIDTH - 4) {
                drift *= -1;
            }
        }

        Rectangle getBounds() {
            return new Rectangle(
                    x,
                    y,
                    size,
                    size
            );
        }
    }

    private class PowerUp {
        double x;
        double y;

        PowerUp(double x, double y) {
            this.x = x;
            this.y = y;
        }

        Rectangle getBounds() {
            return new Rectangle(
                    (int) x - 10,
                    (int) y - 10,
                    20,
                    20
            );
        }

        void apply(Player player) {
            player.rapidFire = true;

            if (player.lives < 5) {
                player.lives++;
            }
        }
    }

    private static class Star {
        int x;
        int y;
        int size;
        int speed;

        Star(
                int x,
                int y,
                int size,
                int speed
        ) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
        }
    }

    private static class Particle {
        double x;
        double y;
        double vx;
        double vy;
        int life;

        Particle(
                double x,
                double y,
                double vx,
                double vy,
                int life
        ) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
        }
    }

    public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        JFrame frame = new JFrame("Star Hopper");
        frame.setName("StarHopper");

        StarHopper game = new StarHopper();

        try {
            ImageIcon icon = new ImageIcon("logo.png");
            frame.setIconImage(icon.getImage());
        } catch (Exception e) {
            System.out.println("Could not load logo.png");
        }

        frame.setContentPane(game);

        // the rest of your existing code continues here...
            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );

            frame.setResizable(false);
            frame.setContentPane(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread(game::stopMusic)
                    );

            game.requestFocusInWindow();
        });
    }
}
