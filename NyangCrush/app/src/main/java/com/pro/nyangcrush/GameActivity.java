package com.pro.nyangcrush;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;

import com.pro.nyangcrush.databinding.ActivityGameBinding;

import java.util.Objects;

public class GameActivity extends Activity {

    ActivityGameBinding binding;
    Dialog dialog;

    // 일시정지 다이얼로그 내부의 버튼들
    Button btn_replay, btn_stop, btn_back, btn_close ;

    //게임 상태
    private static final int GAME_PLAYING = 3;

    boolean dd = false;

    private int plateSize;
    private int division9; // plate를 9로 나눈 값
    private int gameStatus;
    private NyangImageView[][] nyangArray;
    private NyangPosition[][] nyangPositions;
    private boolean touchStatus; //true면 터치가 가능한 상태임

    //먼지 스왑시 필요한 두 먼지의 좌표
    private int e1X;
    private int e1Y;
    private int e2X;
    private int e2Y;

    //view의 변화감지 리스너
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_game);
        binding.setActivity(this);

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
                dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialogAnimation;

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

                //판의 크기를 설정한 후
                binding.gamePlate.post(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            //겹치는게 없을 때까지 판을 셋팅
                            setNyangArray();
                        } while (dd);
                        //basicSetting();
                    }
                });

                //리스너 지우기
                removeOnGlobalLayoutListener( binding.gamePlate.getViewTreeObserver(), mGlobalLayoutListener);
            }
        };//mGlobalLayoutListener

        //plate 넓이 구하기 위한 리스너 등록
        binding.gamePlate.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
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
                    //x
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



    //리스너 삭제 메소드
    private void removeOnGlobalLayoutListener(ViewTreeObserver observer, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if(observer == null) return ;

        observer.removeOnGlobalLayoutListener(listener);
    }//removeOnGlobalLayoutListener


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
}
