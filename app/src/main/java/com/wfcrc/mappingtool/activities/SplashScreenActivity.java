/*
 * Copyright (C) 2011 University of Washington
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

package com.wfcrc.mappingtool.activities;

import mappingtool.wfcrc.com.coralreefmappingtool.R;
import com.wfcrc.mappingtool.application.Collect;
import com.wfcrc.mappingtool.database.ActivityLogger;
import com.wfcrc.mappingtool.logic.FormController;
import com.wfcrc.mappingtool.logic.PropertyManager;
import com.wfcrc.mappingtool.preferences.PreferencesActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplashScreenActivity extends Activity {

    private static final int mSplashTimeout = 2000; // milliseconds
    private static final boolean EXIT = true;

    private int mImageMaxWidth;
    private AlertDialog mAlertDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //checkPermissions
        checkPermissions();



    }

    private void continueLauchingafterPermissions(){
        // must be at the beginning of any activity that can be called from an external intent
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        mImageMaxWidth = getWindowManager().getDefaultDisplay().getWidth();

        // this splash screen should be a blank slate
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.splash_screen);

        // get the shared preferences object
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = mSharedPreferences.edit();

        // get the package info object with version number
        PackageInfo packageInfo = null;
        try {
            packageInfo =
                    getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        boolean firstRun = mSharedPreferences.getBoolean(PreferencesActivity.KEY_FIRST_RUN, true);
        boolean showSplash =
                mSharedPreferences.getBoolean(PreferencesActivity.KEY_SHOW_SPLASH, false);
        String splashPath =
                mSharedPreferences.getString(PreferencesActivity.KEY_SPLASH_PATH,
                        getString(R.string.default_splash_path));

        // if you've increased version code, then update the version number and set firstRun to true
        if (mSharedPreferences.getLong(PreferencesActivity.KEY_LAST_VERSION, 0) < packageInfo.versionCode) {
            editor.putLong(PreferencesActivity.KEY_LAST_VERSION, packageInfo.versionCode);
            editor.commit();

            firstRun = true;
        }

        // do all the first run things
        if (firstRun || showSplash) {
            editor.putBoolean(PreferencesActivity.KEY_FIRST_RUN, false);
            editor.commit();
            startSplashScreen(splashPath);
        } else {
            endSplashScreen();
        }
        //Collect.getInstance().getActivityLogger().logOnStart(this);
    }


    private void endSplashScreen() {

        // launch new activity and close splash screen
        startActivity(new Intent(SplashScreenActivity.this, MainMenuActivity.class));
        finish();
    }


    // decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f) {
        Bitmap b = null;
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            FileInputStream fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            try {
                fis.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            int scale = 1;
            if (o.outHeight > mImageMaxWidth || o.outWidth > mImageMaxWidth) {
                scale =
                    (int) Math.pow(
                        2,
                        (int) Math.round(Math.log(mImageMaxWidth
                                / (double) Math.max(o.outHeight, o.outWidth))
                                / Math.log(0.5)));
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis, null, o2);
            try {
                fis.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
        }
        return b;
    }


    private void startSplashScreen(String path) {

        // add items to the splash screen here. makes things less distracting.
        ImageView iv = (ImageView) findViewById(R.id.splash);
        LinearLayout ll = (LinearLayout) findViewById(R.id.splash_default);

        File f = new File(path);
        if (f.exists()) {
            iv.setImageBitmap(decodeFile(f));
            ll.setVisibility(View.GONE);
            iv.setVisibility(View.VISIBLE);
        }

        // create a thread that counts up to the timeout
        Thread t = new Thread() {
            int count = 0;


            @Override
            public void run() {
                try {
                    super.run();
                    while (count < mSplashTimeout) {
                        sleep(100);
                        count += 100;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endSplashScreen();
                }
            }
        };
        t.start();
    }


    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
	    Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                	    Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "OK");
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }

    @Override
    protected void onStart() {
    	super.onStart();
		//Collect.getInstance().getActivityLogger().logOnStart(this);
    }

    @Override
    protected void onStop() {
		Collect.getInstance().getActivityLogger().logOnStop(this);
    	super.onStop();
    }

    /**
     *
     * Created by maria on 7/28/17
     */
    private int mHasWriteExternalStoragePermission;
    private int mHasReadExternalStoragePermission;
    private int mHasFineLocationPermission;
    private int mHasCoarseLocationPermission;
    private int mHasPhoneStatePermission;
    private static final int WRITE_STORAGE_PERMISSION = 0;
    private static final int READ_STORAGE_PERMISSION = 1;
    private static final int FINE_LOCATION_PERMISSION = 2;
    private static final int COARSE_LOCATION_PERMISSION = 3;
    private static final int PHONE_PERMISSION = 4;
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    /**
     *
     * Created by maria on 7/28/17
     */
    public void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<String>();
        mHasWriteExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        mHasReadExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        mHasFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        mHasCoarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        mHasPhoneStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        if (mHasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(mHasReadExternalStoragePermission!= PackageManager.PERMISSION_GRANTED){
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(mHasFineLocationPermission!= PackageManager.PERMISSION_GRANTED){
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(mHasCoarseLocationPermission!= PackageManager.PERMISSION_GRANTED){
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(mHasPhoneStatePermission != PackageManager.PERMISSION_GRANTED){
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(!permissionsNeeded.isEmpty()){
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[permissionsNeeded.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }else{
            continueLauchingafterPermissions();
        }
    }

    /**
     *
     * Created by maria on 7/28/17
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for permissions
                if (perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    continueLauchingafterPermissions();
                }else {
                    // Permission Denied
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}
