package com.pro.nyangcrush;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pro.nyangcrush.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import hotchemi.android.rate.AppRate;
import hotchemi.android.rate.OnClickButtonListener;

public class MainActivity extends Activity {

    SharedPreferences pref;
    Handler bellHandler;
    ActivityMainBinding binding;
    Dialog dialog;

    // 구글 로그인 클라이언트
    private GoogleSignInClient mGoogleSignInClient;

    // 파이어베이스 데이터베이스
    private FirebaseAuth mAuth; // 파이어베이스 인증
    private DatabaseReference mDatabase;
    private FirebaseDatabase sDatabase;

    // 어댑터, 랭킹 리스트뷰 관련
    private ArrayAdapter adapter;
    ListView ranking;
    ArrayList<User> user_arr = new ArrayList<User>();
    boolean flag;

    // 다이얼로그 내부 버튼
    Button btn_close, btn_logout;

    // 유저 방울 개수
    int user_bell;
    final static int MAX_BELL = 5;

    //배경음
    private MediaPlayer mediaPlayer1, mediaPlayer2;

    // 효과음
    SoundPool soundPool;
    private boolean effectSound = true;
    private boolean backgroundSound = true;
    private int btnClick1;

    // 배경음이랑 효과음 조절 바
    SeekBar seek_back_volume;
    AudioManager audioManager;

    // 배경음
    int bMax;
    int bCurrentVolume;

