package com.sk.ozoneplus;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.vision.text.Line;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.Executor;

import io.fabric.sdk.android.Fabric;

public class GasActivity extends Fragment {
    //TODO set listeners for button if enables enabled button
    //TODO for now when gas button get clicked it only shows particular gas type change that to all in one graph change line colour
    //TODO if toxic area found and confirmed send request to gmaps API and get location and add to DB
    private static final String TAG = "GasActivity";

    private static final int TIME_DELAY = 5000;

    private static final int SERVER_TIME = 0;
    private static final int UPDATE_GRAPH = 1;
    private static final int UPDATE_CLOUD = 2;
    private static final int GET_SERVER_TIME = 5;
    private static final int NO_INTERNET_CONNECTION = 11;

    private static final int NO2 = 0;
    private static final int HUMIDITY = 1;
    private static final int METHANE = 2;
    private static final int CO = 3;
    private static final int SMOKE = 4;
    private static final int TEMPERATURE = 5;

    // SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // server's MAC address
    private static final String address = "84:EF:18:B7:8D:AF";
    //private static String address = "00:21:13:00:66:A8";

    private static Handler responseHandler, commandHandler;
    private MaskDB_Manger maskDB;

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private OutputStream outStream;
    private InputStream inputStream;
    private BluetoothServerSocket mmServerSocket;

    private GraphView graph;
    private Runnable updateGraph, updateCloud, connectBT;
    private LineGraphSeries<DataPoint> no2Series, humiditySeries, methaneSeries, coSeries, smokeSeries, tempSeries;
    private ArrayList<Double> no2Dataset, humidityDataset, methanecDataset, coDataset, smokeDataset, tempDataset;
    private Executor executor;
    private int xAnsis = 0;

    private Spinner spinner;

