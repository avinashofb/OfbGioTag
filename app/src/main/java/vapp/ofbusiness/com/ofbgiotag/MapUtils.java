package vapp.ofbusiness.com.ofbgiotag;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
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

    public static void addMarker(double latitude, double longitude, String markerTitle, Marker mapMarker, GoogleMap googleMap, BitmapDescriptor iconDrawable){
        if (mapMarker != null) {
            mapMarker.remove();
        }
        //Place current location marker
        LatLng latLng = new LatLng(latitude, longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(markerTitle);
        markerOptions.icon(iconDrawable);
        mapMarker = googleMap.addMarker(markerOptions);
        mapMarker.showInfoWindow();
        moveCameraToLocation(latitude, longitude, googleMap);
    }

    public static void moveCameraToLocation(double latitude, double longitude, GoogleMap googleMap){
        //move map camera
        LatLng latLng = new LatLng(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MapConstant.DEFAULT_CAMERA_ZOOM));
    }

    public static void showAreaBoundaryCircle(double latitude, double longitude, GoogleMap googleMap){
        googleMap.addCircle(new CircleOptions()
                .center(new LatLng(latitude, longitude))
                .radius(MapConstant.CIRCLE_RADIUS)
                .fillColor(Color.TRANSPARENT)
                .strokeColor(R.color.circleStrokeColor)
                .strokeWidth((float) 4));
    }

    public static double getDisplacementBetweenCoordinates(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        // distance between latitudes and longitudes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // apply formulae
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) *
                        Math.cos(lat1) *
                        Math.cos(lat2);
        double rad = 6371;
        double c = 2 * Math.asin(Math.sqrt(a));
        return (rad * c) * 1000;
    }
}
