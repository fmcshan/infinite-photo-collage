import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ImageZoom {

    public static final int SCALE_DEFAULT = 1; // 
    double EDGE_TOLERANCE = 0.4; // percent of image width/height to ignore around edges when color matching
    int COLOR_TOLERANCE = Integer.MAX_VALUE; // tolerance for matching colors
    int MAX_ZOOM = 25; // max side length of 1 original pixel before image replacement
    private JFrame frmImageZoomIn;
    private JLabel label = null;
    private BufferedImage image = null;
    private int pixelSize = 1;  // side length of 1 pixel in original image
    private Map<Integer, String> averageColors;

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

    public ImageZoom() throws Exception {
        averageColors = calculateAverageColors();
        initialize();
    }

    private void initialize() throws Exception {
        frmImageZoomIn = new JFrame();
        frmImageZoomIn.setTitle("Image Zoom In and Zoom Out");
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
                        int temp = (int)((notches*SCALE_DEFAULT) + 1);
                        pixelSize = pixelSize + (temp);
                        resize(panel);
                        System.out.printf("pixelSize: %d\n", pixelSize);
                    } else {
                        System.out.printf("zoom maxed out\n");
                        try {
                            replacePixelsWithImages(panel);
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
        // image = resizedImage;

        // place resized image in new jlabel and replace old
        label = new JLabel( new ImageIcon(resizedImage) );
        panel.removeAll();
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

        // create new bufferedimage object with new dimensions
        BufferedImage collage = new BufferedImage(image.getWidth()*pixelSize, image.getHeight()*pixelSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = collage.createGraphics();
        
        // replace each pixel area with new image
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixelColor = image.getRGB(x, y);
                BufferedImage img = null;
                String bestMatchFilename = "";
                int smallestDiff = COLOR_TOLERANCE;

                for (Integer imgColor: averageColors.keySet()) {
                    // old method
                    // if (imgColor > pixelColor - COLOR_TOLERANCE && imgColor < pixelColor + COLOR_TOLERANCE) {
                    //     bestMatch = averageColors.get(imgColor);
                    // }
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
                g2d.drawImage(img, x * pixelSize, y * pixelSize, pixelSize, pixelSize, null);
            }
        }
        g2d.dispose();

        // place collage in new jlabel and replace old
        label = new JLabel( new ImageIcon(collage) );
        panel.removeAll();
        panel.add(label, BorderLayout.CENTER);
        panel.repaint();
        panel.validate();
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

        // https://community.oracle.com/tech/developers/discussion/1206435/convert-java-awt-color-to-hex-string
        // print out RGB and hex code of color
        // String r = Integer.toHexString(averageColor.getRed());
        // String g = Integer.toHexString(averageColor.getGreen());
        // String b = Integer.toHexString(averageColor.getBlue());
        // System.out.printf("average color: #%s%s%s, %s\n", r, g, b, averageColor);

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

}