package com.pro.nyangcrush;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.pro.nyangcrush.databinding.ActivityGameBinding;
import com.pro.nyangcrush.databinding.ActivityMainBinding;

import java.util.Objects;

public class GameActivity extends Activity {

    ActivityGameBinding binding;
    Dialog dialog;

    // 일시정지 다이얼로그 내부의 버튼들
    Button btn_replay, btn_stop, btn_back, btn_close ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_game);
        binding.setActivity(this);

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
        });
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
