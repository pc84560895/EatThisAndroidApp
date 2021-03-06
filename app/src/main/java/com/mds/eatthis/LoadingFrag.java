package com.mds.eatthis;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Darren, Ming Kiang and Stanley.
 */

public class LoadingFrag extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public static final int LOCATION_REQUEST_CODE = 10;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private ProgressBar spinner;
    int switchValue;
    int checkboxValue;
    String locationID;
    String currentLatitude, currentLongtitude;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Storing of layout file in variable v
        View v = inflater.inflate(R.layout.fragment_loading, container, false);
        //loading spinner
        spinner = (ProgressBar) v.findViewById(R.id.progressBar1);
        spinner.setVisibility(View.VISIBLE);
        System.out.println("LoadingFrag");
        //get switch value
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("searchFragData", MODE_PRIVATE);
        switchValue = Integer.parseInt(sharedPreferences.getString("switchValue", ""));
        System.out.println(switchValue + "SWITCH VALUE HERE");
        savePreferences("cLat", "0");
        savePreferences("cLng", "0");

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();

        //If not using location, get inputLocID from sharedPreferences instead
        if (switchValue == 0) {
            locationID = sharedPreferences.getString("inputLocID", "");
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //setting of toolbar title
        getActivity().setTitle("Search");
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (switchValue == 0) {
            findLocationFromID(locationID);
        } else {
            startLocationUpdates();
        }
    }

    //get user inputted location coordinates from ID using Android Places API
    private void findLocationFromID(String locationID) {
        Places.GeoDataApi.getPlaceById(mGoogleApiClient, locationID)
                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(@NonNull PlaceBuffer places) {
                        if (places.getStatus().isSuccess() && places.getCount() > 0) {
                            final Place myPlace = places.get(0);
                            LatLng gotLocation = myPlace.getLatLng();
                            currentLatitude = Double.toString(gotLocation.latitude);
                            currentLongtitude = Double.toString(gotLocation.longitude);
                            savePreferences("cLat", currentLatitude);
                            savePreferences("cLng", currentLongtitude);
                            //After lat and lng stored in preferences, get nearby places
                            loadNearByPlaces(gotLocation.latitude, gotLocation.longitude);
                            Log.i(TAG, "Place found: " + myPlace.getName());
                        } else {
                            Log.e(TAG, "Place not found");
                        }
                        places.release();
                    }
                });
    }

    //Start getting nearbyplaces from a specific location
    private void loadNearByPlaces(double latitude, double longitude) {
        SharedPreferences sharedPref = getActivity().getSharedPreferences("SpinnerData", MODE_PRIVATE);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("searchFragData", MODE_PRIVATE);
        checkboxValue = Integer.parseInt(sharedPreferences.getString("checkboxValue", ""));
        String cuisine = sharedPref.getString("cuisine", "");
        String radius = sharedPref.getString("radius", "");
        String type = "restaurant";

        StringBuilder googlePlacesUrl =
                new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude);
        googlePlacesUrl.append("&types=").append(type); //set the type of places to get which is restarant
        googlePlacesUrl.append("&sensor=false"); //sensor false = not using GPS
        if (checkboxValue == 1 && !cuisine.equals("None")) {
            googlePlacesUrl.append("&keyword=").append(cuisine);
        }
        if (checkboxValue == 1 && !radius.equals("None")) {
            googlePlacesUrl.append("&radius=").append(radius);
        } else {
            googlePlacesUrl.append("&radius=").append(350); //set default radius around location
        }
        googlePlacesUrl.append("&key=" + "AIzaSyCO4NSMZ1u7SGC4pmBO9bqSdaNRrzJuCoE"); //set the api key

        JsonObjectRequest request = new JsonObjectRequest(googlePlacesUrl.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject result) {

                        Log.i(TAG, "onResponse: Result= " + result.toString());
                        try {
                            //check if there are any nearby restaurants being returned
                            if (result.getString("status").equalsIgnoreCase("OK")) {

                                //Send the JSONObject with the nearby places to MapViewFrag
                                Bundle args = new Bundle();
                                Fragment fragment = new MapViewFrag();
                                String nearbyPlaces = result.toString();
                                args.putString("nearbyPlaces", nearbyPlaces);
                                fragment.setArguments(args);
                                replaceFragment(fragment);

                            } else if (result.getString("status").equalsIgnoreCase("ZERO_RESULTS")) {
                                //If there are no restaurant found at the user's location, pop up a dialog box
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LoadingFrag.this.getActivity());
                                alertDialogBuilder.setMessage("No nearby restaurants found")
                                        .setCancelable(false)
                                        .setPositiveButton("Ok",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        //send user back to Search fragment
                                                        Fragment fragment = new SearchFrag();
                                                        replaceFragment(fragment);
                                                    }
                                                });
                                AlertDialog alert = alertDialogBuilder.create();
                                alert.show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "onErrorResponse: Error= " + error);
                        Log.e(TAG, "onErrorResponse: Error= " + error.getMessage());
                    }
                });

        //Adding request to queue
        AppController.getInstance().addToRequestQueue(request);
    }

    //replace fragment when button is clicked
    public void replaceFragment(Fragment fragment) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, fragment);
        ft.commit();
    }

    //request for location updates
    private void startLocationUpdates() {
        System.out.println("Start location updates");
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(2000);
        //Check if permission is granted for location
        if(ActivityCompat.checkSelfPermission(getActivity(),android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else{
            String[] permissionRequested = {android.Manifest.permission.ACCESS_FINE_LOCATION};
            requestPermissions(permissionRequested,   LOCATION_REQUEST_CODE);
        }
    }

    //Method for location permission checking
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==LOCATION_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {
                Toast.makeText(getActivity(), getString(R.string.nolocation),Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(LoadingFrag.this.getActivity(), "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(LoadingFrag.this.getActivity(), "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //When requestLocationUpdates is called
    @Override
    public void onLocationChanged(Location location) {
        currentLatitude = Double.toString(location.getLatitude());
        currentLongtitude = Double.toString(location.getLongitude());
        System.out.println(currentLongtitude+"currlong");
        System.out.println(currentLatitude+"currlat");
        // Remove location updates once gotten updated location
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        savePreferences("cLat", currentLatitude);
        savePreferences("cLng", currentLongtitude);
        //After lat and lng stored in preferences, get nearby places
        loadNearByPlaces(location.getLatitude(), location.getLongitude());

    }

    public void savePreferences(String key, String value){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("LatLng", MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putString(key, value);
        prefEditor.commit();
        System.out.println(sharedPreferences.getString("cLat", "") + "LOADINGFRAGLATVALUE");
        System.out.println(sharedPreferences.getString("cLng", "") + "LOADINGFRAGLATVALUE");
    }
}
