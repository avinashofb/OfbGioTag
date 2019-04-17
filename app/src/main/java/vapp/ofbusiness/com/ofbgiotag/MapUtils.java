package vapp.ofbusiness.com.ofbgiotag;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapUtils {

    public MapUtils(){

    }

    public static String getCompleteAddress(double latitude, double longitude, Context context) {
        StringBuilder address = new StringBuilder();
        address.append("Address - ");
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                address.append(strReturnedAddress);
                Log.d("GEO-TAG_ADDRESS", strReturnedAddress.toString());
            } else {
                Log.w("GEO-TAG_ADDRESS", "No Address returned!");
            }
        } catch (Exception e) {
            Log.w("GEO-TAG_ADDRESS", "Cannot get Address!" + e);
        }
        return address.toString();
    }

    public static void addMarker(double latitude, double longitude, String markerTitle, Marker mapMarker, GoogleMap googleMap){
        if (mapMarker != null) {
            mapMarker.remove();
        }
        //Place current location marker
        LatLng latLng = new LatLng(latitude, longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(markerTitle);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mapMarker = googleMap.addMarker(markerOptions);
        mapMarker.showInfoWindow();
        moveCameraToLocation(latLng, googleMap);
    }

    public static void moveCameraToLocation(LatLng latLng, GoogleMap googleMap){
        //move map camera
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MapConstant.DEFAULT_CAMERA_ZOOM));
    }

    public static void showAreaBoundaryCircle(double latitude, double longitude, GoogleMap googleMap){
        googleMap.addCircle(new CircleOptions()
                .center(new LatLng(latitude, longitude))
                .radius(100)
                .fillColor(Color.TRANSPARENT)
                .strokeColor(R.color.circleStrokeColor)
                .strokeWidth((float) 2));
    }
}
