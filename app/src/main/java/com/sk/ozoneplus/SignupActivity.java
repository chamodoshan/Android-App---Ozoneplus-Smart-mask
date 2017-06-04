package com.sk.ozoneplus;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.TextView;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";
    private IntegrateCloud cloud;
    //TODO modify progress dialog
    private ProgressDialog progressDialog;

    @BindView(R.id.f_name) EditText _fnameText;
    @BindView(R.id.l_name) EditText _lnameText;
    @BindView(R.id.username_txt) EditText _usernameText;
    @BindView(R.id.email) EditText _emailText;
    @BindView(R.id.input_password) EditText _passwordText;
    @BindView(R.id.btn_signup) Button _signupButton;
    @BindView(R.id.date_picker) EditText _datePickerText;
    @BindView(R.id.link_login) TextView _loginLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                finish();
            }
        });
        addListenerOnButton();
    }

    @OnClick(R.id.date_picker)
    public void addListenerOnButton() {
        final Calendar calendar = Calendar.getInstance();
        int yy = calendar.get(Calendar.YEAR);
        int mm = calendar.get(Calendar.MONTH);
        int dd = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePicker = new DatePickerDialog(SignupActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String date = String.valueOf(year) + "-" + String.valueOf(monthOfYear)
                        + "-" + String.valueOf(dayOfMonth);
                _datePickerText.setText(date);
            }
        }, yy, mm, dd);
        datePicker.show();
    }

    @OnClick(R.id.btn_signup)
    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        _signupButton.setEnabled(false);

        progressDialog = new ProgressDialog(SignupActivity.this, R.style.AppTheme_NoActionBar);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        String fname = _fnameText.getText().toString();
        String lname = _lnameText.getText().toString();
        String username = _usernameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();
        String dob = _datePickerText.getText().toString();

        cloud = new IntegrateCloud(fname, lname, username, email, password, dob);
        cloud.execute();
    }

    public void onSignupSuccess() {
        _signupButton.setEnabled(true);
        setResult(RESULT_OK, null);
        //finish();
    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String fname = _fnameText.getText().toString();
        String lname = _lnameText.getText().toString();
        String username = _usernameText.getText().toString();
        String email = _emailText.getText().toString();
        String dob = _datePickerText.getText().toString();
        String password = _passwordText.getText().toString();

        if (fname.isEmpty() || fname.length() < 3) {
            _fnameText.setError("at least 3 characters");
            valid = false;
        } else {
            _fnameText.setError(null);
        }

        if (lname.isEmpty() || lname.length() < 3) {
            _lnameText.setError("at least 3 characters");
            valid = false;
        } else {
            _lnameText.setError(null);
        }

        if (username.isEmpty() || username.length() < 3) {
            _usernameText.setError("at least 3 characters");
            valid = false;
        } else {
            _usernameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if (dob.isEmpty()) {
            _datePickerText.setError("Date of birth is empty");
            valid = false;
        } else {
            _datePickerText.setError(null);
        }

        return valid;
    }

    private class IntegrateCloud extends AsyncTask<Void, Void, Boolean> {

        private final String fname, lname, user_username, email, user_password, dob;

        private final static String connectionString =
                "jdbc:jtds:sqlserver://appmaskdb.database.windows.net:1433;instance=SQLEXPRESS;DatabaseName=AppMaskDB;";
        private final static String userAdmin = "maskAdmin@appmaskdb";
        private final static String password = "Sdgp12345678";

        // Declare the JDBC objects.
        private Connection con = null;
        private Statement stmt = null;

        IntegrateCloud(String fname, String lname, String user_username, String email, String user_password, String dob) {
            this.fname = fname;
            this.lname = lname;
            this.user_username = user_username;
            this.email = email;
            this.user_password = user_password;
            this.dob = dob;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                // Establish the connection.
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                con = DriverManager.getConnection(connectionString, userAdmin, password);

                String SQL = "INSERT INTO [user] (userName, password, email, firstName, lastName, dob)" +
                        "VALUES ('" + user_username + "','" + user_password + "','" + email + "','"
                        + fname + "','" + lname + "','" + dob + "')";

                stmt = con.createStatement();
                if (stmt.executeUpdate(SQL) == 1) return true;

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
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
            if (result) onSignupSuccess();
            else onSignupFailed();
            progressDialog.dismiss();
            cloud = null;
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
    }
}
