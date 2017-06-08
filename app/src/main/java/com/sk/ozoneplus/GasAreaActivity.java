package com.sk.ozoneplus;

import android.app.Fragment;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import io.fabric.sdk.android.Fabric;

public class GasAreaActivity extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnCircleClickListener {

    private GoogleMap googleMap;
    private final String TAG = "GasAreaActivity";
    private Circle circleOptions;
    private MapView mMapView;

    private boolean isPaused = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_gas_area, container, false);

        Fabric.with(getActivity().getApplicationContext(), new Crashlytics());

        mMapView = (MapView) rootView.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                LatLng IIT = new LatLng(6.866632, 79.860360);
                //googleMap.addMarker(new MarkerOptions().position(IIT).title("Marker in IIT"));
                //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(IIT, 15));

                if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                    googleMap.setOnMyLocationChangeListener(listener);
                } else {
                    System.out.println("GPS Permission Denied");
                    Toast.makeText(getActivity().getApplicationContext(), "GPS Permission Denied", Toast.LENGTH_LONG).show();
                }

                circleOptions = googleMap.addCircle(new CircleOptions().center(IIT));
            }
        });

        new GetAffectedLocations().execute();

        return rootView;
    }

    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gas_area);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }*/

    @Override
    public void onPause() {
        super.onPause();
        //TODO Stop onLocationChange listeners
        isPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        //TODO Stop onLocationChange listeners
        isPaused = true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        LatLng IIT = new LatLng(6.866632, 79.860360);
        //googleMap.addMarker(new MarkerOptions().position(IIT).title("Marker in IIT"));
        //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(IIT, 15));

        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            this.googleMap.setMyLocationEnabled(true);
            this.googleMap.setOnMyLocationChangeListener(listener);
        } else {
            System.out.println("GPS Permission Denied");
            Toast.makeText(getActivity().getApplicationContext(), "GPS Permission Denied", Toast.LENGTH_LONG).show();
        }

        circleOptions = this.googleMap.addCircle(new CircleOptions().center(IIT));
    }

    GoogleMap.OnMyLocationChangeListener listener = new GoogleMap.OnMyLocationChangeListener() {

        @Override
        public void onMyLocationChange(Location location) {
            //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
            //new LatLng(location.getLatitude(), location.getLongitude()), 17));
            try {
                Toast.makeText(getActivity().getApplicationContext(),
                        location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_LONG).show();
            } catch (NullPointerException e) {

            }
            //Log.i(TAG, location.getLatitude() + " " + location.getLongitude());
            updateCircle(location);
        }
    };

    public void updateCircle(Location location) {
        circleOptions.setCenter(new LatLng(location.getLatitude(), location.getLongitude()));
        //Radius changing place
        circleOptions.setRadius(50.0);
        circleOptions.setStrokeColor(0xFFFF0000);
        circleOptions.setFillColor(0x7FFF0000);

        /*googleMap.addCircle(new CircleOptions().center(new LatLng(location.getLatitude()
                , location.getLongitude())));*/
    }

    public void addCircle(LatLng location, CustomTag tag) {
        Log.i(TAG, location.latitude + " " + location.longitude);
        googleMap.addCircle(new CircleOptions().center(location)
                .clickable(true)
                .radius(5.0)
                .fillColor(0x7F0000FF)
                .strokeColor(0xFF0000FF).clickable(true)).setTag(tag);
        Log.i(TAG, "Circle added");
    }

    @Override
    public void onCircleClick(Circle circle) {
        Log.i(TAG, "Circle clicked");
        onClick((CustomTag) circle.getTag());
    }

    private void onClick(CustomTag tag) {
        tag.incrementClickCount();
    }

    private class GetAffectedLocations extends AsyncTask<Void, String, Void> {

        private final static String connectionString =
                "jdbc:jtds:sqlserver://appmaskdb.database.windows.net:1433;instance=SQLEXPRESS;DatabaseName=AppMaskDB;";
        private final static String userAdmin = "maskAdmin@appmaskdb";
        private final static String password = "Sdgp12345678";
        // Declare the JDBC objects.
        private Connection con = null;
        private Statement stmt = null;
        private ResultSet rs = null;

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "Cloud is running");
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                // Establish the connection.
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                con = DriverManager.getConnection(connectionString, userAdmin, password);

                String SQL = "SELECT areaId FROM affectedArea";

                stmt = con.createStatement();
                rs = stmt.executeQuery(SQL);

                while (rs.next()) {
                    int areaID = rs.getInt("areaId");

                    Log.i(TAG, "Affected area results received " + areaID);

                    SQL = "SELECT areaGas.gasLevel, areaGas.gasId, location.lot, location.lat, gas.name" +
                            "FROM ((areaGas " +
                            "INNER JOIN location ON areaGas.areaId = location.areaId)" +
                            "INNER JOIN gas ON areaGas.gasId = gas.id)" +
                            "WHERE areaGas.areaId =" + areaID;
                    stmt = con.createStatement();
                    ResultSet resultSet = stmt.executeQuery(SQL);

                    resultSet.next();
                    int gasID = resultSet.getInt("gasId");
                    int gasLevel = resultSet.getInt("gasLevel");

                    Log.i(TAG, "Gas areas results received " + gasID + " " + gasLevel);

                    String lat = resultSet.getString("lat");
                    String lot = resultSet.getString("lot");

                    Log.i(TAG, "Locations received " + lat + " " + lot);

                    String name = resultSet.getString("name");

                    Log.i(TAG, "Gas name received " + name);

                    publishProgress(lat + "", lot + "", name, gasLevel + "");
                }

            } catch (SQLException e) {
                Log.e(TAG, "SQL Exception", e);
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
            } finally {
                if (rs != null) try {
                    rs.close();
                } catch (Exception e) {
                }
                if (stmt != null) try {
                    stmt.close();
                } catch (Exception e) {
                }
                if (con != null) try {
                    con.close();
                } catch (Exception e) {
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.i(TAG, "Value receiving in onProgress " + Arrays.toString(values));
            addCircle(new LatLng(Double.parseDouble(values[0]), Double.parseDouble(values[1]))
                    , new CustomTag(values[2]));
        }
    }

    private static class CustomTag {
        private final String description;
        private int clickCount;

        public CustomTag(String description) {
            this.description = description;
            clickCount = 0;
        }

        public void incrementClickCount() {
            clickCount++;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
