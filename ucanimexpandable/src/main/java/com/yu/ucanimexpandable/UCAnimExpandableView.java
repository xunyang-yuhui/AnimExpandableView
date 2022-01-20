package com.yu.ucanimexpandable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 一款带动画的 “展开/收起”控件
 */
public class UCAnimExpandableView extends View {
    public static final int FOLLOW = 100;
    public static final int NEXT_LEFT = 101;
    public static final int NEXT_RIGHT = 102;
    //测量时最小的高度  （用于动画）
    private float minMeasureHeight;
    //测量时最大的高度
    private float maxMeasureHeight;

    private Context context;
    //展示文字
    private String text;
    //文字颜色
    private int textColor;
    //文字大小
    private int textSize;
    //展开文字
    private String expandText = "展开";
    //折叠文字
    private String closeText = "收起";
    //展开文字颜色
    private int expandColor;
    //展开文字大小
    private int expandSize;
    //折叠文字颜色
    private int closeColor;
    //折叠文字大小
    private int closeSize;
    //折叠文字 模式:跟随，另起一行居左， 另起一行居右
    private int closeMode;
    //最小展示行数
    private int minLines;
    private Paint mPaint;
    private Paint expandPaint;
    private TextPaint closePaint;
    private List<CharInfo> list;
    private float expandWidth;
    private float closeWidth;
    private float ellipsisWidth;
    //是否是展开状态
    private boolean isExpandState = false;
    private boolean isNeedExpand = false;
    // 用于点击偏移
    private final int offset = 5;
    private UCAnimExpandableView.onExpandClickListener onExpandClickListener;
    //用来框选 展开 文字的范围
    private Rect expandRect = new Rect();
    private Rect closeRect = new Rect();
    //判定点击范围
    private Region expandRegion = new Region();
    private Region closeRegion = new Region();

    public UCAnimExpandableView(Context context) {
        this(context, null);
    }

