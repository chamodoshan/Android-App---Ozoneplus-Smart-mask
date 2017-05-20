package com.sk.ozoneplus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void login() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void signup() {
        startActivity(new Intent(this, SignupActivity.class));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.signup:
                signup();
                break;
            case R.id.login:
                login();
                break;
        }
    }
}
