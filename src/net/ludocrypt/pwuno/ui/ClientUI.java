package net.ludocrypt.pwuno.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import net.ludocrypt.pwuno.packets.ChooseSuitRequest;
import net.ludocrypt.pwuno.packets.DrawCardRequest;
import net.ludocrypt.pwuno.packets.LoginRequest;
import net.ludocrypt.pwuno.packets.LogoutRequest;
import net.ludocrypt.pwuno.packets.PlayCardRequest;
import net.ludocrypt.pwuno.packets.PlayersRequest;
import net.ludocrypt.pwuno.server.Card;
import net.ludocrypt.pwuno.server.Card.CardSuit;
import net.ludocrypt.pwuno.server.PrivatePlayer;
import net.ludocrypt.pwuno.server.PwunoClient;
import net.ludocrypt.pwuno.server.PwunoServer.CommonData;

public class ClientUI extends MouseAdapter {

    public static final Map<String, BufferedImage> SPRITES = new HashMap<>();
    public static final Map<String, Clip> SOUNDS = new HashMap<>();

    JFrame frame;
    BufferedImage canvas;
    JPanel drawPanel;

    String name;
    PwunoClient client;

    CommonData commonData = new CommonData();
    List<PrivatePlayer> connectedPlayers;

    Viewport viewport;

    double lastX, lastY;
    boolean middleClick;

    Vector2f mouse;

    int selectedCard = -1;
    int selectedSuit = -1;

    boolean overDeck = false;

