package com.sk.ozoneplus;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fabric.sdk.android.Fabric;

public class LoginActivity extends AppCompatActivity {
    @BindView(R.id.username) EditText _usernameTxt;
    @BindView(R.id.password) EditText _passwordTxt;
    @BindView(R.id.loginButton) Button _logBtn;
    @BindView(R.id.signupLink) TextView _signupLink;

    private UserLoginTask loginTask;
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    private AlertDialog.Builder builder;
    private ProgressDialog progressDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        Fabric.with(this, new Crashlytics());
        buildAlert();

        _signupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
            }
        });

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
    }

    @OnClick(R.id.loginButton)
    public void loginUser() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        _logBtn.setEnabled(false);

        progressDialog.show();

        String username = _usernameTxt.getText().toString();
        String password = _passwordTxt.getText().toString();

        loginTask = new UserLoginTask(username, password);
        loginTask.execute();
        // TODO: Implement your own authentication logic here.

        /*new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onLoginSuccess or onLoginFailed
                        onLoginSuccess();
                        // onLoginFailed();
                        progressDialog.dismiss();
                    }
                }, 3000);*/
    }

    private void buildAlert() {
        builder = new AlertDialog.Builder(LoginActivity.this);
    }

    private void showErrorDialog(String title, String message) {
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                /*.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })*/
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                // By default we just finish the Activity and log them in automatically
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess(String username) {
        _logBtn.setEnabled(true);
        progressDialog.dismiss();
        Crashlytics.setUserName(username);
        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra("username", username);
        //TODO add finish() at last or before last
        finish();
        startActivity(intent);
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
        _logBtn.setEnabled(true);
        progressDialog.dismiss();
    }

    public boolean validate() {
        boolean valid = true;

        String username = _usernameTxt.getText().toString();
        String password = _passwordTxt.getText().toString();

        if (username.isEmpty() || username.length() < 1) {
            showErrorDialog("Invalid email address", "Entered email isn't a valid email address");
            valid = false;
        }

        if (password.isEmpty() || password.length() < 3 || password.length() > 10) {
            showErrorDialog("Invalid password", "Enter between 4 and 10 alphanumeric characters");
            valid = false;
        }

        return valid;
    }

    /*public void next(String username) {
        Crashlytics.setUserName(username);
        progressDialog.dismiss();

        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }*/

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
        protected Boolean doInBackground(@NonNull String... params) {

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
                }

            } catch (SQLException e) {
                e.printStackTrace();
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

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            loginTask = null;

            if (result) {
                //next(username);
                onLoginSuccess(username);
            } else {
                showErrorDialog("Username or Password doesn't match"
                        , "Entered username and password doesn't match");
                onLoginFailed();
            }
        }
    }
}
