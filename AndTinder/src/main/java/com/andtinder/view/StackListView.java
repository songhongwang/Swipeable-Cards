package com.andtinder.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.andtinder.R;
import com.andtinder.model.CardModel;

public class StackListView extends AdapterView<ListAdapter> {
    public static final int INVALID_POINTER_ID = -1;
    private int mActivePointerId = INVALID_POINTER_ID;
    private final DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            clearStack();
            ensureFull();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            clearStack();
        }
    };
    private final Rect boundsRect = new Rect();
    private final Rect childRect = new Rect();
    private final Matrix mMatrix = new Matrix();


    //TODO: determine max dynamically based on device speed
    private int mMaxVisible = 10;
    private GestureDetector mGestureDetector;
    private int mFlingSlop;
    private ListAdapter mListAdapter;
    private float mLastTouchX;
    private float mLastTouchY;
    private View mTopCard;
    private int mTouchSlop;
    private int mNextAdapterPosition;
    private boolean mDragging;

    private int childMargin;
    private int ladder = 6; //每个子view的差值

    public StackListView(Context context) {
        super(context);
        init();
    }

    public StackListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromXml(attrs);
        init();
    }

    public StackListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initFromXml(attrs);
        init();
    }

    private void init() {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        mFlingSlop = viewConfiguration.getScaledMinimumFlingVelocity();
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mGestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    private void initFromXml(AttributeSet attr) {
        TypedArray a = getContext().obtainStyledAttributes(attr, R.styleable.StackListView);

        childMargin = a.getInteger(R.styleable.StackListView_childMargin, 40);

        a.recycle();
    }

    @Override
    public ListAdapter getAdapter() {
        return mListAdapter;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (mListAdapter != null)
            mListAdapter.unregisterDataSetObserver(mDataSetObserver);

        clearStack();
        mTopCard = null;
        mListAdapter = adapter;
        mNextAdapterPosition = 0;
        adapter.registerDataSetObserver(mDataSetObserver);

        ensureFull();

        if (getChildCount() != 0) {
            mTopCard = getChildAt(getChildCount() - 1);
            mTopCard.setLayerType(LAYER_TYPE_HARDWARE, null);
        }
        requestLayout();
    }

    private void ensureFull() {
        while (mNextAdapterPosition < mListAdapter.getCount() && getChildCount() < mMaxVisible) {
            View view = mListAdapter.getView(mNextAdapterPosition, null, this);
            view.setLayerType(LAYER_TYPE_SOFTWARE, null);

            addViewInLayout(view, 0, new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT), false);

            requestLayout();

            mNextAdapterPosition += 1;
        }
    }

    private void clearStack() {
        removeAllViewsInLayout();
        mNextAdapterPosition = 0;
        mTopCard = null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int requestedWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int requestedHeight = getMeasuredHeight() -  getPaddingTop() - getPaddingBottom();
        int childWidth, childHeight;

        childWidth = requestedWidth;
        childHeight = requestedHeight;

        int childWidthMeasureSpec, childHeightMeasureSpec;
        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.AT_MOST);
        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            assert child != null;
            // 子view的宽度依次变大
            child.measure(childWidthMeasureSpec - childMargin +i*ladder, childHeightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        for (int i = 0; i < getChildCount(); i++) {
            boundsRect.set(0, 0, getWidth(), getHeight());

            View view = getChildAt(i);
            int w, h;
            w = view.getMeasuredWidth();
            h = view.getMeasuredHeight();

            Gravity.apply(Gravity.CENTER, w , h, boundsRect, childRect);
            // 子view依次向下错位排列
            view.layout(childRect.left, childRect.top + i * ladder, childRect.right, childRect.bottom + i * ladder);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mTopCard == null) {
            return false;
        }
        if (mGestureDetector.onTouchEvent(event)) { //GestureDetector中拦截的手势就不再处理，标记为已消费
            return true;
        }
        Log.d("Touch Event", MotionEvent.actionToString(event.getActionMasked()) + " ");
        final int pointerIndex;
        final float x, y;
        final float dx, dy;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTopCard.getHitRect(childRect); // 获取卡片所在区域的矩阵

                pointerIndex = event.getActionIndex(); //触控点的索引
                x = event.getX(pointerIndex); //触控点的x值
                y = event.getY(pointerIndex); //触控点y值

                if (!childRect.contains((int) x, (int) y)) { //判断当前点击的点是否在卡片矩阵范围中
                    return false;
                }
                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = event.getPointerId(pointerIndex); //触控点的索引id


                float[] points = new float[]{x - mTopCard.getLeft(), y - mTopCard.getTop()};
                mTopCard.getMatrix().invert(mMatrix);
                mMatrix.mapPoints(points);
                mTopCard.setPivotX(points[0]);
                mTopCard.setPivotY(points[1]);

                break;
            case MotionEvent.ACTION_MOVE:

                pointerIndex = event.findPointerIndex(mActivePointerId);
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);

                dx = x - mLastTouchX;
                dy = y - mLastTouchY;

                if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                }

                if(!mDragging) {
                    return true;
                }

                mTopCard.setTranslationX(mTopCard.getTranslationX() + dx);
                mTopCard.setTranslationY(mTopCard.getTranslationY() + dy);

                //todo 触摸点(x,y)，摆动动画
