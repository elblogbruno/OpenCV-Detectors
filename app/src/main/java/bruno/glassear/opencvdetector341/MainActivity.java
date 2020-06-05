package bruno.glassear.opencvdetector341;



import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.icu.text.SimpleDateFormat;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import android.widget.Toast;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import org.opencv.core.Mat;

import org.opencv.core.Rect;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    public static final int JAVA_DETECTOR = 0;
    public static final int OBJECT_DETECTOR = 1;
    public static final int FACE_DETECTOR_DNN = 2;
    public static final int POSE_DETECTOR_DNN = 3;
    public int                    mDetectorType       = JAVA_DETECTOR; //set  default detector on start
    private String[]               mDetectorName;
    private String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Calendar.getInstance().getTime());
    public String file_uri;


    private Rect[] mRectFaces;
    private CameraBridgeViewBase mOpenCvCameraView;
    OpenCVDetector mOpenCVDetector;
    private Mat                    mRgba;
    private Mat                    mGray;
    public Mat                     mFinalImage;



    private MenuItem               mButtonClose;
    private MenuItem               mDetectorFace;
    private MenuItem               mDetectorObject;
    private MenuItem               mDetectorFaceDnn;
    private MenuItem               mDetectorPoseDnn;


    private boolean is_detection_on = true;
    private boolean network_loaded = false;
    private int detector_index = 0;
    private boolean only_face = false;
    private boolean CameraIndex = true;
    public boolean button_clicked = false;





    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    try {

                        mOpenCVDetector.createFaceDetector(mAppContext);

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.enableView();
                        break;

                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };


    public void FdActivity() {
        mDetectorName = new String[3];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[OBJECT_DETECTOR] = "Deep Network";
        mDetectorName[FACE_DETECTOR_DNN] = "Face detector dnn";
        mDetectorName[POSE_DETECTOR_DNN] = "Pose detector dnn";
        Log.i(TAG, "Instantiated new " + this.getClass());

    }
    /** Called when the activity is first created. */


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        Log.i(TAG, "started" + String.valueOf(mDetectorName));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("Normal Face Detector");
        Boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .getBoolean("isFirstRun", true);

        if (isFirstRun) {
            //show start activity
            startActivity(new Intent(MainActivity.this, PermissionAsk.class));
            Toast.makeText(MainActivity.this, "First Run", Toast.LENGTH_LONG)
                    .show();
        }
        System.loadLibrary("opencv_java3");
        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                .putBoolean("isFirstRun", false).commit();

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();

        // enables full screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // creates an instance of our OpenCV object detector
        mOpenCVDetector = new OpenCVDetector();
        // creates an instance of our photo holder
        final Button button1 = findViewById(R.id.CameraButton);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                button_clicked = true;
                takePhoto(detector_index);
            }
        });


    }




    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }



    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    public void onCameraViewStarted(int width, int height) {
        //System.out.print("createAIDetector !!!!!!!!!!!!!!!!!!");

    }


    public void onCameraViewStopped() {

    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Mat image_vis  = mRgba;
        if (is_detection_on && mDetectorType == JAVA_DETECTOR){
            mRectFaces = mOpenCVDetector.detectFaces(mRgba);
            image_vis = mOpenCVDetector.drawFaces(image_vis, mRectFaces);
        }else if(is_detection_on && mDetectorType == OBJECT_DETECTOR && network_loaded == true){
            System.out.println(image_vis.height());
            System.out.println(image_vis.width());
            image_vis = mOpenCVDetector.detectObjectsDnn(image_vis);
        }else if(is_detection_on && mDetectorType == FACE_DETECTOR_DNN && network_loaded == true){

            image_vis = mOpenCVDetector.detectFacesDnn(image_vis);
        }

        System.out.println(image_vis.height());
        System.out.println(image_vis.width());


        return image_vis;


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Log.i(TAG, "called onCreateOptionsMenu");
        mDetectorFace = menu.add("Face Detector");
        mDetectorObject = menu.add("Object Detector");
        mDetectorFaceDnn = menu.add("Face Detector(DNN)");
        mDetectorPoseDnn = menu.add("Pose Detector(DNN)");


        mButtonClose = menu.add("Close app");

        getMenuInflater();
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mButtonClose)
            System.exit(0);
        else if (item == mDetectorFace)
            DetectorSwap(JAVA_DETECTOR);
        else if (item == mDetectorObject)
            DetectorSwap(OBJECT_DETECTOR);
        else if (item == mDetectorFaceDnn)
            DetectorSwap(FACE_DETECTOR_DNN);
        else if (item == mDetectorPoseDnn)
            DetectorSwap(POSE_DETECTOR_DNN);


        switch (item.getItemId()) {
            case R.id.camera_change:
                cameraSwap();
                return true;
            case R.id.detector_switch:
                detectorSwitch();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }


    public void cameraSwap(){
        if (CameraIndex == true) {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView.setCameraIndex(1);
            mOpenCvCameraView.enableView();
            Toast.makeText(getApplication().getBaseContext(), "Changing to front camera!",
                    Toast.LENGTH_LONG).show();

            CameraIndex = false;
        } else {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView.setCameraIndex(0);
            mOpenCvCameraView.enableView();
            Toast.makeText(getApplication().getBaseContext(), "Changing to back camera!",
                    Toast.LENGTH_LONG).show();
            CameraIndex = true;
        }
    }

    public void loadNetwork(int index){
        if(index == 1){
            mOpenCVDetector.createObjectDetectorDnn(this);
            Log.i(TAG, "loaded object detection");
            network_loaded = true;
        }


        if(index == 2){
            mOpenCVDetector.createFaceDetectorDnn(this);
            Log.i(TAG, "loaded face detection");
            network_loaded = true;
        }

        if(index == 3){
            mOpenCVDetector.createPoseDetectorDnn(this);
            Log.i(TAG, "loaded pose detection");
            network_loaded = true;
        }


    }
    public void DetectorSwap(int detection)  {
        if(detection == OBJECT_DETECTOR){
            this.setTitle("Object Detector with DNN");
            Toast.makeText(this, "Changing to object detection!", Toast.LENGTH_LONG).show();
            mDetectorType = OBJECT_DETECTOR;
            loadNetwork(1);
            detector_index = detection;
            Toast.makeText(this, mDetectorType + "Network loaded!", Toast.LENGTH_LONG).show();

            Log.i(TAG, "Changing to object detection");
        }
        if (detection == JAVA_DETECTOR){
            this.setTitle("Normal Face Detector");
            Toast.makeText(this, "Changing to face detection!", Toast.LENGTH_LONG).show();
            mDetectorType = JAVA_DETECTOR;
            detector_index = detection;
            Log.i(TAG, "Changing to face detection");
        }
        if (detection == FACE_DETECTOR_DNN){
            this.setTitle("Face Detector with DNN");
            Toast.makeText(this, "Changing to face detection with dnn!", Toast.LENGTH_LONG).show();
            mDetectorType = FACE_DETECTOR_DNN;
            loadNetwork(2);
            detector_index = detection;
            Toast.makeText(this, mDetectorType + "Network loaded!", Toast.LENGTH_LONG).show();
            Log.i(TAG, "Changing to face detection Dnn");
        }
        if (detection == POSE_DETECTOR_DNN){
            this.setTitle("Pose Detector with DNN");
            Toast.makeText(this, "Changing to pose detection with dnn!", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "This  detector does not work in realtime! Anyway you can take a photo!", Toast.LENGTH_LONG).show();
            mDetectorType = POSE_DETECTOR_DNN;
            detector_index = detection;
            //Toast.makeText(getBaseContext(), mDetectorType + "Network loaded!", Toast.LENGTH_LONG).show();
            Log.i(TAG, "Changing to pose detection Dnn");
        }

    }


    public void detectorSwitch(){
        if (is_detection_on == true) {
            Toast.makeText(this, "Turning off realtime detector!", Toast.LENGTH_LONG).show();

            is_detection_on = false;
        } else {
            Toast.makeText(this, "Turning on realtime detector", Toast.LENGTH_LONG).show();
            is_detection_on = true;
        }
    }

    public void takePhoto(int detection){
        Mat image = mRgba;
        file_uri = Environment.getExternalStorageDirectory() + "/Images/" + timeStamp + "_image_" + mDetectorName + ".png";
        if(detection == 2){
            mOpenCVDetector.createFaceDetectorDnn(this);
            image = mOpenCVDetector.detectFacesDnn(image);
        }
        if(detection == 3){
            mOpenCVDetector.createPoseDetectorDnn(this);
            image = mOpenCVDetector.detectPoseDnn(image);
        }
        if(detection == 1){
            mOpenCVDetector.createObjectDetectorDnn(this);
            image = mOpenCVDetector.detectObjectsDnn(image);
        }
        Mat image_roi_rgb = new Mat();
        Imgproc.cvtColor(image, image_roi_rgb, Imgproc.COLOR_BGR2RGB);
        mFinalImage = image_roi_rgb;
        Imgcodecs.imwrite(file_uri, image_roi_rgb);

        if(button_clicked == true) {
            Intent myIntent = new Intent(MainActivity.this, PhotoHolder.class);
            myIntent.putExtra("uri", file_uri.toString());
            startActivity(myIntent);
        }else{
            Log.i(TAG, "doing nothing");
        }
    }
}