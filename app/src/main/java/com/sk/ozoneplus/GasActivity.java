package com.sk.ozoneplus;

import android.app.Fragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.sk.ozoneplus.db.MaskDB_Manger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executor;

import io.fabric.sdk.android.Fabric;

public class GasActivity extends Fragment {
    //TODO set listeners for button if enables enabled button
    //TODO for now when gas button get isClicked it only shows particular gas type change that to all in one graph change line colour
    //TODO if toxic area found and confirmed send request to gmaps API and get location and add to DB
    private static final String TAG = "GasActivity";

    private static final int TIME_DELAY = 10000;

    private static final int SERVER_TIME = 0;
    private static final int UPDATE_GRAPH = 1;
    private static final int UPDATE_CLOUD = 2;
    private static final int GET_SERVER_TIME = 5;
    private static final int NO_INTERNET_CONNECTION = 11;
    private static final int MAX_DATAPOINTS = 60;

    private static final int NO2 = 0;
    private static final int HUMIDITY = 1;
    private static final int METHANE = 2;
    private static final int CO = 3;
    private static final int SMOKE = 4;
    private static final int TEMPERATURE = 5;

    private static final double TOXIC_LEVEL_NO2 = 8;
    private static final double TOXIC_LEVEL_HUMIDITY = 9;
    private static final double TOXIC_LEVEL_METHANE = 9;
    private static final double TOXIC_LEVEL_CO = 9;
    private static final double TOXIC_LEVEL_SMOKE = 6;
    private static final double TOXIC_LEVEL_TEMPERATURE = 6;

    // SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // server's MAC address
    private static final String address = "84:EF:18:B7:8D:AF";
    //private static final String address = "20:16:01:20:58:25";
    //private static String address = "00:21:13:00:66:A8";

    private Handler responseHandler, commandHandler;
    private final Handler btHandler = new Handler();
    private MaskDB_Manger maskDB;

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private OutputStream outStream;
    private InputStream inputStream;
    private BluetoothServerSocket mmServerSocket;

    private GraphView graph;
    private Runnable updateGraph, updateCloud, connectBT, getData;
    private LineGraphSeries<DataPoint> no2Series, humiditySeries, methaneSeries, coSeries, smokeSeries, tempSeries;
    private ArrayList<Double> no2Dataset, humidityDataset, methaneDataset, coDataset, smokeDataset, tempDataset;
    private Executor executor;
    private int xAnsis = 0;
    private Calendar calender;
    private Spinner spinner;
    private TextView status;

    private String username;
    private boolean isClicked = false;

    private FloatingActionButton btnConnect;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_gas, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        username = new Bundle().getString("username");

        executor = new Queue();
        maskDB = new MaskDB_Manger(getActivity().getApplicationContext(), username);
        calender = Calendar.getInstance();

