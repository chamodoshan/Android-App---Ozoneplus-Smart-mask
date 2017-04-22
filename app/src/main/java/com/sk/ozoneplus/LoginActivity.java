package com.sk.ozoneplus;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LoginActivity extends AppCompatActivity {

    private EditText txt_username, txt_password;
    private Button login;
    private UserLoginTask loginTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txt_username = (EditText) findViewById(R.id.username);
        txt_password = (EditText) findViewById(R.id.password);
    }

    public void login(View view) {
        String username = txt_username.getText().toString();
        String password = txt_password.getText().toString();

        if (username.length() < 1 || password.length() < 1) {

        } else {
            if (loginTask == null) {
                loginTask = new UserLoginTask(username, password);
                loginTask.execute();
            }
        }
    }

    //TODO complete showerror
    public void showError() {

    }

    public void show(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public void next(String username) {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }


    private class UserLoginTask extends AsyncTask<String, Void, Boolean> {

        private final String username, user_password;

        private final static String connectionString =
                "jdbc:jtds:sqlserver://appmaskdb.database.windows.net:1433;instance=SQLEXPRESS;DatabaseName=AppMaskDB;";
        private final static String userAdmin = "maskAdmin@appmaskdb";
        private final static String password = "Sdgp12345678";

        // Declare the JDBC objects.
        private Connection con = null;
        private Statement stmt = null;
        private ResultSet rs = null;

        public UserLoginTask(String username, String password) {
            this.username = username;
            this.user_password = password;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            try {
                // Establish the connection.
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                con = DriverManager.getConnection(connectionString, userAdmin, password);

                String SQL = "SELECT password FROM [user] WHERE userName = '" + username + "'";

                stmt = con.createStatement();
                rs = stmt.executeQuery(SQL);

                while (rs.next()) {
                    if (rs.getString("password").equals(user_password)) {
                        return true;
                    }
                    //responseHandler.obtainMessage(SERVER_TIME, rs.getString(1)).sendToTarget();
                }

            } catch (SQLException e) {
                e.printStackTrace();
                //TODO add exception to responseHandler
                //responseHandler.sendEmptyMessage(NO_INTERNET_CONNECTION);
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

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            loginTask = null;

            if (result) {
                //show("SUCCESS");
                next(username);
            } else {
                //TODO invoke showerror method
                //show("FAILED");
            }
        }
    }
}
