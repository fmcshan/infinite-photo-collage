import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ImageZoom {

    public static final int SCALE_DEFAULT = 1; //
    int COLOR_TOLERANCE = Integer.MAX_VALUE; // tolerance for matching colors
    int MAX_ZOOM = 250; // max side length 1 image tile
    int INIT_MAX_ZOOM = 10; // max side length of 1 original pixel before image replacement
    private JFrame frmImageZoomIn;
    private JLabel label = null;
    private BufferedImage image = null; // zoom level 0
    private Map<Integer, String> averageColors;
    private int pixelSize = 1;  // side length of 1 pixel in original image
    private ArrayList<String> files = new ArrayList<>();
    private boolean replaced = false;
    private int numZooms = 0;
    private int imageSideLength;
    private double ZOOM_INCR_PERCENT = 0.2;
    private JToggleButton toggleButton;
    private Map<String, ArrayList<int[]>> imageCache;
    private int totalSideLength;
    private int sideLengthInImages;
//    double COLLAGE_CROPPED_PERCENT = 0.45;
    private int tempTotalSideLength;
    private FileUpload fileUpload;
    
    public ImageZoom(BufferedImage image, Map<Integer, String> averageColors) throws Exception {
        this.image = image;
        this.averageColors = averageColors;
        initialize();
    }

    private void initialize() {
        frmImageZoomIn.setVisible(true);

        // create window
        frmImageZoomIn = new JFrame();
        frmImageZoomIn.setTitle("Infinite Photo Collage");
        frmImageZoomIn.setBounds(100, 100, 450, 300);
        frmImageZoomIn.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // add scrolling ability
        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        frmImageZoomIn.getContentPane().add(scrollPane, BorderLayout.CENTER);

        imageSideLength = image.getWidth();
        totalSideLength = imageSideLength;
        // cacheImagesForCollage(image);
        sideLengthInImages = 1;

        JPanel panel = new JPanel();
        scrollPane.setViewportView(panel);
        panel.setLayout(new BorderLayout());

        // add info menu
        panel.add(displayInfoMenu(panel));

        initializeToggle(panel);
        panel.add(displayMenuBar(), BorderLayout.NORTH);

        // display image as icon
        Icon imageIcon = new ImageIcon(image);
        label = new JLabel( imageIcon );
        panel.add(label, BorderLayout.CENTER);
        panel.setFocusable(true);

        // zoom using spacebar
        panel.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == ' ') {
                    zoom(panel);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });

        // zoom using mouse wheel
        panel.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                if (notches > 0) {
                    zoom(panel);
                }
            }
        });

        scrollPane.setViewportView(panel);
