import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ImageZoom {

    int MAX_ZOOM = 250; // max side length 1 image tile before collage replacement
    int INIT_MAX_ZOOM = 10; // max side length of 1 original pixel before image replacement
    private double ZOOM_INCR_PERCENT = 0.2; // percent increase of rendered image side length when zooming

    private JFrame frmImageZoomIn;
    private JLabel label = null;
    private BufferedImage image = null; 
    private JToggleButton toggleButton;
    private JButton infoButton;

    private Map<Integer, String> averageColors; // map of average color to image filename
    private Map<String, ArrayList<int[]>> imageCache = new HashMap<>(); // map of image filename to coordinate location(s) in collage
    private boolean replaced = false; // true after first replacement occurs
    private int numZooms = 0; // number of collage replacements
    private int imageSideLength; // rendered side length in pixels of one image tile
    private int totalSideLength; // side length in pixels of rendered canvas
    private int sideLengthInImages; // side length in images of collage
    private int tempTotalSideLength;
    private int INIT_SIDE_LENGTH = 40;
    
    public ImageZoom(File file, Map<Integer, String> averageColors) throws Exception {
        ArrayList<int[]> list = new ArrayList<>();
        list.add(new int[]{0, 0});
        imageCache.put(file.getPath(), list);
        this.image = ImageIO.read(file);
        this.averageColors = averageColors;
        initialize();
    }

    private void initialize() {
        // create window
        frmImageZoomIn = new JFrame();
        frmImageZoomIn.setVisible(true);
        frmImageZoomIn.setTitle("Infinite Photo Collage");
        frmImageZoomIn.setBounds(100, 100, 450, 300);
        frmImageZoomIn.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        imageSideLength = INIT_SIDE_LENGTH;
        totalSideLength = imageSideLength;
        sideLengthInImages = 1;

        int min = (image.getWidth() > image.getHeight()) ? image.getHeight() : image.getWidth();
        image = image.getSubimage(0, 0, min, min);

        // scale image to some initial size
        BufferedImage scaledImage = new BufferedImage(INIT_SIDE_LENGTH, INIT_SIDE_LENGTH, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = scaledImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, INIT_SIDE_LENGTH, INIT_SIDE_LENGTH, null);
        graphics2D.dispose();

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(24, 24));
        panel.setFocusable(true);
        frmImageZoomIn.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout());

        initializeToggle(panel);
        initializeInfoButton(panel);

        image = scaledImage;
        label = new JLabel( new ImageIcon(scaledImage) );
        panel.removeAll();
        panel.add(displayInfoMenu(panel));
        panel.add(displayMenuBar(), BorderLayout.NORTH);
        panel.add(label, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();

        // zoom using spacebar
        panel.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == ' ') {
                    zoom(panel);
                }
            }
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
    }

    public void zoom(JPanel panel) {
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
                replaced = true;
            } catch (Exception e1) {
                e1.printStackTrace();
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
        BufferedImage resizedImage;
        
        // crop to avoid heap space 
        int width = Math.max(frmImageZoomIn.getWidth(), frmImageZoomIn.getHeight());
        if (totalSideLength > width) {
            int widthInImages = (int) Math.ceil((double) width / (double) imageSideLength);
            if (widthInImages % 2 != 0) {
                widthInImages += 1;
            }
            width = widthInImages * imageSideLength;
            resizedImage = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
        } else {
            resizedImage = new BufferedImage(totalSideLength, totalSideLength, BufferedImage.TYPE_INT_RGB);
        }

        Graphics2D graphics2D = resizedImage.createGraphics();

        drawImageByImage(graphics2D, width);

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
     * Draw collage image by image
     * @param g2d
     */
    public void drawImageByImage(Graphics2D g2d, double width) throws Exception {
        for (String filename : imageCache.keySet()) {
            for (int[] coord : imageCache.get(filename)) {
                
                double frameWidthInImages = width / imageSideLength;
                if (Math.ceil(frameWidthInImages) % 2 != 0) {
                    frameWidthInImages += 1;
                }
                frameWidthInImages = (int) frameWidthInImages;
                int croppedBound = 0;
                if (totalSideLength > width) {
                    croppedBound = (int) Math.ceil((sideLengthInImages - frameWidthInImages) / 2);
                }
                
                if ((coord[0] >= croppedBound && coord[0] <= sideLengthInImages - croppedBound)
                && (coord[1] >= croppedBound && coord[1] <= sideLengthInImages - croppedBound)
                ){
                    // crop image to be square without distortion
                    BufferedImage img = ImageIO.read(new File(filename));
                    int min = img.getWidth();
                    if (img.getWidth() > img.getHeight()) {
                    min = img.getHeight();
                    }
                    img = img.getSubimage(0, 0, min, min);

                    // draw image onto canvas
                    g2d.drawImage(img, 
                    (int)Math.ceil((double)(coord[0] - croppedBound) * imageSideLength),
                    (int)Math.ceil((double)(coord[1] - croppedBound) * imageSideLength),
                    imageSideLength, imageSideLength, null);

                } 
                
            }
        }
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
        double width = Math.max(frmImageZoomIn.getWidth(), frmImageZoomIn.getHeight());
        if (totalSideLength > width) {
            totalSideLength = (int) Math.ceil((double) (tempTotalSideLength * imageSideLength * (1 + ZOOM_INCR_PERCENT)));
        }
        imageSideLength = (int) (Math.ceil((double) totalSideLength / (double) sideLengthInImages) * (1 + ZOOM_INCR_PERCENT));
        totalSideLength = imageSideLength * sideLengthInImages;
        BufferedImage collage = new BufferedImage(totalSideLength, totalSideLength, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = collage.createGraphics();

        drawImageByImage(g2d, width);
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
        
        String imageLength = "Image side length: " + imageSideLength + " px\n";
        String zooms = "Collage replacements: " + numZooms + "\n";
        String stats = imageLength + zooms; 

        // set styling
        Font font = new FontUIResource(Font.MONOSPACED, Font.BOLD, 16);
        info.setFont(font);
        info.setText(stats);
        info.setEditable(false);
        info.setBackground(new Color(255,255,255,200));
        info.setBounds(5, 50, 400, 120);
    
        return info;
    }

    /**
     * Initialize autoplay button functionality
     * @param panel
     */
    public void initializeToggle(JPanel panel) {
        toggleButton = new JToggleButton("Play");
        toggleButton.setFocusable(false);

        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AbstractButton abstractButton = (AbstractButton) e.getSource();
                boolean selected = abstractButton.getModel().isSelected();

                Timer timer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (toggleButton.getText().equals("Play")) {
                            ((Timer) e.getSource()).stop();
                        } else {
                            zoom(panel);
                        }
                    }
                });

                if (selected) {
                    toggleButton.setText("Pause");
                    timer.start();
                } else {
                    toggleButton.setText("Play");
                }
            }
        });
    }

    public void initializeInfoButton(JPanel panel) {
        infoButton = new JButton("Details");
        infoButton.setFocusable(false);

        infoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (e.getSource() == infoButton) {
                    JFrame popup = new JFrame();
                    JPanel panel = new JPanel();
                    popup.setTitle("Infinite Photo Collage");
                    popup.setBounds(300, 300, 400, 600);
                    popup.setVisible(true);
                    popup.add(panel);

                    JTextArea info = new JTextArea("Welcome to Infinite Photo Collage. \n" +
                            "");
                    info.setEditable(false);
                    panel.add(info, BorderLayout.CENTER);
                }
            }
        });
    }

    public JMenuBar displayMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(toggleButton);
        menuBar.add(infoButton);
        return menuBar;
    }

    /**
     * Match images to replace parentImage using a map of average colors
     * @param parentImage
     */
    public void cacheImagesForCollage(BufferedImage parentImage) {
        imageCache = new HashMap<>();
        int numPixels = 0;

        int startX = 0;
        int startY = 0;
        int endX = parentImage.getWidth();
        int endY = parentImage.getHeight();
        sideLengthInImages = endX - startX;

        double width = Math.max(frmImageZoomIn.getWidth(), frmImageZoomIn.getHeight());
        if (totalSideLength > width) {
            int frameWidthInImages = (int) Math.ceil(width / imageSideLength);
            if (frameWidthInImages % 2 != 0) {
                frameWidthInImages += 1;
            }
            // current issue is here
            imageSideLength = (int) (Math.ceil((double) totalSideLength / (double) sideLengthInImages));
            double croppedBound = Math.abs((sideLengthInImages - frameWidthInImages) / 2);
            startX = (int) Math.ceil(croppedBound);
            startY = (int) Math.ceil(croppedBound);
            endX = parentImage.getWidth() - (int) Math.ceil(croppedBound);
            endY = parentImage.getHeight() - (int) Math.ceil(croppedBound);
        }

        tempTotalSideLength = endX - startX;
        sideLengthInImages = tempTotalSideLength;
        
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                numPixels++;

                ArrayList<int[]> coordinates = new ArrayList<>();
                int pixelColor = parentImage.getRGB(x, y);
                String bestMatchFilename = "";
                int smallestDiff = Integer.MAX_VALUE;

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

    }

}