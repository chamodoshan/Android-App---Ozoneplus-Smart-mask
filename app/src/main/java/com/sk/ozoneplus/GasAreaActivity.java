package com.sk.ozoneplus;

import android.app.Fragment;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class GasAreaActivity extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private final String TAG = "GasAreaActivity";
    Circle circleOptions;
    private MapView mMapView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_gas_area, container, false);

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
        ////TODO Stop onLocationChange listeners
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        //TODO Stop onLocationChange listeners
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
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), 17));
            Toast.makeText(getActivity().getApplicationContext(),
                    location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_LONG).show();
            addCircle(location);
        }
    };

    public void addCircle(Location location) {
        circleOptions.setCenter(new LatLng(location.getLatitude(), location.getLongitude()));
        //Radius changing place
        circleOptions.setRadius(100.0);
        circleOptions.setStrokeColor(0xFFFF0000);
        circleOptions.setFillColor(0x7FFF0000);
    }

    private class ConnectCloud extends AsyncTask {

        private String message = "";

        @Override
        protected Object doInBackground(Object[] params) {

            // Create a variable for the connection string.
            /*String connectionUrl = "jdbc:sqlserver://appmaskdb.database.windows.net:1433;database=AppMaskDB;" +
                    "user=maskAdmin@appmaskdb;password=Sdgp12345678;" +
                    "encrypt=true;trustServerCertificate=false;" +
                    "hostNameInCertificate=*.database.windows.net;" +
                    "loginTimeout=30;\n";*/

            // Declare the JDBC objects.
            Connection con = null;
            Statement stmt = null;
            ResultSet rs = null;

            try {
                // Establish the connection.
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                con = DriverManager.getConnection("jdbc:jtds:sqlserver://appmaskdb.database.windows.net:1433;instance=SQLEXPRESS;DatabaseName=AppMaskDB;",
                        "maskAdmin@appmaskdb", "Sdgp12345678");

                // Create and execute an SQL statement that returns some data.
                String SQL = "SELECT * gas";
                stmt = con.createStatement();
                rs = stmt.executeQuery(SQL);

                // Iterate through the data in the result set and display it.
                while (rs.next()) {
                    System.out.println(rs.getString(1) + " " + rs.getString(2));
                    message = rs.getString(1) + " " + rs.getString(2);
                }
            }

            // Handle any errors that may have occurred.
            catch (Exception e) {
                e.printStackTrace();
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

        public String getMessage() {
            return message;
        }
    }
}
