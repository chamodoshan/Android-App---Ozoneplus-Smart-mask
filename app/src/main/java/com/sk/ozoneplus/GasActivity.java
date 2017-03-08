package com.sk.ozoneplus;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabItem;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

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

public class GasActivity extends Fragment {
    //TODO set listeners for button if enables enabled button
    //TODO for now when gas button get clicked it only shows particular gas type change that to all in one graph change line colour
    private static final String TAG = "GasActivity";

    private static final int TIME_DELAY = 5000;

    private static final int SERVER_TIME = 0;
    private static final int UPDATE_GRAPH = 1;
    private static final int UPDATE_CLOUD = 2;
    private static final int GET_SERVER_TIME = 5;
    private static final int NO_INTERNET_CONNECTION = 11;

    // SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // server's MAC address
    private static String address = "84:EF:18:B7:8D:AF";

    private static Handler responseHandler, commandHandler;

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private OutputStream outStream;
    private InputStream inputStream;
    private BluetoothServerSocket mmServerSocket;

    private Button btnConnect;
    private GraphView graph;
    private Runnable updateGraph, updateCloud, blue;
    private LineGraphSeries<DataPoint> nSeries, mSeries, cSeries;
    private ArrayList<Double> nDataset, mDataset, cDataset;
    private Executor executor;
    private int xAnsis = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_gas, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        executor = new Queue();

        btnConnect = (Button) getActivity().findViewById(R.id.connectBT);
        /*btnN = (Button) getActivity().findViewById(R.id.n);
        btnC = (Button) getActivity().findViewById(R.id.co);
        btnM = (Button) getActivity().findViewById(R.id.m);*/

        TabLayout tabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                graph.removeAllSeries();
                switch (tab.getPosition()) {
                    case 1:
                        graph.addSeries(nSeries);
                        break;
                    case 2:
                        graph.addSeries(mSeries);
                        break;
                    case 3:
                        graph.addSeries(cSeries);
                        break;
                    case 4:

                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        //TODO change this onClick to XML file
        btnConnect.setOnClickListener(new View.OnClickListener() {
            boolean clicked = false;

            @Override
            public void onClick(View v) {
                if (!clicked) {
                    executor.execute(blue);
                    executor.execute(updateGraph);
                    //executor.execute(updateCloud);
                    clicked = true;
                }
            }
        });

/*        btnN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graph.removeAllSeries();
                graph.addSeries(nSeries);
            }
        });

        btnM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graph.removeAllSeries();
                graph.addSeries(mSeries);
            }
        });

        btnC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graph.removeAllSeries();
                graph.addSeries(cSeries);
            }
        });*/

        nDataset = new ArrayList<>();
        mDataset = new ArrayList<>();
        cDataset = new ArrayList<>();

        nSeries = new LineGraphSeries<>();
        mSeries = new LineGraphSeries<>();
        cSeries = new LineGraphSeries<>();

        //TODO create SQL Lite database . If already exists update it else create new

        graph = (GraphView) getActivity().findViewById(R.id.graph);
        graph.addSeries(nSeries);
        graph.getViewport().setXAxisBoundsManual(false);
        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.getViewport().setScrollableY(true); // enables vertical scrolling
        graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        graph.getViewport().setScalableY(true); // enables vertical zooming and scrolling
        /*graph.setBackgroundColor(Color.RED);*/
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private final static String connectionString =
            "jdbc:jtds:sqlserver://appmaskdb.database.windows.net:1433;instance=SQLEXPRESS;DatabaseName=AppMaskDB;";
    private final static String userAdmin = "maskAdmin@appmaskdb";
    private final static String password = "Sdgp12345678";
    // Declare the JDBC objects.
    private Connection con = null;
    private Statement stmt = null;
    private ResultSet rs = null;

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
                        Toast.makeText(getActivity().getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                        System.out.println("[RESULT] " + msg.obj.toString());

                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String formattedDate = df.format(c.getTime());

                        formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance());

                        if (!(compareTime(msg.obj.toString(), formattedDate))) {
                            //TODO Display error "Check the phone time and restart"
                        }
                        break;

                    case UPDATE_GRAPH:
                        byte[] readBuf = (byte[]) msg.obj;
                        strIncome = new String(readBuf, 0, msg.arg1);
                        Toast.makeText(getActivity().getApplicationContext(), strIncome, Toast.LENGTH_SHORT).show();
                        String[] a = strIncome.split("/");

                        nDataset.add(Double.parseDouble(a[0]));
                        mDataset.add(Double.parseDouble(a[1]));
                        cDataset.add(Double.parseDouble(a[2]));

                        xAnsis++;
                        break;

                    case UPDATE_CLOUD:
                        executor.execute(updateCloud);
                        System.out.println("[CLOUD HANDLER]");
                        break;

                    case NO_INTERNET_CONNECTION:
                        Snackbar.make(getView(), "Internet isn't connected", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        break;
                }
            }
        };

        updateCloud = new Runnable() {
            @Override
            public void run() {
                try {
                    // Establish the connection.
                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    con = DriverManager.getConnection(connectionString, userAdmin, password);

                    // Create and execute an SQL statement that returns some data.
                    String SQL = "INSERT INTO daily VALUES('OO','N',56,45)";
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

        blue = new Runnable() {
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
                    System.out.println("[BLUE TOOTH ENABLE EXCEPTION] " + e.getMessage());
                }

                // Set up a pointer to the remote node using it's address.
                BluetoothDevice device = btAdapter.getRemoteDevice(address);

                //  Two things are needed to make a connection:
                //  A MAC address, which we got above.
                //  A Service ID or UUID.  In this case we are using the
                //  UUID for SPP.
                try {
                    btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    System.out.append("Connection established\n");
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

                System.out.println("[BLUE HAS BEEN INITIALIZED]");

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

                        Thread.sleep(5000);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };


        updateGraph = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //TODO if data not found leave a space in graph
                        nSeries.appendData(new DataPoint(graph2LastXValue, getValue(nDataset)), true, 40);
                        mSeries.appendData(new DataPoint(graph2LastXValue, getValue(mDataset)), true, 40);
                        cSeries.appendData(new DataPoint(graph2LastXValue, getValue(cDataset)), true, 40);
                        y++;
                        //TODO move this to finally part
                        graph2LastXValue += 5;
                        Thread.sleep(TIME_DELAY);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("[ARRAY OUT OF BOUND]");
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

        commandHandler.sendEmptyMessage(GET_SERVER_TIME);
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
    private double graph2LastXValue = 0d;

    @Override
    public void onResume() {
        super.onResume();
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
    public class ConnectCloud extends AsyncTask<String, String, String> {
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

    public class Queue implements Executor {

        @Override
        public void execute(Runnable command) {
            new Thread(command).start();
            System.out.println("[QUEUE] HAS STARTED");
        }
    }
}
