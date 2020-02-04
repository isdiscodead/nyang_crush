package com.pro.nyangcrush;

import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
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

import com.pro.nyangcrush.databinding.ActivityGameBinding;

import java.util.ArrayList;
import java.util.Objects;

public class GameActivity extends Activity {

    ActivityGameBinding binding;
    Dialog dialog;

    // 일시정지 다이얼로그 내부의 버튼들
    Button btn_replay, btn_stop, btn_back, btn_close ;

    //게임 상태
    private static final int GAME_PLAYING = 3;


    //임시
    boolean dd = false;

    private int plateSize;
    private int division9; // plate를 9로 나눈 값
    private int gameStatus;
    private NyangImageView[][] nyangArray;
    private NyangPosition[][] nyangPositions;
    private boolean touchStatus; //true면 터치가 가능한 상태임
    private int userScore;

    private int combo; //콤보 점수
    private int cnt; //첫터치 반응 확인

    //게임말 스왑시 필요한 두 먼지의 좌표
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
                dialog = new Dialog( GameActivity.this );

                dialog.setCancelable( false );
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                dialog.setContentView( R.layout.diag_pause );

                btn_back = dialog.findViewById( R.id.btn_back );
                btn_stop = dialog.findViewById( R.id.btn_stop );
                btn_replay = dialog.findViewById( R.id.btn_replay );
                btn_close = dialog.findViewById( R.id.btn_close );

                dialog.show();

                btn_back.setOnClickListener( dialClick );
                btn_stop.setOnClickListener( dialClick );
                btn_replay.setOnClickListener( dialClick );
                btn_close.setOnClickListener( dialClick );
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
                Log.i("my",""+plateSize);
                touchStatus = true;
                //plate 넓이, 높이 설정
                //ViewGroup : View의 부모 , view는 textview, editview, button, imageview등
                //자식객체밖에 못건듬
                ViewGroup.LayoutParams plateLayoutParams =  binding.gamePlate.getLayoutParams(); //game_plate
                plateLayoutParams.width = plateSize; //레이아웃 wid어th값 속성 지정
                plateLayoutParams.height = plateSize; //레이아웃 height값 속성 지정
                binding.gamePlate.setLayoutParams(plateLayoutParams); //레이아웃속성 변경 / 원래는 리니어

                //hideBar넓이, 높이 설정                      //레이아웃 속성객체 얻어옴
                ViewGroup.LayoutParams hideBarLayoutParams = binding.gameHideDustBar.getLayoutParams(); //game_hide_dust_bar
                hideBarLayoutParams.width = plateSize;
                hideBarLayoutParams.height = plateSize / 9;
                binding.gameHideDustBar.setLayoutParams(hideBarLayoutParams);
                gameStatus = GAME_PLAYING;


                //판의 크기를 설정한 후
                binding.gamePlate.post(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            //겹치는게 없을 때까지 판을 셋팅
                            setNyangArray();
                        } while (checkNyangArray());
                        basicSetting();
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

                        //처음 터치한 먼지의 좌표
                        e1X = ((int)e1.getX() - plateX) / division9;  // 58 / 115 = 0
                        e1Y = ((int)e1.getY() - plateY) / division9; //993 / 115 = 8.xxx

                        //좌표값 첫 터치 로그 확인
                        Log.i("dd"," e1.getX"+((int)e1.getX() - plateX));
                        Log.i("dd"," e1.getY"+((int)e1.getY() - plateY));

                        //터치를 뗀 위치의 좌표
                        e2X = ((int)e2.getX() - plateX) / division9;
                        e2Y = ((int)e2.getY() - plateY) / division9;

                        Log.i("dd","e1x : "+e1X + " / e1Y : "+e1Y +"/ e2X : "+e2X +"/ e2Y : "+e2Y);

