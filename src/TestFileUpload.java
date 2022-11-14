import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import javafx.event.ActionEvent;

import java.awt.*;

public class TestFileUpload {

  private JFrame frame = new JFrame();
  private JPanel panel = new JPanel();
  private JMenuBar menuBar = new JMenuBar();
  private JButton fileUploadBtn = new JButton("Choose images");
  private String imagesDirectory = "";

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
            File directory = new File(fc.getSelectedFile().getAbsolutePath());
            File[] files = directory.listFiles();
            if (files != null) {
              for (File f : files) {
                int extIdx = f.toString().lastIndexOf(".");
                if (extIdx > 0) {
                  //
                } else {
                  // not all files are images
                }

              }
            }
            // System.out.println(filePath);
          }
        }
      }
      
    });

    menuBar.add(fileUploadBtn);
    panel.setLayout(new BorderLayout());
    panel.add(menuBar, BorderLayout.NORTH);
    panel.setFocusable(true);
  }

    public static void main(String[] args) {
    TestFileUpload test = new TestFileUpload();
    test.run();

  }
  
}
