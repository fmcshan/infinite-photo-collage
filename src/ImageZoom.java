import java.awt.EventQueue;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageZoom {


    public static final int SCALE_DEFAULT = 1;

    private JFrame frmImageZoomIn;
    private static final String inputImage = "img.png";
    private JLabel label = null;
    private double zoom = 1.0;  // zoom factor
    private BufferedImage image = null;


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
    public ImageZoom() throws IOException {
        initialize();
    }

    private void initialize() throws IOException {
        frmImageZoomIn = new JFrame();
        frmImageZoomIn.setTitle("Image Zoom In and Zoom Out");
        frmImageZoomIn.setBounds(100, 100, 450, 300);
        frmImageZoomIn.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JScrollPane scrollPane = new JScrollPane();
        frmImageZoomIn.getContentPane().add(scrollPane, BorderLayout.CENTER);

        image = ImageIO.read(new File(inputImage));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // display image as icon
        Icon imageIcon = new ImageIcon(inputImage);
        label = new JLabel( imageIcon );
        panel.add(label, BorderLayout.CENTER);

        panel.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                double temp = zoom - (notches * 0.2);
                // minimum zoom factor is 1.0
                temp = Math.max(temp, 1.0);
                if (temp != zoom) {
                    zoom = temp;
                    //resizeImage();
                }
            }
        });
        scrollPane.setViewportView(panel);
    }

    public BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        //Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
        //BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        //outputImage.getGraphics().drawImage((java.awt.Image) resultingImage, 0, 0, null);
        return originalImage;
    }


}