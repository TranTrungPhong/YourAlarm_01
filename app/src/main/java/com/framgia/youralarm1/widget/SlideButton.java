package com.framgia.youralarm1.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.framgia.youralarm1.R;

public class SlideButton extends View {
    private static final String TAG = SlideButton.class.getName();
    public static final int SLIDE_LEFT = 1;
    public static final int SLIDE_RIGHT = 2;
    private int mBackgroundColor = Color.TRANSPARENT;
    private int mBackgroundSelectedColor = Color.BLUE;
    private Drawable mDrawable;

    private Paint mDrawablePaint;
    private Paint mPaint;
    private float mRadius;
    private GestureDetector mDetector;
    private OnSlideListener mOnSlideListener;
    private int contentWidth;
    private int contentHeight;
    private float mDistanceX = 0f;
    private float mMoveX = 0f;
    private int mViewWidth;

    public SlideButton(Context context) {
        super(context);
        init(null, 0);
    }

    public SlideButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SlideButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a =
                getContext().obtainStyledAttributes(attrs, R.styleable.SlideButton, defStyle, 0);

        mBackgroundColor =
                a.getColor(R.styleable.SlideButton_slide_buttonColor, Color.TRANSPARENT);
        mBackgroundSelectedColor =
                a.getColor(R.styleable.SlideButton_slide_buttonSelectedColor, Color.BLUE);

        if ( a.hasValue(R.styleable.SlideButton_slide_drawable) ) {
            mDrawable = a.getDrawable(R.styleable.SlideButton_slide_drawable);
            mDrawable.setCallback(this);
        }
        a.recycle();

        mDrawablePaint = new Paint();
        mDrawablePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
        mDetector = new GestureDetector(getContext(), new GestureListener());
        mDetector.setIsLongpressEnabled(false);
    }

    private void invalidateTextPaintAndMeasurements() {
        if ( isSelected() ) {
            mPaint.setColor(mBackgroundSelectedColor);
        } else {
            mPaint.setColor(mBackgroundColor);
            mDrawablePaint.setColor(mBackgroundSelectedColor);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mViewWidth = canvas.getWidth();
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        contentWidth = getWidth() - paddingLeft - paddingRight;
        contentHeight = getHeight() - paddingTop - paddingBottom;
        mRadius = contentWidth < contentHeight ? contentWidth / 2 : contentHeight / 2;

        float scaleMove = Math.abs(mMoveX) / (mViewWidth / 2) ;
        mPaint.setAlpha((int) (255 * Math.abs( 1 - scaleMove)));
        // Draw background.
        float cx = paddingLeft + contentWidth / 2 + mDistanceX;
        float cy = paddingTop + contentHeight / 2;
        if (cx < mRadius ) cx = mRadius;
        if (cx > mViewWidth - mRadius) cx = mViewWidth - mRadius;
        canvas.drawCircle(cx, cy, mRadius, mPaint);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        return super.onCreateDrawableState(extraSpace);

    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
    }

    public void setWidth(int width) {
        getLayoutParams().width = width;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public void setheight(int height) {
        getLayoutParams().height = height;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        mBackgroundColor = backgroundColor;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public OnSlideListener getOnSlideListener() {
        return mOnSlideListener;
    }

    public void setOnSlideListener(OnSlideListener onSlideListener) {
        mOnSlideListener = onSlideListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = mDetector.onTouchEvent(event);
        if ( ! result ) {
            if (event.getAction() == MotionEvent.ACTION_UP ) {
                if (Math.abs(mMoveX) > (contentWidth / 2 - contentHeight) ) {
                    if (mOnSlideListener != null)
                        mOnSlideListener.onSlideListener(this, mMoveX < 0 ? SLIDE_LEFT : SLIDE_RIGHT);
                } else {
                    setSelected(! isSelected());
                    moveViewToInit();
                }
                result = true;
            }
        }
        return result;
    }

    private void moveViewToInit() {
        mMoveX = 0;
        mDistanceX = 0;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            float distance = (float) Math.sqrt(
                    Math.pow(e.getX() - (getPaddingLeft() + contentWidth / 2), 2) +
                            Math.pow(e.getY() - (getPaddingTop() + contentHeight / 2), 2));
            if ( distance <= mRadius ) {
                setSelected(! isSelected());
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mMoveX = e2.getX() - (getX() + mViewWidth/2);
            mDistanceX -= distanceX ;
            invalidate();
            return true;
        }
    }

    public interface OnSlideListener {
        void onSlideListener(View view, int swipedSide);
    }
}