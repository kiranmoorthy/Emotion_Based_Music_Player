import org.opencv.core.Core;

public class OpenCVTest {
    static {
        // Loads OpenCV library
        System.load(new java.io.File("native/windows/opencv_java4110.dll").getAbsolutePath());
    }

    public static void main(String[] args) {
        System.out.println("✅ OpenCV Version: " + Core.VERSION);
    }
}
