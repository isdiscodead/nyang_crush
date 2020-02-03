package com.pro.nyangcrush;

import android.content.Context;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatImageView;

public class NyangImageView extends AppCompatImageView {

    private int blockType;
    private int[] loc;

    public NyangImageView(Context context, int x, int y, int width, int height, int blockType ) {
        super(context);

        setX(x);
        setY(y);
        setLayoutParams( new ViewGroup.LayoutParams(width, height));
        this.blockType = blockType;

        switch ( blockType ){

            case 1:
                setImageDrawable(getResources().getDrawable(R.drawable.block_box,null));
                break;

            case 2:
                setImageDrawable(getResources().getDrawable(R.drawable.block_can,null));
                break;

            case 3:
                setImageDrawable(getResources().getDrawable(R.drawable.block_crewel,null));
                break;

            case 4:
                setImageDrawable(getResources().getDrawable(R.drawable.block_fish,null));
                break;

            case 5:
                setImageDrawable(getResources().getDrawable(R.drawable.block_food,null));
                break;

            case 6:
                setImageDrawable(getResources().getDrawable(R.drawable.block_jelly,null));
                break;
        } // switch

    } // BlockImageView

    public int getBlockType() {
        return blockType;
    }

    public int getAbsoluteX() {
        loc= new int[2];
        this.getLocationOnScreen(loc);
        return loc[0];
    }

    public int getAbsoluteY() {
        loc = new int[2];
        this.getLocationOnScreen(loc);
        return loc[1];
    }
}