    public ClientUI(String ip, String displayName) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        frame = new JFrame("Pwuno");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        InputStream spriteList = ClassLoader.getSystemResourceAsStream("resources/sprites.lst");
        if (spriteList != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(spriteList));
            String line;
            while ((line = reader.readLine()) != null) {
                String name = line.trim().replace(".png", "");
                InputStream imageStream = ClassLoader.getSystemResourceAsStream("resources/" + line.trim());
                if (imageStream != null) {
                    SPRITES.put(name, ImageIO.read(imageStream));
                }
            }
            reader.close();
        }

        InputStream soundList = ClassLoader.getSystemResourceAsStream("resources/sounds.lst");
        if (soundList != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(soundList));
            String line;
            while ((line = reader.readLine()) != null) {
                String name = line.trim().replace(".wav", "");
                AudioInputStream imageStream = AudioSystem.getAudioInputStream(new BufferedInputStream(ClassLoader.getSystemResourceAsStream("resources/" + line.trim())));
                if (imageStream != null) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(imageStream);
                    SOUNDS.put(name, clip);
                }
            }
            reader.close();
        }

        name = displayName;

        client = new PwunoClient(ip, new Listener() {
            @Override
            public void received(Connection c, Object request) {
                if (request instanceof PlayersRequest playersRequest) {
                    if (!playersRequest.fromClient) {
                        PlayersRequest.request(client.getClient());
                    } else {
                        connectedPlayers = playersRequest.response;
                        commonData = playersRequest.commonData;
                    }
                }
                if (request instanceof LogoutRequest) {
                    System.out.println("You have been kicked for joining mid-game");
                    System.exit(0);
                }
                if (request instanceof PlayCardRequest) {
                    playSound("play-" + String.format("%02d", new Random().nextInt(7) + 1));
                }
            }

            @Override
            public void disconnected(Connection c) {
                System.out.println("You have been kicked for joining mid-game");
                System.exit(0);
            }
        });

        LoginRequest.send(client.getClient(), name);

        drawPanel = new JPanel() {
            private static final long serialVersionUID = -145518941919876189L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                draw();

                if (viewport != null) {
                    viewport.drawTransformedImage(g2d, canvas);
                }

            }
        };

        viewport = new Viewport(drawPanel);

        drawPanel.addMouseListener(this);
        drawPanel.addMouseMotionListener(this);
        drawPanel.addMouseWheelListener(this);

        frame.add(drawPanel);

        int newWidth = 640 + frame.getInsets().left + frame.getInsets().right;
        int newHeight = 480 + frame.getInsets().top + frame.getInsets().bottom;
        frame.setSize(newWidth, newHeight);

        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        canvas = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> drawPanel.repaint(), 0, 200, TimeUnit.MILLISECONDS);
    }

    public void draw() {
        Graphics2D g = canvas.createGraphics();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g.setColor(new Color(175, 119, 121));
        g.fillRect(0, 0, 640, 480);

        g.setColor(Color.BLACK);

        g.setStroke(new BasicStroke(3));

        g.drawImage(SPRITES.get("Back"), 30, 195, null);

        boolean lastOverDeck = overDeck;
        overDeck = false;

        if (mouse != null) {
            if (mouse.x > 30 && mouse.x < 30 + 82 && mouse.y > 195 && mouse.y < 195 + 128) {
                g.drawImage(SPRITES.get("Back"), 30, 185, null);
                overDeck = true;
            }
        }

        if (lastOverDeck != overDeck) {
            playSound("flicker-" + String.format("%02d", new Random().nextInt(11) + 1));
        }

        if (commonData.drawLocked) {
            g.setColor(Color.BLACK);
            drawText(g, "+" + commonData.drawStack, 500, 200, 100, 100);
        }

        Card previousCard = Card.draw(new Random(name.hashCode()));
        for (Card card : commonData.stack) {
            Random random = new Random(card.hashCode() + previousCard.hashCode());
            previousCard = card;

            AffineTransform transform = new AffineTransform();
            transform.translate(random.nextInt(-10, 10) + 290, random.nextInt(-10, 10) + 210);
            transform.rotate(Math.toRadians(random.nextInt(-30, 30)), 41, 64);
            transform.scale(0.8, 0.8);

            g.drawImage(SPRITES.get(card.getSpriteName(false)), transform, null);
        }

        if (connectedPlayers != null) {
            int players = connectedPlayers.size() - 1;
            double playerCardWidth = 640.0 / (double) players;

            int i = -1;
            for (PrivatePlayer player : connectedPlayers) {
                if (player.playerName != null) {
                    if (player.id != client.getClient().getID()) {
                        i++;
                        drawUserInfo(g, player, playerCardWidth, i * playerCardWidth, 0);
                    } else {
                        drawUserInfo(g, player, 213, 0, 350);
                    }
                }
            }
        }

        if (commonData.playerTurnId == client.getClient().getID()) {
            if (commonData.stack.peek().isSuit(CardSuit.WILD)) {

                int x = 250;
                int y = 180;
                int wx = 150;
                int wy = 150;

                int separation = 10;

                g.setStroke(new BasicStroke(20));

                g.setColor(Color.BLACK);
                g.drawArc(x, y, wx, wy, separation, 90 - separation - separation);
                g.drawArc(x, y, wx, wy, 90 + separation, 90 - separation - separation);
                g.drawArc(x, y, wx, wy, 180 + separation, 90 - separation - separation);
                g.drawArc(x, y, wx, wy, 270 + separation, 90 - separation - separation);

                g.setStroke(new BasicStroke(10));
                g.setColor(Color.RED);
                g.drawArc(x, y, wx, wy, separation, 90 - separation - separation);

                g.setColor(Color.GREEN);
                g.drawArc(x, y, wx, wy, 90 + separation, 90 - separation - separation);

                g.setColor(Color.YELLOW);
                g.drawArc(x, y, wx, wy, 180 + separation, 90 - separation - separation);

                g.setColor(Color.BLUE);
                g.drawArc(x, y, wx, wy, 270 + separation, 90 - separation - separation);

                if (mouse != null && mouse.x > x && mouse.x < x + wx && mouse.y > y && mouse.y < y + wy) {
                    Vector2f newMouse = new Vector2f(mouse.x - x - wx / 2.0f, mouse.y - y - wy / 2.0f);

                    boolean xPos = newMouse.x > 0;
                    boolean yPos = newMouse.y > 0;

                    if (xPos && yPos) {

                        g.setStroke(new BasicStroke(15));
                        g.setColor(Color.BLUE);
                        g.drawArc(x, y, wx + 10, wy + 10, 270 + separation, 90 - separation - separation);

                        selectedSuit = 0;
                    } else if (xPos && !yPos) {

                        g.setStroke(new BasicStroke(15));
                        g.setColor(Color.RED);
                        g.drawArc(x, y - 10, wx + 10, wy + 10, separation, 90 - separation - separation);

                        selectedSuit = 2;
                    } else if (!xPos && yPos) {

                        g.setStroke(new BasicStroke(15));
                        g.setColor(Color.YELLOW);
                        g.drawArc(x - 10, y, wx + 10, wy + 10, 180 + separation, 90 - separation - separation);

                        selectedSuit = 1;
                    } else if (!xPos && !yPos) {

                        g.setStroke(new BasicStroke(15));
                        g.setColor(Color.GREEN);
                        g.drawArc(x - 10, y - 10, wx + 10, wy + 10, 90 + separation, 90 - separation - separation);

                        selectedSuit = 3;
                    }

                } else {
                    selectedSuit = -1;
                }

            } else {
                selectedSuit = -1;
            }
        } else {
            selectedSuit = -1;
        }

        g.dispose();
    }

    private final Map<String, Integer> fontSizeCache = new ConcurrentHashMap<>();

    private void drawUserInfo(Graphics2D g, PrivatePlayer player, double cardWidth, double xOff, double yOff) {
        int padding = (int) (cardWidth / 7);
        int boxHeight = 40;
        int innerWidth = (int) (cardWidth - 2 * padding);

        int x = padding + (int) xOff;
        int y = 10 + (int) yOff;

        if (player.id == commonData.playerTurnId) {
            g.setColor(Color.YELLOW);
        }

        g.drawRoundRect(x, y, innerWidth, boxHeight, 20, 20);

        g.setColor(Color.BLACK);

        drawText(g, player.playerName, x, y - 3, innerWidth, boxHeight);

        y += boxHeight + 10;

        g.drawRoundRect(x, y, boxHeight, boxHeight, 20, 20);
        drawText(g, String.valueOf(player.hand), x, y - 3, boxHeight, boxHeight);

        if (player.hand > 0) {
            List<Int2> deckPositions = getDeckPositions(player, cardWidth, xOff, y, padding, boxHeight, innerWidth);

            for (int i = 0; i < player.hand; i++) {
                g.drawImage(SPRITES.get(player.cards.isEmpty() ? "Back" : player.cards.get(i).getSpriteName(commonData.drawLocked)), deckPositions.get(i).x(), deckPositions.get(i).y(), null);
            }
        }
    }

    public List<Int2> getDeckPositions(PrivatePlayer player, double cardWidth, double xOff, double yOff, int padding, int boxHeight, int innerWidth) {
        if (player.hand > 0) {
            List<Int2> positions = new ArrayList<Int2>();

            double scrunch = 30;

            double startX = xOff + padding + 40 + 10;
            double startY = yOff;

            if (!player.cards.isEmpty()) {
                startX = xOff + innerWidth + 40 + 10;
                startY = 332;
            } else {
                scrunch = (innerWidth - boxHeight - 82 - padding) / (double) player.hand;
            }

            double compound = 0;

            CardSuit lastSuit = CardSuit.WILD;

            boolean foundCard = false;
            Card lastCard = null;

            int lastSelectedCard = selectedCard;

            for (int i = 0; i < player.hand; i++) {
                int px = (int) (compound + startX);
                int py = (int) startY;

                if (!player.cards.isEmpty()) {
                    Card card = player.cards.get(i);
                    if (mouse != null && mouse.x > px && mouse.x < px + 82 + (i + 1 >= player.hand ? 10 : 0) && mouse.y > py && mouse.y < py + 128) {
                        py += ((Math.abs((mouse.x - px - 41.0)) - 82.0) / 10.0);
                        compound += 20;
                        lastSuit = card.getSuit();
                        lastCard = card;

                        if (i + 1 >= player.hand) {
                            py -= 30;
                            selectedCard = i;
                            foundCard = true;
                        }

                    } else {
                        if (lastCard != null) {
                            if (i > 0) {
                                Int2 lastPos = positions.get(i - 1);
                                positions.remove(i - 1);
                                positions.add(new Int2(lastPos.x, lastPos.y - 30));
                                selectedCard = i - 1;
                                foundCard = true;
                            }
                            lastCard = null;
                        }
                    }
                }

                positions.add(new Int2(px, py));

                if (!player.cards.isEmpty()) {
                    if (player.hand > 10) {
                        Card card = player.cards.get(i);
                        if (lastSuit.equals(card.getSuit())) {
                            compound += 180.0 / (double) (player.hand);
                            continue;
                        }
                        lastSuit = card.getSuit();
                    }
                }

                compound += ((40.0 / (player.hand + 2.0)) + scrunch);
            }

            if (lastSelectedCard != selectedCard) {
                playSound("flicker-" + String.format("%02d", new Random().nextInt(11) + 1));
            }

            if (!player.cards.isEmpty()) {
                if (!foundCard) {
                    selectedCard = -1;
                }
            }

            return positions;
        }

        return List.of();
    }

    private void drawText(Graphics2D g, String text, int x, int y, int width, int height) {
        int fontSize = calculateMaxFontSize(g, text, width, height);

        Font font = new Font("Arial", Font.PLAIN, fontSize);
        g.setFont(font);

        TextLayout tl = new TextLayout(text, font, g.getFontRenderContext());
        int textHeight = (int) (tl.getAscent() / 2.0 + (double) height / 2.0);

        g.drawString(text, x + width / 2 - g.getFontMetrics().stringWidth(text) / 2, y + textHeight);
    }

    private int calculateMaxFontSize(Graphics2D g, String text, int maxWidth, int maxHeight) {
        if (text.isEmpty()) {
            return 1;
        }

        String key = text + "_" + maxWidth;
        if (fontSizeCache.containsKey(key)) {
            return fontSizeCache.get(key);
        }

        int minBound = Math.min(maxWidth, maxHeight);

        int estimatedSize = Math.max(1, minBound / text.length());
        int increment = Math.max(1, (minBound - g.getFontMetrics(new Font(g.getFont().getName(), Font.PLAIN, estimatedSize)).stringWidth(text)) / text.length());

        int fontSize = estimatedSize;
        while (increment > 0) {
            Font font = new Font(g.getFont().getName(), Font.PLAIN, fontSize + increment);
            int textWidth = g.getFontMetrics(font).stringWidth(text);
            int textHeight = g.getFontMetrics(font).getHeight();

            if (textWidth >= maxWidth || textHeight >= maxHeight) {
                increment /= 2;
            } else {
                fontSize += increment;
            }
        }

        fontSizeCache.put(key, fontSize);
        return fontSize;
    }

    @Override
    public void mousePressed(MouseEvent e) {

        if (SwingUtilities.isMiddleMouseButton(e)) {
            middleClick = true;
            lastX = ((double) e.getX() / (double) getScalingRatio().x());
            lastY = ((double) e.getY() / (double) getScalingRatio().y());
            drawPanel.repaint();
        }

        if (SwingUtilities.isLeftMouseButton(e)) {
            if (selectedCard != -1) {
                if (commonData.playerTurnId == client.getClient().getID()) {
                    for (PrivatePlayer player : connectedPlayers) {
                        if (player.id == client.getClient().getID() && player.playerName != null) {
                            Card card = player.cards.get(selectedCard);
                            if (card.canPlayOn(commonData.stack.peek(), commonData.drawLocked)) {
                                PlayCardRequest.request(client.getClient(), selectedCard);

                                playSound("play-" + String.format("%02d", new Random().nextInt(7) + 1));
                            }
                            continue;
                        }
                    }
                }
            }

            if (selectedSuit != -1) {
                if (commonData.playerTurnId == client.getClient().getID()) {
                    if (commonData.stack.peek().isSuit(CardSuit.WILD)) {
                        ChooseSuitRequest.request(client.getClient(), selectedSuit);
                    }
                }
            }

            if (overDeck) {
                if (commonData.playerTurnId == client.getClient().getID()) {
                    DrawCardRequest.request(client.getClient());

                    playSound("draw-" + String.format("%02d", new Random().nextInt(9) + 1));
                }
            }
        }
    }

    private void playSound(String name) {
        Clip clip = SOUNDS.get(name);
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (middleClick) {
            double dx = ((double) e.getX() / (double) getScalingRatio().x()) - lastX;
            double dy = ((double) e.getY() / (double) getScalingRatio().y()) - lastY;
            viewport.setMouse(dx, dy);
            drawPanel.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e)) {
            middleClick = false;
            viewport.pushMouse();
            drawPanel.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!middleClick) {
            mouse = getWorldSpace(new Int2(e.getX(), e.getY()));
            drawPanel.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (viewport != null) {
            viewport.pushMouse();

            lastX = ((double) e.getX() / (double) getScalingRatio().x());
            lastY = ((double) e.getY() / (double) getScalingRatio().y());

            viewport.zoom(lastX, lastY, 1.1, (int) (e.getUnitsToScroll() / 2.0));
        }

        drawPanel.repaint();
    }

    public Vector2f getWorldSpace(Int2 p) {
        return getWorldSpace(p, new Int2(0, 0));
    }

    public Vector2f getWorldSpace(Int2 p, Int2 o) {
        Matrix4f mat = viewport.composeMat();

        Vector3f scale = new Vector3f();
        scale = mat.getScale(scale);

        Vector3f translation = new Vector3f();
        translation = mat.getTranslation(translation);

        return new Vector2f((((p.x() - translation.x * getScalingRatio().x()) / scale.x) - o.x()), (((p.y() - translation.y * getScalingRatio().y()) / scale.y) - o.y()));
    }

    public Int2 getScalingRatio() {
        int panelWidth = drawPanel.getWidth();
        int panelHeight = drawPanel.getHeight();

        return new Int2(panelWidth, panelHeight);
    }

    public static record Int2(int x, int y) {

        public static double distanceSquared(Int2 a, Int2 b) {
            int dx = a.x() - b.x();
            int dy = a.y() - b.y();
            return dx * dx + dy * dy;
        }

        public static double distance(Int2 a, Int2 b) {
            return Math.sqrt(distanceSquared(a, b));
        }

    }

}
