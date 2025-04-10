
import org.opencv.videoio.VideoCapture;
import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class CameraTest {

    static {
        // Load OpenCV native library
        System.load(new java.io.File("native/windows/opencv_java4110.dll").getAbsolutePath());
    }

    // Convert OpenCV Mat to BufferedImage
    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] b = new byte[bufferSize];
        mat.get(0, 0, b); // get all the pixels

        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    public static void main(String[] args) {
        VideoCapture camera = new VideoCapture(0);

        if (!camera.isOpened()) {
            System.out.println("❌ Error: Camera not found");
            return;
        }

        JFrame frame = new JFrame("Webcam Feed");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel imageLabel = new JLabel();
        frame.setContentPane(imageLabel);
        frame.setSize(640, 480);
        frame.setVisible(true);

        Mat matFrame = new Mat();

        while (true) {
            if (camera.read(matFrame)) {
                BufferedImage img = matToBufferedImage(matFrame);
                if (img != null) {
                    ImageIcon icon = new ImageIcon(img);
                    imageLabel.setIcon(icon);
                    imageLabel.repaint();
                }
            }

            if (!frame.isDisplayable()) {
                break;
            }
        }

        camera.release();
        frame.dispose();
    }
}
