import org.opencv.core.Core;

public class OpenCVTest {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); } // Load OpenCV

    public static void main(String[] args) {
        System.out.println("✅ OpenCV Version: " + Core.VERSION);
    }
}
