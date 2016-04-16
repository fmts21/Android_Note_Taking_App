package com.michael.sknotes;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.Address;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class EditorActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private String action;
    private String noteFilter;
    private String oldText;
    private Double latitude;
    private Double longitude;
    private String audio;

    private EditText editor;
    private CoordinatorLayout coordinatorLayout;

    // Map
    public static final int ERROR_DIALOG_REQUEST = 9001;
    private GoogleMap googleMap;
    private GoogleApiClient mLocationClient;

    private Uri uri;
    private Marker marker;

    // Image
    private LinearLayout linearLayout;
    private Uri imageUri;
    public static final int CAMERA_REQUEST_CODE = 100;
    private static final String IMAGE_DIRECTORY_NAME = "com.michael.sknotes.EditorActivity";
    private ImageView imageView;
    private List<String> imageArray = new ArrayList<>();
    private byte[] byteArray;
    private String imageString;

    // Sound
    Button record, stopRecording, play, stopPlaying;
    private MediaRecorder audioRecorder;
    MediaPlayer mediaPlayer = new MediaPlayer();
    public static String audioFileName;

    private ProgressBar mPBar;

    //
    private int numberOfImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        editor = (EditText) findViewById(R.id.edit);

        record = (Button) findViewById(R.id.record);
        stopRecording = (Button) findViewById(R.id.stop);
        play = (Button) findViewById(R.id.play);
        stopPlaying = (Button) findViewById(R.id.stop_playback);

        mPBar = (ProgressBar) findViewById(R.id.progressBar);

        Intent intent = this.getIntent();
        uri = intent.getParcelableExtra(NoteProvider.CONTENT_ITEM_TYPE);

        if (uri == null) {
            action = Intent.ACTION_INSERT;
            setTitle("New Note");
            audioFileName = "";
        } else {
            action = Intent.ACTION_EDIT;
            noteFilter = DBOpenHelper.NOTE_ID + "=" + uri.getLastPathSegment();
            Cursor cursor = getContentResolver().query(uri, DBOpenHelper.ALL_COlUMNS, noteFilter, null, null);
            cursor.moveToFirst();
            oldText = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_TEXT));
            latitude = cursor.getDouble(cursor.getColumnIndex(DBOpenHelper.LATITUDE));
            longitude = cursor.getDouble(cursor.getColumnIndex(DBOpenHelper.LONGITUDE));
            imageArray.clear();
            if (cursor.getString(cursor.getColumnIndex(DBOpenHelper.IMAGE)) != null) {
                imageString = cursor.getString(cursor.getColumnIndex(DBOpenHelper.IMAGE));
                imageArray = new ArrayList(Arrays.asList(imageString.split(",")));
                    for (int i = 0; i < imageArray.size(); i++) {
                        controlImageView(i);
                    }
            }
