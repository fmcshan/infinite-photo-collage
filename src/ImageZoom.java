import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.plaf.FontUIResource;
import javax.swing.JPanel;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
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
    double EDGE_TOLERANCE = 0.4; // percent of image width/height to ignore around edges when color matching
    int COLOR_TOLERANCE = Integer.MAX_VALUE; // tolerance for matching colors
    int MAX_ZOOM = 10; // max side length of 1 original pixel before image replacement
    private JFrame frmImageZoomIn;
    private JLabel label = null;
    private BufferedImage image = null; // zoom level 0
    private int pixelSize = 1;  // side length of 1 pixel in original image
    private Map<Integer, String> averageColors;
    private ArrayList<String> files = new ArrayList<>();
    private boolean replaced = false;
    private int numZooms = 0;

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
        JScrollPane scrollPane = new JScrollPane();
        frmImageZoomIn.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // prompt for user inputs
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter an image file name:");
        String filename = br.readLine();        
        try {
            image = ImageIO.read(new File(filename));
        } catch (IOException e) {
            System.out.println(e);
        }
        getAverageColor(image, false);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // add info menu
        panel.add(displayInfoMenu());

        // display image as icon
        Icon imageIcon = new ImageIcon(filename);
        label = new JLabel( imageIcon );
        panel.add(label, BorderLayout.CENTER);

        // add zoom ability. can only zoom in 
        panel.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                if (notches > 0) {
                    if (pixelSize < MAX_ZOOM) {
                        pixelSize = pixelSize + 1;
                        resize(panel);
                    } else {
                        // System.out.printf("zoom maxed out\n");
                        try {
                            replacePixelsWithImages(panel);
                            replaced = true;
                            pixelSize = 1;
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        });
        scrollPane.setViewportView(panel);
    }

    /**
     * Rescale and display image, which replaces current image
     * @param panel
     */
    public void resize (JPanel panel) {
        // calculate new width and height
        int w = (int)Math.ceil(image.getWidth() * pixelSize);
        int h = (int)Math.ceil(image.getHeight() * pixelSize);

        // create new bufferedimage object with new dimensions
        BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // redraw image pixel by pixel
        Graphics2D graphics2D = resizedImage.createGraphics();
        drawPixelByPixel(graphics2D);
        graphics2D.dispose();

        // place resized image in new jlabel and replace old
        label = new JLabel( new ImageIcon(resizedImage) );
        panel.removeAll();
        panel.add(displayInfoMenu());
        panel.add(label, BorderLayout.CENTER);
        panel.repaint();
        panel.validate();
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
     * Replace all pixel areas visible with images that match the pixel's color. Color is matched up to a certain tolerance value. The displayed collage is a single graphic.
     * @param panel
     * @throws Exception
     */
    public void replacePixelsWithImages(JPanel panel) throws Exception {
        // memory saver: after 1 zoom level, only use "center" images to create the next collage.
        if (replaced) {
            int lengthInImages = (int) Math.sqrt(files.size());
            BufferedImage innerCollage = new BufferedImage(lengthInImages * pixelSize, lengthInImages * pixelSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = innerCollage.createGraphics();
            int var = 0;
            for (int i = 0; i < lengthInImages; i++) {
                for (int j = 0; j < lengthInImages; j++) {
                    String file = files.get(var);
                    // System.out.println( files.get(var));
                    g2d.drawImage(ImageIO.read(new File(file)), i*pixelSize, j*pixelSize, pixelSize, pixelSize, null);
                    var++;
                }
            }
            g2d.dispose();
            image = innerCollage;
            files = new ArrayList<>();
        }

        // create new bufferedimage object with new dimensions
        numZooms++;
        BufferedImage collage = new BufferedImage(image.getWidth()*pixelSize, image.getHeight()*pixelSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = collage.createGraphics();
        
        // replace each pixel area with new image
        for (int x = 0; x < image.getHeight(); x++) {
            for (int y = 0; y < image.getWidth(); y++) {
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
                } catch (IOException err) {
                    // System.out.println(err);
                }

                // calculate coordinates of center images. units are image areas not pixels
                if (x <= (image.getWidth() / 2)+2 && x >= (image.getWidth() / 2) - 3
                        && y <= (image.getHeight() / 2)+2 && y >= (image.getHeight() / 2) - 3) {
                    files.add(bestMatchFilename);
                    // System.out.printf("%d,%d\n", x, y);
                }

                g2d.drawImage(img, x * pixelSize, y * pixelSize, pixelSize, pixelSize, null);
            }
        }
        // System.out.printf("files: %s\n", files.toString());
        g2d.dispose();

        // place collage in new jlabel and replace old
        label = new JLabel( new ImageIcon(collage) );
        panel.removeAll();
        panel.add(displayInfoMenu());
        panel.add(label, BorderLayout.CENTER);
        panel.repaint();
        panel.validate();
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
    public JTextArea displayInfoMenu() {
        JTextArea info = new JTextArea();

        // text being displayed
        String maxLevel = "Pixel length max: " + Integer.toString(MAX_ZOOM) + " px\n";
        String pixelSizing = "Pixel side length: " + Integer.toString(pixelSize) + " px\n";
        String overallSize = "Total side length: " + Integer.toString(image.getWidth() * pixelSize) + " px\n";
        String zooms = "Collage replacements: " + Integer.toString(numZooms) + "\n";
        String stats = maxLevel + pixelSizing + overallSize + zooms;

        // set styling
        Font font = new FontUIResource(Font.MONOSPACED, Font.BOLD, 16);
        info.setFont(font);
        info.setText(stats);
        info.setEditable(false);
        info.setBackground(new Color(255,255,255,200));
        info.setBounds(5, 5, 280, 120);
    
        return info;
    }

}