//        scrollPane.getViewport().setViewPosition(new Point(frmImageZoomIn.getWidth() / 2, frmImageZoomIn.getHeight() / 2));
    }

    public void zoom(JPanel panel) {
        if (!replaced) {
            // before first collage replacement
            if (pixelSize < INIT_MAX_ZOOM) {
                pixelSize = (int)((double) totalSideLength / (double) image.getWidth());
                try {
                    resize(panel);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else {
                try {
                    initialReplacement(panel);
                    replaced = true;
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

        } else {
            // all other replacement cases
            if (imageSideLength < MAX_ZOOM) {
                try {
                    resize(panel);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else {
                try {
                    imageSideLength = INIT_MAX_ZOOM;
                    collageReplacement(panel);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            }
        }
    }


    /**
     * Rescale and display image, which replaces current image
     * @param panel
     */
    public void resize (JPanel panel) throws Exception {
        // calculate new total side length = width = height
        imageSideLength = (int)(imageSideLength * (1 + ZOOM_INCR_PERCENT));
        totalSideLength = (int)(imageSideLength * sideLengthInImages);

        // create new graphic with new dimensions
        BufferedImage resizedImage = new BufferedImage(totalSideLength, totalSideLength, BufferedImage.TYPE_INT_RGB);
        if (numZooms >= 2) {
            int x = (image.getWidth() / 2) - ((frmImageZoomIn.getWidth() + 1000) / 2);
            int y = (image.getHeight() / 2) - ((frmImageZoomIn.getHeight() + 1000) / 2);
            resizedImage.getSubimage(x, y, frmImageZoomIn.getWidth() + 1000, frmImageZoomIn.getHeight() + 1000);
        }

        Graphics2D graphics2D = resizedImage.createGraphics();
        
        if (!replaced) {
            graphics2D.drawImage(image, 0, 0, totalSideLength, totalSideLength, null);
        } else {
            drawImageByImage(graphics2D);
        }
        graphics2D.dispose();

//        JScrollPane scrollPane = (JScrollPane) frmImageZoomIn.getContentPane().getComponent(0);
//        scrollPane.getViewport().setViewPosition(new Point(frmImageZoomIn.getWidth() / 2, frmImageZoomIn.getHeight() / 2));

        // place resized image in new jlabel and replace old
        label = new JLabel( new ImageIcon(resizedImage) );
        panel.removeAll();
        panel.add(displayInfoMenu(panel));
        panel.add(displayMenuBar(), BorderLayout.NORTH);
        panel.add(label, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
    }

    /**
     * Draw collage image by image
     */
    public void drawImageByImage(Graphics2D g2d) throws Exception {
        for (String filename : imageCache.keySet()) {
            BufferedImage img = ImageIO.read(new File(filename));
            for (int[] coord : imageCache.get(filename)) {
                g2d.drawImage(img, coord[0]*imageSideLength, coord[1]*imageSideLength, imageSideLength, imageSideLength, null);
            }
        }
    }

    public void initialReplacement(JPanel panel) throws Exception {
        numZooms++;
        cacheImagesForCollage(image);
        imageSideLength = (int) (Math.ceil((double)totalSideLength / (double)sideLengthInImages) * (1 + ZOOM_INCR_PERCENT));
        totalSideLength = sideLengthInImages * imageSideLength;
        
        BufferedImage collage = new BufferedImage(totalSideLength, totalSideLength, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = collage.createGraphics();
        
        drawImageByImage(g2d);
        g2d.dispose();

//        JScrollPane scrollPane = (JScrollPane) frmImageZoomIn.getContentPane().getComponent(0);
//        scrollPane.getViewport().setViewPosition(new Point(frmImageZoomIn.getWidth() / 2, frmImageZoomIn.getHeight() / 2));

        // place collage in new jlabel and replace old
        label = new JLabel( new ImageIcon(collage) );
        panel.removeAll();
        panel.add(displayInfoMenu(panel));
        panel.add(displayMenuBar(), BorderLayout.NORTH);
        panel.add(label, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
        image = collage;
    }

    /**
     * Replace all pixel areas visible with images that match the pixel's color. Color is matched up to a certain tolerance value. The displayed collage is a single graphic.
     * @param panel
     * @throws Exception
     */
    public void collageReplacement(JPanel panel) throws Exception {
        numZooms++;
        cacheImagesForCollage(image);

        // draw cropped collage
        totalSideLength = (int) Math.ceil((double) (tempTotalSideLength * imageSideLength * 1.5));
        imageSideLength = (int) (Math.ceil((double) totalSideLength / (double) sideLengthInImages) * (1 + ZOOM_INCR_PERCENT));
        totalSideLength = imageSideLength * sideLengthInImages;
        BufferedImage collage = new BufferedImage(totalSideLength, totalSideLength, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = collage.createGraphics();

        drawImageByImage(g2d);
        g2d.dispose();

        // place collage in new jlabel and replace old
        label = new JLabel( new ImageIcon(collage) );
        panel.removeAll();
        panel.add(displayInfoMenu(panel));
        panel.add(displayMenuBar(), BorderLayout.NORTH);
        panel.add(label, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
        image = collage;
    }

    /**
     * Calculate the total difference for each color chanel (R, G, B) between two Colors
     * @param pixelColor
     * @param colorToCompare
     * @return color difference
     */
    public int calculateColorDifference(Color pixelColor, Color colorToCompare) {
        int deltaRed = Math.abs(pixelColor.getRed() - colorToCompare.getRed());
        int deltaGreen = Math.abs(pixelColor.getGreen() - colorToCompare.getGreen());
        int deltaBlue = Math.abs(pixelColor.getBlue() - colorToCompare.getBlue());
        return (deltaRed + deltaGreen + deltaBlue);
    }

    /**
     * Create display of current info about collage
     * @return
     */
    public JTextArea displayInfoMenu(JPanel panel) {
        JTextArea info = new JTextArea();
        int lengthInImages = (int) Math.sqrt(files.size());

        // text being displayed
//        String maxLevel = "Pixel length max: " + Integer.toString(MAX_ZOOM) + " px\n";
        // String pixelSizing = "Pixel side length: " + Integer.toString(pixelSize) + " px\n";
        String overallSize = "Total side length: " + Integer.toString(imageSideLength * lengthInImages) + " px\n";
        String imageLength = "Image side length: " + Integer.toString(imageSideLength) + " px\n";
        String zooms = "Collage replacements: " + Integer.toString(numZooms) + "\n";
        String stats = overallSize + imageLength + zooms;

        // set styling
        Font font = new FontUIResource(Font.MONOSPACED, Font.BOLD, 16);
        info.setFont(font);
        info.setText(stats);
        info.setEditable(false);
        info.setBackground(new Color(255,255,255,200));
        info.setBounds(5, 50, 280, 120);
    
        return info;
    }

    public void initializeToggle(JPanel panel) {
        toggleButton = new JToggleButton("Play");

        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AbstractButton abstractButton = (AbstractButton) e.getSource();
                boolean selected = abstractButton.getModel().isSelected();

                if (selected) {
                    long start = System.currentTimeMillis();
                    toggleButton.setText("Pause");

                    while (true) {
                        if (System.currentTimeMillis() - start > 1000) {
                            zoom(panel);
                        } else {
                            start = System.currentTimeMillis();
                        }
                    }

                } else {
                    toggleButton.setText("Play");
                }
            }
        });
    }

    public JMenuBar displayMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(toggleButton);

        return menuBar;
    }

    public void cacheImagesForCollage(BufferedImage parentImage) {
        imageCache = new HashMap<>();
        int numPixels = 0;

        int startX = 0;
        int startY = 0;
        int endX = parentImage.getWidth();
        int endY = parentImage.getHeight();

        if (replaced) {
            // cropped collage will be 80x80 image tiles large
            startX = (parentImage.getWidth() / 2) - 40;
            startY = (parentImage.getHeight() / 2) - 40;
            endX = (parentImage.getWidth() / 2) + 40;
            endY = (parentImage.getHeight() / 2) + 40;
//            startX = (int)(COLLAGE_CROPPED_PERCENT * parentImage.getWidth());
//            startY = (int)(COLLAGE_CROPPED_PERCENT * parentImage.getHeight());
//            endX = (int)(parentImage.getWidth() - (COLLAGE_CROPPED_PERCENT * parentImage.getWidth()));
//            endY = (int)(parentImage.getHeight() - (COLLAGE_CROPPED_PERCENT * parentImage.getHeight()));
        }

        tempTotalSideLength = endX - startX;
        
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                numPixels++;

                ArrayList<int[]> coordinates = new ArrayList<>();
                int pixelColor = parentImage.getRGB(x, y);
                String bestMatchFilename = "";
                int smallestDiff = COLOR_TOLERANCE;

                // search through color map for closest match
                for (Integer imgColor: averageColors.keySet()) {
                    int diff = calculateColorDifference(new Color(imgColor), new Color(pixelColor));
                    if (diff < smallestDiff) {
                        smallestDiff = diff;
                        bestMatchFilename = averageColors.get(imgColor);
                    }
                }

                // cache image and location(s)
                if (imageCache.containsKey(bestMatchFilename)) {
                    coordinates = imageCache.get(bestMatchFilename);
                }
                coordinates.add(new int[]{x - startX, y - startY});
                imageCache.put(bestMatchFilename, coordinates);
            }
        }

        sideLengthInImages = (int)Math.sqrt(numPixels);
    }

}