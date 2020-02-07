package com.pro.nyangcrush;

import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pro.nyangcrush.databinding.ActivityGameBinding;

import java.util.ArrayList;
import java.util.Objects;

import static android.os.Build.*;
import static com.pro.nyangcrush.FillCompletedListener.*;
import static com.pro.nyangcrush.FillCompletedListener.*;
public class GameActivity extends Activity {

    ActivityGameBinding binding;
    Dialog dialog;

    // 일시정지 다이얼로그 내부의 버튼들
    Button btn_replay, btn_stop, btn_back, btn_close ;

    //게임 상태
    private static final int BEFORE_THE_GAME_START = 0;
    private static final int GAME_PAUSED = 1;
    private static final int GAME_TERMINATED = 2;
    private static final int GAME_PLAYING = 3;

    //배경음악
    MediaPlayer mediaPlayer1, mediaPlayer2, mediaPlayer3, mediaPlayer4;

    //효과음
    SoundPool soundPool;
    private int btnClick1;

    private int plateSize;
    private int division9; // plate를 9로 나눈 값
    private int gameStatus;
    private NyangImageView[][] nyangArray;
    private NyangPosition[][] nyangPositions;
    private boolean touchStatus; //true면 터치가 가능한 상태임

    //점수 부분
    private int userScore;
    private int combo; //콤보 점수
    private int cnt; //첫터치 반응 확인
    private int combotime; //콤보 유지시간

    //타이머부분
    private int timer; //게임 플레이 타임
    private int time; //게임진행 타임
    private boolean timerThreadContoller; //게임중지 / 게임진행 확인용

    //게임말 스왑시 필요한 두 게임블록의 좌표
    private int e1X;
    private int e1Y;
    private int e2X;
    private int e2Y;

    //view의 변화감지 리스너
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;

    //제스처 감지
    private GestureDetector detector;

    //핸들러
    private Handler handler;

    //게임말 스왑 완료 감지 콜백 스왑리스너
    SwapCompletedListener swapCompletedListener;

