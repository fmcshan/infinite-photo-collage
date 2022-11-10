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
import java.util.concurrent.TimeUnit;

public class ImageZoom {

    public static final int SCALE_DEFAULT = 1; // 
    double EDGE_TOLERANCE = 0.4; // percent of image width/height to ignore around edges when color matching
    int COLOR_TOLERANCE = Integer.MAX_VALUE; // tolerance for matching colors
    int MAX_ZOOM = 250; // max side length 1 image tile
    int INIT_MAX_ZOOM = 10; // max side length of 1 original pixel before image replacement
    private JFrame frmImageZoomIn;
    private JLabel label = null;
    private BufferedImage image = null; // zoom level 0
    private int pixelSize = 1;  // side length of 1 pixel in original image
    private Map<Integer, String> averageColors;
    private ArrayList<String> files = new ArrayList<>();
    private boolean replaced = false;
    private int numZooms = 0;
    private int imageSideLength;
    private double ZOOM_INCR_PERCENT = 0.5;
    private JToggleButton toggleButton;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ImageZoom window = new ImageZoom();
                    window.frmImageZoomIn.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    public ImageZoom() throws Exception {
        averageColors = calculateAverageColors();
        initialize();
    }

    /**
     * Calculate average color of every image in dataset. Map color to name of image file.
     * @return
     * @throws IOException
     */
    public Map<Integer, String> calculateAverageColors() throws IOException {
        Map<Integer, String> map = new HashMap<>();
        File directory = new File("images");
        File[] dirFiles = directory.listFiles();
        if (dirFiles != null) {
            for (File f : dirFiles) {
                BufferedImage curr_img = ImageIO.read(f);
                int imgColor = getAverageColor(curr_img, false).getRGB();
                map.put(imgColor, f.getPath());
            }
        }
        return map;
    }

