import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

public class CameraTest {

    static{
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Loads OpenCV Library
    }

    public static void main(String[] args) {
        VideoCapture camera =new VideoCapture(0);

        if(!camera.isOpened()){
            System.out.println("❌ Error: Camera not found");
            return;
        }

        Mat frame=new Mat();

        while(true){
            if(camera.read(frame)){
                HighGui.imshow("Webcam Feed",frame);
            }

            if(HighGui.waitKey(30)=='q'){
                break;
            }
        }

        camera.release();
        HighGui.destroyAllWindows();


    }

}
