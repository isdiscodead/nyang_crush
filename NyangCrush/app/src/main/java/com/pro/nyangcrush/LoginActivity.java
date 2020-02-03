package com.pro.nyangcrush;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.pro.nyangcrush.databinding.ActivityLoginBinding;
import com.pro.nyangcrush.databinding.ActivityMainBinding;

public class LoginActivity extends Activity {

    ActivityLoginBinding binding;

    Button btn_logIn, btn_join;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        binding.setActivity(this);

        btn_logIn = binding.btnLogIn;
        btn_join = binding.btnJoin;

        btn_logIn.setOnClickListener( click );

    } // onCreate()

    View.OnClickListener click = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()){

                case R.id.btn_logIn:
                    /* 임시로 */
                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(i);
                    break;

                case R.id.btn_join:
                    break;

            } // switch

        }
    };

}