    private void initialize() throws Exception {
        // create window
        frmImageZoomIn = new JFrame();
        frmImageZoomIn.setTitle("Infinite Photo Collage");
        frmImageZoomIn.setBounds(100, 100, 450, 300);
        frmImageZoomIn.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // add scrolling ability
        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        frmImageZoomIn.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // prompt for user inputs
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter an image file name:");
        String filename = br.readLine();        
        try {
            image = ImageIO.read(new File(filename));
            files.add(filename);
            imageSideLength = image.getWidth();
        } catch (IOException e) {
            System.out.println(e);
        }
        getAverageColor(image, false);

        JPanel panel = new JPanel();
        scrollPane.setViewportView(panel);
        panel.setLayout(new BorderLayout());

        // add info menu
        panel.add(displayInfoMenu(panel));

        initializeToggle(panel);
        panel.add(displayMenuBar(), BorderLayout.NORTH);

        // display image as icon
        Icon imageIcon = new ImageIcon(filename);
        label = new JLabel( imageIcon );
        panel.add(label, BorderLayout.CENTER);

        panel.setFocusable(true);
        panel.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == ' ') {
                    zoom(panel);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        // add zoom ability. can only zoom in 
        panel.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                if (notches > 0) {
                    zoom(panel);
                }
            }
        });

        scrollPane.setViewportView(panel);
    }

    public void zoom(JPanel panel) {
        if (!replaced) {
            // before first collage replacement
            if (pixelSize < INIT_MAX_ZOOM) {
                pixelSize++;
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
                    replacePixelsWithImages(panel);
                    replaced = true;
                    pixelSize = 1;
                    imageSideLength = INIT_MAX_ZOOM;
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
        int lengthInImages = (int) Math.sqrt(files.size());
        imageSideLength = (int)(imageSideLength * (1 + ZOOM_INCR_PERCENT));
        int totalLength = imageSideLength * lengthInImages;

        // create new graphic with new dimensions
        BufferedImage resizedImage = new BufferedImage(totalLength, totalLength, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        
        if (!replaced) {
            graphics2D.drawImage(image, 0, 0, totalLength, totalLength, null);
        } else {
            drawImageByImage(graphics2D);
        }

        graphics2D.dispose();

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
     * Draw an image one pixel area at a time on a given graphic. This does not draw over the original image. Pixel area refers to the space an original pixel should cover.
     * @param g
     */
    public void drawPixelByPixel(Graphics2D g) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) { 
                // get the RGB color from space of original pixel
                int color = image.getRGB(x, y);
                int red = (color & 0x00ff0000) >> 16;
                int green = (color & 0x0000ff00) >> 8;
                int blue = color & 0x000000ff;

                // fill resized pixel with color
                g.setColor(new Color(red, green, blue));
                g.fillRect(x*pixelSize, y*pixelSize, pixelSize, pixelSize);
            }
        }
    }

    /**
     * Draw collage image by image
     */
    public void drawImageByImage(Graphics2D g2d) throws Exception {
        int lengthInImages = (int) Math.sqrt(files.size());
        int index = 0;
        for (int x = 0; x < lengthInImages; x++) {
            for (int y = 0; y < lengthInImages; y++) {
                // fix
                g2d.drawImage(ImageIO.read(new File(files.get(index))), x*imageSideLength, y*imageSideLength, imageSideLength, imageSideLength, null);
                index++;
            }
        }
    }

    public void initialReplacement(JPanel panel) throws Exception {
        numZooms++;
        files = new ArrayList<>();
        int lengthInImages = image.getWidth();
        imageSideLength = (int) Math.ceil((double)imageSideLength / (double)lengthInImages);
        int totalSideLength = lengthInImages * imageSideLength;

        BufferedImage collage = new BufferedImage(totalSideLength, totalSideLength, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = collage.createGraphics();


        for (int x = 0; x < image.getHeight(); x++) {
            for (int y = 0; y < image.getWidth(); y++) {
                int pixelColor = image.getRGB(x, y);
                String bestMatchFilename = "";
                int smallestDiff = COLOR_TOLERANCE;

                // search through map for closest color match
                for (Integer imgColor: averageColors.keySet()) {
                    int diff = calculateColorDifference(new Color(imgColor), new Color(pixelColor));
                    if (diff < smallestDiff) {
                        smallestDiff = diff;
                        bestMatchFilename = averageColors.get(imgColor);
                    }
                }

                try {
                    g2d.drawImage(ImageIO.read(new File(bestMatchFilename)), x * imageSideLength, y * imageSideLength, imageSideLength, imageSideLength, null);
                    files.add(bestMatchFilename);
                } catch (IOException err) {
                    System.out.println(err);
                }

            }
        }
        // System.out.printf("files: %s\n", files.toString());
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
     * Replace all pixel areas visible with images that match the pixel's color. Color is matched up to a certain tolerance value. The displayed collage is a single graphic.
     * @param panel
     * @throws Exception
     */
    public void replacePixelsWithImages(JPanel panel) throws Exception {
        numZooms++;
        BufferedImage collage = null;

        // memory saver: after 1 zoom level, only use "center" images to create the next collage.
        // calculate coordinates of center images. units are image areas not pixels
        int centerFile = files.size() / 2;
        files.subList(centerFile + 9, files.size()).clear();
        files.subList(0, centerFile - 7).clear();

        // draw cropped collage
        int lengthInImages = (int) Math.sqrt(files.size());
        collage = new BufferedImage(lengthInImages * imageSideLength, lengthInImages * imageSideLength, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = collage.createGraphics();
        int var = 0;
        for (int i = 0; i < lengthInImages; i++) {
            for (int j = 0; j < lengthInImages; j++) {
                String file = files.get(var);
                g2d.drawImage(ImageIO.read(new File(file)), i*imageSideLength, j*imageSideLength, imageSideLength, imageSideLength, null);
                var++;
            }
        }
        g2d.dispose();
        image = collage;
        files = new ArrayList<>();

        imageSideLength = 5;

        // replace each pixel area with new image
        for (int x = 0; x < image.getHeight(); x += 5) {
            for (int y = 0; y < image.getWidth(); y += 5) {
                int pixelColor = image.getRGB(x, y);
                BufferedImage img = null;
                String bestMatchFilename = "";
                int smallestDiff = COLOR_TOLERANCE;

                // search through map for closest color match
                for (Integer imgColor: averageColors.keySet()) {
                    int diff = calculateColorDifference(new Color(imgColor), new Color(pixelColor));
                    if (diff < smallestDiff) {
                        smallestDiff = diff;
                        bestMatchFilename = averageColors.get(imgColor);
                    }
                }

                try {
                    img = ImageIO.read(new File(bestMatchFilename));
                    files.add(bestMatchFilename);
                } catch (IOException err) {
                    // System.out.println(err);
                }

//                g2d.drawImage(img, x * imageSideLength, y * imageSideLength, imageSideLength, imageSideLength, null);
            }
        }
        drawImageByImage(g2d);
        // System.out.printf("files: %s\n", files.toString());
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
     * Calculate the average color of a given image. Choose to include or exclude the edge colors with a set threshold.
     * @param img
     * @param includeEdges
     * @return average color
     */
    public Color getAverageColor(BufferedImage img, boolean includeEdges) {
        int red = 0;
        int green = 0;
        int blue = 0;
        int numPixels = 0;
        int startX = 0;
        int startY = 0;
        int endX = img.getWidth();
        int endY = img.getHeight();

        if (!includeEdges) {
            startX = (int)(EDGE_TOLERANCE * img.getWidth());
            startY = (int)(EDGE_TOLERANCE * img.getHeight());
            endX = (int)(img.getWidth() - (EDGE_TOLERANCE * img.getWidth()));
            endY = (int)(img.getHeight() - (EDGE_TOLERANCE * img.getHeight()));
        }

        // from every pixel, get the total R,G,B values
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int color = img.getRGB(x, y);
                red += (color & 0x00ff0000) >> 16;
                green += (color & 0x0000ff00) >> 8;
                blue += color & 0x000000ff;
                numPixels++;
            }
        }

        // resulting average color
        Color averageColor = new Color(red/numPixels, green/numPixels, blue/numPixels);
        return averageColor;
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
//        String pixelSizing = "Pixel side length: " + Integer.toString(pixelSize) + " px\n";
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

}