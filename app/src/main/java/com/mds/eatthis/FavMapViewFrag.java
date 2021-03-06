package com.mds.eatthis;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;
import static com.mds.eatthis.DatabaseConstants.PlaceID;
import static com.mds.eatthis.DatabaseConstants.RestaurantLat;
import static com.mds.eatthis.DatabaseConstants.RestaurantLocation;
import static com.mds.eatthis.DatabaseConstants.RestaurantLong;
import static com.mds.eatthis.DatabaseConstants.RestaurantName;
import static com.mds.eatthis.DatabaseConstants.TABLE_NAME;
import static com.mds.eatthis.R.id.favmap;

/**
 * Created by Darren, Ming Kiang and Stanley.
 */

public class FavMapViewFrag extends Fragment implements OnMapReadyCallback{
    private GoogleMap gMap;
    MapView mMapView;
    private ImageButton heartButton;
    private TextView restaurant;
    private TextView address;
    String placeid;
    String placeName;
    String vicinity;
    double placeLatitude;
    double placeLongitude;
    LatLng placeLatLng;
    int favheartid = 0;
    private DatabaseEventsData locationdetails;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //Storing of layout file in variable v
        View v = inflater.inflate(R.layout.fragment_favmap, container, false);

        //Storing of variables gotten from FavouritesFrag
        placeid = getArguments().getString("placeid");
        placeName = getArguments().getString("placeName");
        vicinity = getArguments().getString("address");
        placeLatitude = getArguments().getDouble("lat");
        placeLongitude = getArguments().getDouble("lng");

        //Initializing map
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e){
            e.printStackTrace();
        }
        mMapView = (MapView) v.findViewById(favmap);
        mMapView.onCreate(savedInstanceState);
        //start mapView immediately
        mMapView.onResume();
        //call onMapReady
        mMapView.getMapAsync(this);

        restaurant = (TextView) v.findViewById(R.id.restaurant);
        address = (TextView) v.findViewById(R.id.address);
        heartButton = (ImageButton) v.findViewById(R.id.favourited);

        locationdetails = new DatabaseEventsData(FavMapViewFrag.this.getActivity());

        heartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(favheartid == 1){
                    try{
                        addEvent();
                        Toast.makeText(FavMapViewFrag.this.getActivity(),"Added to favourites", Toast.LENGTH_SHORT).show();
                        heartButton.setBackgroundResource(R.drawable.favourited);
                        favheartid = 0;

                    }finally{
                        locationdetails.close();
                    }
                }else{
                    //delete restaurant name and location from database
                    removeEvent(placeid);

                    Toast.makeText(FavMapViewFrag.this.getActivity(),"Removed from favourites", Toast.LENGTH_SHORT).show();
                    heartButton.setBackgroundResource(R.drawable.favouriteborder);
                    favheartid = 1;
                }

            }
        });

        restaurant.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                findWebSite(placeid);
            }
        });

        locationdetails.close();
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //setting of title on toolbar
        getActivity().setTitle("Map");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        // Add a marker on restaurant and move the camera
        placeLatLng = new LatLng(placeLatitude, placeLongitude);
        MarkerOptions option = new MarkerOptions().position(placeLatLng).title("Restaurant location");
        Marker userLocMarker = gMap.addMarker(option);
        //set text view to appropriate text
        address.setText(vicinity);
        //Making the text with an underline
        SpannableString content = new SpannableString(placeName);
        content.setSpan(new UnderlineSpan(),0,placeName.length(),0);
        //set textview to appropriate text
        restaurant.setText(content);
        userLocMarker.showInfoWindow();
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(placeLatitude, placeLongitude), 17.0f));
    }


    private void addEvent() {
        SQLiteDatabase db = locationdetails.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(RestaurantName, placeName);
        values.put(RestaurantLocation, vicinity);
        values.put(PlaceID, placeid);
        values.put(RestaurantLat, placeLatitude);
        values.put(RestaurantLong, placeLongitude);
        db.insertOrThrow(TABLE_NAME, null, values);

    }

    private void removeEvent(String placeid){
        SQLiteDatabase db = locationdetails.getWritableDatabase();

        try{
            db.delete(TABLE_NAME,"PlaceID = ?",new String[]{placeid});
        }finally {
            db.close();
        }
    }

    private void findWebSite(String placeid){
        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/details/json?");
        googlePlacesUrl.append("placeid=").append(placeid);
        googlePlacesUrl.append("&key=AIzaSyCO4NSMZ1u7SGC4pmBO9bqSdaNRrzJuCoE");

        JsonObjectRequest request = new JsonObjectRequest( googlePlacesUrl.toString(),null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject result) {
                        //Print out results in console
                        Log.i(TAG, "onResponse: Result= " + result.toString());
                        try {
                            if(result.getString("status").equalsIgnoreCase("OK")){
                                //TODO: Down here?
                                if (result.getJSONObject("result").has("website")) {
                                    String website = result.getJSONObject("result").getString("website");

                                    Uri uri = Uri.parse(website);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivity(intent);
                                } else {
                                    Log.i(TAG, "No website found.");

                                    Toast.makeText(getActivity(), "No Website found", Toast.LENGTH_SHORT).show();
                                }
                            }else if(result.getString("status").equalsIgnoreCase("ZERO_RESULTS")){
                                Log.i(TAG, "No results found");
                            }
                        } catch (JSONException e1) {
                            e1.printStackTrace();
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

    public void replaceFragment(Fragment fragment){
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, fragment);
        ft.commit();
    }

    @Override
    public void onResume() {

        super.onResume();

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){

                    Fragment fragment = new FavouritesFrag();
                    replaceFragment(fragment);

                    return true;

                }

                return false;
            }
        });
    }
}
