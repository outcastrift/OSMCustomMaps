package osm.custommaps.create;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.UUID;

import osm.custommaps.MapEditor;

/**
 * Created by dmumpton on 8/3/2015.
 */
public class CameraActivity extends Activity {
    public final String APP_TAG = "customMaps";
    public String photoFileName;// = "photo.jpg";

    private static final int CAMERA = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        photoFileName = UUID.randomUUID().toString() + ".jpg";
        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cam.putExtra(MediaStore.EXTRA_OUTPUT, getPhotoFileUri(photoFileName));
        if (cam.resolveActivity(getPackageManager()) != null)
            startActivityForResult(cam, CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri takenPhotoUri = getPhotoFileUri(photoFileName);
        String mapFilename = takenPhotoUri.getPath();

        if (resultCode == -1) {
            Intent createMap = new Intent(this, MapEditor.class);
            createMap.putExtra("osm.custommaps.BitmapFile", mapFilename);
            createMap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(createMap);
        } else
            finish();

    }

    public Uri getPhotoFileUri(String fileName) {
        // Only continue if the SD Card is mounted
        if (isExternalStorageAvailable()) {
            // Get safe storage directory for photos
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), APP_TAG);
            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                Log.d(APP_TAG, "failed to create directory");
            }
            // Return the file target for the photo based on filename
            return Uri.fromFile(new File(mediaStorageDir.getPath() + File.separator + fileName));
        }
        return null;
    }

    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }
}