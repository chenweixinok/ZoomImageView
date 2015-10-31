package com.android.zoomimageview.zoomimageview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by Administrator on 2015/10/31.
 */
public class ZoomImageView extends ImageView implements ScaleGestureDetector.OnScaleGestureListener,
        View.OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener {

    private boolean mOnce;

    /**
     *   ��ʼ������ֵ
     * @param context
     */
    private float mInitScale;

    /**
     * ˫���Ŵ�ֵ�����ֵ
     */
    private float mMidScale;

    /**
     * �Ŵ�����ֵ
     */
    private float mMaxScale;

    private Matrix mScaleMatrix;

    /**
     * �����û���ָ����ʱ���ŵı���
     */
    private ScaleGestureDetector mScaleGestureDetector;

    // .................................�����ƶ�
    /**
     * ��¼��һ�ζ�㴥�ص�����
     */
    private int mLastPointerCount;

    private float mLastX;
    private float mLastY;
    private int mTouchSlop;
    private boolean isCanDrag;

    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    //--------------˫���Ŵ�����С
    private GestureDetector mGestureDetector;
    private boolean isAutoScale;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScaleMatrix = new Matrix();
        setScaleType(ScaleType.MATRIX);

        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener(){
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {

                        if (isAutoScale){
                            return true;
                        }

                        float x = e.getX();
                        float y = e.getY();

                        if (getScale() < mMidScale){
                            //  mScaleMatrix.postScale(mMidScale/getScale(),
                            //        mMidScale/getScale(),x,y);
                            //setImageMatrix(mScaleMatrix);
                            postDelayed(new AutoScaleRunnable(mMidScale,x,y),16);
                            isAutoScale = true;
                        }else {
                            //mScaleMatrix.postScale(mInitScale/getScale(),
                            //        mInitScale/getScale(),x,y);
                            //setImageMatrix(mScaleMatrix);
                            postDelayed(new AutoScaleRunnable(mInitScale,x,y),16);
                            isAutoScale = true;
                        }

                        return true;
                    }
                });
    }

    /**
     * �Զ��Ŵ�����С
     */
    private class AutoScaleRunnable implements Runnable{

        /**
         * ���ŵ�Ŀ��ֵ
         */
        private float mTargetScale;
        //�������ĵ�
        private float x;
        private float y;

        private final float BIGGER = 1.07f;
        private final float SMALL = 0.93f;

        private float tmpScale;

        public AutoScaleRunnable(float mTargetScale, float x, float y) {
            this.mTargetScale = mTargetScale;
            this.x = x;
            this.y = y;

            if (getScale() < mTargetScale){
                tmpScale = BIGGER;
            }
            if (getScale() > mTargetScale){
                tmpScale = SMALL;
            }
        }

        @Override
        public void run() {
            //��������
            mScaleMatrix.postScale(tmpScale,tmpScale,x,y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);

            float currentScale = getScale();
            if ((tmpScale > 1.0f && currentScale < mTargetScale)||
                    (tmpScale < 1.0f && currentScale > mTargetScale)){
                postDelayed(this,16);
            }else{
                /**
                 * ����Ϊ���ǵ�Ŀ��ֵ
                 */
                float scale = mTargetScale/currentScale;
                mScaleMatrix.postScale(scale,scale,x,y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mScaleMatrix);

                isAutoScale = false;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    /**
     * ��ȡImageView������ɵ�ͼƬ
     */
    @Override
    public void onGlobalLayout() {

        if (!mOnce){
            //�õ��ؼ��Ŀ�͸�
            int width = getWidth();
            int height = getHeight();
            //�õ����ǵ�ͼƬ���Լ���͸�
            Drawable d = getDrawable();
            if (d == null)
                return;
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();

            float scale = 1.0f;
            /**
             * ���ͼƬ�Ŀ�����ڿؼ��Ŀ�ȣ����Ǹ߶�С�ڿؼ��߶ȣ����ǽ�����С
             */
            if (dw > width && dh < height){
                scale = width * 1.0f / dw;
            }
            /**
             * ���ͼƬ�ĸ߶ȴ��ڿؼ��ĸ߶ȣ����ǿ��С�ڿؼ���ȣ����ǽ�����С
             */
            if (dh > height && dw < width){
                scale = height * 1.0f / dh;
            }

            if ((dw > width && dh > height)||(dw <width && dh <height)){
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dw);
            }

            /**
             * �õ���ʼ������ʱ�ı���
             */
            mInitScale = scale;
            mMaxScale = mInitScale * 4;
            mMidScale = mInitScale * 2;

            //��ͼƬ�ƶ����ؼ�����
            int dx = getWidth()/2 - dw/2;
            int dy = getHeight()/2 - dh/2;

            mScaleMatrix.postTranslate(dx,dy);
            mScaleMatrix.postScale(mInitScale,mInitScale, width/2, height/2);
            setImageMatrix(mScaleMatrix);

            mOnce = true;
        }

    }

    /**
     * ��ȡ��ǰͼƬ������ֵ
     * @return
     */
    public float getScale(){
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    /**
     * ���ŵ�����:initScale maxScale
     * @param detector
     * @return
     */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();

        if (getDrawable() == null){
            return true;
        }

        //���ŷ�Χ�Ŀ���
        if ((scale < mMaxScale && scaleFactor > 1.0f)||(scale > mInitScale && scaleFactor < 1.0f)){
            if (scale * scaleFactor < mInitScale){
                scaleFactor = mInitScale/scale;
            }
            if (scale * scaleFactor > mMaxScale){
                scale = mMaxScale/scale;
            }
            //����
            mScaleMatrix.postScale(scaleFactor,scaleFactor,detector.getFocusX(),detector.getFocusY());
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);
        }
        return true;
    }

    /**
     * ���ε���������,���ͼƬ�Ŵ���С�Ժ�ÿ�͸ߣ��Լ�l,r,t,b
     * @return
     */
    private RectF getMatrixRectF(){
        Matrix matrix = mScaleMatrix;
        RectF rectF = new RectF();

        Drawable d = getDrawable();
        if (d != null){
            rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }

        return rectF;
    }

    /**
     * �����ŵ�ʱ����б߽�����Լ����ǵ�λ�ÿ���
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rect = getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        //����ʱ���б߽��飬��ֹ���ְױ�
        if (rect.width() >= width){
            if (rect.left > 0){
                deltaX = - rect.left;
            }
            if (rect.right < width){
                deltaX = width - rect.right;
            }
        }

        if (rect.height() >= height){
            if (rect.top > 0){
                deltaY = - rect.top;
            }
            if (rect.bottom < height){
                deltaY = height - rect.bottom;
            }
        }

        //�����Ȼ��߸߶�С�ڿؼ��Ŀ�͸ߣ���������С�
        if (rect.width() < width){
            deltaX = width/2f - rect.right + rect.width()/2f;
        }
        if (rect.height() < height){
            deltaY = height/2f - rect.bottom + rect.height()/2f;
        }
        mScaleMatrix.postTranslate(deltaX,deltaY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (mGestureDetector.onTouchEvent(event)){
            return true;
        }
        mScaleGestureDetector.onTouchEvent(event);

        float x = 0;
        float y = 0;
        //�õ���㴥�ص�����
        int pointerCount = event.getPointerCount();
        for (int i = 0;i < pointerCount;i++){
            x += event.getX(i);
            y += event.getY(i);
        }

        y /=pointerCount;
        x /=pointerCount;

        if (mLastPointerCount != pointerCount){
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;
        RectF rectF = getMatrixRectF();
        switch (event.getAction()){
            //�����ViewPager�ĳ�ͻ
            case MotionEvent.ACTION_DOWN:
                if (rectF.width() > getWidth() + 0.01||rectF.height() > getHeight() + 0.01){
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:

                if (rectF.width() > getWidth() + 0.01||rectF.height() > getHeight() + 0.01){
                    if (getParent() instanceof ViewPager)
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                float dx = x - mLastX;
                float dy = y - mLastY;

                if (!isCanDrag){
                    isCanDrag = isMoveAction(dx,dy);
                }

                if (isCanDrag){
                    if (getDrawable() != null){
                        isCheckLeftAndRight = isCheckTopAndBottom = true;
                        //������С�ڿؼ���ȣ�����������ƶ�
                        if (rectF.width() < getWidth()){
                            isCheckLeftAndRight = false;
                            dx = 0;
                        }
                        //����߶�С�ڿؼ��߶ȣ������������ƶ�
                        if (rectF.height() < getHeight()){
                            isCheckTopAndBottom = false;
                            dy = 0;
                        }

                        mScaleMatrix.postTranslate(dx,dy);
                        checkBorderWhenTranslate();
                        setImageMatrix(mScaleMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPointerCount = 0;
                break;
        }

        return true;
    }

    /**
     * ���ƶ�ʱ�����б߽���
     */
    private void checkBorderWhenTranslate() {

        RectF rectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectF.top > 0 && isCheckTopAndBottom){
            deltaY = - rectF.top;
        }
        if (rectF.bottom < height && isCheckTopAndBottom){
            deltaY = height - rectF.bottom;
        }

        if (rectF.left > 0 && isCheckLeftAndRight){
            deltaX = - rectF.left;
        }
        if (rectF.right < width && isCheckLeftAndRight){
            deltaX = width - rectF.right;
        }
        mScaleMatrix.postTranslate(deltaX,deltaY);
    }

    /**
     * �ж��Ƿ���move
     * @param dx
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx*dx + dy*dy) > mTouchSlop;
    }
}
