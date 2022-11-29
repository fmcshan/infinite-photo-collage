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

    int MAX_ZOOM = 250; // max side length 1 image tile
    int INIT_MAX_ZOOM = 10; // max side length of 1 original pixel before image replacement
    private double ZOOM_INCR_PERCENT = 0.2;

    private JFrame frmImageZoomIn;
    private JLabel label = null;
    private BufferedImage image = null;
    private JToggleButton toggleButton;
    private JButton infoButton;

    private Map<Integer, String> averageColors; // map of average color to corresponding image filename
    private Map<String, ArrayList<int[]>> imageCache; // map of image filename to location coordinates relative to collage being rendered
    private boolean replaced = false; // if first collage replacement has occured
    private int numZooms = 0; // number of collage replacements that have occured
    private int imageSideLength; // side length in rendered pixels of one image
    private int totalSideLength; // side length in rendered pixels of entire canvas
    private int sideLengthInImages; // side length in images of collage being rendered
    private int tempTotalSideLength;
    
    public ImageZoom(BufferedImage image, Map<Integer, String> averageColors) throws Exception {
        this.image = image;
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

        imageSideLength = image.getWidth();
        totalSideLength = imageSideLength;
        sideLengthInImages = 1;

        JPanel panel = new JPanel();
        panel.setFocusable(true);
        // scrollPane.setViewportView(panel);
        frmImageZoomIn.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout());

        initializeToggle(panel);
        initializeInfoButton(panel);

        label = new JLabel( new ImageIcon(image) );
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
        if (numZooms >= 2) {
            // // take the larger of the width vs. height
            // int widthInImages = (int) Math.ceil((double)frmImageZoomIn.getWidth() / (double)imageSideLength);
            // int heightInImages = (int) Math.ceil((double)frmImageZoomIn.getHeight() / (double)imageSideLength);
            // if (widthInImages > heightInImages) {
            //     sideLengthInImages = heightInImages;
            // } else {
            //     sideLengthInImages = widthInImages;
            // }
            // resizedImage = new BufferedImage(sideLengthInImages * imageSideLength, sideLengthInImages * imageSideLength, BufferedImage.TYPE_INT_RGB);

            // use the max of frame width vs height for side length
            int resizedLength = (frmImageZoomIn.getWidth() > frmImageZoomIn.getHeight()) ? frmImageZoomIn.getWidth() : frmImageZoomIn.getHeight();

            resizedImage = new BufferedImage(frmImageZoomIn.getWidth(), frmImageZoomIn.getWidth(), BufferedImage.TYPE_INT_RGB);

        } else {
            resizedImage = new BufferedImage(totalSideLength, totalSideLength, BufferedImage.TYPE_INT_RGB);
        }
        // resizedImage = new BufferedImage(totalSideLength, totalSideLength, BufferedImage.TYPE_INT_RGB);

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
                // g2d.drawImage(img, coord[0]*imageSideLength, coord[1]*imageSideLength, imageSideLength, imageSideLength, null);
                if (numZooms < 2) {
                    // crop image to be square without distortion
                    int min = (image.getWidth() > image.getHeight()) ? image.getWidth() : image.getHeight();
                    img = img.getSubimage(0, 0, min, min);
                
                    // draw image onto canvas
                    g2d.drawImage(img, coord[0]*imageSideLength, coord[1]*imageSideLength, imageSideLength, imageSideLength, null);

                } else {
                    // crop center of collage, remove edge images
                    // if ((coord[1] < (int)Math.ceil(((double)(sideLengthInImages/2) + (double)(sideLengthInImages/4))) 
                    //     && coord[1] > (int)Math.ceil(((double)(sideLengthInImages/2) - (double)(sideLengthInImages/4))))
                    if ((coord[0]*imageSideLength < (int)Math.ceil(((double)(sideLengthInImages/2) + (double)(sideLengthInImages/4)) * imageSideLength) 
                        && coord[0]*imageSideLength > (int)Math.ceil(((double)(sideLengthInImages/2) - (double)(sideLengthInImages/4)) * imageSideLength - 1)) 
                        && (coord[1]*imageSideLength < (int)Math.ceil(((double)(sideLengthInImages/2) + (double)(sideLengthInImages/4)) * imageSideLength) 
                        && coord[1]*imageSideLength > (int)Math.ceil(((double)(sideLengthInImages/2) - (double)(sideLengthInImages/4)) * imageSideLength - 1)) 
                    ){
                        // crop image to be square without distortion
                        int min = (image.getWidth() > image.getHeight()) ? image.getWidth() : image.getHeight();
                        img = img.getSubimage(0, 0, min, min);

                        // draw image onto canvas
                        g2d.drawImage(img, 
                        (int)Math.ceil((double)(coord[0]*imageSideLength) - (double)((sideLengthInImages/4))), 
                        (int)Math.ceil((double)(coord[1]*imageSideLength) - (double)((sideLengthInImages/4))), 
                        imageSideLength, imageSideLength, null);
    
                    } 
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
        if (replaced) {
            totalSideLength = (int) Math.ceil((double) (tempTotalSideLength * imageSideLength * 1.5));
        }
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

        // text being displayed
//        String maxLevel = "Pixel length max: " + Integer.toString(MAX_ZOOM) + " px\n";
        // String pixelSizing = "Pixel side length: " + Integer.toString(pixelSize) + " px\n";
        // String overallSize = "Total side length: " + Integer.toString(imageSideLength * lengthInImages) + " px\n";
        String imageLength = "Image side length: " + Integer.toString(imageSideLength) + " px\n";
        String zooms = "Collage replacements: " + Integer.toString(numZooms) + "\n";
        String stats = imageLength + zooms;

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

                    JTextArea info = new JTextArea("Info");
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
     * Create a cache for a collage to be rendered from. Given some parent image, match each pixel in image to the average color of an image in 
     * given map of average colors. Store image filename and map to coordinate location(s) relative to the parent image. 
     * @param parentImage
     */
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
        }

        // used later to calculate size of cropped collage
        tempTotalSideLength = endX - startX;
        
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                numPixels++;

                ArrayList<int[]> coordinates = new ArrayList<>();
                int pixelColor = parentImage.getRGB(x, y);
                String bestMatchFilename = "";
                int smallestDiff = Integer.MAX_VALUE;

                // linear search through color map for closest match
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