        spinner = (Spinner) getActivity().findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(getActivity().getApplicationContext(),
                        R.array.gas_list, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                graph.removeAllSeries();
                switch (position) {
                    case NO2:
                        graph.addSeries(no2Series);
                        break;
                    case HUMIDITY:
                        graph.addSeries(humiditySeries);
                        break;
                    case METHANE:
                        graph.addSeries(methaneSeries);
                        break;
                    case CO:
                        graph.addSeries(coSeries);
                        break;
                    case SMOKE:
                        graph.addSeries(smokeSeries);
                        break;
                    case TEMPERATURE:
                        graph.addSeries(tempSeries);
                        break;
                }
                spinner.setSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinner.setSelection(NO2);
            }
        });

        status = (TextView) getActivity().findViewById(R.id.status);

        btnConnect = (FloatingActionButton) getActivity().findViewById(R.id.connectBT);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isClicked) {
                    isClicked = true;
                    executor.execute(connectBT);
                    executor.execute(updateGraph);
                    //executor.execute(updateCloud);
                    graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                        int s = calender.get(Calendar.SECOND);

                        @Override
                        public String formatLabel(double value, boolean isValueX) {
                            if (isValueX) {
                                return super.formatLabel(labelFormer(s + value), isValueX);
                            } else {
                                // show currency for y values
                                return super.formatLabel(value, isValueX);
                            }
                        }
                    });

                    status.setText(DateFormat.getDateTimeInstance().format(new Date()));
                }
            }
        });

        initializeGraph();
    }

    private LineGraphSeries<DataPoint> declareGraph(int Color) {
        LineGraphSeries<DataPoint> lineGraphSeries = new LineGraphSeries<>();
        lineGraphSeries.setAnimated(true);
        lineGraphSeries.setColor(Color);
        lineGraphSeries.setThickness(5);
        return lineGraphSeries;
    }

    public void initializeGraph() {
        no2Dataset = new ArrayList<>();
        humidityDataset = new ArrayList<>();
        methaneDataset = new ArrayList<>();
        coDataset = new ArrayList<>();
        smokeDataset = new ArrayList<>();
        tempDataset = new ArrayList<>();

        no2Series = declareGraph(Color.GREEN);
        humiditySeries = declareGraph(Color.RED);
        methaneSeries = declareGraph(Color.YELLOW);
        coSeries = declareGraph(Color.BLUE);
        smokeSeries = declareGraph(Color.GRAY);
        tempSeries = declareGraph(Color.MAGENTA);

        graph = (GraphView) getActivity().findViewById(R.id.graph);
        graph.addSeries(no2Series);
        graph.getViewport().setXAxisBoundsManual(false);
        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.getViewport().setScrollableY(true); // enables vertical scrolling
        graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        graph.getViewport().setScalableY(true); // enables vertical zooming and scrolling
        //graph.getViewport()
        graph.getGridLabelRenderer().setHorizontalAxisTitle("seconds");
        /*graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            int s = calender.get(Calendar.SECOND);

            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return super.formatLabel(labelFormer(s + value), isValueX);
                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });*/

        //graph.getGridLabelRenderer().setHumanRounding(false);
        //graph.getGridLabelRenderer().setNumHorizontalLabels(2);

        /*StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        staticLabelsFormatter.setHorizontalLabels(label);
        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);*/
        /*graph.setBackgroundColor(Color.RED);*/
    }

    public double labelFormer(double value) {
        return value - ((int) value / 60) * 60;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(getActivity().getApplicationContext(), new Crashlytics());
        //maskDB = new MaskDB_Manger()
    }

    @Override
    public void onStart() {
        super.onStart();

        //TODO get system time and server time if both not sync finish()
//        finish();

        commandHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case GET_SERVER_TIME:
                        new ConnectCloud().execute("GET_TIME");
                        System.out.println("[GET TIME MESSAGE HAS BEEN SENT]");
                        break;
                }
            }
        };

        responseHandler = new Handler() {
            public void handleMessage(Message msg) {
                String strIncome = null;
                switch (msg.what) {
                    case SERVER_TIME:
                        System.out.println("[RESULT] " + msg.obj.toString());

                        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(Calendar.getInstance());

                        if (!(compareTime(msg.obj.toString(), formattedDate))) {
                            //TODO Display error "Check the phone time and restart"
                        }
                        break;

                    case UPDATE_GRAPH:
                        strIncome = (String) msg.obj;
                        String[] a = strIncome.split("/");

                        Log.i(TAG, "Bluetooth Data " + strIncome);

                        /*no2Dataset.add(Double.parseDouble(a[NO2]));
                        humidityDataset.add(Double.parseDouble(a[HUMIDITY]));
                        methaneDataset.add(Double.parseDouble(a[METHANE]));
                        coDataset.add(Double.parseDouble(a[CO]));
                        smokeDataset.add(Double.parseDouble(a[SMOKE]));
                        tempDataset.add(Double.parseDouble(a[TEMPERATURE]));*/

                        addToDataSet(no2Dataset, NO2, a);
                        addToDataSet(humidityDataset, HUMIDITY, a);
                        addToDataSet(methaneDataset, METHANE, a);
                        addToDataSet(coDataset, CO, a);
                        addToDataSet(smokeDataset, SMOKE, a);
                        addToDataSet(tempDataset, TEMPERATURE, a);

                        xAnsis++;
                        break;

                    case UPDATE_CLOUD:
                        executor.execute(updateCloud);
                        Log.i(TAG, "Cloud update request sent");
                        //System.out.println("[CLOUD HANDLER]");
                        break;

                    case NO_INTERNET_CONNECTION:
                        //showToast("Internet isn't connected");
                        Log.i(TAG, "No internet connection");
                        break;
                }
            }
        };

        connectBT();
        updateGraph();

        //commandHandler.sendEmptyMessage(GET_SERVER_TIME);
    }

    private void addToDataSet(ArrayList<Double> dataset, int gasType, String[] a) {
        try {
            double val = Double.parseDouble(a[gasType]);
            dataset.add(val);
            checkToxicLevel(val, gasType);
            Log.i(TAG, "Data adding to list " + gasType + " " + a[gasType]);
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            dataset.add(0d);
            Log.e(TAG, "Data didn't receive " + gasType, e);
        }
    }

    private void checkToxicLevel(Double val, int gas) {
        //alert();
        switch (gas) {
            case NO2:
                if (val >= TOXIC_LEVEL_NO2) alert(gas, val);
                break;
            case HUMIDITY:
                break;
            case METHANE:
                break;
            case CO:
                break;
            case SMOKE:
                break;
            case TEMPERATURE:
                break;
        }
    }

    private void alert(int toxicGas, Double val) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getActivity())
                        .setSmallIcon(R.drawable.i_c_launcher_ozone_web)
                        .setContentTitle("Gas Alert")
                        .setContentText("ALERT BITCH");

        Intent notificationIntent = new Intent(getActivity(), GasAreaActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getActivity(), 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getActivity()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());

        Vibrator v = (Vibrator) getActivity().getApplicationContext()
                .getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(5000);


        new SendToxicLocation(toxicGas, val, getLastBestLocation()).execute();
    }

    private Location getLastBestLocation() {
        Log.i(TAG, "GPS Locating");
        LocationManager locationManager = (LocationManager) getActivity()
                .getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) {
            GPSLocationTime = locationGPS.getTime();
        }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if (0 < GPSLocationTime - NetLocationTime) {
            return locationGPS;
        } else {
            return locationNet;
        }
    }

    public void showToast(String message) {
        try {
            Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
        } catch (NullPointerException e) {
            //e.printStackTrace();
        }
    }

    public void showSnackBar(String message) {
        try {
            Snackbar.make(getActivity().findViewById(R.id.activity_gas),
                    message, Snackbar.LENGTH_LONG).setAction("Action", null).show();
        } catch (NullPointerException e) {
            //e.printStackTrace();
        }
    }

    /**
     * Initialize updateGraph Runnable
     */
    private void updateGraph() {
        updateGraph = new Runnable() {
            @Override
            public void run() {
                try {
                    //double graphHoriLabel = calender.get(Calendar.SECOND);

                    double no2 = getValue(no2Dataset);
                    double hum = getValue(humidityDataset);
                    double methane = getValue(methaneDataset);
                    double co = getValue(coDataset);
                    double smk = getValue(smokeDataset);
                    double temp = getValue(tempDataset);

                    no2Series.appendData(new DataPoint(graphHoriLabel, no2), true, MAX_DATAPOINTS);
                    humiditySeries.appendData(new DataPoint(graphHoriLabel, hum), true, MAX_DATAPOINTS);
                    methaneSeries.appendData(new DataPoint(graphHoriLabel, methane), true, MAX_DATAPOINTS);
                    coSeries.appendData(new DataPoint(graphHoriLabel, co), true, MAX_DATAPOINTS);
                    smokeSeries.appendData(new DataPoint(graphHoriLabel, smk), true, MAX_DATAPOINTS);
                    tempSeries.appendData(new DataPoint(graphHoriLabel, temp), true, MAX_DATAPOINTS);

                    graph.getViewport().setMaxX(graphHoriLabel);
                    graph.getViewport().setMinX(0);

                    y++;
                        /*insertDaily(graphHoriLabel, no2, 1);
                        insertDaily(graphHoriLabel, hum, 2);
                        insertDaily(graphHoriLabel, methane, 3);*/
                    graphHoriLabel += 10;
                    btHandler.postDelayed(this, TIME_DELAY);
                } catch (ArrayIndexOutOfBoundsException e) {
                    btHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    public void insertDaily(double hour, double level, int gas) {
        maskDB.insertDaily((int) hour, (int) level, gas);
    }

    /**
     * Initialize bluetooth Runnable
     */
    private void connectBT() {
        connectBT = new Runnable() {
            @Override
            public void run() {
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                try {
                    while (true) {
                        if (!btAdapter.isEnabled()) {
                            btAdapter.enable();
                            Thread.sleep(2000);
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                    //System.out.println("[BLUE TOOTH ENABLE EXCEPTION] " + e.getMessage());
                    Log.e(TAG, "Exception", e);
                    enableBtn();
                    showSnackBar("Bluetooth disconnected");
                    return;
                }

                Log.i(TAG, "Bluetooth going to send");

                // Set up a pointer to the remote node using it's address.
                BluetoothDevice device = btAdapter.getRemoteDevice(address);

                Log.i(TAG, "Bluetooth request sent");

                //  Two things are needed to make a connection:
                //  A MAC address, which we got above.
                //  A Service ID or UUID.  In this case we are using the
                //  UUID for SPP.
                try {
                    btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    Log.i(TAG, "Bluetooth discovered");

                    //  Discovery is resource intensive.  Make sure it isn't going on
                    //  When you attempt to connectCloud and pass your message.
                    btAdapter.cancelDiscovery();


                } catch (IOException e) {
                    Log.e(TAG, "Bluetooth Listen Exception", e);
                    enableBtn();
                    showSnackBar("Bluetooth disconnected");
                    return;
                }

                // Establish the connection.  This will block until it connects.
                try {
                    btSocket.connect();
                    Log.i(TAG, "\n...Connection established and data linek opned...");
                } catch (IOException e) {
                    try {
                        btSocket.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "Bluetooth socket close exception", e2);
                    }
                    Log.e(TAG, "Bluetooth connection exception", e);
                    enableBtn();
                    showSnackBar("Bluetooth disconnected");
                    return;
                }

                // Create a data stream so we can talk to server.
                try {
                    outStream = btSocket.getOutputStream();
                    inputStream = btSocket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    enableBtn();
                    showSnackBar("Bluetooth disconnected");
                    return;
                }

                //System.out.println("[BLUE HAS BEEN INITIALIZED]");

                //String message = "send data\n";

                /*byte[] msgBuffer = message.getBytes();
                try {
                    outStream.write(msgBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    enableBtn();
                    showSnackBar("Bluetooth disconnected");
                    return;
                }*/

                //int numBytes; // bytes returned from read()

                try {
                    mmServerSocket = btAdapter.listenUsingRfcommWithServiceRecord(address, MY_UUID);
                    btAdapter.cancelDiscovery();
                    Log.i(TAG, "Bluetooth Listener");
                } catch (IOException e) {
                    e.printStackTrace();
                    enableBtn();
                    showSnackBar("Bluetooth disconnected");
                    return;
                }

                getData.run();
                //beginListenForData();
            }
        };

        getData = new Runnable() {
            public void run() {
                try {
                    Log.i(TAG, "Receiving Data");
                    //if (btSocket.isConnected()) throw new NullPointerException("BT Socket is Null");
                    byte[] rawBytes = new byte[1024];
                    int bytes = inputStream.read(rawBytes);
                    final String string = new String(rawBytes, 0, bytes);
                    responseHandler.obtainMessage(UPDATE_GRAPH, string).sendToTarget();
                    btHandler.postDelayed(this, TIME_DELAY);
                } catch (Exception ex) {
                    Log.i(TAG, "Bluetooth data receive exception", ex);
                    enableBtn();
                    showSnackBar("Bluetooth disconnected");
                }
            }
        };

        /*getData = new Runnable() {
            public void run() {
                try {
                    int byteCount = inputStream.available();
                    if (byteCount > 0) {
                        byte[] rawBytes = new byte[byteCount];
                        int bytes = inputStream.read(rawBytes);
                        final String string = new String(rawBytes, 0, bytes);
                        responseHandler.obtainMessage(UPDATE_GRAPH, string).sendToTarget();
                        btHandler.postDelayed(this, TIME_DELAY);
                    } else {
                        Log.i(TAG, "No data");
                    }
                } catch (Exception ex) {
                    Log.i(TAG, "Bluetooth data receive exception", ex);
                    enableBtn();
                    showSnackBar("Bluetooth disconnected");
                }
            }
        };*/
    }

    /**
     * @param serverDate server's time in String format
     * @param sysDate    system's time in String format
     *                   Date format example 2017-02-27 12:08:29.073
     * @return boolean compares both time and return
     */
    private boolean compareTime(String serverDate, String sysDate) {
        String[] server = serverDate.split(" ");
        String[] sys = sysDate.split(" ");

        if (server[0].equals(sys[0])) {
            String[] serverTime = server[1].split(":");
            String[] sysTime = server[1].split(":");

            if (serverTime[0].equals(sysTime[0])) {
                //TODO implement if statement for compare minutes
                return true;
            }
        }
        return false;
    }

    int y = 0;
    private double graphHoriLabel = 0d;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void enableBtn() {
        isClicked = false;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private double getValue(ArrayList<Double> list) throws ArrayIndexOutOfBoundsException {
        try {
            return list.get(y);
        } catch (Exception e) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * ConnectCloud uses to deal with cloud small information
     * Such as server time, cloud connection
     * Possible params for doInBackground are GET_TIME , CLOUD_CONNECTION
     */
    private class ConnectCloud extends AsyncTask<String, String, String> {
        private final static String TAG = "ConnectCloud";
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
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                // Establish the connection.
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                con = DriverManager.getConnection(connectionString, userAdmin, password);

                String SQL = null;

                switch (params[0]) {
                    case "GET_TIME":
                        // Create and execute an SQL statement that returns some data.
                        SQL = "SELECT GETDATE() AS CurrentDateTime";
                        break;
                }

                System.out.println("[SQL PREPARED]");

                stmt = con.createStatement();
                rs = stmt.executeQuery(SQL);

                while (rs.next()) {
                    System.out.println(rs.getString(1));
                    responseHandler.obtainMessage(SERVER_TIME, rs.getString(1)).sendToTarget();
                }

            } catch (SQLException e) {
                e.printStackTrace();
                //TODO add exception to responseHandler
                responseHandler.sendEmptyMessage(NO_INTERNET_CONNECTION);
            } catch (Exception e) {
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

        //TODO implement error handling
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    private class SendToxicLocation extends AsyncTask<String, String, String> {

        private final static String connectionString =
                "jdbc:jtds:sqlserver://appmaskdb.database.windows.net:1433;instance=SQLEXPRESS;DatabaseName=AppMaskDB;";
        private final static String userAdmin = "maskAdmin@appmaskdb";
        private final static String password = "Sdgp12345678";
        // Declare the JDBC objects.
        private Connection con = null;
        private Statement stmt = null;
        private ResultSet rs = null;

        private int toxicGas;
        private double level;
        private Location location;

        public SendToxicLocation(int toxicGas, double level, Location location) {
            this.toxicGas = toxicGas;
            this.level = level;
            this.location = location;
        }

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "Cloud is running " + location.toString());
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                int areaId;

                // Establish the connection.
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                con = DriverManager.getConnection(connectionString, userAdmin, password);

                String SQL = "INSERT INTO affectedArea (dangerLevel) VALUES (" + level + ")";

                stmt = con.createStatement();
                int a = stmt.executeUpdate(SQL);

                Log.i(TAG, "Inserted into affected area");

                if (a > 0) {
                    SQL = "SELECT areaId FROM affectedArea WHERE areaId = (SELECT MAX(areaId) FROM affectedArea)";
                    stmt = con.createStatement();
                    rs = stmt.executeQuery(SQL);

                    areaId = -1;

                    while (rs.next()) {
                        areaId = rs.getInt("areaId");
                    }

                    Log.i(TAG, "AreaId received " + areaId);

                    if (areaId != -1) {
                        SQL = "INSERT INTO location (areaId, lat, lot) VALUES (" + areaId
                                + "," + location.getLatitude() + "," + location.getLongitude() + ")";
                        stmt = con.createStatement();
                        int row = stmt.executeUpdate(SQL);

                        Log.i(TAG, "Location added");

                        SQL = "INSERT INTO areaGas (areaId, gasId, gasLevel) " +
                                "VALUES (" + areaId + "," + toxicGas + "," + level + ")";
                        stmt = con.createStatement();
                        row = stmt.executeUpdate(SQL);

                        Log.i(TAG, "Area gas added");
                    }
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
    }

    /**
     * A subclass used to create new thread from runnable
     */
    private class Queue implements Executor {

        @Override
        public void execute(@NonNull Runnable command) {
            new Thread(command).start();
            System.out.println("[QUEUE] HAS STARTED");
        }
    }
}
