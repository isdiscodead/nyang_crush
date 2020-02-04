package com.pro.nyangcrush;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.pro.nyangcrush.databinding.ActivityMainBinding;

import java.util.Objects;

public class MainActivity extends Activity {

     ActivityMainBinding binding;
     Dialog dialog;
     private MediaPlayer mediaPlayer, mediaPlayer2;

     // 다이얼로그 내부 버튼
    Button btn_close, btn_logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setActivity(this);

        /* 도움말 버튼 눌렀을 때 다이얼로그 생성 */
        binding.btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog = new Dialog( MainActivity.this );

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;

                dialog.setContentView( R.layout.diag_howtp );

                btn_close = dialog.findViewById(R.id.btn_close);
                btn_close.setOnClickListener( dialClick );

                dialog.show();
            }
        });

        /* 설정 버튼 눌렀을 때 다이얼로그 생성 */
        binding.btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog = new Dialog( MainActivity.this );

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;

                dialog.setContentView( R.layout.diag_setting );

                btn_close = dialog.findViewById(R.id.btn_close);
                btn_logout = dialog.findViewById( R.id.btn_logout );
                btn_close.setOnClickListener( dialClick );
                btn_logout.setOnClickListener( dialClick );

                dialog.show();
            }
        });

        /* 랭킹 버튼 눌렀을 때 다이얼로그 생성 */
        binding.btnRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog = new Dialog( MainActivity.this );

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;

                dialog.setContentView( R.layout.diag_rank );

                btn_close = dialog.findViewById(R.id.btn_close);
                btn_close.setOnClickListener( dialClick );

                dialog.show();
            }
        });

        binding.btnGameStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, GameActivity.class);
                startActivity(i);

                //mediaPlayer2.start();
                mediaPlayer.stop();


            }
        });

        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic1);
        mediaPlayer2 = MediaPlayer.create(this, R.raw.backgroundmusic2);
        mediaPlayer.setLooping(true);
        mediaPlayer2.setLooping(true);

    }//onCreate()

    @Override
    protected void onStart() {
        super.onStart();

        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic1);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mediaPlayer.stop();
        mediaPlayer2.stop();
    }

    // 다이어그램 온클릭 리스너
    View.OnClickListener dialClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch ( view.getId() ){
                case R.id.btn_close :
                    dialog.dismiss();
                    break;
                case R.id.btn_logout :
                    Toast.makeText(MainActivity.this, "로그아웃", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };
}
