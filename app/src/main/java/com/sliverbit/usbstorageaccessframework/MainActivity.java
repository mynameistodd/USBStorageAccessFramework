package com.sliverbit.usbstorageaccessframework;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static android.provider.DocumentsContract.EXTRA_INITIAL_URI;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String[] MIME_TYPE = {"text/plain"};
    private static final String PREF_URI = "pref_uri";
    private static final Uri URI_DIRECTORY = Uri.parse("content://com.android.externalstorage.documents");

    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;

    private TextView uriTextView;
    private TextView availableTextView;
    private TextView fileContentsTextView;
    private Button buttonSave;
    private Button buttonFind;
    private Button buttonRead;

    private SharedPreferences prefs;

//    Info
//    https://commonsware.com/blog/2017/11/15/storage-situation-removable-storage.html

//    Overview
//    https://developer.android.com/guide/topics/providers/document-provider

//    Reading
//    https://developer.android.com/reference/android/content/Intent.html#ACTION_OPEN_DOCUMENT
//    1. listen for usb plug in / unplug
//    2. Use saved URI (if available) to read file
//    3. No saved URI? - Send intent to launch system picker (document navigator)
//    4. Specify EXTRA_INITIAL_URI to set document navigator location
//    5. Specify takePersistableUriPermission(Uri, int) to keep access across reboots.
//    6. Set multiple MIME types with setType(*/*) and EXTRA_MIME_TYPES (consider using a custom MIME type (text/custom), if allowed).

//    Writing
//    https://developer.android.com/reference/android/content/Intent.html#ACTION_CREATE_DOCUMENT
//    1. Use saved URI (if available) to write file
//    3. No saved URI? - Send intent to launch system picker (document navigator)
//    4. Specify EXTRA_INITIAL_URI to set document navigator location
//    5. Specify takePersistableUriPermission(Uri, int) to keep access across reboots.
//    6. Set multiple MIME types with setType(*/*) and EXTRA_MIME_TYPES (consider using a custom MIME type (text/custom), if allowed).


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uriTextView = findViewById(R.id.uriTextView);
        availableTextView = findViewById(R.id.availableTextView);
        fileContentsTextView = findViewById(R.id.fileContentsTextView);
        buttonSave = findViewById(R.id.button_save);
        buttonFind = findViewById(R.id.button_find);
        buttonRead = findViewById(R.id.button_read);

        prefs = getPreferences(MODE_PRIVATE);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFile("profile.txt");
            }
        });

        buttonFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findFile();
            }
        });

        buttonRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = getFileUri();
                if (uri != null) {
                    readFile(uri);
                } else {
                    Toast.makeText(getApplicationContext(), "Unknown File URI!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Uri uri = getFileUri();
        if (uri != null) {
            Log.i(TAG, "Uri: " + uri.toString());
            uriTextView.setText(uri.toString());

            //check if file exists
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                pfd.close();

                availableTextView.setText("True");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                availableTextView.setText("False");
            } catch (IOException e) {
                e.printStackTrace();
            }


            //try to read file
        }
    }

    private Uri getFileUri() {
        Uri fileUri = null;
        if (prefs.contains(PREF_URI)) {
            fileUri = Uri.parse(prefs.getString(PREF_URI, null));
        }
        return fileUri;
    }

//    private String getFileUriString() {
//        String fileUriString = "content://com.android.externalstorage.documents/document/";
//        Uri fileUri = getFileUri();
//        if (fileUri != null) {
//            fileUriString = fileUri.toString();
//        }
//        return fileUriString;
//    }

    private void findFile() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, MIME_TYPE);
        intent.putExtra(EXTRA_INITIAL_URI, URI_DIRECTORY);

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private void createFile(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        // Filter to only show results that can be "opened", such as
        // a file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Create a file with the requested MIME type.
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, MIME_TYPE);
        intent.putExtra(EXTRA_INITIAL_URI, URI_DIRECTORY);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    private void readFile(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
            FileInputStream fileInputStream = new FileInputStream(pfd.getFileDescriptor());
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            fileInputStream.close();
            pfd.close();

            Gson gson = new Gson();
            Profile profile = gson.fromJson(stringBuilder.toString(), Profile.class);

            fileContentsTextView.setText(stringBuilder.toString());

            Toast.makeText(this, "File Read", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Uri uri = null;
        if ((requestCode == READ_REQUEST_CODE || requestCode == WRITE_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(TAG, "Uri: " + uri.toString());

                //Save the URI to shared prefs
                prefs.edit().putString(PREF_URI, uri.toString()).apply();
            }
        }
        if (uri == null) return;

        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (requestCode == READ_REQUEST_CODE) {

            readFile(uri);

        } else if (requestCode == WRITE_REQUEST_CODE) {

            Profile profile = new Profile();
            profile.id = 1;
            profile.firstName = "Todd";
            profile.lastName = "D";
            profile.weight = 150;
            profile.age = 35;
            profile.sex = "Male";

            Gson gson = new Gson();
            String json = gson.toJson(profile);

            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                fileOutputStream.close(); //clear out contents

                fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                fileOutputStream.write(json.getBytes());
                // Let the document provider know you're done by closing the stream.
                fileOutputStream.close();
                pfd.close();

                fileContentsTextView.setText(json);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
