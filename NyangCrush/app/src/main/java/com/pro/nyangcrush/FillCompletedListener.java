package com.pro.nyangcrush;

public class FillCompletedListener {

    /**
     * 블록을 3개 이상 모이게해서 터트린 후
     * 새로운 블록들로 필드가 다시 채워진것을 체크하기 위한 리스너
     */

    // 채워야할 공백 갯수
    private int totalNullCount;

    // 공백 채운 갯수
    private int fillCount;

    private FillCallback fillCallback;

    public FillCompletedListener() {
        totalNullCount = 100;
    }

    interface FillCallback {
        void onFillComplete();
    }

    public void setFillCallback(FillCallback callback) {
            this.fillCallback = callback;
    }

    public void setTotalNullCount(int totalNullCount) {
        this.totalNullCount = totalNullCount;
    }

    public void fillComplete() {
        fillCount++;

        // 공백을 모두 채웠다면 콜백 호출
        if(fillCallback != null && fillCount == totalNullCount) {
            // 변수 초기화
            totalNullCount = 100;
            fillCount = 0;

            //콜백 호출
            fillCallback.onFillComplete();
        }
    }
}
