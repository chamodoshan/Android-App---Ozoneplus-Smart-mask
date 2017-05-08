package com.sk.ozoneplus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class FirstActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
    }

    public void login() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void signup() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
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
