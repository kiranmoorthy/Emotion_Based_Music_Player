import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

public class FaceDetection {
    static {
        // Loads OpenCV library
        System.load(new java.io.File("native/windows/opencv_java4110.dll").getAbsolutePath());
    }

    public static void main(String[] args) {

        VideoCapture camera=new VideoCapture(0);
        if(!camera.isOpened()){
            System.out.println("❌ Error: Camera not found");
            return;
        }

        CascadeClassifier faceCascade=new CascadeClassifier("src/resources/haarcascade_frontalface_default.xml");
        if(faceCascade.empty()){
            System.out.println("❌ Error: Haar Cascade XML file not found!");
            return;
        }

        Mat frame=new Mat();

        while(true){
            if(camera.read(frame)){
                Mat grayFrame=new Mat();
                Imgproc.cvtColor(frame,grayFrame,Imgproc.COLOR_BGR2GRAY);

                MatOfRect faces=new MatOfRect();
                faceCascade.detectMultiScale(grayFrame,faces);

                for(Rect rect: faces.toArray()){
                    Imgproc.rectangle(frame,rect.tl(),rect.br(),new Scalar(0,255,0),3);
                }

                HighGui.imshow("Face Detection",frame);
            }

            if(HighGui.waitKey(30)=='q'){
                break;
            }
        }

        camera.release();
        HighGui.destroyAllWindows();


    }
}
