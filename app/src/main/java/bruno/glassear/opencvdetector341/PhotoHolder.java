package bruno.glassear.opencvdetector341;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.lang.reflect.Method;


public class PhotoHolder extends AppCompatActivity  {
    private ImageView imgTakenPic;
    private MenuItem               mButtonClose;
    MainActivity image;
    private String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    private static final String TAG = "OCVSample::Activity";
    private String file_uri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_holder);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        image = new MainActivity();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);


        Bundle extras = getIntent().getExtras();
        file_uri = extras.get("uri").toString();


        imgTakenPic = (ImageView)findViewById(R.id.imageView2);

        // enables full screen
        getPhoto();

        final Button button1 = findViewById(R.id.share_button);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sharePhoto();
                image.button_clicked = false;
            }
        });
        final Button button2 = findViewById(R.id.again);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                image.button_clicked = false;
                Intent myIntent = new Intent(PhotoHolder.this, MainActivity.class);
                PhotoHolder.this.startActivity(myIntent);
            }
        });
    }
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
    public boolean onCreateOptionsMenu(Menu menu) {

        Log.i(TAG, "called onCreateOptionsMenu");

        mButtonClose = menu.add("Close app");
        getMenuInflater();
        // Inflate the menu items for use in the action bar
        return super.onCreateOptionsMenu(menu);


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mButtonClose)
            System.exit(1);

        return true;
    }
    public void getPhoto(){
        imgTakenPic.setImageURI(Uri.parse(file_uri));
    }
    public void sharePhoto(){
        if(Build.VERSION.SDK_INT>=24){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Would you like to share the photo?")

                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("image/*");
                        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(file_uri)));
                        startActivity(Intent.createChooser(share,"Share via"));

                    }
                }).setNegativeButton("No", null).show();
    }
}
