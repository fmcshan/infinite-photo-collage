import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

public class TestFileUpload {

  private JFrame frame = new JFrame();
  private JPanel panel = new JPanel();
  private JTextArea prompt = new JTextArea("");
  private JButton fileUploadBtn = new JButton("Choose directory");

  public void run() {
    
    frame.setTitle("Test");
    frame.setBounds(100, 100, 450, 300);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
    frame.add(panel);

    fileUploadBtn.setFocusable(false);
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
              /*
              averageColors = calculateAverageColors(imagesDirectoryPath);
              Random rand = new Random();
              image = ImageIO.read(files.get(rand.nextInt(givenList.size())));

              // continue to collage visuals
              */
            }
            // System.out.println(filePath);
          }
        }
      }
      
    });

    // menuBar.add(fileUploadBtn);
    // panel.setLayout(new BorderLayout());
    panel.add(fileUploadBtn, BorderLayout.CENTER);
    panel.setFocusable(true);
  }

    public static void main(String[] args) {
    TestFileUpload test = new TestFileUpload();
    test.run();

  }
  
}
