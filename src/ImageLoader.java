import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageLoader {
    /**
     * Measure the time it takes to load 50 images of the same size for 10 trials
     * @throws IOException
     */
    public static void loadImages() throws IOException {
        File directory = new File("images/experiment-images");
        File[] dirFiles = directory.listFiles();
        int numberOfTrials = 10;
        if (dirFiles != null) {
            for (File dirFile : dirFiles) {
                if (dirFile.isDirectory()) {
                    System.out.println("Directory name: " + dirFile.getName());
                    File[] imageFiles = dirFile.listFiles();
                    if (imageFiles != null) {
                        long total = 0;
                        for (int trial = 0; trial < numberOfTrials; trial++) {
                            long startTime = System.nanoTime();
                            for (int i = 0; i < 50; i++) {
                                File f = imageFiles[i];
                                BufferedImage curr_img = ImageIO.read(f);
                            }
                            long endTime = System.nanoTime();
                            long duration = (endTime - startTime) / 1000000;
                            System.out.println("Time to load: " + duration + " ms");
                            total += duration;
                        }
                        long average = total / numberOfTrials;
                        System.out.println("Average time to load: " + average + " ms");
                        System.out.println();
                    }
                }
            }
        }
    }
}
