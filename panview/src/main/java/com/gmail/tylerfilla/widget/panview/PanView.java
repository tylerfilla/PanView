package com.gmail.tylerfilla.widget.panview;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import java.util.Timer;
import java.util.TimerTask;

public class PanView extends FrameLayout {

    private static final long SCROLL_CHANGE_EXPIRATION = 200000000L;

    private static final boolean DEF_FILL_VIEWPORT_HEIGHT = false;
    private static final boolean DEF_FILL_VIEWPORT_WIDTH = false;

    private static final boolean DEF_USE_NATIVE_SMOOTH_SCROLL = true;

    private boolean fillViewportHeight;
    private boolean fillViewportWidth;

    private boolean useNativeSmoothScroll;

    private HorizontalScrollView scrollViewX;
    private ScrollView scrollViewY;

    private ScrollbarLens scrollbarLens;

    private long timeLastScrollChangeX;
    private long timeLastScrollChangeY;

    private boolean isScrollingX;
    private boolean isScrollingY;

    private int oldScrollX;
    private int oldScrollY;

    private OnPanChangeListener panChangeListener;
    private OnPanStopListener panStopListener;

    public PanView(Context context) {
        super(context);

        initialize();
        configure();
    }

    public PanView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize();
        handleAttrs(attrs, 0, 0);
        configure();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PanView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initialize();
        handleAttrs(attrs, defStyleAttr, defStyleRes);
        configure();
    }

    private void initialize() {
        scrollViewX = new HorizontalScrollView(getContext()) {

            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                // If scroll has expired
                if (System.nanoTime() - timeLastScrollChangeX > SCROLL_CHANGE_EXPIRATION) {
                    isScrollingX = false;
                }

                return true;
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                // Awaken scrollbars
                scrollbarLens.awakenScrollBars();

                // Send event to super for scroll behavior
                super.onTouchEvent(event);

                // Offset the touch location to account for horizontal scroll
                event.offsetLocation(getScrollX() - getLeft(), 0f);

                // Send event to vertical scroll view
                scrollViewY.dispatchTouchEvent(event);

                // Always consider events handled
                return true;
            }

            @Override
            protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);

                // Set X scroll flag
                isScrollingX = true;

                // Store time of this scroll change
                timeLastScrollChangeX = System.nanoTime();

                // Tell the scrollbar view to redraw
                scrollbarLens.postInvalidate();

                // Notify listener
                if (panChangeListener != null) {
                    panChangeListener.onPanChange(l, scrollViewY.getScrollY(), oldl, oldScrollY);
                }

                // Store value for later use elsewhere
                oldScrollX = l;
            }

        };

        scrollViewY = new ScrollView(getContext()) {

            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                // Awaken scrollbars
                scrollbarLens.awakenScrollBars();

                // If scroll has expired
                if (System.nanoTime() - timeLastScrollChangeY > SCROLL_CHANGE_EXPIRATION) {
                    isScrollingY = false;
                }

                // If scrolling or should start scrolling, intercept the event
                return isScrollingX || isScrollingY || super.onInterceptTouchEvent(event);
            }

            @Override
            protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);

                // Set Y scroll flag
                isScrollingY = true;

                // Store time of this scroll change
                timeLastScrollChangeY = System.nanoTime();

                // Tell the scrollbar view to redraw
                scrollbarLens.postInvalidate();

                // Notify listener
                if (panChangeListener != null) {
                    panChangeListener.onPanChange(scrollViewX.getScrollX(), t, oldScrollX, oldt);
                }

                // Store value for later use elsewhere
                oldScrollY = t;
            }

        };

        scrollbarLens = new ScrollbarLens(getContext());

        fillViewportHeight = DEF_FILL_VIEWPORT_HEIGHT;
        fillViewportWidth = DEF_FILL_VIEWPORT_WIDTH;

        useNativeSmoothScroll = DEF_USE_NATIVE_SMOOTH_SCROLL;

        // Schedule scroll expiration checks (a bit hacky, but this whole thing is, too...)
        new Timer().scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                // If X scroll has expired
                if (isScrollingX && System.nanoTime() - timeLastScrollChangeX > SCROLL_CHANGE_EXPIRATION) {
                    isScrollingX = false;

                    // Park X scroll
                    scrollViewX.scrollTo(scrollViewX.getScrollX(), scrollViewX.getScrollY());

                    // If not scrolling vertically, either, and stop listener is set, notify it
                    if (!isScrollingY && panStopListener != null) {
                        post(new Runnable() {

                            @Override
                            public void run() {
                                panStopListener.onPanStop();
                            }

                        });
                    }
                }

                // If Y scroll has expired
                if (isScrollingY && System.nanoTime() - timeLastScrollChangeY > SCROLL_CHANGE_EXPIRATION) {
                    isScrollingY = false;

                    // Park Y scroll
                    scrollViewY.scrollTo(scrollViewY.getScrollX(), scrollViewY.getScrollY());

                    // If not scrolling horizontally, either, and stop listener is set, notify it
                    if (!isScrollingX && panStopListener != null) {
                        post(new Runnable() {

                            @Override
                            public void run() {
                                panStopListener.onPanStop();
                            }

                        });
                    }
                }
            }

        }, 0L, 200L);
    }

    private void handleAttrs(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // Get styled attributes array
        TypedArray styledAttrs = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.PanView, defStyleAttr, defStyleRes);

        fillViewportHeight = styledAttrs.getBoolean(R.styleable.PanView_fillViewportHeight, fillViewportHeight);
        fillViewportWidth = styledAttrs.getBoolean(R.styleable.PanView_fillViewportWidth, fillViewportWidth);

        useNativeSmoothScroll = styledAttrs.getBoolean(R.styleable.PanView_useNativeSmoothScroll, useNativeSmoothScroll);

        // Recycle styled attributes array
        styledAttrs.recycle();
    }

    private void configure() {
        // Disable native scrollbars
        scrollViewX.setHorizontalScrollBarEnabled(false);
        scrollViewY.setVerticalScrollBarEnabled(false);

        // Fill viewport for each axis
        scrollViewX.setFillViewport(fillViewportWidth);
        scrollViewY.setFillViewport(fillViewportHeight);
    }

    public boolean isFillViewportWidth() {
        return fillViewportWidth;
    }

    public void setFillViewportWidth(boolean fillViewportWidth) {
        this.fillViewportWidth = fillViewportWidth;
    }

    public boolean isFillViewportHeight() {
        return fillViewportHeight;
    }

    public void setFillViewportHeight(boolean fillViewportHeight) {
        this.fillViewportHeight = fillViewportHeight;
    }

    public boolean isUseNativeSmoothScroll() {
        return useNativeSmoothScroll;
    }

    public void setUseNativeSmoothScroll(boolean useNativeSmoothScroll) {
        this.useNativeSmoothScroll = useNativeSmoothScroll;
    }

    public int getPanX() {
        return scrollViewX.getScrollX();
    }

    public void setPanX(int panX) {
        scrollViewX.scrollTo(panX, scrollViewX.getScrollY());
    }

    public int getPanY() {
        return scrollViewY.getScrollY();
    }

    public void setPanY(int panY) {
        scrollViewY.scrollTo(scrollViewY.getScrollX(), panY);
    }

    public void setOnPanChangeListener(OnPanChangeListener panChangeListener) {
        this.panChangeListener = panChangeListener;
    }

    public void setOnPanStopListener(OnPanStopListener panStopListener) {
        this.panStopListener = panStopListener;
    }

    public void panTo(int x, int y) {
        scrollViewX.scrollTo(x, scrollViewX.getScrollY());
        scrollViewY.scrollTo(scrollViewY.getScrollX(), y);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void smoothPanTo(int x, int y) {
        // If we should use native smooth scrolling (or need to)
        if (useNativeSmoothScroll || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Scroll natively
            scrollViewX.smoothScrollTo(x, scrollViewX.getScrollY());
            scrollViewY.smoothScrollTo(scrollViewY.getScrollX(), y);
        } else {
            // Use object animators for optional nicer scrolling
            ObjectAnimator animScrollX = ObjectAnimator.ofInt(scrollViewX, "scrollX", x);
            ObjectAnimator animScrollY = ObjectAnimator.ofInt(scrollViewY, "scrollY", y);

            // Use support library fast-out-slow-in interpolator (should be default, but configurable)
            animScrollX.setInterpolator(new FastOutSlowInInterpolator());
            animScrollY.setInterpolator(new FastOutSlowInInterpolator());

            // Use predefined "medium" animation time (should be default, but configurable)
            int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
            animScrollX.setDuration(duration);
            animScrollY.setDuration(duration);

            // Start smooth scroll
            animScrollX.start();
            animScrollY.start();
        }
    }

    public void panBy(int dx, int dy) {
        scrollViewX.scrollBy(dx, 0);
        scrollViewY.scrollBy(0, dy);
    }

    public void smoothPanBy(int dx, int dy) {
        // If we should use native smooth scrolling
        if (useNativeSmoothScroll) {
            // Scroll natively
            scrollViewX.smoothScrollBy(dx, 0);
            scrollViewY.smoothScrollBy(0, dy);
        } else {
            // Scroll smoothly to final position
            smoothPanTo(scrollViewX.getScrollX() + dx, scrollViewY.getScrollY() + dy);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Enforce number of children
        if (getChildCount() != 1) {
            if (getChildCount() > 1) {
                throw new IllegalStateException("PanView can only host one direct child");
            }

            return;
        }

        // Splice scroll views between this view and its child
        View child = getChildAt(0);
        removeAllViews();
        scrollViewY.addView(child);
        scrollViewX.addView(scrollViewY);
        addView(scrollViewX);

        // Add scrollbar lens
        addView(scrollbarLens);
    }

    private class ScrollbarLens extends ScrollView {

        public ScrollbarLens(Context context) {
            super(context);

            // Expand to whatever we're in
            setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            // Use a transparent background, lest we forfeit our lensiness
            setBackgroundColor(0);

            // Enable both scrollbars
            setHorizontalScrollBarEnabled(true);
            setVerticalScrollBarEnabled(true);
        }

        @Override
        public boolean awakenScrollBars() {
            return super.awakenScrollBars();
        }

        @Override
        protected int computeHorizontalScrollExtent() {
            return getWidth();
        }

        @Override
        protected int computeHorizontalScrollOffset() {
            return scrollViewX.getScrollX();
        }

        @Override
        protected int computeHorizontalScrollRange() {
            return scrollViewY.getChildAt(0).getWidth();
        }

        @Override
        protected int computeVerticalScrollExtent() {
            return getHeight();
        }

        @Override
        protected int computeVerticalScrollOffset() {
            return scrollViewY.getScrollY();
        }

        @Override
        protected int computeVerticalScrollRange() {
            return scrollViewY.getChildAt(0).getHeight();
        }

    }

}
