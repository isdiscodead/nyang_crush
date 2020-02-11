package com.pro.nyangcrush;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class RankAdapter extends ArrayAdapter {

    Context context;
    ArrayList<User> arr;    // 유저 정보 배열
    int resource;   // 레이아웃 리소스

    // 생성자
    public RankAdapter( Context context, int resource, ArrayList<User> arr ) {
        super(context, resource, arr);

        this.context = context;
        this.resource = resource;
        this.arr = arr;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 생성자로 초기화 된 리소스를 통해 뷰 객체 inflate
        LayoutInflater linf
                = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = linf.inflate(resource, null);

        User user = arr.get(position);  // i번째 유저의 정보

        // xml 내부의 뷰 객체들
        TextView rank = convertView.findViewById(R.id.rank);
        TextView user_name = convertView.findViewById(R.id.user_name);
        TextView score = convertView.findViewById(R.id.score);

        // 값 세팅
        rank.setText(""+( position+1 ));
        user_name.setText(user.getName());
        score.setText(user.getScore());

        return convertView;
    }

}
