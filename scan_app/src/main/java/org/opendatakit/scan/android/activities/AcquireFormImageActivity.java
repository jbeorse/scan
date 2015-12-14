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
  Intent processPhoto;

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
      if (extras.containsKey("photoName")) {
        photoName = extras.getString("photoName");
        Log.d(LOG_TAG, "Photo name: " + photoName);
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
      // In the camera case, prepare the directory before launching so that there is a place
      // to save the picture
      try {
        prepareToProcessForm();
        prepareOutputDir();
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

    File destFile;
    Uri uri;

    try {
      switch (requestCode) {
      case R.integer.new_image:
        Log.d(LOG_TAG, "Returned from Camera");
        finishActivity(resultCode);

        // Verify that the new picture exists
        destFile = new File(ScanUtils.getPhotoPath(photoName));
        if (!destFile.exists()) {
          failAndReturn(this.getString(R.string.error_file_creation));
          finish();
          return;
        }

        // Process the form in the background
        Log.d(LOG_TAG, "Photographed form: " + ScanUtils.getPhotoPath(photoName));
        Toast.makeText(this, "Processing photo in background...", Toast.LENGTH_LONG).show();
        startService(processPhoto);
        break;

      case R.integer.existing_image:
        Log.d(LOG_TAG, "Returned from file picker");
        finishActivity(resultCode);

        // Verify that the new file exists and is an image
        uri = data.getData();
        Log.d(LOG_TAG, "File Uri Selected: " + uri.toString());
        File sourceFile = new File(uri.getPath());
        if (!sourceFile.exists() || !ScanUtils.imageFilter.accept(sourceFile, uri.toString())) {
          failAndReturn(this.getString(R.string.error_finding_file));
          finish();
          return;
        }

        prepareToProcessForm();
        prepareOutputDir();

        // Copy the new file into the Scan file system
        destFile = new File(ScanUtils.getPhotoPath(photoName));
        FileUtils.copyFile(sourceFile, destFile);

        Log.d(LOG_TAG, "Copied form: " + ScanUtils.getPhotoPath(photoName));
        Toast.makeText(this, "Processing photo in background...", Toast.LENGTH_LONG).show();
        startService(processPhoto);
        break;

      case R.integer.image_directory:
        Log.d(LOG_TAG, "Returned from folder picker");
        finishActivity(resultCode);

        uri = data.getData();
        Log.d(LOG_TAG, "Directory Uri Selected: " + uri.toString());

        // Validate the directory
        File dir = new File(uri.getPath());
        if (!dir.exists() || !dir.isDirectory()) {
          failAndReturn(this.getString(R.string.error_finding_dir));
          finish();
          return;
        }

        // Find the directory search preference
        SharedPreferences settings = PreferenceManager
            .getDefaultSharedPreferences(getApplicationContext());
        String dirSearch = settings
            .getString("directory_search", getString(R.string.default_directory_search));
        boolean isRecursive = dirSearch.equals("recursive");

        if (processImagesInFolder(dir, isRecursive)) {
          Toast.makeText(this, R.string.background_processing, Toast.LENGTH_LONG).show();
        } else {
          Toast.makeText(this, R.string.error_empty_folder, Toast.LENGTH_LONG).show();
        }
        break;

      default:
        failAndReturn(this.getString(R.string.error_acquire_bad_return));
        finish();
        return;
      }
    } catch (Exception e) {
      failAndReturn(e.toString());
      finish();
      return;
    }

    finish();
  }

  private void prepareOutputDir() throws Exception {
    String outputPath = ScanUtils.getOutputPath(photoName);

    //Try to create an output folder
    boolean createSuccess = new File(outputPath).mkdirs();
    if (!createSuccess) {
      throw (new Exception("Could not create output folder [" + outputPath + "].\n"
          + "There may be a problem with the device's storage."));
    }
    //Create an output directory for the segments
    boolean segDirSuccess = new File(outputPath, "segments").mkdirs();
    if (!segDirSuccess) {
      throw (new Exception("Could not create output folder for segments."));
    }
  }

  /**
   * Create the file structure for storing the scan data
   */
  private void prepareToProcessForm() throws Exception {

    if (templatePaths.length > 0) {
      String[] parts = templatePaths[0].split("/");
      String templateName = parts[parts.length - 1];
      photoName = templateName + "_" + ScanUtils.COLLECT_INSTANCE_NAME_DATE_FORMAT.format(new
          Date());
    } else {
      photoName = "taken_" + ScanUtils.COLLECT_INSTANCE_NAME_DATE_FORMAT.format(new Date());
    }

    String inputPath = ScanUtils.getPhotoPath(photoName);
    String outputPath = ScanUtils.getOutputPath(photoName);

    //This is the configuration JSON passed into ODKScan-core
    //see: scan/bubblebot_lib/jni/ODKScan-core/processViaJSON.md
    JSONObject config = new JSONObject();
    try {
      config.put("trainingDataDirectory", ScanUtils.getTrainingExampleDirPath());
      config.put("trainingModelDirectory", ScanUtils.getTrainedModelDir());
      config.put("inputImage", inputPath);
      config.put("outputDirectory", outputPath);
      config.put("templatePaths", new JSONArray(Arrays.asList(templatePaths)));
    } catch (Exception e) {
      failAndReturn(e.toString());
      finish();
      return;
    }

    // Create the intent to process the acquired image.
    processPhoto = new Intent(this, ProcessFormsService.class);
    processPhoto.putExtra("photoName", photoName);
    processPhoto.putExtra("config", config.toString());
  }

  /**
   * Recursively process all images in a folder and then search subfolders
   *
   * @param dir         The folder to read from
   * @param isRecursive Whether to recursively search sub folders
   */
  private boolean processImagesInFolder(File dir, boolean isRecursive) {
    if (!dir.exists() || !dir.isDirectory()) {
      return false;
    }

    boolean foundForm = false;

    // Process all the images in the folder
    for (File curr : dir.listFiles(ScanUtils.imageFilter)) {
      try {
        prepareToProcessForm();
        prepareOutputDir();

        // Copy the new file into the Scan file system
        File destFile = new File(ScanUtils.getPhotoPath(photoName));
        FileUtils.copyFile(curr, destFile);

        Log.d(LOG_TAG, "Acquired form: " + ScanUtils.getPhotoPath(photoName));
        startService(processPhoto);

        foundForm = true;

      } catch (Exception e) {
        Log.e(LOG_TAG, "Error processing image: " + curr.getPath());
        Log.e(LOG_TAG, e.toString());
        continue;
      }
    }

    // If we are not recursing, we are done
    if (!isRecursive) {
      return foundForm;
    }

    // Recurse through each subdirectory
    for (File currDir : dir.listFiles(ScanUtils.subdirFilter)) {
      foundForm |= processImagesInFolder(currDir, isRecursive);
    }

    return foundForm;
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