    //게임말 채우기 감지 콜백
    FillCompletedListener fillCompletedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_game);
        binding.setActivity(this);

        handler = new Handler();
        nyangArray = new NyangImageView[9][9]; //게임 판 9X9
        nyangPositions = new NyangPosition[9][9]; //게임말 9X9배치


        /* pause 버튼 눌렀을 때 다이얼로그 생성 */
        binding.btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundPool.play(btnClick1, 1,1, 1,0,1);
                dialog = new Dialog(GameActivity.this);

                dialog.setCancelable(false);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;

                dialog.setContentView(R.layout.diag_pause);

                btn_back = dialog.findViewById(R.id.btn_back);
                btn_stop = dialog.findViewById(R.id.btn_stop);
                btn_replay = dialog.findViewById(R.id.btn_replay);
                btn_close = dialog.findViewById(R.id.btn_close);

                dialog.show();

                btn_back.setOnClickListener(dialClick);
                btn_stop.setOnClickListener(dialClick);
                btn_replay.setOnClickListener(dialClick);
                btn_close.setOnClickListener(dialClick);

                //게임 중지
                if (gameStatus == GAME_PLAYING && touchStatus) {
                    pauseGame();

                }


            }
        });//btnPause.setOnClickListener

        /*//화면이 켜진상태유지
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //상단 틀 없애기*/


        //plate가 그려진 후 넓이와 높이를 구하기 위한 리스너
        mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //plate에 아이템을 9x9로 배치하기 위해 정확히 9로 나눠지는 수치를 계산
                plateSize = ( binding.gamePlate.getWidth() / 9) * 9;
                //plate 넓이, 높이 설정
                //ViewGroup : View의 부모 , view는 textview, editview, button, imageview등
                //자식객체밖에 못건듬
                ViewGroup.LayoutParams plateLayoutParams =  binding.gamePlate.getLayoutParams(); //game_plate
                plateLayoutParams.width = plateSize; //레이아웃 wid어th값 속성 지정
                plateLayoutParams.height = plateSize; //레이아웃 height값 속성 지정
                binding.gamePlate.setLayoutParams(plateLayoutParams); //레이아웃속성 변경 / 원래는 리니어

                //hideBar넓이, 높이 설정                      //레이아웃 속성객체 얻어옴
                ViewGroup.LayoutParams hideBarLayoutParams = binding.gameHideNyangBar.getLayoutParams(); //game_hide_Nyang_bar
                hideBarLayoutParams.width = plateSize;
                hideBarLayoutParams.height = plateSize / 9;
                binding.gameHideNyangBar.setLayoutParams(hideBarLayoutParams);

                //판의 크기를 설정한 후
                binding.gamePlate.post(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            //겹치는게 없을 때까지 판을 셋팅
                            setNyangArray();
                        } while (checkNyangArray());
                        basicSetting();
                        startGame();
                    }
                });

                //리스너 지우기
                removeOnGlobalLayoutListener( binding.gamePlate.getViewTreeObserver(), mGlobalLayoutListener);
            }
        };//mGlobalLayoutListener

        //plate 넓이 구하기 위한 리스너 등록
        binding.gamePlate.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);

        //터치 및 스와이프 인식 리스너
        detector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return false;
            }
            @Override
            public void onShowPress(MotionEvent motionEvent) {
            }
            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }
            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }
            @Override
            public void onLongPress(MotionEvent motionEvent) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float v1, float v2) {
                //게임말를 이동시키기 위해 스와이프를 한 경우 onFling이벤트가 발생
                //터치가 불가능한 상태일 경우 return false
                if(!touchStatus) {
//                    Log.i("dd", "touchStatus false");
                    return false;
                } else {
//                    Log.i("dd", "touchStatus true");
                    touchStatus = false;
                    int[] plateLocation = new int[2]; //위치를 받을 배열 설정

                    binding.gamePlate.getLocationOnScreen(plateLocation); //plate의 절대 좌표 가져오기
                    int plateX = plateLocation[0]; //plateX : 22
                    int plateY = plateLocation[1]; //plateY : 415

                    if(e1 == null || e2 == null) return false;  //nullpointer exception 방지

                    //e1 및 e2 이벤트 위치가 plate 바깥쪽일 경우 return false
                    if(e1.getX() >= plateX
                            && e1.getX() <= plateX + plateSize
                            && e1.getY() >= plateY
                            && e1.getY() <= plateY + plateSize
                            && e2.getX() >= plateX
                            && e2.getX() <= plateX + plateSize
                            && e2.getY() >= plateY
                            && e2.getY() <= plateY + plateSize) {

                        //처음 터치한 게임블록의 좌표
                        e1X = ((int)e1.getX() - plateX) / division9;  // 58 / 115 = 0
                        e1Y = ((int)e1.getY() - plateY) / division9; //993 / 115 = 8.xxx

                        //좌표값 첫 터치 로그 확인
//                        Log.i("dd"," e1.getX"+((int)e1.getX() - plateX));
//                        Log.i("dd"," e1.getY"+((int)e1.getY() - plateY));

                        //터치를 뗀 위치의 좌표
                        e2X = ((int)e2.getX() - plateX) / division9;
                        e2Y = ((int)e2.getY() - plateY) / division9;

//                        Log.i("dd","e1x : "+e1X + " / e1Y : "+e1Y +"/ e2X : "+e2X +"/ e2Y : "+e2Y);

                        //스왑할 게임블록의 좌표 조정
                        //두칸 이상의 범위를 드래그한 경우 좌표값을 한칸으로 조정해줌
                        e2X = Math.abs(e1X - e2X) >= 2 ? //Math.abs절대값
                                e1X > e2X ?
                                        e1X - 1 :
                                        e1X + 1
                                : e2X;
                        //x첫터치 좌표 2일때, 후 x좌표 3일경우
                        // 후 좌표값 e2X
                        //x첫터치 좌표 3일때, 후 x좌표 1일경우
                        //Math.abs(e1X - e2X) >= 2 | -> e1X - 1 = 2좌표 출

                        //x좌표식과 동일
                        e2Y = Math.abs(e1Y - e2Y) >= 2 ?
                                e1Y > e2Y ?
                                        e1Y - 1 :
                                        e1Y + 1
                                : e2Y;

                        //좌표 1 -> 2 && 1 -> 2 대각선드래그 // 1 == 1 & 2 == 2 제자리드래그
                        if((e1X != e2X && e1Y != e2Y) || (e1X == e2X && e1Y == e2Y)) {
//                            Log.i("잘못된 드래그 ->", e1Y+" -> "+e2Y+" || "+e1X+" -> "+e2X);
                            //잘못된 드래그 방지
                            //1. 대각선 드래그
                            //2. 제자리 드래그
                            touchStatus = true;
                            return false;
                        }

                        //스왑 효과음 재생
//                        if(effectSound)
//                            soundPool.play(swapSound, effectSoundVolume, effectSoundVolume,  1,  0,  1);

                        //스왑 완료시 호출할 콜백 등록
                        swapCompletedListener = new SwapCompletedListener();
                        SwapCompletedListener.SwapCallback swapCallback = new SwapCompletedListener.SwapCallback() {
                            @Override
                            public void onSwapComplete(boolean restore) {
                                if(restore) {
                                    //되돌리기의 경우
                                    touchStatus = true;
                                    return;
                                }

                                if(!checkNyangArray()) {
                                    //터트릴 블록 하나도 없을 경우 다시 스왑함으로써 원상태로 되돌림
                                    swapNyang(e1X, e2X, e1Y, e2Y, true); //좌표 1 , 2 , 6 , 6
                                    mediaPlayer4.start();

                                    //콤보 유지시간중 블록을 잘못 건드려 터트릴게 없을 경우 콤보 초기화
                                    combo = 0;
                                    binding.count.setText(""+combo);

                                } else {
                                    fillCompletedListener = new FillCompletedListener();
                                    FillCompletedListener.FillCallback fillCallback = new FillCompletedListener.FillCallback() {
                                        @Override
                                        public void onFillComplete() {
                                            if(gameStatus != GAME_PLAYING) return;

                                            if(!checkNyangArray()) {
                                                touchStatus = true;
                                                cnt = 0;
                                            } else {
                                                fillBlank();

                                            }
                                        }
                                    };
                                    //첫터치 3개이상 블록을 터트렸을때 반응
                                    cnt++; //

                                    //x x
                                    //x x
                                    //x x
                                    //위 같이 터치 후 2개 연속 터질경우 콤보 증가
                                    int sum = 0;
                                    for(int q = 0 ; q < nyangArray.length ; q++) {
                                        String row = "";
                                        for(int w = 0 ; w < nyangArray[q].length ; w++) {

                                            if(q >= 1 && w >= 1) {
                                                if (nyangArray[q - 1][w - 1] == nyangArray[q][w] ) {
                                                    sum++;
                                                    combo++;

                                                    //서로 매치되는부분
                                                    //x x x
                                                    //o o o
                                                    if(sum >= 2){
                                                        combo--;
                                                    }

                                                }

                                            }
                                        }
                                        binding.count.setText(""+combo);
                                        sum = 0;
                                    }

                                    fillCompletedListener.setFillCallback(fillCallback); //콜백 등록
                                    fillBlank();
                                }
                            }
                        };
                        swapCompletedListener.setSwapCallback(swapCallback);

                        //두 게임말 스왑
                        swapNyang(e1X, e2X, e1Y, e2Y, false);
                        return true;
                    } else {
                        touchStatus = true;
                        return false;
                    }
                }
            }
        });//GestureDetector

        //효과음 사운드풀 초기화
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            soundPool = new SoundPool.Builder().setMaxStreams(2).build();
        }else{
            soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 1);
        }

        btnClick1 = soundPool.load(this, R.raw.can, 1);

        mediaPlayer1 = MediaPlayer.create(this,R.raw.backgroundmusic1);
        mediaPlayer2 = MediaPlayer.create(this,R.raw.backgroundmusic2);
        mediaPlayer3 = MediaPlayer.create(this, R.raw.toy);
        mediaPlayer4 = MediaPlayer.create(this,R.raw.kitty);
        mediaPlayer1.setLooping(true);
        mediaPlayer2.setLooping(true);
        mediaPlayer3.setLooping(false);
        mediaPlayer4.setLooping(false);


        mediaPlayer2.start();
    }//onCreate

    /**
     * plate에 게임블록을 채워넣음
     */
    private void setNyangArray(){
        division9 = plateSize/9;
        for(int q = 0 ; q < nyangArray.length ; q++) {
            for(int w = 0 ; w < nyangArray[q].length ; w++) {
                if(nyangArray[q][w] == null) {
                    //게임블록 포지션만을 저장하는 배열
                    nyangPositions[q][w] = new NyangPosition((int)binding.gamePlate.getX() + (division9 * w),
                            (int)binding.gamePlate.getY() + (division9 * q));

                    //실제 게임블록이 저장되는 배열 배열판에 게임말이미지 등록
                    nyangArray[q][w] = new NyangImageView(GameActivity.this
                            , (int)binding.gamePlate.getX() + (division9 * w)
                            , (int)binding.gamePlate.getY() + (division9 * q)
                            , division9
                            , division9
                            , (int)(Math.random() * 6) + 1);


                    binding.layout.addView(nyangArray[q][w]);


                }
            }
        }
    }//setDustArray


    //게임말 스왑 애니메이션
    private void swapNyang(final int x1, final int x2, final int y1, final int y2, final boolean restore) {
        //restore가 true면 되돌리기 작업임
        //게임말1 : 사용자가 처음 터치한 블록
        //게임말2 : 사용자가 교환하려고 드래그한 자리에 있는 블록

        //게임말1 좌표 얻어오기
        final int nyang1X = (int)nyangPositions[y1][x1].getX();
        final int nyang1Y = (int)nyangPositions[y1][x1].getY();
//        Log.i("dd", "nyang1X"+ nyang1X+"// Y " +nyang1Y );


        //게임말2 좌표 얻어오기
        final int nyang2X = (int)nyangPositions[y2][x2].getX();
        final int nyang2Y = (int)nyangPositions[y2][x2].getY();

        //게임말1을 게임말2 쪽으로 이동
        TranslateAnimation translateAnimation1 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , x2 - x1
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , y2 - y1);
        translateAnimation1.setDuration(300);
        translateAnimation1.setFillEnabled(true);
        translateAnimation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                nyangArray[y2][x2].setX(nyang2X);
                nyangArray[y2][x2].setY(nyang2Y);

                //애니메이션이 끝난것을 리스너에게 알려주기위한 메소드
                swapCompletedListener.swapAnimationEnd(restore);
            }

            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        nyangArray[y1][x1].startAnimation(translateAnimation1);

        //게임말2를 게임말1 쪽으로 이동
        TranslateAnimation translateAnimation2 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , x1 - x2
                ,Animation.RELATIVE_TO_SELF , 0
                ,Animation.RELATIVE_TO_SELF , y1 - y2);
        translateAnimation2.setDuration(300);
        translateAnimation2.setFillEnabled(true);
        translateAnimation2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                nyangArray[y1][x1].setX(nyang1X);
                nyangArray[y1][x1].setY(nyang1Y);

                //애니메이션이 끝난것을 리스너에게 알려주기위한 메소드
                swapCompletedListener.swapAnimationEnd(restore);
            }

            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        nyangArray[y2][x2].startAnimation(translateAnimation2);

        //게임말 스왑
        NyangImageView tmpNyang = nyangArray[y2][x2];
        nyangArray[y2][x2] = nyangArray[y1][x1];
        nyangArray[y1][x1] = tmpNyang;
    }//swapAni


    /**
     * 블록 삭제 후 공백 채우는 메소드
     */

    private void fillBlank() {
        int totalNullCount = 0;

//        for(int q = 0 ; q < nyangArray.length ; q++) {
//            String row = "";
//            for(int w = 0 ; w < nyangArray[q].length ; w++) {
//                row += nyangArray[q][w] == null ? 1 : 0;
//                row += " ";
//            }
////            Log.i("dustArray "+q,"row"+ row);
//        }

        for(int q = 0 ; q < nyangArray.length ; q++) {
            final ArrayList<NyangImageView> newNyangList = new ArrayList<>();
            final ArrayList<NyangImageView> nyangList = new ArrayList<>();
            for(int w = 0 ; w < nyangArray.length ; w++) {
                nyangList.add(nyangArray[w][q]);
            }
            int nullCount = 0;

            for(int w = nyangArray.length - 1 ; w >= 0 ; w--) {
                if(nyangArray[w][q] == null) {
                    nullCount++;
                    totalNullCount++;

                    NyangImageView nyangImageView = new NyangImageView(GameActivity.this
                            , (int)binding.gameHideNyangBar.getX() + (division9 * q)
                            , (int)binding.gameHideNyangBar.getY()
                            , division9
                            , division9
                            , (int)(Math.random() * 6) + 1);
                    binding.layout.addView(nyangImageView);
                    binding.gameHideNyangBar.bringToFront();
                    newNyangList.add(nyangImageView);

                    if(w == 0) {
                        //첫번 째 칸이 0일 경우 plate를 채움
                        fillPlate(newNyangList, q);
                    }

                } else {
                    if(nullCount >= 1) {
                        final int x = q;
                        final int y = w;
                        final int newY = w + nullCount;

                        nyangArray[newY][x] = nyangList.get(y);
                        TranslateAnimation tranAnimation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF , 0
                                ,Animation.RELATIVE_TO_SELF , 0
                                ,Animation.RELATIVE_TO_SELF , 0
                                ,Animation.RELATIVE_TO_SELF , nullCount);
                        tranAnimation.setInterpolator(new AccelerateInterpolator());
                        tranAnimation.setDuration(200 + (nullCount * 100));
                        tranAnimation.setFillEnabled(true);
                        tranAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                //실행 순서가 보장 되지 않음
                                nyangArray[newY][x].setX(nyangPositions[newY][x].getX());
                                nyangArray[newY][x].setY(nyangPositions[newY][x].getY());

                                if(y == 0) {
                                    //블록들을 아래로 옮기고 난 후 공백을 채워넣음
                                    fillPlate(newNyangList, x);
                                }
                            }

                            @Override
                            public void onAnimationStart(Animation animation) {

                            }
                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        nyangArray[y][x].startAnimation(tranAnimation);
                    }
                }

            }
        }

        fillCompletedListener.setTotalNullCount(totalNullCount);
    }//fillblank


    private void fillPlate(final ArrayList<NyangImageView> newNyangList, final int x) {

        for(int q = 0 ; q < newNyangList.size() ; q++) {
            final int index = q;


            //생성되있는 블록들을 아래로 이동시키며 채워넣음
            nyangArray[newNyangList.size() - index - 1][x] = newNyangList.get(index);

            TranslateAnimation tranAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF , 0
                    ,Animation.RELATIVE_TO_SELF , 0
                    ,Animation.RELATIVE_TO_SELF , 0
                    ,Animation.RELATIVE_TO_SELF , newNyangList.size() - q);
            tranAnimation.setDuration(200 + ((newNyangList.size() - q) * 100));
            tranAnimation.setFillEnabled(true);
            tranAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    nyangArray[newNyangList.size() - index - 1][x].setX(nyangPositions[newNyangList.size() - index - 1][x].getX());
                    nyangArray[newNyangList.size() - index - 1][x].setY(nyangPositions[newNyangList.size() - index - 1][x].getY());

                    fillCompletedListener.fillComplete();

                }

                @Override
                public void onAnimationStart(Animation animation) {

                }
                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            newNyangList.get(q).startAnimation(tranAnimation);
        }
    }//fliiplate


    /**
     * 현재 plate에 동일한 모양의 블록이 일렬로 3개 이상 나열된 곳이 있는지 체크 후
     * 있다면, 나열된 블록 삭제 후 점수 획득 및 true 리턴
     * 없다면, false 리턴
     * @return
     */
    private boolean checkNyangArray() {
        final ArrayList<String> removeList = new ArrayList<>();
        boolean flag = false; //리턴할 변수

        for (int q = 0; q < nyangArray.length; q++) {
            for (int w = 0; w < nyangArray[q].length; w++) {
                int verticalMin = q - 2 < 0 ? 0 : q - 2;
                int verticalMax = q + 2 >= nyangArray.length ? nyangArray.length - 1 : q + 2;
                int horizontalMin = w - 2 < 0 ? 0 : w - 2;
                int horizontalMax = w + 2 >= nyangArray.length ? nyangArray.length - 1 : w + 2;

                /*Log.i("현재 좌표", q+", "+w);
                Log.i("verticalMin", ""+verticalMin);
                Log.i("verticalMax", ""+verticalMax);
                Log.i("horizontalMin", ""+horizontalMin);
                Log.i("horizontalMax", ""+horizontalMax);*/

                int count = 0;

                for (int e = verticalMin + 1; e <= verticalMax; e++) {
                    //세로 탐색
                    if (nyangArray[e - 1][w].getNyangType() == nyangArray[e][w].getNyangType()) {
                        count++;

                    } else {
                        count = 0;
                    }

                    if (count >= 2) {

                        /**
                         * 카운팅이 2 이상 된 경우 연속된 3개의 블록이 있다는 의미 이므로
                         * 현재 검사한 블록의 좌표(q, w)를 기준으로 인접한 같은 모양의 블록을 모두 삭제함
                         * 범위는 기준점(q, w)에서 부터 최대 2칸
                         */
                        flag = true;

                        removeList.add(q + "," + w);

                        for (int r = q + 1; r <= verticalMax; r++) {
                            if (nyangArray[q][w].getNyangType() == nyangArray[r][w].getNyangType()) {
                                if (binding.layout.getViewWidget(nyangArray[r][w]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_nyang);
                                    nyangArray[r][w].startAnimation(anim);
                                }

                                binding.layout.removeView(nyangArray[r][w]);
                                removeList.add(r + "," + w);

                            } else {
                                break;
                            }
                        }

                        for (int r = q - 1; r >= verticalMin; r--) {
                            if (nyangArray[q][w].getNyangType() == nyangArray[r][w].getNyangType()) {
                                if (binding.layout.getViewWidget(nyangArray[r][w]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_nyang);
                                    nyangArray[r][w].startAnimation(anim);
                                }

                                binding.layout.removeView(nyangArray[r][w]);
                                removeList.add(r + "," + w);
                            } else {
                                break;
                            }
                        }
                        break;
                    }
                }

                count = 0;
                for (int e = horizontalMin + 1; e <= horizontalMax; e++) {
                    //가로 탐색
                    if (nyangArray[q][e - 1].getNyangType() == nyangArray[q][e].getNyangType()) {
                        count++;
                    } else {
                        count = 0;
                    }

                    if (count >= 2) {
                        flag = true;

                        removeList.add(q + "," + w);
                        for (int r = w + 1; r <= horizontalMax; r++) {
                            if (nyangArray[q][w].getNyangType() == nyangArray[q][r].getNyangType()) {
                                if (binding.layout.getViewWidget(nyangArray[q][r]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_nyang);
                                    nyangArray[q][r].startAnimation(anim);
                                }

                                binding.layout.removeView(nyangArray[q][r]);
                                removeList.add(q + "," + r);
                            } else {
                                break;
                            }
                        }

                        for (int r = w - 1; r >= horizontalMin; r--) {
                            if (nyangArray[q][w].getNyangType() == nyangArray[q][r].getNyangType()) {
                                if (binding.layout.getViewWidget(nyangArray[q][r]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_nyang);
                                    nyangArray[q][r].startAnimation(anim);
                                }

                                binding.layout.removeView(nyangArray[q][r]);
                                removeList.add(q + "," + r);
                            } else {
                                break;
                            }
                        }
                        break;
                    }
                }

                if (count >= 2) {

                } else {

                }
            }
        }

        for (int q = 0; q < removeList.size(); q++) {
            //실제 nyangArray에서 블록 삭제
            int i = Integer.parseInt(removeList.get(q).split(",")[0]);
            int j = Integer.parseInt(removeList.get(q).split(",")[1]);
            nyangArray[i][j] = null;

        }

        if (flag && gameStatus == GAME_PLAYING) {

            //점수 갱신
            userScore += (removeList.size() * 10); //기본블록 점수 90
            timer += 1;

            //콤보유지시간 5초
            combotime = 5;

            //5콤보이상 콤보유지시간 3초
            if (combo >= 5) {
                combotime = 3;
            }

            //10콤보이상 유지 2초;
            else if (combo >= 10) {
                combotime = 2;
            }


            //콤보부분


            if (cnt > 0) { //첫터치 1초과 반응 후
                combo++; //콤보증가
//                Log.i("fa ", "2쌍방 : "+ cnt);

                if (removeList.size() >= 12) {
                    //콤보중 4블록이상 터졌을때 추가점수
                    userScore += removeList.size() * combo;
                }
            }

            binding.count.setText("" + combo);

            //콤보 점수
            // 30 * 콤보
            userScore += (removeList.size() / 3) * combo;

//            highScore = highScore < userScore ? userScore : highScore;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    binding.nowScore.setText("" + String.format("%,d", userScore));
//                    highScoreView.setText(""+String.format("%,d", highScore));

                }
            });


            //블록 터지는 효과음
            /*if(effectSound)
                soundPool.play(dustRemoveSound, effectSoundVolume, effectSoundVolume,  1,  0,  1);
            */
        }

        //먼지 터지는 효과음
        mediaPlayer3.start();

        return flag;
    }//checklist




    //리스너 삭제 메소드
    private void removeOnGlobalLayoutListener(ViewTreeObserver observer, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if(observer == null) return ;

        observer.removeOnGlobalLayoutListener(listener);
    }//removeOnGlobalLayoutListener


    /**
     * 터치 이벤트 등록
     * @param event
     *      * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    //plate가 다 셋팅되고나서 게임 시작을 위한 초기 셋팅
    private void basicSetting() {
        //점수 셋팅
        userScore = 0;

        //콤보유지 시간
        combotime = 0;

        binding.count.setText(""+combo);

        gameStatus = GAME_PLAYING;
        touchStatus = true;
        //초기 셋팅 타이머
        timerThreadContoller = true;

        timer = 30;//시작 타임
        time = 0;//진행 시간
        binding.nowScore.setText(""+String.format("%,d", userScore));
//        highScoreView.setText(""+String.format("%,d", highScore));

        //콤보셋팅
        combo = 0;

        //미디어 플레이어 셋팅
//        setMediaPlayer();

    }


    // 다이어그램 온클릭 리스너
    View.OnClickListener dialClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch ( view.getId() ){
                case R.id.btn_replay :
                    soundPool.play(btnClick1, 1,1, 1,0,1);
                    gameReplay();
                    dialog.dismiss();
                    break;
                case R.id.btn_stop :
                    soundPool.play(btnClick1, 1,1, 1,0,1);
                    endGame();
                    dialog.dismiss();
                    break;
                default:
                    soundPool.play(btnClick1, 1,1, 1,0,1);
                    dialog.dismiss();
                    continueGame();
                    break;

            }
        }
    };

    ////////////////////////////////////////////////////
    //      게임 진행 ( 시작 - 종료 - 리플레이 )      //
    ////////////////////////////////////////////////////

    //게임 시작
    private void startGame() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //사운드 재생
        /*if(backgroundSound)
            mediaPlayer.start();*/

        //효과음 재생
        /*if(effectSound)
            gameStartSoundReturnNumber = soundPool.play(gameStartSound, effectSoundVolume, effectSoundVolume,  1,  0,  1.0f);*/


        binding.time.setText(""+timer);

        //관련 변수 초기화
        timerThreadContoller = true; //타이머 쓰레드 컨트롤 변수
        gameStatus = GAME_PLAYING;

        // Game Start 메시지 보이게 함
        binding.gameStartMessage.setVisibility(View.VISIBLE);
        binding.gameStartMessage.bringToFront();

        // 글자 날아오는 애니메이션
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.ready_go_anim);
        binding.gameStartMessage.startAnimation(animation);

        // 1.5초 뒤 게임 타이머 시작
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //글자 사라지는 애니메이션
                Animation animation2 = AnimationUtils.loadAnimation(GameActivity.this, R.anim.ready_go_anim2);
                binding.gameStartMessage.clearAnimation();  // 이전 애니메이션 정보 삭제 ( 오류 방지 )
                binding.gameStartMessage.startAnimation(animation2);
                animation2.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // 애니메이션 끝나면 사라지게 !
                        binding.gameStartMessage.setVisibility(View.GONE);
                    }
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                startGameTimer();    // 타이머 시작
                touchStatus = true; // 터치 가능
            }
        }, 1500);
    }

    //게임 타이머 시작
    private void startGameTimer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //타이머 루프
                while(timerThreadContoller) {
                    try {
                        Thread.sleep(1000);
                        timer--; //순수 게임시간
                        time++;
                        combotime--;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // 1초 마다 타이머 바 갱신
                                binding.time.setText(timer+"");

                                if(combotime == 0){
                                    binding.count.setText(""+combo);
                                    combo = 0;
                                    combotime = 1 ;
                                }

                            }
                        });

                        if(timer <= 0) {
                            //게임 종료 조건
                            timerThreadContoller = false;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //게임 상태가 GAME_PLAYING 일때만 게임 종료로 넘어감
                //이유는 onDestory에서도 위의 타이머 루프를 빠져나오기 때문. 그 땐 endGame()이 실행되면 안됨
                if(gameStatus != GAME_PLAYING)  {
                    return;
                } else {
                    endGame();
                }
            }
        }).start();
    }

    //게임 종료
    private void endGame() {
        //기록 저장
        // saveRecord();

        // mediaPlayer.stop();
        touchStatus = false;
        gameStatus = GAME_TERMINATED;

        handler.post(new Runnable() {
            @Override
            public void run() {
                //게임 종료 다이얼로그 생성
                final Dialog dialog = new Dialog(GameActivity.this);

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;
                dialog.setContentView( R.layout.diag_game_end );
                dialog.setCancelable(false);
                dialog.show();

                TextView score = dialog.findViewById(R.id.score);
                TextView playTime = dialog.findViewById(R.id.time);
                TextView isBestTime = dialog.findViewById(R.id.best_time);
                TextView isBestScore = dialog.findViewById(R.id.best_score);

                Button replay_btn = dialog.findViewById(R.id.btn_replay);
                replay_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        gameReplay();
                    }
                });

                Button stop_btn = dialog.findViewById(R.id.btn_stop);
                stop_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        soundPool.play(btnClick1, 1,1, 1,0,1);
                        dialog.dismiss();
                        finish();

                    }
                });

                score.setText(String.format("%,d", userScore));
                playTime.setText(String.format("%d", time));

                isBestScore.setVisibility(userScore == 100 ? View.VISIBLE : View.INVISIBLE);
                isBestTime.setVisibility(Integer.parseInt(playTime.getText().toString()) == 999 ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    //게임 다시하기
    private void gameReplay() {
        //기존 plate 내의 게임말들 전부 제거
        for(int q = 0 ; q < nyangArray.length ; q++) {
            for(int w = 0 ; w < nyangArray[q].length ; w++) {
                binding.layout.removeView(nyangArray[q][w]);
                nyangArray[q][w] = null;
            }
        }
        do {
            //겹치는게 없을 때까지 plate 셋팅
            setNyangArray();
        } while (checkNyangArray());
        soundPool.play(btnClick1, 1,1, 1,0,1);
        basicSetting();
        startGame();
    }



    //게임 중지
    private void pauseGame() {
        if(gameStatus == GAME_TERMINATED) return;
        timerThreadContoller = false;
        gameStatus = GAME_PAUSED; //GAME_PAUSED = 1
        touchStatus = false;
    }

    //게임 이어하기
    private void continueGame() {
        if(gameStatus == GAME_TERMINATED) return;
        timerThreadContoller = true;
        startGameTimer();
        gameStatus = GAME_PLAYING;
        touchStatus = true;
    }

    ////////////////////////////////////////
    // 생명 주기                          //
    ////////////////////////////////////////
    @Override
    public void onBackPressed() {
        binding.btnPause.performClick();
        //게임속 음악 끄고 타이틀화면 음악 재생
        mediaPlayer2.pause();
        //mediaPlayer1.start();

    }

    @Override
    protected void onPause() {
        super.onPause();

        if(gameStatus == GAME_PLAYING ) {
            binding.btnPause.performClick();
        }
        mediaPlayer2.pause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mediaPlayer2 != null)
            mediaPlayer2.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mediaPlayer2.stop();
    }
}
