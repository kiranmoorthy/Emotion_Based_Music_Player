import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class FaceDetectionTest {

    static {
        // Load OpenCV DLL
        System.load(new java.io.File("native/windows/opencv_java4110.dll").getAbsolutePath());
    }

    // Loads Cascade XML File
    static String cascadePath = new java.io.File("src/resources/haarcascade_frontalface_default.xml").getAbsolutePath();


    // Converts Mat to BufferedImage for display in Swing
    public static BufferedImage matToBufferedImage(Mat mat) {
        int type = (mat.channels() > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }

    public static void main(String[] args) {

        VideoCapture camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("❌ Error: Camera not found");
            return;
        }

        CascadeClassifier faceCascade = new CascadeClassifier(cascadePath);
        if (faceCascade.empty()) {
            System.out.println("❌ Error: Haar Cascade XML file not found!");
            return;
        }

        JFrame window = new JFrame("Face Detection");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(640, 480);
        JLabel label = new JLabel();
        window.add(label);
        window.setVisible(true);

        Mat frame = new Mat();

        while (window.isVisible()) {
            if (camera.read(frame)) {

                Mat gray = new Mat();
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

                MatOfRect faces = new MatOfRect();
                faceCascade.detectMultiScale(gray, faces);

                for (Rect rect : faces.toArray()) {
                    Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3);
                }

                ImageIcon image = new ImageIcon(matToBufferedImage(frame));
                label.setIcon(image);
                label.repaint();
            }
        }

        camera.release();
    }
}
