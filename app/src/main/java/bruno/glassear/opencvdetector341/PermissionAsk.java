package bruno.glassear.opencvdetector341;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.marcoscg.easypermissions.EasyPermissions;

import java.io.File;

public class PermissionAsk extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = {EasyPermissions.WRITE_EXTERNAL_STORAGE,EasyPermissions.CAMERA};
        int requestCode = 0;
        EasyPermissions.requestPermissions(PermissionAsk.this, permissions, requestCode);

        File folder = new File(Environment.getExternalStorageDirectory() + "/Images/");
        if(folder.exists()){
            Toast.makeText(PermissionAsk.this, "Creating folder", Toast.LENGTH_SHORT).show();
        }else{
            folder.mkdir();
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int grantResult = grantResults[i];
            if (permission.equals(EasyPermissions.WRITE_EXTERNAL_STORAGE)) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Intent myIntent = new Intent(PermissionAsk.this, MainActivity.class);
                    startActivity(myIntent);
                }
            }
        }
    }
}