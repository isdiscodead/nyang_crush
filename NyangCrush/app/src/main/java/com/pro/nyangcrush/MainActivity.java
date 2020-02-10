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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
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
     //배경음
     private MediaPlayer mediaPlayer, mediaPlayer2;

     // 다이얼로그 내부 버튼
    Button btn_close, btn_logout;

    // 유저 방울 개수
    int user_bell;
    final static int MAX_BELL = 5;

    //효과음
    SoundPool soundPool;
    private boolean effectSound;
    private int btnClick1;

    // 방울 배열
    ImageView[] bells = new ImageView[MAX_BELL];

    // 어플리케이션 종료 및 시작 시간 ( 추가된 방울 계산하기 위함 )
    Long stop_time;
    Long start_time;
    int wait_time;

    // 도움말 다이얼로그 애니메이션
    private Animation helpAnim;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setActivity(this);

        // SharedPreference 초기화
        pref = getSharedPreferences("SHARE", MODE_PRIVATE );

        //효과음 사운드풀 초기화
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            soundPool = new SoundPool.Builder().setMaxStreams(2).build();
        }else{
            soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 1);
        }

        btnClick1 = soundPool.load(this, R.raw.can, 1);

        /* 도움말 버튼 눌렀을 때 다이얼로그 생성 */
        binding.btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundPool.play(btnClick1, 1,1, 1,0,1);
                dialog = new Dialog( MainActivity.this );

                // 다이얼로그 타이틀 삭제
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                // 배경 프레임 삭제
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                // 다이얼로그 커스텀 애니메이션 적용
                dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;

                dialog.setContentView( R.layout.diag_howtp );

                btn_close = dialog.findViewById(R.id.btn_close);
                btn_close.setOnClickListener( dialClick );

                dialog.show();

                // 애니메이션 개체들
                final ImageView foot = dialog.findViewById(R.id.nyang_foot);
                final ImageView block1 = dialog.findViewById(R.id.block1);
                final ImageView block2 = dialog.findViewById(R.id.block4);
                final ImageView block3 = dialog.findViewById(R.id.block3);
                final ImageView block4 = dialog.findViewById(R.id.block2);

                final Handler handler = new Handler();

                // 도움말 애니메이션 반복 쓰레드
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while( dialog.isShowing() ) {
                            try {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 고양이 발 움직임
                                        helpAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.finger_anim);
                                        helpAnim.setFillAfter(true);    // 움직인 상태를 계속 유지
                                        foot.startAnimation(helpAnim);

                                        // 두 개의 블록 스왑
                                        helpAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.help_dialog_dust_anim2);
                                        helpAnim.setFillAfter(true);
                                        block4.startAnimation(helpAnim);
                                        helpAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.help_dialog_dust_anim);
                                        helpAnim.setFillAfter(true);
                                        block3.startAnimation(helpAnim);

                                        // 맞춰진 블록 삭제
                                        helpAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.remove_nyang);
                                        helpAnim.setFillAfter(true);
                                        helpAnim.setStartOffset(2000);
                                        helpAnim.setDuration(500);
                                        block1.startAnimation(helpAnim);
                                        block2.startAnimation(helpAnim);

                                    }
                                });
                                Thread.sleep(4000); // 4초 간격으로 재실행

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });

        /* 설정 버튼 눌렀을 때 다이얼로그 생성 */
        binding.btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundPool.play(btnClick1, 1,1, 1,0,1);
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
                soundPool.play(btnClick1, 1,1, 1,0,1);
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
                soundPool.play(btnClick1, 1,1, 1,0,1);

                pref = getSharedPreferences("SHARE", MODE_PRIVATE);
                user_bell = pref.getInt("bell", 5);

                // 남은 방울이 없다면 return
                if ( user_bell == 0 ) {
                    Toast.makeText(MainActivity.this, "남은 방울이 없다냥 !", Toast.LENGTH_SHORT).show();
                    return;
                }

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

        /* 벨 핸들러 */
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
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putInt("bell", user_bell);
                    edit.commit();
                    fill_bells();
                    wait_time = 1800;
                } else if ( wait_time == 0 && user_bell == 4 ) {
                    // 이제 다 찬 경우 ( 더이상 충전 x )
                    bellHandler.removeMessages(0);  // 핸들러 삭제
                    user_bell ++;
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putInt("bell", user_bell);
                    edit.commit();
                    fill_bells();
                }


            }
        };

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
        for( int i = 0 ; i < MAX_BELL; i++ ) {
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
    @SuppressLint("HandlerLeak")
    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.stop();
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
        user_bell = pref.getInt("bell", 5);
        edit.putInt("bell", user_bell);
        edit.putLong("time", System.currentTimeMillis() - ( 1800 - wait_time) * 1000 ); // 종료 시간 기록
        edit.commit();
        bellHandler.removeMessages(0);
        Log.i("bell", "onStop");
        Log.i("bell", "onStop: " + user_bell);
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onResume() {
        super.onResume();

        // 저장된 방울 개수 가져옴 ( 기본값 5 )
        user_bell = pref.getInt("bell",  0);

        // 저장된 종료 시간 가져오고, 시작 시간도 구한다.
        stop_time = pref.getLong("time", System.currentTimeMillis() );
        start_time = System.currentTimeMillis();

        // 만약 현재 저장되어있는 방울이 5개 미만이라면
        // 지난 시간을 계산하여 방울을 추가해준다.
        if ( user_bell < MAX_BELL ) {
            // 개당 30분 대기
            // wait_time = 60 * 30;
            int plus_bell =  (int)(( start_time - stop_time ) / 1800000 );
            // 핸들러에서 사용할 wait_time 설정
            wait_time = 1800 - (int)(( start_time - stop_time ) / 1000 ) ;
            if ( wait_time <= 0 ) {
                wait_time *= -1;
            }
            user_bell += plus_bell;
            if ( user_bell > MAX_BELL ) {
                user_bell = MAX_BELL;
            }
        }

        // 방울 채우기
        fill_bells();

        // 방울이 5개 미만이라면 wait_time 을 주고 handler 를 통해 계속해서 1초에 한 번씩 시간을 증가시킨다.
        if ( user_bell < MAX_BELL ) {
            bellHandler.sendEmptyMessage(0);
        }
    }


    // 다이어그램 온클릭 리스너
    View.OnClickListener dialClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch ( view.getId() ){
                case R.id.btn_close :
                    soundPool.play(btnClick1, 1,1, 1,0,1);
                    dialog.dismiss();
                    break;
                case R.id.btn_logout :
                    Toast.makeText(MainActivity.this, "로그아웃", Toast.LENGTH_SHORT).show();
                    soundPool.play(btnClick1, 1,1, 1,0,1);
                    break;

            }
        }
    };
}