//                mTopCard.setRotation(40 * mTopCard.getTranslationX() / (getWidth() / 2.f));
//                mTopCard.setRotation(getDisorderedRotation());

                mLastTouchX = x;
                mLastTouchY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mDragging) {
                    return true;
                }
                mDragging = false;
                mActivePointerId = INVALID_POINTER_ID;
                ValueAnimator animator = ObjectAnimator.ofPropertyValuesHolder(mTopCard,
                        PropertyValuesHolder.ofFloat("translationX", 0),
                        PropertyValuesHolder.ofFloat("translationY", 0),
//                        PropertyValuesHolder.ofFloat("rotation", (float) Math.toDegrees(mRandom.nextGaussian() * DISORDERED_MAX_ROTATION_RADIANS)),
                        PropertyValuesHolder.ofFloat("rotation", 0),
                        PropertyValuesHolder.ofFloat("pivotX", mTopCard.getWidth() / 2.f),
                        PropertyValuesHolder.ofFloat("pivotY", mTopCard.getHeight() / 2.f)
                ).setDuration(250);
                animator.setInterpolator(new AccelerateInterpolator());
                animator.start();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                pointerIndex = event.getActionIndex();
                final int pointerId = event.getPointerId(pointerIndex);

                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = event.getX(newPointerIndex);
                    mLastTouchY = event.getY(newPointerIndex);

                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;
        }

        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mTopCard == null) {
            return false;
        }
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        final int pointerIndex;
        final float x, y;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTopCard.getHitRect(childRect);

                CardModel cardModel = (CardModel)getAdapter().getItem(getChildCount()-1);

                if (cardModel.getOnClickListener() != null) {
                    cardModel.getOnClickListener().OnClickListener();
                }
                pointerIndex = event.getActionIndex();
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);

                if (!childRect.contains((int) x, (int) y)) {
                    return false;
                }

                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = event.getPointerId(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                pointerIndex = event.findPointerIndex(mActivePointerId);
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);
                if (Math.abs(x - mLastTouchX) > mTouchSlop || Math.abs(y - mLastTouchY) > mTouchSlop) {
                    float[] points = new float[]{x - mTopCard.getLeft(), y - mTopCard.getTop()};
                    mTopCard.getMatrix().invert(mMatrix);
                    mMatrix.mapPoints(points);
                    mTopCard.setPivotX(points[0]);
                    mTopCard.setPivotY(points[1]);
                    return true;
                }
        }

        return false;
    }

    @Override
    public View getSelectedView() {
        // TODO: 获取选中的view
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSelection(int position) {
        // TODO: 获取选中的view
        throw new UnsupportedOperationException();
    }

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d("Fling", "Fling with " + velocityX + ", " + velocityY);
            final View topCard = mTopCard;
            float dx = e2.getX() - e1.getX();
            float dy = e2.getY() - e1.getY();
            if ((Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) &&
                    (Math.abs(velocityX) > mFlingSlop * 3 || Math.abs(velocityY) > mFlingSlop * 3)) {
                float targetX = topCard.getX();
                float targetY = topCard.getY();
                long duration = 0;

                boundsRect.set(0 - topCard.getWidth() - 100, 0 - topCard.getHeight() - 100, getWidth() + 100, getHeight() + 100);

                while (boundsRect.contains((int) targetX, (int) targetY)) {
                    targetX += velocityX / 10;
                    targetY += velocityY / 10;
                    duration += 100;
                }

                duration = Math.min(500, duration);

                mTopCard = getChildAt(getChildCount() - 2);
                CardModel cardModel = (CardModel)getAdapter().getItem(getChildCount() - 1);

                if(mTopCard != null)
                    mTopCard.setLayerType(LAYER_TYPE_HARDWARE, null);

                if (cardModel.getOnCardDismissedListener() != null) {
                    if ( targetX > 0 ) {
                        cardModel.getOnCardDismissedListener().onLike();
                    } else {
                        cardModel.getOnCardDismissedListener().onDislike();
                    }
                }

                // 移除card 动画
                topCard.animate()
                        .setDuration(duration)
                        .alpha(.75f)
                        .setInterpolator(new LinearInterpolator())
                        .x(targetX)
                        .y(targetY)
                        .rotation(Math.copySign(45, velocityX))
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                removeViewInLayout(topCard);
                                ensureFull();
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                onAnimationEnd(animation);
                            }
                        });
                return true;
            } else
                return false;
        }
    }
}
