package com.pro.nyangcrush;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.os.Bundle;

import com.pro.nyangcrush.databinding.ActivityGameBinding;
import com.pro.nyangcrush.databinding.ActivityMainBinding;

public class GameActivity extends Activity {

    ActivityGameBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_game);
        binding.setActivity(this);


    }
}
