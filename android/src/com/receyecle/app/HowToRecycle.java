package com.receyecle.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by perrasr on 3/24/17.
 */

public class HowToRecycle extends Activity {

    private String classifier;
    private String recycleable;
    private String instructions;
    private String other_op;
    private int classifier_id;
    private TextView classifierTv;
    private TextView howTv;
    private TextView other_op_lbl;
    private TextView other_opTv;
    private TextView recyclableTv;


    private static final String TAG = HowToRecycle.class.getSimpleName();

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    private static final String LOCATION_ADDRESS_KEY = "location-address";

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Represents a geographical location.
     */
    private Location mLastLocation;

    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     */
    private boolean mAddressRequested;

    /**
     * The formatted location address.
     */
    private String mAddressOutput;

    private AddressResultReceiver mResultReceiver;

    private boolean allowpermission =false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.how_to);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mResultReceiver = new AddressResultReceiver(new Handler());
        mAddressRequested = false;
        mAddressOutput = "";


        classifier = getIntent().getStringExtra("classifier");
        classifier_id = getIntent().getIntExtra("classifier_id",0);
        recycleable = getIntent().getStringExtra("recycleable");
        instructions = getIntent().getStringExtra("instructions");
        other_op = getIntent().getStringExtra("other_op");
        Log.v("Classifier: ", classifier);
        Log.v("classifier_id: ", "" + classifier_id);
        Log.v("recycleable: ", recycleable);
        Log.v("instructions: ", "B" + instructions+ "E");
        Log.v("otherop: ", "B" + other_op + "E");

        classifierTv = (TextView) findViewById(R.id.classifier);
        howTv = (TextView) findViewById(R.id.how_to);
        other_op_lbl = (TextView) findViewById(R.id.other_op_lbl);
        other_opTv = (TextView) findViewById(R.id.other_op);
        recyclableTv = (TextView) findViewById(R.id.YN_recycleable_var);


        classifierTv.setText(classifier.toUpperCase());
        howTv.setText(instructions);

        if(recycleable.equalsIgnoreCase("y")){
            recyclableTv.setText("Recyclable!");
            recyclableTv.setTextColor(Color.parseColor("#008000"));
        }else{
            recyclableTv.setText("Not Recyclable!");
            recyclableTv.setTextColor(Color.parseColor("#CC0F0F"));
        }

        if(other_op.isEmpty()){
            other_op_lbl.setVisibility(View.INVISIBLE);
            other_opTv.setVisibility(View.INVISIBLE);

        }else{
            other_op_lbl.setVisibility(View.VISIBLE);
            other_opTv.setVisibility(View.VISIBLE);

            //sets link clickable
            int i1 = other_op.indexOf("\"");

            if(i1 == -1){
                other_opTv.setText(other_op);
            }else {
                final String[] split = other_op.split("\"");

                other_opTv.setMovementMethod(LinkMovementMethod.getInstance());
                other_opTv.setText(other_op, TextView.BufferType.SPANNABLE);
                final Spannable mySpannable = (Spannable) other_opTv.getText();
                final ClickableSpan myClickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Log.v("In click", split[1]);

                        Uri uri = Uri.parse(split[1]); // missing 'http://' will cause crashed
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                };
                mySpannable.setSpan(myClickableSpan, i1 + 1, i1 + 1 + split[1].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mySpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#0000FF")),  i1 + 1, i1 + 1 + split[1].length(), 0);
            }
        }

        Button done = (Button) findViewById(R.id.done_howto);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //check to see if permission for location is set, if yes then get state and send state and classifier_id to updateQueried.php
                //to tally up scanned items
                //if its not allowed then send classifier_id and "zz" as the state abrev
                if(allowpermission) {
                    Log.v("!!!", "BEFORE CHECK PERSMISSION");
                    if (mLastLocation != null) {
                        Log.v("NOTNULL", mLastLocation + "");
                        startIntentService();
                        return;
                    }

                    // If we have not yet retrieved the user location, we process the user's request by setting
                    // mAddressRequested to true. As far as the user is concerned, pressing the Fetch Address button
                    // immediately kicks off the process of getting the address.
                    mAddressRequested = true;
                }else{
                    sendToDB("ZZ");
                }


            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        checkPermission();
    }

    /**
     * Gets the address for the last known location.
     */
    @SuppressWarnings("MissingPermission")
    private void getAddress() {
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            Log.w(TAG, "onSuccess:null");
                            return;
                        }

                        mLastLocation = location;
                        Log.v("LKL", mLastLocation + "");

                        // Determine whether a Geocoder is available.
                        if (!Geocoder.isPresent()) {
                            return;
                        }

                        // If the user pressed the fetch address button before we had the location,
                        // this will be set to true indicating that we should kick off the intent
                        // service after fetching the location.
                        if (mAddressRequested) {
                            startIntentService();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getLastLocation:onFailure", e);
                    }
                });
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    private void startIntentService() {
        Log.v("!!", "INSIDE START INTENT SERVICE");

        Location location = mLastLocation;

        // Make sure that the location data was really sent over through an extra. If it wasn't,
        // send an error error message and return.
        if (location == null) {
            Log.v(TAG, "ERROR NO LOCATION");
            return;
        }

        // Errors could still arise from using the Geocoder (for example, if there is no
        // connectivity, or if the Geocoder is given illegal location data). Or, the Geocoder may
        // simply not have an address for a location. In all these cases, we communicate with the
        // receiver using a resultCode indicating failure. If an address is found, we use a
        // resultCode indicating success.

        // The Geocoder used in this sample. The Geocoder's responses are localized for the given
        // Locale, which represents a specific geographical or linguistic region. Locales are used
        // to alter the presentation of information such as numbers or dates to suit the conventions
        // in the region they describe.
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        // Address found using the Geocoder.
        List<Address> addresses = null;

        try {
            // Using getFromLocation() returns an array of Addresses for the area immediately
            // surrounding the given latitude and longitude. The results are a best guess and are
            // not guaranteed to be accurate.
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, we get just a single address.
                    1);
        } catch (IOException ioException) {

            Log.e(TAG, "ERROR", ioException);
        } catch (IllegalArgumentException illegalArgumentException) {

            Log.e(TAG, "ERROR " + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            Log.v(TAG, "ERROR empty");
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            Log.v("STATECODE OF HEAVEN",getUSStateCode(address));
            sendToDB(getUSStateCode(address));

        }


    }

    private String getUSStateCode(Address USAddress){
        String fullAddress = "";
        for(int j = 0; j <= USAddress.getMaxAddressLineIndex(); j++)
            if (USAddress.getAddressLine(j) != null)
                fullAddress = fullAddress + " " + USAddress.getAddressLine(j);

        String stateCode = null;
        Pattern pattern = Pattern.compile(" [A-Z]{2} ");
        String helper = fullAddress.toUpperCase().substring(0, fullAddress.toUpperCase().indexOf("USA"));
        Matcher matcher = pattern.matcher(helper);
        while (matcher.find())
            stateCode = matcher.group().trim();

        return stateCode;
    }
    /**
     * Return the current state of the permissions needed.
     */

    private void checkPermission(){
        Log.v("!!!", "IN CHECK PERSMISSION");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Log.v("!!!", "BEFORE CHECK");
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                Log.v("!!!", "GRANTED");
                allowpermission = true;
                getAddress();

            }else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                    Toast.makeText(this, "app needs to be able to save pictures",
                            Toast.LENGTH_SHORT).show();
                    Log.v("!!!", "INSIDE IF OF ELSE IN CHECK PERSMISSION");
                }
                Log.v("!!!", "AFTER IF PERMISSION");
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
                Log.v("!!!", "AFTER REQUEST PERMISSION");
            }
        }else{
                getAddress();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_PERMISSIONS_REQUEST_CODE){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //then request is rejected
                Toast.makeText(getApplicationContext(),
                        "RecEYEcle will not track your location for data analysis", Toast.LENGTH_SHORT).show();
            }else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                allowpermission = true;
                getAddress();
            }
        }
    }


    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            Log.v("ADRESS:", "here");
            Log.v("ADRESS:", mAddressOutput);
            //THIS IS WHERE WE GOT ADDRESS ABREV
            sendToDB(mAddressOutput);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                Log.v("SUCESS:", getString(R.string.address_found));
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            mAddressRequested = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save whether the address has been requested.
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);

        // Save the address string.
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }


    private void sendToDB(String state){
        //send this to db
        Log.v("SENDTO DB", state);

        Response.Listener<String> responseListener = new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    Log.v("HTR", "done button: in on response");

                    JSONObject jsonResponse = new JSONObject(response);
                    boolean success = jsonResponse.getBoolean("success");

                    Log.v("HTR", "done button: below setting success bool");

                    if (success) {

                        Intent intent = new Intent();
                        intent.setClass(HowToRecycle.this, Camera2VideoImageActivity.class);
                        startActivity(intent);

                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(HowToRecycle.this);
                        builder.setMessage("Data Analytics Failed")
                                .setNegativeButton("Retry", null)
                                .create()
                                .show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };

        SendScanRequest sendScanRequest = new SendScanRequest(Integer.toString(classifier_id), state, responseListener);
        RequestQueue queue = Volley.newRequestQueue(HowToRecycle.this);
        queue.add(sendScanRequest);


    }

}