    private void loadState(){
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,pref.getInt("seek_back_volume",bMax),0);
    }

    private  void saveState(){
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("seek_back_volume", seek_back_volume.getProgress());
        editor.commit();
    }

    private void initPref(){
        pref=getSharedPreferences("SHARE",MODE_PRIVATE);
    }

    // 방울 배열
    ImageView[] bells = new ImageView[MAX_BELL];

    // 어플리케이션 종료 및 시작 시간 ( 추가된 방울 계산하기 위함 )
    Long stop_time;
    Long start_time;
    int wait_time;

    // 도움말 다이얼로그 애니메이션
    private Animation helpAnim;

    /**
     *  <<<< onCreate() 여기서 시작 >>>>
     * @param savedInstanceState
     */
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // 오디오 매니저 관련 변수 초기화
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        bMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);  // 최고 음량
        bCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);   // 기본

        // 기본 seekbar
        initPref();
        loadState();

        // 앱 평점
        AppRate.with(this)
                .setInstallDays(0) // default 10, 0 means install day.
                .setLaunchTimes(2) // 앱 나갔다가 다시 들어오는 수. /default=10
                .setRemindInterval(2) // default 1
                .setShowLaterButton(true) // default true
                .setDebug(false) // default false
                .setOnClickButtonListener(new OnClickButtonListener() { // callback listener.
                    @Override
                    public void onClickButton(int which) {
                        Log.d(MainActivity.class.getName(), Integer.toString(which));
                    }
                })
                .monitor();

        // Show a dialog if meets conditions
        AppRate.showRateDialogIfMeetsConditions(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setActivity(this);

        // SharedPreference 초기화
        pref = getSharedPreferences("SHARE", MODE_PRIVATE);

        // 데이터베이스 초기화
        sDatabase = FirebaseDatabase.getInstance(); // 데이터베이스 레퍼런스 객체
        mDatabase = sDatabase.getReference("users"); // 파이어베이스 DB 객체

        // 효과음 사운드풀 초기화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().setMaxStreams(2).build();
        } else {
            soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 1);
        }

        btnClick1 = soundPool.load(this, R.raw.can, 1);

        /* 도움말 버튼 눌렀을 때 다이얼로그 생성 */
        binding.btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (effectSound)
                    soundPool.play(btnClick1, 1, 1, 1, 0, 1);
                dialog = new Dialog(MainActivity.this);

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
                        while (dialog.isShowing()) {
                            try {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 고양이 발 움직임
                                        helpAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.finger_anim);
                                        helpAnim.setFillAfter(true);    // 움직인 상태를 계속 유지
                                        foot.startAnimation(helpAnim);

                                        // 두 개의 블록 스왑
                                        helpAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.help_dialog_nyang_anim2);
                                        helpAnim.setFillAfter(true);
                                        block4.startAnimation(helpAnim);
                                        helpAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.help_dialog_nyang_anim);
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

        // 구글 로그인 정보 ( 로그아웃 기능을 위해 필요함 )
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        /* 설정 버튼 눌렀을 때 다이얼로그 생성 */
        binding.btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (effectSound)
                    soundPool.play(btnClick1, 1, 1, 1, 0, 1);

                dialog = new Dialog( MainActivity.this );

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;

                dialog.setContentView( R.layout.diag_setting );

                btn_close = dialog.findViewById(R.id.btn_close);
                btn_logout = dialog.findViewById( R.id.btn_logout );
                btn_close.setOnClickListener( dialClick );
                btn_logout.setOnClickListener( dialClick );

                final Switch effectSoundSwitch = dialog.findViewById(R.id.setting_effect_sound);
                final Switch backgroundSoundSwitch = dialog.findViewById(R.id.setting_background_sound);

                // 음향조절 바
                seek_back_volume=dialog.findViewById(R.id.seek_back_volume);

                seek_back_volume.setMax(bMax);
                seek_back_volume.setProgress(pref.getInt("seek_back_volume", bMax));

                effectSoundSwitch.setChecked(effectSound);
                backgroundSoundSwitch.setChecked(backgroundSound);

                effectSoundSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    effectSound = !effectSound;

                    if (effectSound) {
                        soundPool.play(btnClick1, 1, 1, 1, 0, 1);
                    }
                    }
                });

                seek_back_volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress,0);
                        saveState();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                backgroundSoundSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (backgroundSound) {
                            mediaPlayer1.pause();
                            seek_back_volume.setProgress(0);
                        } else {
                            mediaPlayer1.start();
                        }

                        backgroundSound = !backgroundSound;

                       if (effectSound)
                            soundPool.play(btnClick1, 1, 1, 1, 0, 1);


                    }
                });

                dialog.show();

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {

                    }
                });
            }
        });

        /* 랭킹 버튼 눌렀을 때 다이얼로그 생성 */
        binding.btnRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (effectSound)
                    soundPool.play(btnClick1, 1, 1, 1, 0, 1);
                dialog = new Dialog( MainActivity.this );

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;

                dialog.setContentView( R.layout.diag_rank );

                btn_close = dialog.findViewById(R.id.btn_close);
                btn_close.setOnClickListener( dialClick );

                // 랭크 생성을 위한 DB 접근
                // 여기 표시하는 거는 limitToLast(2) 아까도 설명한거이지만 상위 2개만 보여줍니다.
                mDatabase.orderByChild("Score").limitToLast(50).addListenerForSingleValueEvent(  // 데이터 추가, 업데이트 메서드
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                Intent intent = getIntent();    // 로그인 하면서 넘어온 본인 정보 가져옴
                                String userid = intent.getExtras().getString("userid");
                                String user_s = dataSnapshot.child(userid).child("Score").getValue().toString();
                                binding.myScore.setText(user_s);    // 현재 유저 본인 최고 점수 ( DB 등록된 점수 )

                                // adapter.clear();
                                for ( DataSnapshot snapshot : dataSnapshot.getChildren() ) {

                                    userid = snapshot.child("id").getValue().toString();
                                    int score = Integer.parseInt(snapshot.child("Score").getValue().toString()); //점수
                                    String name = snapshot.child("name").getValue().toString(); //이름

                                    User user = new User();
                                    user.setUserId(userid);
                                    user.setName(name);
                                    user.setScore(score);

                                    user_arr.add(user);
                                }

                                userid = intent.getExtras().getString("userid");
                                adapter = new RankAdapter(MainActivity.this, R.layout.rank, user_arr, userid );
                                ranking = dialog.findViewById(R.id.ranking);
                                ranking.setAdapter(adapter);

                                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        adapter.clear();    // 데이터 중복 표시를 방지하기 위해 클리어
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.i("rank", "실패 : ");
                            }
                        });

                dialog.show();

            }
        });

        binding.btnGameStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (effectSound)
                    soundPool.play(btnClick1, 1, 1, 1, 0, 1);

                //LoginActivity에서 받은 사용자 Id값을  게임 실행시  GameActivity보냄
                Intent i = new Intent(MainActivity.this, GameActivity.class);
                Intent intent = getIntent();
                String userid = intent.getExtras().getString("userid");
                i.putExtra("userid",userid);

                user_bell = pref.getInt("bell", 5);

                // 남은 방울이 없다면 return
                if (user_bell == 0) {
                    Toast.makeText(MainActivity.this, "남은 방울이 없다냥 !", Toast.LENGTH_SHORT).show();
                    return;
                }

                startActivity(i);

                //mediaPlayer2.start();
                mediaPlayer1.stop();
            }
        });

        mediaPlayer1 = MediaPlayer.create(this, R.raw.backgroundmusic1);
        mediaPlayer2 = MediaPlayer.create(this, R.raw.backgroundmusic2);
        mediaPlayer1.setLooping(true);
        mediaPlayer2.setLooping(true);

        /* 벨 핸들러 */
        bellHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                bellHandler.sendEmptyMessageDelayed(0, 1000);
                wait_time--;

                // 분, 초로 분할
                int minute = wait_time / 60;
                int sec = wait_time % 60;

                binding.bellTime.setText(String.format("%02d:%02d", minute, sec));

                if (wait_time == 0 && user_bell < 4) {
                    // 아직 더 채워야 하는 경우
                    user_bell ++;
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putInt("bell", user_bell);
                    edit.apply();
                    fill_bells();
                    wait_time = 1800;
                } else if ( wait_time == 0 && user_bell == 4 ) {
                    // 이제 다 찬 경우 ( 더이상 충전 x )
                    bellHandler.removeMessages(0);  // 핸들러 삭제
                    user_bell++;
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putInt("bell", user_bell);
                    edit.apply();
                    fill_bells();
                }


            }
        };
    }// onCreate()

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
        //앱 재실행시 사용자 본인 점수 표시
        sDatabase = FirebaseDatabase.getInstance();
        mDatabase = sDatabase.getReference("users");

        mDatabase.orderByChild("users").addListenerForSingleValueEvent(
                new ValueEventListener () {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Intent intent = getIntent();
                        String userid = intent.getExtras().getString("userid");
                        String user_s = dataSnapshot.child(userid).child("Score").getValue().toString();
                        binding.myScore.setText(user_s);//현재 유저 본인점수
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("MainActivity", "실패 : ");
                    }
                });

        mediaPlayer1 = MediaPlayer.create(this, R.raw.backgroundmusic1);
        mediaPlayer1.setLooping(true);
        if(backgroundSound)
        mediaPlayer1.start();
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer1.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer1.stop();
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
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onResume() {
        super.onResume();

        // 저장된 방울 개수 가져옴 ( 기본값 5 )
        user_bell = pref.getInt("bell",  5);

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
    final View.OnClickListener dialClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch ( view.getId() ){
                case R.id.btn_close:
                    if (effectSound)
                        soundPool.play(btnClick1, 1, 1, 1, 0, 1);

//                  boolean effect = sf.getBoolean("effectSound", effectSound);
//                  boolean background = sf.getBoolean("background", backgroundSound);

                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("effect", effectSound);
                    editor.putBoolean("background", backgroundSound);
                    editor.apply();

                    dialog.dismiss();
                    break;

                case R.id.btn_logout:

                    // Firebase sign out
                    FirebaseAuth.getInstance().signOut();

                    // Google sign out
                    mGoogleSignInClient.signOut();

                    // 로그인 화면으로 전환
                    Intent i = new Intent( MainActivity.this, LoginActivity.class );
                    startActivity(i);
                    finish();

                    if (effectSound)
                        soundPool.play(btnClick1, 1, 1, 1, 0, 1);
                    break;

            }
        }
    };


}
