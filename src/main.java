import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class Image extends Component {
    BufferedImage image;

    public Image (String filename) {
        try {
            image = ImageIO.read(new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int newImageWidth = image.getWidth() * 2;
        int newImageHeight = image.getHeight() * 2;
        BufferedImage resizedImage = new BufferedImage(newImageWidth , newImageHeight, 1);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, newImageWidth , newImageHeight , null);
        g.dispose();
    }

    public void paint(Graphics g) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                int red = (color & 0x00ff0000) >> 16;
                int green = (color & 0x0000ff00) >> 8;
                int blue = color & 0x000000ff;
                g.setColor(new Color(red, green, blue));
                g.drawLine(x, y, x, y);
            }
        }
    }

    public void resizeScale (double scaleFactor) {
        int w = (int)Math.round(image.getWidth() * scaleFactor);
        int h = (int)Math.round(image.getHeight() * scaleFactor);
        BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, w, h, null);
        graphics2D.dispose();
        image = resizedImage;
    }

    public void resizeToPixels (double pixels, boolean isWidth) {
        int w = 0;
        int h = 0;
        if (isWidth) {
            // autoscale height
            w = (int)pixels;
            h = (int)Math.round(pixels/image.getWidth() * image.getHeight());
        } else {
            // autoscale width
            h = (int)pixels;
            w = (int)Math.round(pixels/image.getHeight() * image.getWidth());
        }
        BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, w, h, null);
        graphics2D.dispose();
        image = resizedImage;
    }

    public void resizeToSquare () {
        int length = 0;
        if (image.getWidth() < image.getHeight()) {
            length = image.getWidth();
        } else {
            length = image.getHeight();
        }
        BufferedImage resizedImage = new BufferedImage(length, length, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, length, length, null);
        graphics2D.dispose();
        image = resizedImage;
    }

}

public class main {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        int randIdx = (int)(Math.random() * 10);
        Image img = new Image("images/fruit_0000.jpg");
        // img.resizeScale(0.3);
        img.resizeToPixels(200, false);
        frame.add(img);

        frame.setPreferredSize(new Dimension(800, 600));

        // add ZoomEvent listener
        
        frame.pack();
        frame.setVisible(true);
    }
}
