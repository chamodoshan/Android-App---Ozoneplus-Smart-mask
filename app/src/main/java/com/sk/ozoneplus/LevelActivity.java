package com.sk.ozoneplus;

import android.app.Fragment;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.sk.ozoneplus.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class LevelActivity extends Fragment {
    private AppCompatActivity app;
    private CloudIntegrator cloud;
    private String user;

    private final static int DAY = 0;
    private final static int MONTH = 1;
    private final static int YEAR = 2;

    private final static int N = 3;
    private final static int C = 4;
    private final static int M = 5;

    private GraphView graph;
    private LineGraphSeries<DataPoint> nSeries, mSeries, cSeries;

    private final static String connectionString =
            "jdbc:jtds:sqlserver://appmaskdb.database.windows.net:1433;" +
                    "instance=SQLEXPRESS;DatabaseName=AppMaskDB;";
    private final static String userAdmin = "maskAdmin@appmaskdb";
    private final static String password = "Sdgp12345678";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_level, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        app = (AppCompatActivity) getActivity();
        user = new Bundle().getString("username");

        initializeGraph();

        /*Toolbar toolbar = (Toolbar) app.findViewById(R.id.toolbar);
        app.setSupportActionBar(toolbar);*/

        FloatingActionButton fab = (FloatingActionButton) app.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cloud = (CloudIntegrator) new CloudIntegrator().execute(MONTH);
                Snackbar.make(view, "SQL Passed", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        TabLayout tabLayout = (TabLayout) getActivity().findViewById(R.id.levels_tabs);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                cloud = new CloudIntegrator();
                switch (tab.getPosition()) {
                    case DAY:
                        break;
                    case MONTH:
                        break;
                    case YEAR:
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
    }

    /*@Override
    public void onCreateView(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }*/

    public void initializeGraph() {
        //TODO create SQL Lite database . If already exists update it else create new

        graph = (GraphView) getActivity().findViewById(R.id.level_graph);

        nSeries = new LineGraphSeries<>();
        nSeries.setAnimated(true);
        nSeries.setThickness(5);
        nSeries.setColor(Color.GREEN);

        mSeries = new LineGraphSeries<>();
        mSeries.setAnimated(true);
        mSeries.setThickness(5);
        mSeries.setColor(Color.RED);

        cSeries = new LineGraphSeries<>();
        cSeries.setAnimated(true);
        cSeries.setThickness(5);
        cSeries.setColor(Color.YELLOW);

        graph.addSeries(nSeries);
        graph.addSeries(mSeries);
        graph.addSeries(cSeries);

        graph.getViewport().setXAxisBoundsManual(false);
        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.getViewport().setScrollableY(true); // enables vertical scrolling
        graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        graph.getViewport().setScalableY(true); // enables vertical zooming and scrolling
        //graph.getGridLabelRenderer().setHorizontalAxisTitle("Time");

        /*StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        staticLabelsFormatter.setHorizontalLabels(label);
        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);*/
        /*graph.setBackgroundColor(Color.RED);*/
    }

    double graphHoriLabel = 0;

    public void updateGraph(int gasType, double val) {
        switch (gasType) {
            case N:
                appendData(nSeries, graphHoriLabel, val);
                //nSeries.appendData(new DataPoint(graphHoriLabel, n), true, 40);
                break;
            case C:
                appendData(cSeries, graphHoriLabel, val);
                //mSeries.appendData(new DataPoint(graphHoriLabel, m), true, 40);
                break;
            case M:
                appendData(mSeries, graphHoriLabel, val);
                //cSeries.appendData(new DataPoint(graphHoriLabel, c), true, 40);
                break;
        }
    }

    public void appendData(LineGraphSeries graphSeries, double horiLabel, double val) {
        graphSeries.appendData(new DataPoint(horiLabel, val), true, 40);
    }

    private class CloudIntegrator extends AsyncTask<Integer, String, ResultSet> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            graphHoriLabel = 0;

            nSeries = new LineGraphSeries<>();
            nSeries.setAnimated(true);
            nSeries.setThickness(5);
            nSeries.setColor(Color.GREEN);

            mSeries = new LineGraphSeries<>();
            mSeries.setAnimated(true);
            mSeries.setThickness(5);
            mSeries.setColor(Color.RED);

            cSeries = new LineGraphSeries<>();
            cSeries.setAnimated(true);
            cSeries.setThickness(5);
            cSeries.setColor(Color.YELLOW);
        }

        @Override
        protected ResultSet doInBackground(Integer... params) {
            Connection con = null;
            Statement stmt = null;
            ResultSet rs = null;

            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                con = DriverManager.getConnection(connectionString, userAdmin, password);

                String SQL = null;

                switch (params[0]) {
                    case DAY:
                        SQL = "SELECT * FROM daily WHERE userName = 'sk'";
                        break;
                    case MONTH:
                        SQL = "SELECT * FROM monthly WHERE userName = 'sk'";
                        break;
                    case YEAR:
                        SQL = "SELECT * FROM yearly WHERE userName = 'sk'";
                        break;
                }

                System.out.println("[SQL PREPARED]");

                stmt = con.createStatement();
                rs = stmt.executeQuery(SQL);

                /*while (rs.next()) {
                    System.out.println(rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3));
                    //responseHandler.obtainMessage(SERVER_TIME, rs.getString(1)).sendToTarget();
                }*/

                return rs;

            } catch (SQLException e) {
                e.printStackTrace();
                //TODO add exception to responseHandler
                //responseHandler.sendEmptyMessage(NO_INTERNET_CONNECTION);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (rs != null) try {
                    rs.close();
                } catch (Exception ignored) {
                }
                if (stmt != null) try {
                    stmt.close();
                } catch (Exception ignored) {
                }
                if (con != null) try {
                    con.close();
                } catch (Exception ignored) {
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(ResultSet s) {
            //if(s == null) throw new NullPointerException();
            try {
                if (s.isClosed()) throw new NullPointerException();
                super.onPostExecute(s);
                //updateGraph(s);

                int gasType;
                while (s.next()) {
                    switch (s.getString("gasType")) {
                        case "N":
                            gasType = N;
                            break;
                        case "C":
                            gasType = C;
                            break;
                        case "M":
                            gasType = M;
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    updateGraph(gasType, s.getDouble("level"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            cloud = null;
        }
    }
}
