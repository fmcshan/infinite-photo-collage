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
import java.io.File;
import java.io.IOException;

public class ImageZoom {


    public static final int SCALE_DEFAULT = 1;

    private JFrame frmImageZoomIn;
    private static final String inputImage = "fruit_0000.jpg";
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
                if (notches > 0) {
                    //System.out.println(notches);
                    double temp = (notches * 0.2) + 1;
                    //System.out.println(temp);
                    resizeScale(temp, panel);
                    //resizeScale(temp);
                    //zoom = temp;
                    panel.repaint();
                    // minimum zoom factor is 1.0
//                temp = Math.max(temp, 1.0);
//                if (temp != zoom) {
//                    zoom = temp;
//                    resizeScale(zoom);
//                    panel.repaint();
//                }
                }
                }
        });
        scrollPane.setViewportView(panel);
    }

    public void resizeScale (double scaleFactor, JPanel panel) {
        int w = (int)Math.round(label.getIcon().getIconWidth() * scaleFactor);
        int h = (int)Math.round(label.getIcon().getIconHeight() * scaleFactor);

        BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, w, h, null);
        graphics2D.dispose();
        image = resizedImage;

        label = new JLabel( new ImageIcon(image) );
        panel.remove(0);
        panel.add(label, BorderLayout.CENTER);

    }


}