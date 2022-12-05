import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.*;

public class FileUpload {

  private JFrame frame = new JFrame();
  private JPanel panel = new JPanel();
  private JTextArea prompt = new JTextArea("");
  private JButton fileUploadBtn = new JButton("Choose directory");
  double EDGE_TOLERANCE = 0.4; // percent of image width/height to ignore around edges when color matching
  private BufferedImage image;
  private ImageZoom imageZoom;
  private Map<Integer, String> averageColors;

  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          FileUpload fileUpload = new FileUpload();
          fileUpload.run();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public BufferedImage run() throws Exception {
    frame.setTitle("Infinite Photo Collage");
    frame.setBounds(100, 100, 450, 300);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
    frame.add(panel);

    fileUploadBtn.setFocusable(false);

    prompt.setText("Select a directory of images to upload.");
    prompt.setEditable(false);
    panel.add(prompt, BorderLayout.CENTER);
    panel.add(fileUploadBtn, BorderLayout.CENTER);
    panel.setFocusable(true);

    fileUploadBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        if (e.getSource() == fileUploadBtn) {
          // popup, user can only choose directories
          JFileChooser fc = new JFileChooser();
          fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

          // check directory contains only image file types
          int response = fc.showOpenDialog(null);
          if (response == JFileChooser.APPROVE_OPTION) {
            String imagesDirectoryPath = fc.getSelectedFile().getAbsolutePath();
            File directory = new File(fc.getSelectedFile().getAbsolutePath());
            File[] files = directory.listFiles();
            if (files != null) {
              // we assume all files are image files
              // process files and choose random one to start
              try {
                averageColors = calculateAverageColors(imagesDirectoryPath);
              } catch (IOException ioException) {
                ioException.printStackTrace();
              }
              Random rand = new Random();
              try {
                // crop initial image to be square. use min of width and height
                File file = files[rand.nextInt(files.length)];
                imageZoom = new ImageZoom(file, averageColors);
                frame.setVisible(false);

              } catch (IOException ioException) {
                ioException.printStackTrace();
              } catch (Exception exception) {
                exception.printStackTrace();
              }
            }
          }
        }
      }
    });

    return image;
  }

  /**
   * Calculate average color of every image in dataset. Map color to name of image file.
   * @return
   * @throws IOException
   */
  public Map<Integer, String> calculateAverageColors(String path) throws IOException {
    Map<Integer, String> map = new HashMap<>();
    File directory = new File(path);
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
}
