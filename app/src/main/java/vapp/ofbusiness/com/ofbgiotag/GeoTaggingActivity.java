package vapp.ofbusiness.com.ofbgiotag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class GeoTaggingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int FINISHING_ACTIVITY_RESULT_CODE = 121;

    private ImageView imageHolder;
    private static final int CAMERA_REQUEST_CODE = 123;
    private View acceptOrRejectContainer;
    private Button acceptButton, rejectButton, capturedImageButton;
    private TextView geoAddress;
    private TextView mapWarningView;

    private GoogleMap mGoogleMap;
    private SupportMapFragment mapFrag;
    private LocationRequest mLocationRequest;
    private Location lastKnownLocation;
    private Marker mapMarker;
    private FusedLocationProviderClient mFusedLocationClient;

    private String imgLat = "";
    private String imgLong = "";
    private String imgLatRef = "";
    private String imgLongRef = "";
    private Float geoTaggedLat;
    private Float geoTaggedLong;

    private double selectedLat;
    private double selectedLong;

    private double distanceBetweenClickedLocation;

    private boolean isGeotaggedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageHolder = (ImageView) findViewById(R.id.captured_photo);
        capturedImageButton = (Button) findViewById(R.id.photo_button);
        acceptOrRejectContainer = findViewById(R.id.accept_or_reject_container);
        acceptButton = findViewById(R.id.accept_button);
        rejectButton = findViewById(R.id.reject_button);
        geoAddress = findViewById(R.id.address_tv);
        mapWarningView = findViewById(R.id.map_error_view);

        capturedImageButton.setVisibility(View.VISIBLE);
        geoAddress.setVisibility(View.GONE);
        acceptOrRejectContainer.setVisibility(View.GONE);
        mapWarningView.setVisibility(View.VISIBLE);
        mapWarningView.setText("This is your current Location. If you're not satisfied please find manually by clicking here.");

        if(!MapUtils.areThereMockPermissionApps(this)){
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(false);
                alertBuilder.setTitle("Uninstall GPS spoofing Application !");
                alertBuilder.setMessage("Uninstall GPS Spoofing Application and disable GPS Spoofing permission.");
                AlertDialog alert = alertBuilder.create();
                alert.show();
        }

        capturedImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String isGeoTagged =  Boolean.toString(isGeotaggedLocation);
                String address = MapUtils.getCompleteAddress(geoTaggedLat, geoTaggedLong, GeoTaggingActivity.this);
                if(selectedLat != 0 || selectedLong != 0) {
                    distanceBetweenClickedLocation = MapUtils.getDisplacementBetweenCoordinates(geoTaggedLat.doubleValue(), geoTaggedLong.doubleValue(), selectedLat, selectedLong);
                }else{
                    distanceBetweenClickedLocation = MapUtils.getDisplacementBetweenCoordinates(geoTaggedLat.doubleValue(), geoTaggedLong.doubleValue(), lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                }
                Toast.makeText(GeoTaggingActivity.this, " Is GioTagged - " + isGeoTagged + " Address - " + address + "Distance - " + String.valueOf(distanceBetweenClickedLocation) , Toast.LENGTH_SHORT).show();
            }
        });

        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        mapWarningView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ChooseLocationActivity.class);
                intent.putExtra("LAT", lastKnownLocation.getLatitude());
                intent.putExtra("LONG", lastKnownLocation.getLongitude());
                startActivityForResult(intent, FINISHING_ACTIVITY_RESULT_CODE);
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    Uri tempUri = getImageUri(getApplicationContext(), bitmap);
                    File finalFile = new File(getRealPathFromURI(tempUri));

                    if (finalFile != null) {
                        try {
                            ExifInterface exifInterface = new ExifInterface(finalFile.toString());
                            imgLat = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                            imgLong = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                            imgLatRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                            imgLongRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);


                            if (imgLat != null || imgLong != null) {

                                if (imgLatRef != null && imgLatRef.equals("N")) {
                                    geoTaggedLat = locationConvertToDegree(imgLat);
                                } else {
                                    geoTaggedLat = 0f - locationConvertToDegree(imgLat);
                                }
                                Log.d("GIO_TAG_LAT", String.valueOf(geoTaggedLat));

                                if (imgLongRef != null && imgLongRef.equals("E")) {
                                    geoTaggedLong = locationConvertToDegree(imgLong);
                                } else {
                                    geoTaggedLong = 0f - locationConvertToDegree(imgLong);
                                }
                                Log.d("GIO_TAG_LONG", String.valueOf(geoTaggedLong));
                            }

                        } catch (IOException e) {
                            Log.e("", "Error occurred while fetching location from Image" + e);
                        }

                    }
                    imageHolder.setImageBitmap(bitmap);
                    mGoogleMap.clear();
                    capturedImageButton.setVisibility(View.GONE);
                    acceptOrRejectContainer.setVisibility(View.VISIBLE);
                    mapWarningView.setVisibility(View.GONE);
                    geoAddress.setVisibility(View.VISIBLE);
                    if ((geoTaggedLat == null && geoTaggedLong == null) || (geoTaggedLat.equals(0.0) && geoTaggedLong.equals(0.0))) {
                        ifImageIsNotGeoTagged();
                    } else {
                        ifImageIsGeoTagged();
                    }
                }
                break;

            case FINISHING_ACTIVITY_RESULT_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if(data.getExtras().containsKey(ChooseLocationActivity.ARG_SELECTED_LAT)) {
                        selectedLat = data.getDoubleExtra(ChooseLocationActivity.ARG_SELECTED_LAT, 0);
                        selectedLong = data.getDoubleExtra(ChooseLocationActivity.ARG_SELECTED_LONG, 0);
                        mGoogleMap.clear();
                        MapUtils.addMarker(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), "Last Location", mapMarker, mGoogleMap, BitmapDescriptorFactory.fromResource(R.drawable.ic_location_off_red_700_36dp));
                        MapUtils.addMarker(selectedLat, selectedLong, "Updated Location", mapMarker, mGoogleMap, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        MapUtils.moveCameraToLocation(selectedLat, selectedLong,mGoogleMap);
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                mGoogleMap.setMyLocationEnabled(false);
                            }
                        }
                    }
                }
                break;


        }
    }

    private void ifImageIsNotGeoTagged(){
        isGeotaggedLocation = false;
        Toast.makeText(this, "Is Gio-Tagged Location - " + isGeotaggedLocation, Toast.LENGTH_LONG).show();
        geoTaggedLong = Float.parseFloat(String.valueOf(lastKnownLocation.getLongitude()));
        geoTaggedLat = Float.parseFloat(String.valueOf(lastKnownLocation.getLatitude()));
//        if(selectedLat != 0d || selectedLong != 0d){
//            geoAddress.setText(MapUtils.getCompleteAddress(selectedLat, selectedLong, this));
//            MapUtils.addMarker(selectedLat, selectedLong, "Clicked Image Location", mapMarker, mGoogleMap, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//        }else {
//            geoAddress.setText(MapUtils.getCompleteAddress(geoTaggedLat, geoTaggedLong, this));
//            MapUtils.addMarker(geoTaggedLat, geoTaggedLong, "Clicked Image Location", mapMarker, mGoogleMap, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//        }

        geoAddress.setText(MapUtils.getCompleteAddress(geoTaggedLat, geoTaggedLong, this));
        MapUtils.addMarker(geoTaggedLat, geoTaggedLong, "Clicked Image Location", mapMarker, mGoogleMap, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mGoogleMap.setMyLocationEnabled(false);
            }
        }
    }

    private void ifImageIsGeoTagged(){
        isGeotaggedLocation = true;
        Toast.makeText(this, "Is Gio-Tagged Location - " + isGeotaggedLocation, Toast.LENGTH_LONG).show();
        geoAddress.setText(MapUtils.getCompleteAddress(geoTaggedLat, geoTaggedLong, this));
        MapUtils.addMarker(geoTaggedLat, geoTaggedLong, "Clicked Image Location", mapMarker, mGoogleMap, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mGoogleMap.setMyLocationEnabled(false);
            }
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        String path = "";
        if (getContentResolver() != null) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                path = cursor.getString(idx);
                cursor.close();
            }
        }
        return path;
    }

    public static Float locationConvertToDegree(String stringDMS) {
        Float result = null;
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        Double D0 = new Double(stringD[0]);
        Double D1 = new Double(stringD[1]);
        Double FloatD = D0 / D1;

        String[] stringM = DMS[1].split("/", 2);
        Double M0 = new Double(stringM[0]);
        Double M1 = new Double(stringM[1]);
        Double FloatM = M0 / M1;

        String[] stringS = DMS[2].split("/", 2);
        Double S0 = new Double(stringS[0]);
        Double S1 = new Double(stringS[1]);
        Double FloatS = S0 / S1;

        result = new Float(FloatD + (FloatM / 60) + (FloatS / 3600));

        return result;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000); // two minute interval
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);


        checkPermissionForExternalStorage(this);

    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                lastKnownLocation = location;
                MapUtils.moveCameraToLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), mGoogleMap);
            }
        }
    };

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private boolean checkPermissionForExternalStorage(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showDialog("External storage", context,
                            Manifest.permission.READ_EXTERNAL_STORAGE);

                } else {
                    ActivityCompat
                            .requestPermissions(
                                    (Activity) context,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                checkForLocationPermission();
                return true;
            }

        } else {
            return true;
        }
    }

    private void checkForLocationPermission(){
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    public void showDialog(final String msg, final Context context, final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{permission},
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(GeoTaggingActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do your stuff
                    checkForLocationPermission();
                } else {
                    Toast.makeText(GeoTaggingActivity.this, "GET_ACCOUNTS Denied", Toast.LENGTH_SHORT).show();
                    checkForLocationPermission();
                }
                break;
        }
    }
}
