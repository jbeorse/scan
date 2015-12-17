package org.opendatakit.scan.android.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.app.Activity;

import android.widget.Toast;
import org.apache.commons.io.FileUtils;
import android.preference.MultiSelectListPreference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.common.android.activities.BaseActivity;
import org.opendatakit.scan.android.R;
import org.opendatakit.scan.android.application.Scan;
import org.opendatakit.scan.android.services.ProcessFormsService;
import org.opendatakit.scan.android.utils.ScanUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

/**
 * AcquireFormImageActivity launches the Android camera app or a file picker app to capture a form
 * image. It also creates a directory for data about the form to be stored.
 **/
public class AcquireFormImageActivity extends BaseActivity {
  private static final String LOG_TAG = "ODKScan";

  String photoName;
  String[] templatePaths;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Default to taking pictures to acquire form images
    int acquisitionCode = R.integer.take_picture;
    photoName = null;
    templatePaths = null;

    try {
      Bundle extras = getIntent().getExtras();
      if (extras == null) {
        extras = new Bundle();
        Log.i("SCAN", "No bundle");
      }
      if (extras.containsKey("acquisitionMethod")) {
        acquisitionCode = extras.getInt("acquisitionMethod");
        Log.d(LOG_TAG, "Acquisition code: " + acquisitionCode);
      }
      if (extras.containsKey("templatePaths")) {
        templatePaths = extras.getStringArray("templatePaths");
      } else {
        SharedPreferences settings = PreferenceManager
            .getDefaultSharedPreferences(getApplicationContext());
        Log.d(LOG_TAG, "No template path passed");

        Set<String> selectedTemplates = settings.getStringSet("select_templates", null);
        if (selectedTemplates == null || selectedTemplates.isEmpty()) {
          throw new Exception("No templates selected");
        }

        templatePaths = selectedTemplates.toArray(new String[selectedTemplates.size()]);
        extras.putStringArray("templatePaths", templatePaths);
      }
      if (extras.containsKey("photoName")) {
        photoName = extras.getString("photoName");
        Log.d(LOG_TAG, "Photo name: " + photoName);
      }

      launchAcquireIntent(acquisitionCode);
    } catch (Exception e) {
      //Display an error dialog if something goes wrong.
      failAndReturn(e.toString());
      finish();
      return;
    }
  }

  /**
   * Launch an app to acquire an image of a form to process
   *
   * @param acquisitionCode the method for acquiring the image (e.g. camera, file picker)
   */
  private void launchAcquireIntent(int acquisitionCode) {
    Intent acquireIntent;
    File root;
    Uri uri;

    switch (acquisitionCode) {
    default:
      // Default to capturing a new image from the camera, but log this as an error
      Log.e(LOG_TAG, "Error: Invalid Acquisition Code. Defaulting to camera capture.");
    case R.integer.take_picture:
      photoName = ScanUtils.setPhotoName(templatePaths);

      // In the camera case, prepare the directory before launching so that there is a place
      // to save the picture
      try {
        ScanUtils.prepareOutputDir(photoName);
      } catch (Exception e) {
        failAndReturn(e.toString());
        finish();
        return;
      }

      // Store the new image here
      Uri imageUri = Uri.fromFile(new File(ScanUtils.getPhotoPath(photoName)));

      // Create the intent
      acquireIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
      acquireIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
      acquireIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

      // Check that there exists an app that can handle this intent
      if (acquireIntent.resolveActivity(getPackageManager()) == null) {
        failAndReturn(this.getString(R.string.error_no_camera));
        finish();
        return;
      }

      Log.d(LOG_TAG, "Taking picture");
      startActivityForResult(acquireIntent, R.integer.new_image);
      break;

    case R.integer.pick_file:
      // Initialize at the root of the SD Card
      root = new File(android.os.Environment.getExternalStorageDirectory().getPath());
      uri = Uri.fromFile(root);

      // Create the intent
      acquireIntent = new Intent("org.openintents.action.PICK_FILE");
      acquireIntent.setData(uri);
      acquireIntent.putExtra("org.openintents.extra.TITLE", R.string.select_image_title);
      acquireIntent.putExtra("org.openintents.extra.BUTTON_TEXT", R.string.select_image_button);

      // Check that there exists an app that can handle this intent
      if (acquireIntent.resolveActivity(getPackageManager()) == null) {
        failAndReturn(this.getString(R.string.error_no_file_picker));
        finish();
        return;
      }

      Log.d(LOG_TAG, "Picking file");
      startActivityForResult(acquireIntent, R.integer.existing_image);
      break;

    case R.integer.pick_directory:
      // Initialize at the root of the SD Card
      root = new File(android.os.Environment.getExternalStorageDirectory().getPath());
      uri = Uri.fromFile(root);

      // Create the intent
      acquireIntent = new Intent("org.openintents.action.PICK_DIRECTORY");
      acquireIntent.setData(uri);

      // Check that there exists an app that can handle this intent
      if (acquireIntent.resolveActivity(getPackageManager()) == null) {
        failAndReturn(this.getString(R.string.error_no_folder_picker));
        finish();
        return;
      }

      Log.d(LOG_TAG, "Picking folder");
      startActivityForResult(acquireIntent, R.integer.image_directory);
      break;
    }
  }

  /**
   * Display an error message to the user and return to the main menu
   *
   * @param message Error message
   */
  private void failAndReturn(String message) {
    Log.d(LOG_TAG, "Failed to acquire image: " + message);

    // Show failed intent message
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(message);
    builder.setCancelable(false).setNeutralButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
        setResult(RESULT_CANCELED);
        finish();
      }
    });
    builder.show();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Log.d(LOG_TAG, "AcquireFormImage onActivityResult " + requestCode);

    if (resultCode == Activity.RESULT_FIRST_USER) {
      Log.d(LOG_TAG, "First User");
      finish();
      return;
    } else if (resultCode == Activity.RESULT_CANCELED) {
      Log.d(LOG_TAG, "Canceled");
      finish();
      return;
    } else if (resultCode != RESULT_OK) {
      failAndReturn(this.getString(R.string.error_acquire_bad_return));
      finish();
      return;
    }

    finishActivity(resultCode);

    // Grap the URI, if it exists
    Uri uri = data.getData();

    // Find the directory search preference
    SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(getApplicationContext());
    String dirSearch = settings
        .getString("directory_search", getString(R.string.default_directory_search));
    boolean isRecursive = dirSearch.equals("recursive");

    Intent processPhoto = new Intent(this, ProcessFormsService.class);
    processPhoto.putExtra("templatePaths", templatePaths);
    processPhoto.putExtra("opCode", requestCode);
    if (uri != null) {
      processPhoto.putExtra("uri", uri.toString());
    }
    processPhoto.putExtra("isRecursive", isRecursive);
    if (requestCode == R.integer.new_image) {
      processPhoto.putExtra("photoName", photoName);
    }

    // Process the form in the background
    Log.d(LOG_TAG,
        String.format(this.getString(R.string.captured_form), ScanUtils.getPhotoPath(photoName)));
    Toast.makeText(this, R.string.processing_in_background, Toast.LENGTH_LONG).show();

    startService(processPhoto);

    finish();
  }

  @Override
  protected void onDestroy() {
    //Try to remove the forms directory if the photo couldn't be captured:
    //Note: this won't delete the folder if it has any files in it.
    File capturedImage = new File(ScanUtils.getPhotoPath(photoName));
    if (!capturedImage.exists()) {
      new File(ScanUtils.getOutputPath(photoName) + "/segments").delete();
      new File(ScanUtils.getOutputPath(photoName)).delete();
    }
    super.onDestroy();
  }

  public void databaseAvailable() {
    // TODO Auto-generated method stub

  }

  public void databaseUnavailable() {
    // TODO Auto-generated method stub

  }

  public String getAppName() {
    return ScanUtils.getODKAppName();
  }
}