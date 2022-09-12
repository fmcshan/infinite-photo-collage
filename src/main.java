import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class Image extends Component {
    BufferedImage image;

    Image() {
        try {
            image = ImageIO.read(new File("img.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}

public class main {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        Image image = new Image();
        frame.add(image);
        frame.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        frame.pack();
        frame.setVisible(true);
    }
}
