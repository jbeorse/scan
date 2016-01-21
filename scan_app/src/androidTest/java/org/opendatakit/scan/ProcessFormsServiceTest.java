/*
 * Copyright (C) 2015 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.scan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.test.ServiceTestCase;
import android.util.Log;
import android.widget.Toast;
import org.junit.*;

import org.junit.runner.RunWith;

import android.test.suitebuilder.annotation.LargeTest;
import org.opendatakit.scan.services.ProcessFormsService;
import org.opendatakit.scan.utils.ScanUtils;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class ProcessFormsServiceTest extends ServiceTestCase<ProcessFormsService> {
  public static String LOGTAG = "ODK Scan Process Forms Service Test";

  private static final String MNH_TEMPLATE_PATH =
      "/sdcard/opendatakit/tables/config/scan/form_templates/ordMNH";
  private static final String IMAGE_FOLDER_PATH = "file:///sdcard/TestData5";
  private static final String IMAGE_PATH = "file:///sdcard/TestData5/11_photo.jpg";

  private CountDownLatch latch;

  public ProcessFormsServiceTest() {
    super(ProcessFormsService.class);
  }

  public ProcessFormsServiceTest(Class<ProcessFormsService> serviceClass) {
    super(serviceClass);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setupService();

    /*
    latch = new CountDownLatch(6);
    getService().setFinishedProcessingLatch(latch);
    */
  }

  public void testProcessFlatFolder() {
    /*
    String[] templatePaths = { MNH_TEMPLATE_PATH };

    latch = new CountDownLatch(1);

    Intent processPhoto0 = new Intent(getSystemContext(), ProcessFormsService.class);
    processPhoto0.putExtra("templatePaths", templatePaths);
    processPhoto0.putExtra("opCode", R.integer.existing_image);
    processPhoto0.putExtra("uri", IMAGE_PATH);
    processPhoto0.putExtra("id", "photo0");
    startService(processPhoto0);


    Intent processPhoto1 = new Intent(getSystemContext(), ProcessFormsService.class);
    processPhoto1.putExtra("templatePaths", templatePaths);
    processPhoto1.putExtra("opCode", R.integer.existing_image);
    processPhoto1.putExtra("uri", IMAGE_PATH);
    processPhoto1.putExtra("id", "photo1");
    startService(processPhoto1);

    Intent processPhoto2 = new Intent(getSystemContext(), ProcessFormsService.class);
    processPhoto2.putExtra("templatePaths", templatePaths);
    processPhoto2.putExtra("opCode", R.integer.existing_image);
    processPhoto2.putExtra("uri", IMAGE_PATH);
    processPhoto2.putExtra("id", "photo2");
    startService(processPhoto2);

    Intent processPhoto3 = new Intent(getSystemContext(), ProcessFormsService.class);
    processPhoto3.putExtra("templatePaths", templatePaths);
    processPhoto3.putExtra("opCode", R.integer.existing_image);
    processPhoto3.putExtra("uri", IMAGE_PATH);
    processPhoto3.putExtra("id", "photo3");
    startService(processPhoto3);

    Intent processPhoto4 = new Intent(getSystemContext(), ProcessFormsService.class);
    processPhoto4.putExtra("templatePaths", templatePaths);
    processPhoto4.putExtra("opCode", R.integer.existing_image);
    processPhoto4.putExtra("uri", IMAGE_PATH);
    processPhoto4.putExtra("id", "photo4");
    startService(processPhoto4);

    Intent processFolder0 = new Intent(getSystemContext(), ProcessFormsService.class);
    processFolder0.putExtra("templatePaths", templatePaths);
    processFolder0.putExtra("opCode", R.integer.image_directory);
    processFolder0.putExtra("uri", IMAGE_FOLDER_PATH);
    processFolder0.putExtra("isRecursive", false);
    processFolder0.putExtra("id", "folder0");

    startService(processFolder0);

    try {
      latch.await();
    } catch(Exception e) {
      fail("Service failed to shutdown correctly");
    }
    */

  }

}