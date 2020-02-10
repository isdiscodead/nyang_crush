package com.pro.nyangcrush;

import android.content.Context;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatImageView;

/** /////////////
 *  블럭이미지
///////////// **/

public class NyangImageView extends AppCompatImageView {

    private int nyangType;
    private int[] location;

    public NyangImageView(Context context, int x, int y, int width, int height, int nyangType) {
        super(context);

        setX(x);
        setY(y);
        setLayoutParams(new ViewGroup.LayoutParams(width, height));
        this.nyangType = nyangType;

        switch (nyangType) {
            case 1:
                setImageDrawable(getResources().getDrawable(R.drawable.block_box));
                break;
            case 2:
                setImageDrawable(getResources().getDrawable(R.drawable.block_can));
                break;
            case 3:
                setImageDrawable(getResources().getDrawable(R.drawable.block_crewel));
                break;
            case 4:
                setImageDrawable(getResources().getDrawable(R.drawable.block_fish));
                break;
            case 5:
                setImageDrawable(getResources().getDrawable(R.drawable.block_food));
                break;
            case 6:
                setImageDrawable(getResources().getDrawable(R.drawable.block_jelly));
                break;
        }
    }

    public int getNyangType() {
        return nyangType;
    }

    // 절대위치값
    public int getAbsoluteX() {
        location = new int[2];
        this.getLocationOnScreen(location);
        return location[0];
    }

    public int getAbsoluteY() {
        location = new int[2];
        this.getLocationOnScreen(location);
        return location[1];
    }
}