    public UCAnimExpandableView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UCAnimExpandableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.UCAnimExpandableView);
        //需要显示的文字
        text = array.getString(R.styleable.UCAnimExpandableView_uc_text);
        //文字颜色
        textColor = array.getColor(R.styleable.UCAnimExpandableView_uc_textColor, Color.BLACK);
        ///文字大小
        textSize = array.getDimensionPixelSize(R.styleable.UCAnimExpandableView_uc_textSize, 15);
        //展开文字
        expandText = array.getString(R.styleable.UCAnimExpandableView_uc_expandText);
        //展开文字大小
        expandSize = array.getDimensionPixelSize(R.styleable.UCAnimExpandableView_uc_textSize, 12);
        //展开文字颜色
        expandColor = array.getColor(R.styleable.UCAnimExpandableView_uc_expandColor, Color.parseColor("#ff5656"));
        //收起文字
        closeText = array.getString(R.styleable.UCAnimExpandableView_uc_closeText);
        //收起文字大小
        closeSize = array.getDimensionPixelSize(R.styleable.UCAnimExpandableView_uc_closeSize, 12);
        //收起文字颜色
        closeColor = array.getColor(R.styleable.UCAnimExpandableView_uc_closeColor, Color.parseColor("#ff5656"));
        //收起文字位置 模式
        closeMode = array.getInteger(R.styleable.UCAnimExpandableView_uc_closeMode, NEXT_RIGHT);
        //最小行数
        minLines = array.getInteger(R.styleable.UCAnimExpandableView_uc_minLines, 5);
        array.recycle();
        init();
        initText(text);
    }

    private void init() {
        if (TextUtils.isEmpty(expandText)) {
            expandText = "展开";
        }
        if (TextUtils.isEmpty(closeText)) {
            closeText = "收起";
        }
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(textColor);
        mPaint.setTextSize(textSize);
        mPaint.setStyle(Paint.Style.FILL);
        ellipsisWidth = mPaint.measureText("...");

        expandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        expandPaint.setColor(expandColor);
        expandPaint.setTextSize(expandSize);
        expandPaint.setStyle(Paint.Style.FILL);
        expandWidth = expandPaint.measureText(expandText);

        closePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        closePaint.setColor(closeColor);
        closePaint.setTextSize(closeSize);
        closePaint.setStyle(Paint.Style.FILL);
        closeWidth = closePaint.measureText(closeText);

        list = new ArrayList<>();
    }

    //对文案初始化
    private void initText(String text) {
        list.clear();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        char[] arrays = text.toCharArray();
        for (char c : arrays) {
            String str = String.valueOf(c);
            float textWidth = mPaint.measureText(str);
            CharInfo charInfo = new CharInfo();
            charInfo.setStr(str);
            charInfo.setWidth(textWidth);
            list.add(charInfo);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    //测量 宽度
    private int measureWidth(int widthMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int size = MeasureSpec.getSize(widthMeasureSpec);

        if (MeasureSpec.EXACTLY != mode) {
            size = 200;
        }
        return size;
    }

    private int measureHeight(int heightMeasureSpec) {
        //测量的高度
        float height = 0;
        //行数
        int lines = 1;
        //绘制文字左顶点
        float startY = getPaddingTop();
        //绘制文字起始位置
        float startX = getPaddingLeft();
        int size = MeasureSpec.getSize(heightMeasureSpec);
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        //文字高度
        float textHeight = mPaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;
        //记录下的宽度
        float width = startX;

        if ("".equals(text) || text == null) {
            return 0;
        }

        for (CharInfo info : list) {
            //换行
            if ("\n".equals(info.str)) {
                Log.i("yuhuizhong","do this ----->>>>>>>lines is :"+lines);
                lines = lines + 1;
                startY = startY + textHeight;
                startX = getPaddingLeft();
                width = startX;
                continue;
            }

            //小于当前行数
            if (lines < minLines) {
                isNeedExpand = false;
                //加上当前字符及右padding后超过行宽，则换行
                if (getPaddingRight() + width + info.width > getMeasuredWidth()) {
                    startY = startY + textHeight;
                    startX = getPaddingLeft();
                    width = startX + info.width;
                    lines = lines + 1;
                } else {
                    width = width + info.width;
                }
            } else if (lines == minLines) {
                //去掉当前字符 并替换为... 和 展开
                if (startX + getPaddingRight() + width + info.width + expandWidth + ellipsisWidth > getMeasuredWidth()) {
                    startY = startY + textHeight;
                    startX = getPaddingLeft();
                    width = startX + info.width;
                    lines = lines + 1;
                    isNeedExpand = true;
                } else {
                    //未超过正常测量
                    width = width + info.width;
                    isNeedExpand = false;
                    minMeasureHeight =getPaddingTop() +  minLines * textHeight;
                }
            } else {
                //超过当前行数
                isNeedExpand = true;
                if (getPaddingRight() + width + info.width > getMeasuredWidth()) {
                    startY = startY + textHeight;
                    startX = getPaddingLeft();
                    width = startX + info.width;
                    lines = lines + 1;
                } else {
                    width = width + info.width;
                }
            }
        }

        //需要展示 “展开/收起”
        if (isNeedExpand) {
            if (closeMode == FOLLOW) {
                if (width + closeWidth + getPaddingRight() > getMeasuredWidth()) {
                    startY = startY + closePaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;
                }
            } else if (closeMode == NEXT_LEFT) {
                startY = startY + closePaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;
            } else if (closeMode == NEXT_RIGHT) {
                startY = startY + closePaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;
            }
        }
        minMeasureHeight =minMeasureHeight +getPaddingBottom();
        maxMeasureHeight = startY + textHeight + getPaddingBottom();

        if (isNeedExpand) {
            if (isExpandState) {
                height = maxMeasureHeight;
            } else {
                height = minMeasureHeight;
            }
        } else {
            height = maxMeasureHeight;
        }

        if (MeasureSpec.EXACTLY == mode) {
            if (size == 0){
                if (isNeedExpand) {
                    if (isExpandState) {
                        height = maxMeasureHeight;
                    } else {
                        height = minMeasureHeight;
                    }
                } else {
                    height = maxMeasureHeight;
                }
            } else {
                height = size;
            }
        }
        Log.i("yuhuizhong","do this ---->>>>height is ： "+height);
        return (int) height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int currentLines = 1;
        float lineHeight = getPaddingTop() - mPaint.getFontMetrics().top;
        int paddingRight = getPaddingRight();
        float textWidth = getPaddingLeft();

        if (isExpandState) {            //展开状态
            for (CharInfo charInfo : list) {
                if ("\n".equals(charInfo.str)) {
                    lineHeight = lineHeight + mPaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;
                    textWidth = getPaddingLeft();
                    currentLines = currentLines + 1;
                    continue;
                }

                if (textWidth + charInfo.getWidth() + paddingRight > getMeasuredWidth()) {
                    //换行
                    lineHeight = lineHeight + mPaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;
                    textWidth = getPaddingLeft();
                    currentLines = currentLines + 1;
                    canvas.drawText(charInfo.str, (int) textWidth, lineHeight, mPaint);
                    textWidth = textWidth + charInfo.getWidth();
                } else {
                    canvas.drawText(charInfo.str, (int) textWidth, lineHeight, mPaint);
                    textWidth = textWidth + charInfo.getWidth();
                }
            }
            canvas.save();
            if (closeMode == FOLLOW) {
                if (textWidth + closeWidth + paddingRight > getMeasuredWidth()) {
                    closeRect.left = getPaddingLeft();
                    closeRect.top = (int) (lineHeight - (closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top) - offset);
                    closeRect.right = (int) (getPaddingLeft() + closeWidth);
                    closeRect.bottom = (int) (lineHeight + closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top);
                    lineHeight = lineHeight + closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top;
                    canvas.drawText(closeText, (int) getPaddingLeft(), lineHeight, closePaint);
                } else {
                    canvas.drawText(closeText, textWidth, lineHeight, closePaint);
                    closeRect.left = (int) textWidth - offset;
                    closeRect.top = (int) (lineHeight - (closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top) - offset);
                    closeRect.right = (int) (textWidth + closeWidth);
                    closeRect.bottom = (int) (lineHeight + closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top);
                }
            } else if (closeMode == NEXT_LEFT) {
                closeRect.left = getPaddingLeft();
                closeRect.top = (int) (lineHeight - (closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top) - offset);
                closeRect.right = (int) (getPaddingLeft() + closeWidth);
                closeRect.bottom = (int) (lineHeight + closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top);
                lineHeight = lineHeight + closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top;
                canvas.drawText(closeText, (int) getPaddingLeft(), lineHeight, closePaint);
            } else if (closeMode == NEXT_RIGHT) {
                closeRect.left = (int) (getMeasuredWidth() - closeWidth - getPaddingRight() - offset);
                closeRect.top = (int) (lineHeight - (closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top) - offset);
                closeRect.right = (int) getMeasuredWidth() - getPaddingRight();
                closeRect.bottom = (int) (lineHeight + closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top);
                lineHeight = lineHeight + closePaint.getFontMetrics().bottom - closePaint.getFontMetrics().top;
                canvas.drawText(closeText, (int) getMeasuredWidth() - closeWidth - getPaddingRight(), lineHeight, closePaint);
            }

            closeRegion.setEmpty();
            closeRegion.set(closeRect);
        } else {                            //收起状态
            for (CharInfo charInfo : list) {
                if ("\n".equals(charInfo.str)) {
                    if (currentLines >= minLines) {
                        //当前行为最小行数，且遇到换行，添加“...”和 “展开”，结束绘制
                        canvas.drawText("...", (int) getMeasuredWidth() - expandWidth - ellipsisWidth - getPaddingRight(), lineHeight, mPaint);
                        /**
                         * 圈选一个点击范围
                         */
                        expandRect.left = (int) (getMeasuredWidth() - expandWidth - getPaddingRight() - offset);
                        expandRect.top = (int) (lineHeight - (expandPaint.getFontMetrics().bottom - expandPaint.getFontMetrics().top) - offset);
                        expandRect.right = (int) getMeasuredWidth() - getPaddingRight();
                        expandRect.bottom = (int) (lineHeight + expandPaint.getFontMetrics().bottom - expandPaint.getFontMetrics().top);
                        canvas.drawText(expandText, (int) getMeasuredWidth() - expandWidth - getPaddingRight(), lineHeight, expandPaint);
                        expandRegion.setEmpty();
                        expandRegion.set(expandRect);
                        break;
                    } else {
                        //未超过最小行数，直接换行
                        lineHeight = lineHeight + mPaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;
                        textWidth = getPaddingLeft();
                        currentLines = currentLines + 1;
                        continue;
                    }
                }

                if (textWidth + charInfo.getWidth() + paddingRight > getMeasuredWidth()) {
                    //换行
                    lineHeight = lineHeight + mPaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;
                    textWidth = getPaddingLeft();
                    currentLines = currentLines + 1;
                    canvas.drawText(charInfo.getStr(), (int) textWidth, lineHeight, mPaint);
                    textWidth = textWidth + charInfo.getWidth();
                } else {
                    if (currentLines == minLines) {
                        if (textWidth + charInfo.getWidth() + expandWidth + ellipsisWidth + paddingRight > getMeasuredWidth()) {
                            canvas.drawText("...", (int) textWidth, lineHeight, mPaint);
                            expandRect.left = (int) (textWidth + ellipsisWidth - offset);
                            expandRect.top = (int) (lineHeight - (expandPaint.getFontMetrics().bottom - expandPaint.getFontMetrics().top) - offset);
                            expandRect.right = (int) (textWidth + ellipsisWidth + expandWidth);
                            expandRect.bottom = (int) (lineHeight + expandPaint.getFontMetrics().bottom - expandPaint.getFontMetrics().top);
                            canvas.drawText(expandText, textWidth + ellipsisWidth, lineHeight, expandPaint);
                            expandRegion.setEmpty();
                            expandRegion.set(expandRect);
                            return;
                        }
                    }
                    canvas.drawText(charInfo.str, (int) textWidth, lineHeight, mPaint);
                    textWidth = textWidth + charInfo.getWidth();
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_UP) {

            if (isExpandState) {
                //展开状态
                boolean isClickClose = closeRegion.contains(x, y);
                if (isClickClose) {
                    isExpandState = false;
                    requestLayout();
                    invalidate();
                    startCloseAnim();
                    if (onExpandClickListener != null) {
                        onExpandClickListener.onExpandChange(isExpandState);
                    }
                } else {
                    if (onExpandClickListener != null) {
                        onExpandClickListener.onExtraClick();
                    }
                }
            } else {
                boolean isClickExpand = expandRegion.contains(x, y);

                if (isClickExpand) {
                    isExpandState = true;
                    requestLayout();
                    invalidate();
                    startExpandAnim();
                    if (onExpandClickListener != null) {
                        onExpandClickListener.onExpandChange(isExpandState);
                    }
                } else {
                    if (onExpandClickListener != null) {
                        onExpandClickListener.onExtraClick();
                    }
                }
            }
        }
        return true;
    }

    private void startExpandAnim() {
        ViewWrapper viewWrapper = new ViewWrapper(this);
        ObjectAnimator expandAnim = ObjectAnimator.ofFloat(viewWrapper, "Height",
                minMeasureHeight, maxMeasureHeight).setDuration(300);
        expandAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                invalidate();
            }
        });

        expandAnim.start();
    }

    private void startCloseAnim() {
        ViewWrapper viewWrapper = new ViewWrapper(this);
        ObjectAnimator closeAnim = ObjectAnimator.ofFloat(viewWrapper,
                "Height", maxMeasureHeight, minMeasureHeight).setDuration(300);
        closeAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                invalidate();
            }
        });
        closeAnim.start();
    }



    public int getCloseMode() {
        return closeMode;
    }

    public void setCloseMode(int closeMode) {
        this.closeMode = closeMode;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        isNeedExpand = false;
        isExpandState = false;
        getLayoutParams().height = 0;
        initText(text);
        requestLayout();
        invalidate();
    }

    public void setOnExpandClickListener(UCAnimExpandableView.onExpandClickListener onExpandClickListener) {
        this.onExpandClickListener = onExpandClickListener;
    }

    class CharInfo {
        private String str;
        private float width;

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public float getWidth() {
            return width;
        }

        public void setWidth(float width) {
            this.width = width;
        }
    }

    class ViewWrapper {
        private View rView;

        public ViewWrapper(View target) {
            rView = target;
        }

        public int getHeight() {
            return rView.getLayoutParams().height;
        }

        public void setHeight(float height) {
            rView.getLayoutParams().height = (int) height;
            rView.requestLayout();
        }
    }

    public interface onExpandClickListener {
        //展开/收起 状态变化监听
        void onExpandChange(boolean isExpand);
        //点击除"展开/收起"其他地方监听
        void onExtraClick();
    }
}