    private static String username;

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
                        graph.addSeries(smokeSeries);
                        break;
                }
                spinner.setSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinner.setSelection(NO2);
            }
        });

        FloatingActionButton btnConnect = (FloatingActionButton) getActivity().findViewById(R.id.connectBT);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            boolean clicked = false;

            @Override
            public void onClick(View v) {
                if (!clicked) {
                    executor.execute(connectBT);
                    executor.execute(updateGraph);
                    //executor.execute(updateCloud);
                    clicked = true;
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
        methanecDataset = new ArrayList<>();
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
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.getViewport().setScrollableY(true); // enables vertical scrolling
        graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        graph.getViewport().setScalableY(true); // enables vertical zooming and scrolling
        //graph.getViewport()
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time");

        /*StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        staticLabelsFormatter.setHorizontalLabels(label);
        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);*/
        /*graph.setBackgroundColor(Color.RED);*/
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
                        showToast(msg.obj.toString());
                        System.out.println("[RESULT] " + msg.obj.toString());

                        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(Calendar.getInstance());

                        if (!(compareTime(msg.obj.toString(), formattedDate))) {
                            //TODO Display error "Check the phone time and restart"
                        }
                        break;

                    case UPDATE_GRAPH:
                        byte[] readBuf = (byte[]) msg.obj;
                        strIncome = new String(readBuf, 0, msg.arg1);
                        showToast(strIncome);
                        String[] a = strIncome.split("/");

                        no2Dataset.add(Double.parseDouble(a[0]));
                        humidityDataset.add(Double.parseDouble(a[1]));
                        methanecDataset.add(Double.parseDouble(a[2]));
                        coDataset.add(Double.parseDouble(a[3]));
                        smokeDataset.add(Double.parseDouble(a[4]));
                        tempDataset.add(Double.parseDouble(a[5]));

                        xAnsis++;

                        Log.v(TAG, "Bluetooth Data Received");
                        break;

                    case UPDATE_CLOUD:
                        executor.execute(updateCloud);
                        Log.i(TAG, "Cloud update request sent");
                        //System.out.println("[CLOUD HANDLER]");
                        break;

                    case NO_INTERNET_CONNECTION:
                        Snackbar.make(getView(), "Internet isn't connected", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        Log.e(TAG, "No internet connection");
                        break;
                }
            }
        };

        updateCloud();
        connectBT();
        updateGraph();

        //commandHandler.sendEmptyMessage(GET_SERVER_TIME);
    }

    public void showToast(String message) {
        try {
            Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
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
                while (true) {
                    try {
                        double no2 = getValue(no2Dataset);
                        double hum = getValue(humidityDataset);
                        double methane = getValue(methanecDataset);
                        double co = getValue(coDataset);
                        double smk = getValue(smokeDataset);
                        double temp = getValue(tempDataset);

                        //TODO if data not found leave a space in graph
                        no2Series.appendData(new DataPoint(graphHoriLabel, no2), true, 40);
                        humiditySeries.appendData(new DataPoint(graphHoriLabel, hum), true, 40);
                        methaneSeries.appendData(new DataPoint(graphHoriLabel, methane), true, 40);
                        coSeries.appendData(new DataPoint(graphHoriLabel, co), true, 40);
                        smokeSeries.appendData(new DataPoint(graphHoriLabel, smk), true, 40);
                        tempSeries.appendData(new DataPoint(graphHoriLabel, temp), true, 40);

                        y++;
                        //TODO move this to finally part

                        /*insertDaily(graphHoriLabel, no2, 1);
                        insertDaily(graphHoriLabel, hum, 2);
                        insertDaily(graphHoriLabel, methane, 3);*/
                        graphHoriLabel++;
                        Thread.sleep(TIME_DELAY);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        //System.out.println("[ARRAY OUT OF BOUND]");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //TODO add if condition (not necessary)
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
                            Thread.sleep(1000);
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                    //System.out.println("[BLUE TOOTH ENABLE EXCEPTION] " + e.getMessage());
                    Log.e(TAG, "Exception", e);
                }

                Log.i(TAG, "Bluetooth request sent");

                // Set up a pointer to the remote node using it's address.
                BluetoothDevice device = btAdapter.getRemoteDevice(address);

                //  Two things are needed to make a connection:
                //  A MAC address, which we got above.
                //  A Service ID or UUID.  In this case we are using the
                //  UUID for SPP.
                try {
                    btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    //System.out.append("Connection established\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //  Discovery is resource intensive.  Make sure it isn't going on
                //  When you attempt to connectCloud and pass your message.
                btAdapter.cancelDiscovery();

                // Establish the connection.  This will block until it connects.
                try {
                    btSocket.connect();
                    Log.v(TAG, "\n...Connection established and data link opened...");
                } catch (IOException e) {
                    try {
                        btSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }

                // Create a data stream so we can talk to server.
                try {
                    outStream = btSocket.getOutputStream();
                    inputStream = btSocket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //System.out.println("[BLUE HAS BEEN INITIALIZED]");

                String message = "send data\n";
                int time = 0;

                byte[] msgBuffer = message.getBytes();
                try {
                    outStream.write(msgBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte[] buffer = new byte[1024];
                int numBytes; // bytes returned from read()

                try {
                    mmServerSocket = btAdapter.listenUsingRfcommWithServiceRecord(address, MY_UUID);
                    btAdapter.cancelDiscovery();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (true) {
                    try {
                        numBytes = inputStream.read(buffer);
                        // Send the obtained bytes to the UI activity.
                        responseHandler.obtainMessage(UPDATE_GRAPH, numBytes, -1, buffer).sendToTarget();
                        //TODO add this to time task
                        if (time == 60) {
                            // Update time to inform responseHandler
                            responseHandler.obtainMessage(UPDATE_CLOUD).sendToTarget();
                            time = 0;
                        }
                        time += 5;

                        Thread.sleep(TIME_DELAY);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private final static String connectionString =
            "jdbc:jtds:sqlserver://appmaskdb.database.windows.net:1433;instance=SQLEXPRESS;DatabaseName=AppMaskDB;";
    private final static String userAdmin = "maskAdmin@appmaskdb";
    private final static String password = "Sdgp12345678";
    // Declare the JDBC objects.
    private Connection con = null;
    private Statement stmt = null;
    private ResultSet rs = null;

    private void updateCloud() {
        updateCloud = new Runnable() {
            @Override
            public void run() {
                try {
                    // Establish the connection.
                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    con = DriverManager.getConnection(connectionString, userAdmin, password);

                    // Create and execute an SQL statement that returns some data.
                    String SQL = "INSERT INTO daily (userName, gasType, hour, level) " +
                            "VALUES ('sk','C',12,7)";
                    stmt = con.createStatement();
                    rs = stmt.executeQuery(SQL);
                } catch (Exception e) {
                    e.printStackTrace();
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
            }
        };
    }

    private void getDateFromCloud(final String query) {
        Runnable getData = new Runnable() {
            @Override
            public void run() {
                try {
                    // Establish the connection.
                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    con = DriverManager.getConnection(connectionString, userAdmin, password);

                    // Create and execute an SQL statement that returns some data.
                    stmt = con.createStatement();
                    rs = stmt.executeQuery(query);

                    LineGraphSeries<DataPoint> quaries = new LineGraphSeries<>();

                    while (rs.next()) {
                        String day = rs.getString("day");
                        String level = rs.getString("level");
                        //showToast(day);
                        //showToast(level);
                        quaries.appendData(new DataPoint(Double.parseDouble(day), Double.parseDouble(level)), true, 40);
                    }

                    graph.addSeries(quaries);
                } catch (Exception e) {
                    e.printStackTrace();
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
            }
        };
        executor.execute(getData);
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

    public class UpdateDB extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
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
