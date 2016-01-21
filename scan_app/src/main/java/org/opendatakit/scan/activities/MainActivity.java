/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.scan.activities;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import android.app.*;
import org.opendatakit.common.android.activities.BaseActivity;
import org.opendatakit.common.android.activities.IInitResumeActivity;
import org.opendatakit.common.android.fragment.AboutMenuFragment;
import org.opendatakit.common.android.listener.DatabaseConnectionListener;
import org.opendatakit.common.android.logic.PropertiesSingleton;
import org.opendatakit.common.android.utilities.DependencyChecker;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebLogger;

import android.app.FragmentManager.BackStackEntry;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import org.opendatakit.scan.R;
import org.opendatakit.scan.application.Scan;
import org.opendatakit.scan.fragments.InitializationFragment;
import org.opendatakit.scan.fragments.InstructionsFragment;
import org.opendatakit.scan.fragments.MainMenuFragment;
import org.opendatakit.scan.fragments.ScanPreferencesFragment;
import org.opendatakit.scan.services.ProcessFormsService;
import org.opendatakit.scan.utils.ScanUtils;

public class MainActivity extends BaseActivity
    implements DatabaseConnectionListener, IInitResumeActivity {

  private static final String TAG = "ODKScan MainActivity";
  private static final String CURRENT_FRAGMENT = "currentFragment";

  public enum ScreenType {
    INITIALIZATION_SCREEN,
    MAIN_MENU_SCREEN,
    ABOUT_SCREEN,
    SETTINGS_SCREEN,
    INSTRUCTIONS_SCREEN
  }

  ;

  /**
   * The active screen -- retained state
   */
  ScreenType activeScreenType = ScreenType.MAIN_MENU_SCREEN;

  /**
   * used to determine whether we need to change the menu (action bar)
   * because of a change in the active fragment.
   */
  private ScreenType lastMenuType = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.activity_main_activity);

    DependencyChecker dc = new DependencyChecker(this);
    boolean dependable = dc.checkDependencies();
    if (!dependable) { // dependencies missing
      return;
    }

    if (savedInstanceState != null) {
      // if we are restoring, assume that initialization has already occurred.
      activeScreenType = ScreenType.valueOf(savedInstanceState.containsKey(CURRENT_FRAGMENT) ?
          savedInstanceState.getString(CURRENT_FRAGMENT) :
          activeScreenType.name());
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(CURRENT_FRAGMENT, activeScreenType.name());
  }

  @Override
  protected void onResume() {
    super.onResume();

    DependencyChecker dc = new DependencyChecker(this);
    boolean dependable = dc.checkDependencies();
    if (!dependable) { // dependencies missing
      return;
    }

    swapScreens(activeScreenType);
  }

  @Override
  public void onPostResume() {
    super.onPostResume();
    Scan.getInstance().establishDatabaseConnectionListener(this);
  }

  @Override
  public void databaseAvailable() {
    FragmentManager mgr = this.getFragmentManager();
    int idxLast = mgr.getBackStackEntryCount() - 1;
    if (idxLast >= 0) {
      BackStackEntry entry = mgr.getBackStackEntryAt(idxLast);
      Fragment newFragment = null;
      newFragment = mgr.findFragmentByTag(entry.getName());
      if (newFragment instanceof DatabaseConnectionListener) {
        ((DatabaseConnectionListener) newFragment).databaseAvailable();
      }
    }
  }

  @Override
  public void databaseUnavailable() {
    FragmentManager mgr = this.getFragmentManager();
    int idxLast = mgr.getBackStackEntryCount() - 1;
    if (idxLast >= 0) {
      BackStackEntry entry = mgr.getBackStackEntryAt(idxLast);
      Fragment newFragment = null;
      newFragment = mgr.findFragmentByTag(entry.getName());
      if (newFragment instanceof DatabaseConnectionListener) {
        ((DatabaseConnectionListener) newFragment).databaseUnavailable();
      }
    }
  }

  private void popBackStack() {
    FragmentManager mgr = getFragmentManager();
    int idxLast = mgr.getBackStackEntryCount() - 2;
    if (idxLast < 0) {
      Intent result = new Intent();
      this.setResult(RESULT_OK, result);
      finish();
    } else {
      BackStackEntry entry = mgr.getBackStackEntryAt(idxLast);
      swapScreens(ScreenType.valueOf(entry.getName()));
    }
  }

  @Override
  public void initializationCompleted() {
    popBackStack();
  }

  @Override
  public void onBackPressed() {
    popBackStack();
  }

  public ScreenType getCurrentScreenType() {
    return activeScreenType;
  }

  public void swapScreens(ScreenType newScreenType) {
    WebLogger.getLogger(getAppName()).i(TAG, "swapScreens: Transitioning from " +
        ((activeScreenType == null) ? "-none-" : activeScreenType.name()) +
        " to " + newScreenType.name());
    FragmentManager mgr = this.getFragmentManager();
    FragmentTransaction trans = null;
    Fragment newFragment = null;
    switch (newScreenType) {
    case MAIN_MENU_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if (newFragment == null) {
        newFragment = new MainMenuFragment();
      }
      break;
    case ABOUT_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if (newFragment == null) {
        newFragment = new AboutMenuFragment();
      }
      break;
    case INITIALIZATION_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if (newFragment == null) {
        newFragment = new InitializationFragment();
      }
      break;
    case SETTINGS_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if (newFragment == null) {
        newFragment = new ScanPreferencesFragment();
      }
      break;
    case INSTRUCTIONS_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if (newFragment == null) {
        newFragment = new InstructionsFragment();
      }
      break;
    default:
      throw new IllegalStateException("Unexpected default case");
    }

    boolean matchingBackStackEntry = false;
    for (int i = 0; i < mgr.getBackStackEntryCount(); ++i) {
      BackStackEntry e = mgr.getBackStackEntryAt(i);
      WebLogger.getLogger(ScanUtils.getODKAppName())
          .i(TAG, "BackStackEntry[" + i + "] " + e.getName());
      if (e.getName().equals(newScreenType.name())) {
        matchingBackStackEntry = true;
      }
    }

    if (matchingBackStackEntry) {
      if (trans != null) {
        WebLogger.getLogger(ScanUtils.getODKAppName())
            .e(TAG, "Unexpected active transaction when popping " + "state!");
        trans = null;
      }
      // flush backward, to the screen we want to go back to
      activeScreenType = newScreenType;
      mgr.popBackStackImmediate(activeScreenType.name(), 0);
    } else {
      // add transaction to show the screen we want
      if (trans == null) {
        trans = mgr.beginTransaction();
      }
      activeScreenType = newScreenType;
      trans.replace(R.id.activity_main_activity, newFragment, activeScreenType.name());
      trans.addToBackStack(activeScreenType.name());
    }

    // and see if we should re-initialize...
    if ((activeScreenType != ScreenType.INITIALIZATION_SCREEN) && Scan.getInstance()
        .shouldRunInitializationTask(getAppName())) {
      WebLogger.getLogger(getAppName())
          .i(TAG, "swapToFragmentView -- calling clearRunInitializationTask");
      // and immediately clear the should-run flag...
      Scan.getInstance().clearRunInitializationTask(getAppName());
      // OK we should swap to the InitializationFragment view
      // this will skip the transition to whatever screen we were trying to
      // go to and will instead show the InitializationFragment view. We
      // restore to the desired screen via the setFragmentToShowNext()
      //
      // NOTE: this discards the uncommitted transaction.
      if (trans != null) {
        trans.commit();
      }
      swapScreens(ScreenType.INITIALIZATION_SCREEN);
    } else {
      if (trans != null) {
        trans.commit();
      }
      invalidateOptionsMenu();
    }
  }

  private void changeOptionsMenu(Menu menu) {
    MenuInflater menuInflater = this.getMenuInflater();

    if (activeScreenType == ScreenType.MAIN_MENU_SCREEN) {
      menuInflater.inflate(R.menu.scan_manager, menu);
    }
    lastMenuType = activeScreenType;

    ActionBar actionBar = getActionBar();
    actionBar.show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    changeOptionsMenu(menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    if (lastMenuType != activeScreenType) {
      changeOptionsMenu(menu);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    String appName = getAppName();
    WebLogger.getLogger(appName).d(TAG, "[onOptionsItemSelected] selecting an item");

    switch (item.getItemId()) {
    case R.id.menu_scan_about:
      swapScreens(ScreenType.ABOUT_SCREEN);
      return true;
    case R.id.menu_scan_instructions:
      swapScreens(ScreenType.INSTRUCTIONS_SCREEN);
      return true;
    case R.id.menu_scan_preferences:
      swapScreens(ScreenType.SETTINGS_SCREEN);
      return true;
    case R.id.processImage:
      intent = new Intent(getApplication(), AcquireFormImageActivity.class);
      intent.putExtra("acquisitionMethod", R.integer.pick_file);
      intent.putExtra("intentRequestCode", R.integer.scan_main_menu);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      return true;
    case R.id.processFolder:
      intent = new Intent(getApplication(), AcquireFormImageActivity.class);
      intent.putExtra("acquisitionMethod", R.integer.pick_directory);
      intent.putExtra("intentRequestCode", R.integer.scan_main_menu);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public String getAppName() {
    return ScanUtils.getODKAppName();
  }

  public void testProcessFormsService() {
    final String MNH_TEMPLATE_PATH =
        "/sdcard/opendatakit/tables/config/scan/form_templates/ordMNH";
    final String IMAGE_FOLDER_PATH = "file:///sdcard/TestData5";
    final String IMAGE_PATH = "file:///sdcard/TestData5/11_photo.jpg";

    String[] templatePaths = { MNH_TEMPLATE_PATH };

    Intent processPhoto0 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto0.putExtra("templatePaths", templatePaths);
    processPhoto0.putExtra("opCode", R.integer.existing_image);
    processPhoto0.putExtra("uri", IMAGE_PATH);
    startService(processPhoto0);

    Intent processPhoto1 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto1.putExtra("templatePaths", templatePaths);
    processPhoto1.putExtra("opCode", R.integer.existing_image);
    processPhoto1.putExtra("uri", IMAGE_PATH);
    startService(processPhoto1);

    Intent processPhoto2 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto2.putExtra("templatePaths", templatePaths);
    processPhoto2.putExtra("opCode", R.integer.existing_image);
    processPhoto2.putExtra("uri", IMAGE_PATH);
    startService(processPhoto2);

    Intent processPhoto3 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto3.putExtra("templatePaths", templatePaths);
    processPhoto3.putExtra("opCode", R.integer.existing_image);
    processPhoto3.putExtra("uri", IMAGE_PATH);
    startService(processPhoto3);

    Intent processPhoto4 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto4.putExtra("templatePaths", templatePaths);
    processPhoto4.putExtra("opCode", R.integer.existing_image);
    processPhoto4.putExtra("uri", IMAGE_PATH);
    startService(processPhoto4);

    Intent processPhoto5 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto5.putExtra("templatePaths", templatePaths);
    processPhoto5.putExtra("opCode", R.integer.existing_image);
    processPhoto5.putExtra("uri", IMAGE_PATH);
    startService(processPhoto5);

    Intent processPhoto6 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto6.putExtra("templatePaths", templatePaths);
    processPhoto6.putExtra("opCode", R.integer.existing_image);
    processPhoto6.putExtra("uri", IMAGE_PATH);
    startService(processPhoto6);

    Intent processPhoto7 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto7.putExtra("templatePaths", templatePaths);
    processPhoto7.putExtra("opCode", R.integer.existing_image);
    processPhoto7.putExtra("uri", IMAGE_PATH);
    startService(processPhoto7);

    Intent processPhoto8 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto8.putExtra("templatePaths", templatePaths);
    processPhoto8.putExtra("opCode", R.integer.existing_image);
    processPhoto8.putExtra("uri", IMAGE_PATH);
    startService(processPhoto8);

    Intent processPhoto9 = new Intent(getApplication(), ProcessFormsService.class);
    processPhoto9.putExtra("templatePaths", templatePaths);
    processPhoto9.putExtra("opCode", R.integer.existing_image);
    processPhoto9.putExtra("uri", IMAGE_PATH);
    startService(processPhoto9);

    /*

    Intent processFolder0 = new Intent(getApplication(), ProcessFormsService.class);
    processFolder0.putExtra("templatePaths", templatePaths);
    processFolder0.putExtra("opCode", R.integer.image_directory);
    processFolder0.putExtra("uri", IMAGE_FOLDER_PATH);
    processFolder0.putExtra("isRecursive", false);
    startService(processFolder0);
    */


  }

}