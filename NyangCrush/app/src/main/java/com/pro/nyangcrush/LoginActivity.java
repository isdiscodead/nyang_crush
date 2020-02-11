package com.pro.nyangcrush;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.pro.nyangcrush.databinding.ActivityLoginBinding;
import com.pro.nyangcrush.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class LoginActivity extends FragmentActivity {

    ActivityLoginBinding binding;

    //Firebase
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference mDatabase;
    private FirebaseDatabase sDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        binding.setActivity(this);

        // 파이어베이스 인증 객체 선언
        firebaseAuth = FirebaseAuth.getInstance();
        sDatabase = FirebaseDatabase.getInstance(); // 데이터베이스 레퍼런스 객체
        mDatabase = sDatabase.getReference(); // 파이어베이스 DB 객체

        //로그인 시도할 액티비티에서 유저데이터 요청
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        // 구글 로그인 버튼에 대한 이벤트 처리
        binding.btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 이벤트 발생했을때, 구글 로그인 버튼에 대한 (구글정보를 인텐트로 넘기는 값)
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

    } // onCreate()

    //Intent Result 반환
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //RC_SIGN_IN을 통해 로그인 확인여부 코드가 정상 전달되었다면
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            //구글버튼 로그인 누르고 구글 사용자 확인시 실행
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();

                Log.i("log", "이름 =" + account.getDisplayName());
                Log.i("log", "이메일=" + account.getEmail());
                Log.i("log", "getId()=" + account.getId());
                Log.i("log", "getAccount()=" + account.getAccount());
                Log.i("log", "getIdToken()=" + account.getIdToken());

                //구글 이용자 확인된 사람 정보 파이어베이스로 넘기기
                firebaseAuthWithGoogle(account);
                onStart( account );
            } else {
                Log.i("log", "실패했음1");
            }
        }else {
            Log.i("log", "실패했음2");
        }
    }

    //구글 파이어베이스로 값넘기기
    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {
        //파이어베이스로 받은 구글사용자가 확인된 이용자의 값을 토큰으로 받고
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                            String name = acct.getDisplayName();
                            String userid = acct.getId();
                            String email = acct.getEmail();

                            intent.putExtra("userid",userid);
                            intent.putExtra("name",name);
                            intent.putExtra("email",email);


                            Map<String, Object> taskMap = new HashMap<String, Object>();
                            taskMap.put("name", name);
                            taskMap.put("email", email);
                            taskMap.put("Score", 200);
                            taskMap.put("id",userid);

                            mDatabase.child("users").child(userid).updateChildren(taskMap);

                            startActivity(intent);
                            finish();

                            Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                        } else
                        {

                            Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    public void onStart ( GoogleSignInAccount acct )
    { // 사용자가 현재 로그인되어 있는지 확인
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if(currentUser!=null) {// 만약 로그인이 되어있으면 다음 액티비티 실행
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);

            String name = acct.getDisplayName();
            String userid = acct.getId();
            String email = acct.getEmail();

            intent.putExtra("userid",userid);
            intent.putExtra("name",name);
            intent.putExtra("email",email);

            startActivity(intent);
            finish();
        }
    }

}
