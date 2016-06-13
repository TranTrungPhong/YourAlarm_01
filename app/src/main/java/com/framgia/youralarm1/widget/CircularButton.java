package com.framgia.youralarm1.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.framgia.youralarm1.R;

public class CircularButton extends View {
    private String mText;
    private int mBackgroundColor = Color.TRANSPARENT;
    private int mBackgroundSelectedColor = Color.BLUE;
    private int mTextColor = Color.WHITE;
    private float mTextSize = 0;
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private Paint mPaint;
    private float mTextWidth;
    private float mTextHeight;
    private float mRadius;
    private GestureDetector mDetector;
    private OnClickInteractionListener mOnClickInteractionListener;
    private int contentWidth;
    private int contentHeight;

    public CircularButton(Context context) {
        super(context);
        init(null, 0);
    }

    public CircularButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CircularButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a =
                getContext().obtainStyledAttributes(attrs, R.styleable.CircularButton, defStyle, 0);

        mBackgroundColor =
                a.getColor(R.styleable.CircularButton_backgroundColor, Color.TRANSPARENT);
        mBackgroundSelectedColor =
                a.getColor(R.styleable.CircularButton_backgroundSelectedColor, Color.BLUE);
        mText = a.getString(R.styleable.CircularButton_text);
        mTextColor = a.getColor(R.styleable.CircularButton_textColor, Color.WHITE);
        mTextSize = a.getDimension(R.styleable.CircularButton_textSize, mTextSize);

        if ( a.hasValue(R.styleable.CircularButton_exampleDrawable) ) {
            mExampleDrawable = a.getDrawable(R.styleable.CircularButton_exampleDrawable);
            mExampleDrawable.setCallback(this);
        }
        a.recycle();

        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
        mDetector = new GestureDetector(CircularButton.this.getContext(), new GestureListener());
        mDetector.setIsLongpressEnabled(false);
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mTextWidth = mTextPaint.measureText(mText);
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
        if ( isSelected() ) {
            mPaint.setColor(mBackgroundSelectedColor);
        } else {
            mPaint.setColor(mBackgroundColor);
            mTextPaint.setColor(mBackgroundSelectedColor);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        contentWidth = getWidth() - paddingLeft - paddingRight;
        contentHeight = getHeight() - paddingTop - paddingBottom;
        // Draw background.
        mRadius = contentWidth < contentHeight ? contentWidth / 2 : contentHeight / 2;
        canvas.drawCircle(paddingLeft + contentWidth / 2, paddingTop + contentHeight / 2, mRadius,
                          mPaint);
        // Draw the text.
        canvas.drawText(mText, paddingLeft + (contentWidth - mTextWidth) / 2,
                        paddingTop + (contentHeight / 2) + mTextHeight * 3 / 2, mTextPaint);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        return super.onCreateDrawableState(extraSpace);

    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int textColor) {
        mTextColor = textColor;
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

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float textSize) {
        mTextSize = textSize;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    private void resetColorButton() {
        setBackgroundColor(mBackgroundColor);
        setTextColor(mTextColor);
    }

    private void changeColorButton() {
        setBackgroundColor(Color.YELLOW);
        setTextColor(Color.RED);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        invalidateTextPaintAndMeasurements();
        invalidate();
    }
    public OnClickInteractionListener getOnClickInteractionListener() {
        return mOnClickInteractionListener;
    }

    public void setOnClickInteractionListener(
            OnClickInteractionListener onClickInteractionListener) {
        mOnClickInteractionListener = onClickInteractionListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = mDetector.onTouchEvent(event);
        if ( ! result ) {
            float distance = (float) Math.sqrt(
                    Math.pow(event.getX() - (getPaddingLeft() + contentWidth / 2), 2) +
                            Math.pow(event.getY() - (getPaddingTop() + contentHeight / 2), 2));
            if ( distance <= mRadius && event.getAction() == MotionEvent.ACTION_UP ) {
                if ( mOnClickInteractionListener != null )
                    mOnClickInteractionListener.onClickInteractionListener(this, getId(), isSelected
                            ());
                invalidateTextPaintAndMeasurements();
                invalidate();
                result = true;
            }
        }
        return result;
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
    }

    public interface OnClickInteractionListener {
        void onClickInteractionListener(View view, int id, boolean isSelected);
    }
}