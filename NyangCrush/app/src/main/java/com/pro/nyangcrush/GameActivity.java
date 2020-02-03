package com.pro.nyangcrush;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pro.nyangcrush.databinding.ActivityGameBinding;
import com.pro.nyangcrush.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Objects;

public class GameActivity extends Activity {

    ActivityGameBinding binding;
    Dialog dialog;

    // 일시정지 다이얼로그 내부의 버튼들
    Button btn_replay, btn_stop, btn_back, btn_close ;


    //게임화면
    TextView plate, hideDustBar;

    FrameLayout layout;
    boolean dd = false;

    //게임 판 사이즈 결정
    private int plateSize;
    private int division9; // plate를 9로 나눈 값
    private DustImageView[][] dustArray;
    private DustPosition[][] dustPositions;

    //view의 변화감지 리스너
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;

    //먼지 채우기 감지 콜백
    private FillCompletedListener fillCompletedListener;

    //제스처 감지
    private GestureDetector detector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_game);
        binding.setActivity(this);

        plate = findViewById(R.id.game_plate);
        hideDustBar = findViewById(R.id.game_hide_dust_bar);
        layout = findViewById(R.id.frame);

        dustArray = new DustImageView[9][9]; //게임 말 9 x 9 배치
        dustPositions = new DustPosition[9][9]; //게임말 이동

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
        });

        //plate가 그려진 후 넓이와 높이를 구하기 위한 리스너
        mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { //전체 뷰가 그려질 때
            @Override
            public void onGlobalLayout() {
                //plate에 아이템을 9x9로 배치하기 위해 정확히 9로 나눠지는 수치를 계산
                plateSize = (plate.getWidth() / 9) * 9; //plate.getWidth() : (1038 / 9) * 9 plateSize = 1035 사이즈값 지정
                //plate 넓이, 높이 설정
                Log.i("dd", ""+plate.getWidth() );
                ViewGroup.LayoutParams plateLayoutParams = plate.getLayoutParams(); //game_plate
                plateLayoutParams.width = plateSize; //레이아웃 width값 속성 지정
                plateLayoutParams.height = plateSize; //레이아웃 height값 속성 지정
                plate.setLayoutParams(plateLayoutParams); //레이아웃속성 변경

                //hideBar넓이, 높이 설정                      //레이아웃 속성객체 얻어옴
                ViewGroup.LayoutParams hideBarLayoutParams = hideDustBar.getLayoutParams(); //game_hide_dust_bar
                hideBarLayoutParams.width = plateSize; //plateSize = 1035
                hideBarLayoutParams.height = plateSize / 9; //plateSzie 1035 / 9 = 115
                hideDustBar.setLayoutParams(hideBarLayoutParams);


                //판의 크기를 설정한 후
                plate.post(new Runnable() {
                    @Override
                    public void run() {

                        do {
                            //겹치는게 없을 때까지 판을 셋팅
                            setDustArray(); //먼지채워넣기
                        } while (dd);

                    }
                });

                //리스너 지우기
                removeOnGlobalLayoutListener(plate.getViewTreeObserver(), mGlobalLayoutListener);
            }

        };
        //plate 넓이 구하기 위한 리스너 등록
        plate.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);//plate판 217 지정 리스너등록


    }//onCreate()


    /**
     * plate에 먼지를 채워넣음
     */
    private void setDustArray() {
        division9 = plateSize/9; //plateSize 1035 / 9  = division9=115

        for(int q = 0 ; q < dustArray.length ; q++) {
            for(int w = 0 ; w < dustArray[q].length ; w++) {
                if(dustArray[q][w] == null) {
                    //먼지 포지션만을 저장하는 배열
                    dustPositions[q][w] = new DustPosition((int)plate.getX() + (division9 * w), (int)plate.getY() + (division9 * q));

                    //실제 먼지가 저장되는 배열 배열판에 게임말이미지 등록
                    dustArray[q][w] = new DustImageView(GameActivity.this
                            , (int)plate.getX() + (division9 * w)
                            , (int)plate.getY() + (division9 * q)
                            , division9
                            , division9
                            , (int)(Math.random() * 6) + 1);
                    layout.addView(dustArray[q][w]);
                    Log.i("##### x", ""+dustArray[q][w].getX());
                    Log.i("##### ax", ""+dustArray[q][w].getAbsoluteX());
                }
            }
        }
    }

    //리스너 삭제 메소드
    private void removeOnGlobalLayoutListener(ViewTreeObserver observer, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if(observer == null) return ;
        observer.removeOnGlobalLayoutListener(listener);
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
}