                        //스왑할 먼지의 좌표 조정
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
                            Log.i("잘못된 드래그 ->", e1Y+" -> "+e2Y+" || "+e1X+" -> "+e2X);
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
                                    //터트릴 먼지가 하나도 없을 경우 다시 스왑함으로써 원상태로 되돌림
                                    swapNyang(e1X, e2X, e1Y, e2Y, true); //좌표 1 , 2 , 6 , 6


                                } else {
                                    fillCompletedListener = new FillCompletedListener();
                                    FillCompletedListener.FillCallback fillCallback = new FillCompletedListener.FillCallback() {
                                        @Override
                                        public void onFillComplete() {
                                            if(gameStatus != GAME_PLAYING) return;

                                            if(!checkNyangArray()) {
                                                touchStatus = true;

                                                combo = 0;
                                                cnt = 0;
                                            } else {
                                                fillBlank();

                                            }
                                        }
                                    };
                                    //첫터치 3개이상 블록을 터트렸을때 반응
                                    cnt++;
                                    Log.i("fa","첫 터짐 반응 : "+cnt);
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


    }//onCreate

    /**
     * plate에 먼지를 채워넣음
     */
    private void setNyangArray(){
        division9 = plateSize/9;
        for(int q = 0 ; q < nyangArray.length ; q++) {
            for(int w = 0 ; w < nyangArray[q].length ; w++) {
                if(nyangArray[q][w] == null) {
                    //먼지 포지션만을 저장하는 배열
                    nyangPositions[q][w] = new NyangPosition((int)binding.gamePlate.getX() + (division9 * w),
                            (int)binding.gamePlate.getY() + (division9 * q));

                    //실제 먼지가 저장되는 배열 배열판에 게임말이미지 등록
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
        //게임말1 : 사용자가 처음 터치한 먼지
        //게임말2 : 사용자가 교환하려고 드래그한 자리에 있는 먼지

        //게임말1 좌표 얻어오기
        final int nyang1X = (int)nyangPositions[y1][x1].getX();
        final int nyang1Y = (int)nyangPositions[y1][x1].getY();
        Log.i("dd", "nyang1X"+ nyang1X+"// Y " +nyang1Y );


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
     * 먼지 삭제 후 공백 채우는 메소드
     */

    private void fillBlank() {
        int totalNullCount = 0;

  /*      for(int q = 0 ; q < nyangArray.length ; q++) {
            String row = "";
            for(int w = 0 ; w < nyangArray[q].length ; w++) {
                row += nyangArray[q][w] == null ? "x" : "o";
                row += " ";
            }
            Log.i("dustArray "+q, row);
        }*/

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
                            , (int)binding.gameHideDustBar.getX() + (division9 * q)
                            , (int)binding.gameHideDustBar.getY()
                            , division9
                            , division9
                            , (int)(Math.random() * 6) + 1);
                    binding.layout.addView(nyangImageView);
                    binding.gameHideDustBar.bringToFront();
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
                        Log.i("dd","null : "+ nullCount);
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
                                    //먼지들을 아래로 옮기고 난 후 공백을 채워넣음
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


            //생성되있는 먼지들을 아래로 이동시키며 채워넣음
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
     * 현재 plate에 동일한 모양의 먼지가 일렬로 3개 이상 나열된 곳이 있는지 체크 후
     * 있다면, 나열된 먼지 삭제 후 점수 획득 및 true 리턴
     * 없다면, false 리턴
     * @return
     */
    private boolean checkNyangArray() {
        final ArrayList<String> removeList = new ArrayList<>();
        boolean flag = false; //리턴할 변수

        for(int q = 0 ; q < nyangArray.length ; q++) {
            for(int w = 0 ; w < nyangArray[q].length ; w++) {
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

                for(int e = verticalMin + 1 ; e <= verticalMax ; e++) {
                    //세로 탐색
                    if(nyangArray[e - 1][w].getNyangType() == nyangArray[e][w].getNyangType()) {
                        count++;

                    } else {
                        count = 0;
                    }

                    if(count >= 2) {

                        /**
                         * 카운팅이 2 이상 된 경우 연속된 3개의 먼지가 있다는 의미 이므로
                         * 현재 검사한 먼지의 좌표(q, w)를 기준으로 인접한 같은 모양의 먼지를 모두 삭제함
                         * 범위는 기준점(q, w)에서 부터 최대 2칸
                         */
                        flag = true;

                        removeList.add(q+","+w);

                        for(int r = q + 1 ; r <= verticalMax ; r++) {
                            if(nyangArray[q][w].getNyangType() == nyangArray[r][w].getNyangType()) {
                                if(binding.layout.getViewWidget(nyangArray[r][w]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_nyang);
                                    nyangArray[r][w].startAnimation(anim);
                                }

                                binding.layout.removeView(nyangArray[r][w]);
                                removeList.add(r+","+w);

                            } else {
                                break;
                            }
                        }

                        for(int r = q - 1 ; r >= verticalMin ; r--) {
                            if(nyangArray[q][w].getNyangType() == nyangArray[r][w].getNyangType()) {
                                if(binding.layout.getViewWidget(nyangArray[r][w]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_nyang);
                                    nyangArray[r][w].startAnimation(anim);
                                }

                                binding.layout.removeView(nyangArray[r][w]);
                                removeList.add(r+","+w);
                            } else {
                                break;
                            }
                        }
                        break;
                    }
                }

                count = 0;
                for(int e = horizontalMin + 1 ; e <= horizontalMax ; e++) {
                    //가로 탐색
                    if(nyangArray[q][e - 1].getNyangType() == nyangArray[q][e].getNyangType()) {
                        count++;
                    } else {
                        count = 0;
                    }

                    if(count >= 2) {
                        flag = true;

                        removeList.add(q+","+w);
                        for(int r = w + 1 ; r <= horizontalMax ; r++) {
                            if(nyangArray[q][w].getNyangType() == nyangArray[q][r].getNyangType()) {
                                if(binding.layout.getViewWidget(nyangArray[q][r]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_nyang);
                                    nyangArray[q][r].startAnimation(anim);
                                }

                                binding.layout.removeView(nyangArray[q][r]);
                                removeList.add(q+","+r);
                            } else {
                                break;
                            }
                        }

                        for(int r = w - 1 ; r >= horizontalMin ; r--) {
                            if(nyangArray[q][w].getNyangType() == nyangArray[q][r].getNyangType()) {
                                if(binding.layout.getViewWidget(nyangArray[q][r]) != null) {
                                    //리무브 애니메이션
                                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.remove_nyang);
                                    nyangArray[q][r].startAnimation(anim);
                                }

                                binding.layout.removeView(nyangArray[q][r]);
                                removeList.add(q+","+r);
                            } else {
                                break;
                            }
                        }
                        break;
                    }
                }

                if(count >= 2){

                }else{

                }
            }
        }

        for(int q = 0 ; q < removeList.size() ; q++) {
            //실제 dustArray에서 먼지 삭제
            int i = Integer.parseInt(removeList.get(q).split(",")[0]);
            int j = Integer.parseInt(removeList.get(q).split(",")[1]);
            nyangArray[i][j] = null;

        }

        if(flag && gameStatus == GAME_PLAYING) {
            //시간초 추가
//            stackedNumber -= (float)removeList.size() / 2;
            //점수 갱신
            userScore += (removeList.size() * 10);

            //콤보부분
            if(cnt > 0){ //첫터치 1이상 반응 후
                combo++; //콤보증가
            }
            Log.i("fa","combo : "+combo);

                 //콤보 점수
                userScore += combo * 5;


//            highScore = highScore < userScore ? userScore : highScore;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    binding.userScore.setText(""+String.format("%,d", userScore));
//                    highScoreView.setText(""+String.format("%,d", highScore));

                }
            });


            //먼지 터지는 효과음
            /*if(effectSound)
                soundPool.play(dustRemoveSound, effectSoundVolume, effectSoundVolume,  1,  0,  1);
            */
   }

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
        binding.userScore.setText(""+String.format("%,d", userScore));
//        highScoreView.setText(""+String.format("%,d", highScore));

        //콤보셋팅
        combo = 0;

        //미디어 플레이어 셋팅
//        setMediaPlayer();

//        timer = 0;
//        stackedNumber = 0;
    }


    // 다이어그램 온클릭 리스너
    View.OnClickListener dialClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch ( view.getId() ){
                case R.id.btn_replay :
                    break;
                case R.id.btn_stop :
                    break;
                default:
                    dialog.dismiss();
                    break;

            }
        }
    };


    /////////////////////////생명 주기
    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }
}
