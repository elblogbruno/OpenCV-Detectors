package bruno.glassear.opencvdetector341;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Environment;
import android.support.annotation.ArrayRes;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.lang.Math;

import static org.opencv.core.Core.minMaxLoc;

public class OpenCVDetector {
    private static final String TAG = "OCVSample::OpenCVDetector";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;


    private Net net_object;//object dnn detector
    private Net net_face;
    private Net net_pose; //face dnn detector
    private File mCascadeFile;//Normal face detector
    private CascadeClassifier mJavaDetector;//Normal face detector
    private int mDetectorType = JAVA_DETECTOR;//Normal face detector
    public  int id1 = 0;
    private String[] mDetectorName;//Normal face detector
    private float mRelativeFaceSize = 0.2f;//Normal face detector
    private int mAbsoluteFaceSize = 0;//Normal face detector
    private String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Calendar.getInstance().getTime());
    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};



    // loads cascade_classifier for sample not dnn face detector
    public void createFaceDetector(Context context) throws IOException {

        InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
        FileOutputStream os = new FileOutputStream(mCascadeFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();

        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        mJavaDetector.load( mCascadeFile.getAbsolutePath() );
        if (mJavaDetector.empty()) {
            Log.e(TAG, "Failed to load cascade classifier");
            mJavaDetector = null;
        } else
            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

        cascadeDir.delete();
    }


    //Load object detector with dnn module network
    public void createObjectDetectorDnn(Context context){
        String proto = getPath("MobileNetSSD_deploy.prototxt", context);
        String weights = getPath("MobileNetSSD_deploy.caffemodel", context);
        net_object = Dnn.readNetFromCaffe(proto, weights);
        Log.i(TAG, "Network loaded successfully");
    }

    //Load face detector with dnn module network
    @SuppressLint("LongLogTag")
    public void createFaceDetectorDnn(Context context){
        String proto = getPath("face_detection_dnn.prototxt", context);
        String weights = getPath("res10_300x300_ssd_iter_140000.caffemodel", context);
        net_face = Dnn.readNetFromCaffe(proto, weights);
        Log.i(TAG, "Face detector Network loaded successfully");
    }

    //Load pose detector with dnn module network
    public void createPoseDetectorDnn(Context context){
        String proto = getPath("pose_deploy_linevec_faster_4_stages.prototxt", context);
        String weights = getPath("pose_iter_160000.caffemodel", context);
        net_pose = Dnn.readNetFromCaffe(proto, weights);
        Log.i(TAG, "Pose detector Network loaded successfully");
    }


    public Mat detectPoseDnn(Mat frame){
        long timerstart = System.currentTimeMillis();
        final int IN_WIDTH = 368;
        final int IN_HEIGHT = 368;
        final double IN_SCALE_FACTOR = 1.0/255;
        final double CONFIDENCE = 0.1;
        int frameWidth  = frame.width();

        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

        Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                new Size(IN_WIDTH, IN_HEIGHT),
                new Scalar(0, 0, 0), false,false);
        net_pose.setInput(blob);

        Mat output = net_pose.forward();
        Mat output_reshape = output.reshape(1, 44);

        // find the position of the body parts
        ArrayList<Point> points = new ArrayList();
        for (int i=0; i < 15; i++) {

            //confidence map of corresponding body's part.
            Mat probMap = output_reshape.row(i);
            int probMapSize = (int)Math.sqrt(probMap.total());
            probMap = probMap.reshape(1, probMapSize);
            Log.i(TAG, "Finding global maxima of the probMap!");
            // Find global maxima of the probMap.
            Core.MinMaxLocResult minmax = minMaxLoc(probMap);

            //Scale the point to fit on the original image
            float ratio_x = (float)frame.width() / probMapSize;
            float ratio_y = (float)frame.height() / probMapSize;

            if (minmax.maxVal > CONFIDENCE) {
                float x = (float)minmax.maxLoc.x * ratio_x;
                float y = (float)minmax.maxLoc.y * ratio_y;
                Log.i(TAG, "Drawing points!");
                Imgproc.circle(frame, new Point(x, y), 10, new Scalar(0, 0, 255),10);
            }
        }
        long timerend = System.currentTimeMillis();
        long tooktime = timerend-timerstart;

        Log.i(TAG, String.valueOf(tooktime));
        return frame;


    }

    public Mat detectFacesDnn(Mat frame){
        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
        //final double IN_SCALE_FACTOR = 0.007843;
        final double IN_SCALE_FACTOR = 1.0;
        final double CONFIDENCE = 0.5;
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        //Imgproc.resize(frame, frame, new Size(IN_WIDTH, IN_HEIGHT));

        Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                new Size(IN_WIDTH, IN_HEIGHT),
                new Scalar(104.0, 177.0, 123.0), false,false);
        net_face.setInput(blob);
        Mat detections = net_face.forward();
        // timer end
        /*int cols = frame.cols();
        int rows = frame.rows();
        Size cropSize;
        if ((float)cols / rows > WH_RATIO) {
            cropSize = new Size(rows * WH_RATIO, rows);
        } else {
            cropSize = new Size(cols, cols / WH_RATIO);
        }
        int y1 = (int)(rows - cropSize.height) / 2;
        int y2 = (int)(y1 + cropSize.height);
        int x1 = (int)(cols - cropSize.width) / 2;
        int x2 = (int)(x1 + cropSize.width);
        Mat subFrame = frame.submat(y1, y2, x1, x2);
        cols = subFrame.cols();
        rows = subFrame.rows();*/
        int cols = IN_WIDTH;
        int rows = IN_HEIGHT;
        float x_ratio = (float)frame.cols() / IN_WIDTH;
        float y_ratio = (float)frame.rows() / IN_HEIGHT;
        detections = detections.reshape(1, (int)detections.total() / 7);
        System.out.print("detections " + detections.rows());
        for (int i = 0; i < detections.rows(); ++i) {
            double confidence = detections.get(i, 2)[0];
            if (confidence > CONFIDENCE) {
                //int classId = (int) detections.get(i, 1)[0];
                /*int xLeftBottom = (int) (detections.get(i, 3)[0] * cols);
                int yLeftBottom = (int) (detections.get(i, 4)[0] * rows);
                int xRightTop = (int) (detections.get(i, 5)[0] * cols);
                int yRightTop = (int) (detections.get(i, 6)[0] * rows);*/
                int xLeftBottom = (int) ((detections.get(i, 3)[0] * cols) * x_ratio);
                int yLeftBottom = (int) ((detections.get(i, 4)[0] * rows) * y_ratio);
                int xRightTop = (int) ((detections.get(i, 5)[0] * cols) * x_ratio);
                int yRightTop = (int) ((detections.get(i, 6)[0] * rows) * y_ratio);

                // Draw rectangle around detected object.
                Imgproc.rectangle(frame, new Point(xLeftBottom, yLeftBottom),
                        new Point(xRightTop, yRightTop),
                        new Scalar(0, 255, 0));
            }
        }
        return frame;
    }

    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }

    public Mat detectObjectsDnn(Mat frame){
        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 0.007843;
        final double MEAN_VAL = 127.5;
        final double THRESHOLD = 0.8;
        // Get a new frame
        System.out.println(frame.height());
        System.out.println(frame.width());

        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        // Forward image through network.
        Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                new Size(IN_WIDTH, IN_HEIGHT),
                new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false,false);
        net_object.setInput(blob);
        // timer start

        Mat detections = net_object.forward();
        // timer end
        int cols = frame.cols();
        int rows = frame.rows();
        Size cropSize;
        if ((float)cols / rows > WH_RATIO) {
            cropSize = new Size(rows * WH_RATIO, rows);
        } else {
            cropSize = new Size(cols, cols / WH_RATIO);
        }
        int y1 = (int)(rows - cropSize.height) / 2;
        int y2 = (int)(y1 + cropSize.height);
        int x1 = (int)(cols - cropSize.width) / 2;
        int x2 = (int)(x1 + cropSize.width);
        Mat subFrame = frame.submat(y1, y2, x1, x2);
        cols = subFrame.cols();
        rows = subFrame.rows();
        detections = detections.reshape(1, (int)detections.total() / 7);
        System.out.print("detections " + detections.rows());
        for (int i = 0; i < detections.rows(); ++i) {
            double confidence = detections.get(i, 2)[0];
            if (confidence > THRESHOLD) {
                int classId = (int)detections.get(i, 1)[0];
                int xLeftBottom = (int)(detections.get(i, 3)[0] * cols);
                int yLeftBottom = (int)(detections.get(i, 4)[0] * rows);
                int xRightTop   = (int)(detections.get(i, 5)[0] * cols);
                int yRightTop   = (int)(detections.get(i, 6)[0] * rows);
                // Draw rectangle around detected object.
                Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom),
                        new Point(xRightTop, yRightTop),
                        new Scalar(0, 255, 0));
                String label = classNames[classId] + ": " + confidence;
                int[] baseLine = new int[1];
                Size labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);
                // Draw background for label.
                Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom - labelSize.height),
                        new Point(xLeftBottom + labelSize.width, yLeftBottom + baseLine[0]),
                        new Scalar(255, 255, 255), Core.FILLED);
                // Write class name and confidence.
                Imgproc.putText(subFrame, label, new Point(xLeftBottom, yLeftBottom),
                        Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0));
            }
        }
        return frame;
    }

    public Rect[] detectFaces(Mat image_bgr) {
        Mat image_gray =  new Mat();
        Imgproc.cvtColor(image_bgr, image_gray, Imgproc.COLOR_BGR2GRAY); //change crop to rgba
        // run detector
        // store detected faces into -> facesArray
        // return false if no faces, true one or more faces
        MatOfRect faces = new MatOfRect();
        if (mAbsoluteFaceSize == 0) {
            int height = image_gray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(image_gray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        }

        Rect[] facesArray = faces.toArray();
        return facesArray;
    }

    public Mat drawFaces(Mat image, Rect[] faces) {
        //Just a function that draws rectangles on the faces
        for (int i = 0; i < faces.length; i++) {
            //Log.i(TAG, "Drawing rectangles");
            Imgproc.rectangle(image, faces[i].tl(), faces[i].br(), FACE_RECT_COLOR, 3);
        }
        return image;
    }

    public List<Mat> cropObjects(Mat image, Rect[] rois) {
        Log.i(TAG, "Cropping objects");
        System.out.println(rois.length);

        List<Mat> list_rois = new ArrayList<>();
        for (int i = 0; i < rois.length; i++) {
            Mat cropped_roi = cropROI(image, rois[i], i);
            list_rois.add(cropped_roi);

        }
        return list_rois;
    }


    public void saveCroppedRois(List<Mat> list_rois) {
        int id = 0;
        id1 = id;
        for (Mat roi : list_rois) {
            Boolean bool = Imgcodecs.imwrite(Environment.getExternalStorageDirectory() + "/Images/" + timeStamp.toString() + "_Face_Crop" + id + ".png", roi);

            if (bool)
                Log.i(TAG, "SUCCESS writing image to external storage");
            else
                Log.i(TAG, "Fail writing image to external storage");
            id++;
        }
    }

    public Mat cropROI(Mat image, Rect roi, int id) {
        Rect rectCrop = new Rect(roi.x, roi.y, roi.width, roi.height);  //Crops the face with x,y,width and height
        Mat image_roi_bgr = new Mat(image, rectCrop); //Saves the crop to a new mat called image_roi_bgr(in blue color)
        Mat image_roi_rgb = new Mat();
        Imgproc.cvtColor(image_roi_bgr, image_roi_rgb, Imgproc.COLOR_BGR2RGB); //change crop to rgba
        return image_roi_rgb;
    }


}
