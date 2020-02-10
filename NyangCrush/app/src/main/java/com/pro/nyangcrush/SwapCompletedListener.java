package com.pro.nyangcrush;

public class SwapCompletedListener {

    /**
     * 블럭 스왑시 스왑이 완료되었는지 체크하기위한 리스너
     */

    private int animationEndCount;
    private SwapCallback swapCallback;

    interface SwapCallback {
        void onSwapComplete(boolean restore);
    }

    public void setSwapCallback(SwapCallback callback) {
        swapCallback = callback;
    }

    // restore는 '일반적인 스왑' 및 '되돌리기 스왑'을 구분하기위한 플래그
    // 일반적인 스왑 : 사용자가 블럭을 드래그
    // 되돌리기 스왑 : 블럭 스왑 후 터트릴 블럭이 하나도 없을 때 원상태로 되돌리기 위한 스왑
    public void swapAnimationEnd(boolean restore) {
        animationEndCount++;

        // 스왑 애니메이션 2개가 완료되었는지 체크
        if(swapCallback != null && animationEndCount == 2) {
            animationEndCount = 0;
            swapCallback.onSwapComplete(restore);
        }
    }

}
