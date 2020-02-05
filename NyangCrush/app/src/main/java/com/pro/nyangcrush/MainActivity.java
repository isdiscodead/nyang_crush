package com.pro.nyangcrush;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.pro.nyangcrush.databinding.ActivityMainBinding;

import java.util.Objects;

public class MainActivity extends Activity {

     SharedPreferences pref;
     Handler bellHandler;

     ActivityMainBinding binding;
     Dialog dialog;
     private MediaPlayer mediaPlayer, mediaPlayer2;

     // 다이얼로그 내부 버튼
    Button btn_close, btn_logout;

    // 유저 방울 개수
    int user_bell;
    final static int MAX_BELL = 5;

    // 방울 배열
    ImageView[] bells = new ImageView[MAX_BELL];

    // 어플리케이션 종료 및 시작 시간 ( 추가된 방울 계산하기 위함 )
    Long stop_time;
    Long start_time;
    int wait_time;

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

    // 방울 채우기 메서드
    public void fill_bells() {
        // bell 배열 채우기
        bells[0] = binding.bell1;
        bells[1] = binding.bell2;
        bells[2] = binding.bell3;
        bells[3] = binding.bell4;
        bells[4] = binding.bell5;

        // 방울 개수만큼 fill_bell 로, 나머지는 empty
        for( int i = 0 ; i < 5; i++ ) {
            if ( user_bell < i+1 ) {
                bells[i].setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.bell_empty));
            } else {
                bells[i].setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.bell_fill));
            }
        }
    }

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

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor edit = pref.edit();
        edit.putInt("bell", user_bell);
        edit.putLong("time", System.currentTimeMillis() - ( 1800 - wait_time) * 1000 ); // 종료 시간 기록
        edit.commit();
        bellHandler.removeMessages(0);
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onResume() {
        super.onResume();

        // SharedPreference 초기화
        pref = getSharedPreferences("SHARE", MODE_PRIVATE );

        // 저장된 방울 개수 가져옴 ( 기본값 5 )
        user_bell = pref.getInt("bell",  0 );

        // 저장된 종료 시간 가져오고, 시작 시간도 구한다.
        stop_time = pref.getLong("time", System.currentTimeMillis() );
        start_time = System.currentTimeMillis();

        // 만약 현재 저장되어있는 방울이 5개 미만이라면
        // 지난 시간을 계산하여 방울을 추가해준다.
        if ( user_bell <= MAX_BELL ) {
            // 개당 30분 대기
            // wait_time = 60 * 30;
            int plus_bell =  (int)(( start_time - stop_time ) / 1800000 );
            // 핸들러에서 사용할 wait_time 설정
            wait_time = 1800 - (int)(( start_time - stop_time ) / 1000 ) ;
/*            if ( wait_time <= 0 ) {
                wait_time = 1800;
            }*/
            user_bell += plus_bell;
            if ( user_bell > MAX_BELL ) {
                user_bell = MAX_BELL;
            }
        }

        // 방울 채우기
        fill_bells();

        // 방울이 5개 미만이라면 wait_time 을 주고 handler 를 통해 계속해서 1초에 한 번씩 시간을 증가시킨다.
        if ( user_bell < 5 ) {
            bellHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    bellHandler.sendEmptyMessageDelayed(0, 1000);
                    wait_time --;

                    // 분, 초로 분할
                    int minute = wait_time / 60 ;
                    int sec = wait_time % 60 ;

                    binding.bellTime.setText(String.format("%02d:%02d", minute, sec));

                    if ( wait_time == 0 && user_bell < 4 ) {
                        // 아직 더 채워야 하는 경우
                        user_bell ++;
                        fill_bells();
                        wait_time = 1800;
                    } else if ( wait_time == 0 && user_bell == 4 ) {
                        // 이제 다 찬 경우
                        bellHandler.removeMessages(0);
                        user_bell ++;
                        fill_bells();
                    }
                }
            };

            bellHandler.sendEmptyMessage(0);
        }
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