//            numberOfImages = imageArray.size();
            audioFileName = cursor.getString(cursor.getColumnIndex(DBOpenHelper.AUDIO));
            if (audioFileName != null && !audioFileName.isEmpty()) {
                play.setEnabled(true);
                audio = audioFileName;
            }
            editor.setText(oldText);
            editor.requestFocus();
        }

        if (serviceok()) {
            if (inimap()) {

                mLocationClient = new GoogleApiClient.Builder(this)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();

                mLocationClient.connect();

            }
        }

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File path = new File(getFilesDir(), "audiofile");
                if (!path.exists()) {
                    path.mkdir();
                }

                audioFileName = dateString() + ".mp4";

                final File audioFile = new File(path, audioFileName);

                audioRecorder = new MediaRecorder();
                audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                audioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.DEFAULT);
                audioRecorder.setOutputFile(audioFile.getPath());

                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                try {
                    audioRecorder.prepare();
                    audioRecorder.start();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                stopRecording.setVisibility(View.VISIBLE);

                Snackbar.make(coordinatorLayout, "Recording Started", Snackbar.LENGTH_SHORT).show();
            }
        });

        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioRecorder.stop();
                audioRecorder.release();
                audioRecorder = null;

                stopRecording.setVisibility(View.GONE);
                play.setEnabled(true);

                Snackbar.make(coordinatorLayout, "Recording successed", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private boolean inimap() {
        if (googleMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            googleMap = mapFragment.getMap();
        }
        return (googleMap != null);
    }

    private void gotoLocation(double lat, double lng, float zoomFactor) throws IOException {
        LatLng latLng = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoomFactor);
        googleMap.moveCamera(update);

        Geocoder gc = new Geocoder(this);
        List<Address> list = gc.getFromLocation(latLng.latitude, latLng.longitude, 1);

        if (list.size() > 0) {
            Address address = list.get(0);
            addMarker(address, latLng.latitude, latLng.longitude);
        }
    }

    private void showCurrentLocation() throws IOException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location currentlocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);

        if (currentlocation == null) {
            Snackbar.make(coordinatorLayout, "Couldn't connect", Snackbar.LENGTH_SHORT).show();
        } else {
            LatLng latLng = new LatLng(currentlocation.getLatitude(), currentlocation.getLongitude());
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 15);
            googleMap.animateCamera(update);

            Geocoder gc = new Geocoder(this);
            List<Address> list = gc.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (list.size() > 0) {
                Address address = list.get(0);
                addMarker(address, latLng.latitude, latLng.longitude);
            }

            latitude = latLng.latitude;
            longitude = latLng.longitude;
        }

    }

    private void addMarker(Address address, double lat, double lng) {
        MarkerOptions options = new MarkerOptions()
                .title(address.getLocality())
                .position(new LatLng(lat, lng))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        String country = address.getCountryCode();
        if (country.length() > 0) {
            options.snippet(country);
        }

        if (marker == null) {
            marker = googleMap.addMarker(options);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finishEditing();
                break;
            case R.id.add_picture:
                takePicture();
                break;
            case R.id.action_delete:
                DialogInterface.OnClickListener dialogClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int button) {
                                if (button == DialogInterface.BUTTON_POSITIVE) {
                                    deleteNote();
                                }
                            }
                        };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.are_you_sure))
                        .setPositiveButton(getString(android.R.string.yes), dialogClickListener)
                        .setNegativeButton(getString(android.R.string.no), dialogClickListener)
                        .show();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteNote() {
        getContentResolver().delete(NoteProvider.CONTENT_URI, noteFilter, null);
        Snackbar.make(coordinatorLayout, R.string.note_deleted, Snackbar.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void finishEditing() {
        String newText = editor.getText().toString().trim();
        int NumberOfCurrentImages = imageArray.size();
        String imageCheck = TextUtils.join(",", imageArray);

        switch (action) {
            case Intent.ACTION_INSERT:
                if (newText.length() == 0 && audioFileName.isEmpty() && NumberOfCurrentImages == 0) {
                    setResult(RESULT_CANCELED);
                } else {
                    InsertNote(newText);
                }
                break;
            case Intent.ACTION_EDIT:
                if (newText.length() == 0 && audioFileName.isEmpty() && NumberOfCurrentImages == 0) {
                    deleteNote();
                } else if (oldText.equals(newText) && audio == audioFileName && imageCheck == imageString) {
                    setResult(RESULT_CANCELED);
                    Log.d("Test", "Cancelled");
                } else {
                    UpdateNote(newText);
                    Log.d("Test", "Updated");
                }
                finish();

        }
    }

    private void InsertNote(String noteText) {
        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, noteText);
        values.put(DBOpenHelper.LATITUDE, latitude);
        values.put(DBOpenHelper.LONGITUDE, longitude);
        if (!imageArray.isEmpty()) {
            values.put(DBOpenHelper.IMAGE, TextUtils.join(",", imageArray));
        }
        values.put(DBOpenHelper.AUDIO, audioFileName);

        getContentResolver().insert(NoteProvider.CONTENT_URI, values);
        setResult(RESULT_OK);
    }

    private void UpdateNote(String noteText) {
        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, noteText);
        values.put(DBOpenHelper.IMAGE, TextUtils.join(",", imageArray));
        values.put(DBOpenHelper.AUDIO, audioFileName);
        getContentResolver().update(NoteProvider.CONTENT_URI, values, noteFilter, null);

        Snackbar.make(coordinatorLayout, getString(R.string.all_deleted), Snackbar.LENGTH_SHORT).show();
        setResult(RESULT_OK);
    }

    public Boolean serviceok() {
        int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Snackbar.make(coordinatorLayout, "Cannot connect to Mapping service", Snackbar.LENGTH_SHORT).show();
        }

        return false;
    }

    public void play(View view) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        Intent intent = new Intent(this, PlayAudio.class);
        startService(intent);
        stopPlaying.setVisibility(View.VISIBLE);
    }

    public void stop(View view) {
        if (mediaPlayer == null)
            return;

        Intent intent = new Intent(this, PlayAudio.class);
        stopService(intent);
        stopPlaying.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, PlayAudio.class);
        stopService(intent);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (uri == null) {
            new showCurrentLocationSync(mPBar).execute();
        } else {
            new goToLocationSync(mPBar).execute();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void controlImageView(int i) {

        File path = new File(getFilesDir(), "images");

        File imageFile = new File(path, imageArray.get(i));

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath(), options);

        linearLayout = (LinearLayout) findViewById(R.id.linearForImage);

        imageView = new ImageView(this);
        imageView.setOnClickListener(onClickImageViewListener(i));
        imageView.setOnLongClickListener(onLongClickListener(i));
        imageView.setId(i);
        imageView.setPadding(4, 2, 4, 2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(190, 190);
        imageView.setLayoutParams(params);
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        linearLayout.addView(imageView);
    }

    private void takePicture() {
        File path = new File(getFilesDir(), "images");
        if (!path.exists()) {
            path.mkdir();
        }
        String imageFileName = dateString() + ".jpg";
        File imagePath = new File(path, imageFileName);
        imageUri = FileProvider.getUriForFile(this, IMAGE_DIRECTORY_NAME, imagePath);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
        imageArray.add(imageFileName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (linearLayout != null) {
                    linearLayout.removeAllViews();
                }
                for (int i = 0; i < imageArray.size(); i++) {
                controlImageView(i);
                }

            } else if (resultCode == RESULT_CANCELED) {
                Snackbar.make(coordinatorLayout, "Camera cancelled", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(coordinatorLayout, "Failed to take a picture", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private View.OnClickListener onClickImageViewListener(final int i) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView imageView = (ImageView) findViewById(i);

                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                Bitmap bitmap = drawable.getBitmap();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byteArray = stream.toByteArray();

                Intent intent = new Intent(EditorActivity.this, ImageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("picture", byteArray);
                startActivity(intent);
            }
        };
    }

    private View.OnLongClickListener onLongClickListener(final int i) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                DialogInterface.OnClickListener dialogClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int button) {
                                if (button == DialogInterface.BUTTON_POSITIVE) {
                                    linearLayout = (LinearLayout) findViewById(R.id.linearForImage);

                                    linearLayout.removeViewAt(i);
                                    imageArray.remove(i);
                                    linearLayout.invalidate();
                                }
                            }
                        };

                AlertDialog.Builder builder = new AlertDialog.Builder(EditorActivity.this);
                builder.setMessage(getString(R.string.are_you_sure_image))
                        .setPositiveButton(getString(android.R.string.yes), dialogClickListener)
                        .setNegativeButton(getString(android.R.string.no), dialogClickListener)
                        .show();
                return true;
            }

        };
    }

    private String dateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateTime = dateFormat.format(new Date());
        return currentDateTime;
    }

    class showCurrentLocationSync extends AsyncTask<Void, Integer, Void> {

        private ProgressBar pBar;

        public showCurrentLocationSync(ProgressBar pBar) {
            this.pBar = pBar;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        showCurrentLocation();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            for (int i = 0; i < 11; i++) {
                try {
                    Thread.sleep(100);
                    publishProgress(i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            pBar.setProgress(values[0]);

        }

    }

    class goToLocationSync extends AsyncTask<Integer, Integer, Void> {

        private ProgressBar pBar;

        public goToLocationSync(ProgressBar pBar) {
            this.pBar = pBar;
        }


        @Override
        protected Void doInBackground(Integer... params) {

            publishProgress(10);

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            pBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            try {
                gotoLocation(latitude, longitude, 15);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
