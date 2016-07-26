package com.gmail.tylerfilla.widget.panview;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;

public class PanView extends FrameLayout {

    private static final long SCROLL_CHANGE_EXPIRATION = 200000000l;

    private static final boolean DEF_FILL_VIEWPORT_HEIGHT = false;
    private static final boolean DEF_FILL_VIEWPORT_WIDTH = false;

    private static final boolean DEF_USE_NATIVE_SMOOTH_SCROLL = true;

    private boolean fillViewportHeight;
    private boolean fillViewportWidth;

    private boolean useNativeSmoothScroll;

    private List<OnPanChangedListener> panChangedListenerList;
    private List<OnPanStoppedListener> panStoppedListenerList;

    private HorizontalScrollView scrollViewX;
    private ScrollView scrollViewY;

    private ScrollbarLens scrollbarLens;

    private boolean spliced;

    private long timeLastScrollChangeX;
    private long timeLastScrollChangeY;

    private boolean isScrollingX;
    private boolean isScrollingY;

    private int oldScrollX;
    private int oldScrollY;

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
        fillViewportHeight = DEF_FILL_VIEWPORT_HEIGHT;
        fillViewportWidth = DEF_FILL_VIEWPORT_WIDTH;

        useNativeSmoothScroll = DEF_USE_NATIVE_SMOOTH_SCROLL;

        panChangedListenerList = new ArrayList<>();
        panStoppedListenerList = new ArrayList<>();

        scrollViewX = new HorizontalScrollView(getContext()) {

            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                // Intercept all touch events
                return true;
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                // Send event to super for scroll behavior
                super.onTouchEvent(event);

                // Offset the touch location to account for horizontal scroll
                event.offsetLocation(getScrollX() - getLeft(), 0f);

                // Send event to vertical scroll view
                scrollViewY.dispatchTouchEvent(event);

                // Never let events propagate naturally here
                return true;
            }

            @Override
            protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);

                // Set X scroll flag
                isScrollingX = true;

                // Store time of this scroll change
                timeLastScrollChangeX = System.nanoTime();

                // Store X scroll value for later use elsewhere
                oldScrollX = l;

                // Notify listener(s)
                for (OnPanChangedListener listener : panChangedListenerList) {
                    listener.onPanChanged(l, scrollViewY.getScrollY(), oldl, oldScrollY);
                }
            }

            @Override
            protected boolean awakenScrollBars() {
                // Redirect to scrollbar lens
                scrollbarLens.awakenScrollBars();

                return super.awakenScrollBars();
            }

        };

        scrollViewY = new ScrollView(getContext()) {

            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
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

                // Store Y scroll value for later use elsewhere
                oldScrollY = t;

                // Notify listener(s)
                for (OnPanChangedListener listener : panChangedListenerList) {
                    listener.onPanChanged(scrollViewX.getScrollX(), t, oldScrollX, oldt);
                }
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                // Get child
                View child = getChildAt(0);

                // Sanity check
                if (child == null) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    return;
                }

                // Get child layout parameters
                ViewGroup.LayoutParams layoutParams = child.getLayoutParams();

                // Flags determining alteration state of width and height params
                boolean setWidth = false;
                boolean setHeight = false;

                // If width is set to match parent
                if (layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                    // Modify width to 'redirect' parenthood to root PanView
                    layoutParams.width = PanView.this.getMeasuredWidth();

                    // Set width alteration flag
                    setWidth = true;
                }

                // If height is set to match parent
                if (layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    // Modify height to 'redirect' parenthood to root PanView
                    layoutParams.height = PanView.this.getMeasuredHeight();

                    // Set height alteration flag
                    setHeight = true;
                }

                // If either width or height was altered
                if (setWidth || setHeight) {
                    // Update child layout parameters prior to measuring
                    child.setLayoutParams(layoutParams);
                }

                // Measure like normal
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);

                // Restore original width if altered
                if (setWidth) {
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                }

                // Restore original height if altered
                if (setHeight) {
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                }

                // Update child layout parameters after restoring
                child.setLayoutParams(layoutParams);

                // If should fill viewport width, but is currently too small
                if (fillViewportWidth && child.getMeasuredWidth() < PanView.this.getMeasuredWidth()) {
                    int old = layoutParams.width;

                    layoutParams.width = PanView.this.getMeasuredWidth();

                    child.setLayoutParams(layoutParams);

                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

                    layoutParams.width = old;

                    child.setLayoutParams(layoutParams);
                }
            }

            @Override
            protected boolean awakenScrollBars() {
                // Redirect to scrollbar lens
                scrollbarLens.awakenScrollBars();

                return super.awakenScrollBars();
            }

        };

        scrollbarLens = new ScrollbarLens(getContext());

        // Schedule scroll expiration checks (a bit hacky...)
        post(new Runnable() {

            @Override
            public void run() {
                // Whether panning has just stopped
                boolean stopped = false;

                // If X scroll has expired
                if (isScrollingX && System.nanoTime() - timeLastScrollChangeX > SCROLL_CHANGE_EXPIRATION) {
                    isScrollingX = false;

                    // Park X scroll
                    scrollViewX.scrollTo(scrollViewX.getScrollX(), scrollViewX.getScrollY());

                    // If not scrolling vertically, either
                    if (!isScrollingY) {
                        stopped = true;
                    }
                }

                // If Y scroll has expired
                if (isScrollingY && System.nanoTime() - timeLastScrollChangeY > SCROLL_CHANGE_EXPIRATION) {
                    isScrollingY = false;

                    // Park Y scroll
                    scrollViewY.scrollTo(scrollViewY.getScrollX(), scrollViewY.getScrollY());

                    // If not scrolling horizontally, either
                    if (!isScrollingX) {
                        stopped = true;
                    }
                }

                // Notify listeners if just stopped
                if (stopped) {
                    for (OnPanStoppedListener listener : panStoppedListenerList) {
                        listener.onPanStopped();
                    }
                }

                // Set up next iteration
                postDelayed(this, 200l);
            }

        });

        // Disable native scrollbars
        scrollViewX.setHorizontalScrollBarEnabled(false);
        scrollViewY.setVerticalScrollBarEnabled(false);
    }

    private void handleAttrs(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // Array of internal attributes specific to View
        int[] internalStyleableView = null;

        // Indices of relevant attributes in the above array
        int internalStyleableView_fadeScrollbars = -1;
        int internalStyleableView_scrollbarAlwaysDrawHorizontalTrack = -1;
        int internalStyleableView_scrollbarAlwaysDrawVerticalTrack = -1;
        int internalStyleableView_scrollbarDefaultDelayBeforeFade = -1;
        int internalStyleableView_scrollbarFadeDuration = -1;
        int internalStyleableView_scrollbarSize = -1;
        int internalStyleableView_scrollbarStyle = -1;
        int internalStyleableView_scrollbarThumbHorizontal = -1;
        int internalStyleableView_scrollbarThumbVertical = -1;
        int internalStyleableView_scrollbarTrackHorizontal = -1;
        int internalStyleableView_scrollbarTrackVertical = -1;
        int internalStyleableView_scrollbars = -1;

        // Flag indicating success of extraction of above information
        boolean internalStyleableViewExtracted = false;

        try {
            // Get private "styleable" from android.R
            Class internalStyleable = Class.forName("android.R$styleable");

            // Extract relevant information from android.R.styleable
            internalStyleableView = (int[]) internalStyleable.getDeclaredField("View").get(null);
            internalStyleableView_fadeScrollbars = internalStyleable.getDeclaredField("View_fadeScrollbars").getInt(null);
            internalStyleableView_scrollbarAlwaysDrawHorizontalTrack = internalStyleable.getDeclaredField("View_scrollbarAlwaysDrawHorizontalTrack").getInt(null);
            internalStyleableView_scrollbarAlwaysDrawVerticalTrack = internalStyleable.getDeclaredField("View_scrollbarAlwaysDrawVerticalTrack").getInt(null);
            internalStyleableView_scrollbarDefaultDelayBeforeFade = internalStyleable.getDeclaredField("View_scrollbarDefaultDelayBeforeFade").getInt(null);
            internalStyleableView_scrollbarFadeDuration = internalStyleable.getDeclaredField("View_scrollbarFadeDuration").getInt(null);
            internalStyleableView_scrollbarSize = internalStyleable.getDeclaredField("View_scrollbarSize").getInt(null);
            internalStyleableView_scrollbarStyle = internalStyleable.getDeclaredField("View_scrollbarStyle").getInt(null);
            internalStyleableView_scrollbarThumbHorizontal = internalStyleable.getDeclaredField("View_scrollbarThumbHorizontal").getInt(null);
            internalStyleableView_scrollbarThumbVertical = internalStyleable.getDeclaredField("View_scrollbarThumbVertical").getInt(null);
            internalStyleableView_scrollbarTrackHorizontal = internalStyleable.getDeclaredField("View_scrollbarTrackHorizontal").getInt(null);
            internalStyleableView_scrollbarTrackVertical = internalStyleable.getDeclaredField("View_scrollbarTrackVertical").getInt(null);
            internalStyleableView_scrollbars = internalStyleable.getDeclaredField("View_scrollbars").getInt(null);

            // Consider extraction successful if we made it this far
            internalStyleableViewExtracted = true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        // If extraction was successful
        if (internalStyleableViewExtracted) {
            // Get styled attributes for View
            TypedArray styledAttrsView = getContext().getTheme().obtainStyledAttributes(attrs, internalStyleableView, defStyleAttr, defStyleRes);

            /* Redirect to scrollbar lens any attributes pertaining to scrollbars */

            boolean fadeScrollbars = styledAttrsView.getBoolean(internalStyleableView_fadeScrollbars, scrollbarLens.isScrollbarFadingEnabled());
            int scrollbarStyle = styledAttrsView.getInteger(internalStyleableView_scrollbarStyle, scrollbarLens.getScrollBarStyle());
            int scrollbars = styledAttrsView.getInteger(internalStyleableView_scrollbars, (scrollbarLens.isHorizontalScrollBarEnabled() ? 0x00000100 : 0) | (scrollbarLens.isVerticalScrollBarEnabled() ? 0x00000200 : 0));

            setScrollbarFadingEnabled(fadeScrollbars);
            // noinspection WrongConstant
            setScrollBarStyle(scrollbarStyle);
            setHorizontalScrollBarEnabled((scrollbars & 0x00000100) > 0);
            setVerticalScrollBarEnabled((scrollbars & 0x00000200) > 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                int scrollbarDefaultDelayBeforeFade = styledAttrsView.getInteger(internalStyleableView_scrollbarDefaultDelayBeforeFade, scrollbarLens.getScrollBarDefaultDelayBeforeFade());
                int scrollbarFadeDuration = styledAttrsView.getInteger(internalStyleableView_scrollbarFadeDuration, scrollbarLens.getScrollBarFadeDuration());
                int scrollbarSize = styledAttrsView.getDimensionPixelSize(internalStyleableView_scrollbarSize, scrollbarLens.getScrollBarSize());

                setScrollBarDefaultDelayBeforeFade(scrollbarDefaultDelayBeforeFade);
                setScrollBarFadeDuration(scrollbarFadeDuration);
                setScrollBarSize(scrollbarSize);
            }

            // Recycle styled attributes for View
            styledAttrsView.recycle();
        }

        // Get styled attributes for PanView
        TypedArray styledAttrsPanView = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.PanView, defStyleAttr, defStyleRes);

        fillViewportHeight = styledAttrsPanView.getBoolean(R.styleable.PanView_fillViewportHeight, fillViewportHeight);
        fillViewportWidth = styledAttrsPanView.getBoolean(R.styleable.PanView_fillViewportWidth, fillViewportWidth);

        useNativeSmoothScroll = styledAttrsPanView.getBoolean(R.styleable.PanView_useNativeSmoothScroll, useNativeSmoothScroll);

        // Recycle styled attributes for PanView
        styledAttrsPanView.recycle();
    }

    private void configure() {
        // Fill viewport for each axis
        scrollViewX.setFillViewport(fillViewportWidth);
        scrollViewY.setFillViewport(fillViewportHeight);
    }

    @Override
    public boolean isScrollbarFadingEnabled() {
        return scrollbarLens.isScrollbarFadingEnabled();
    }

    @Override
    public void setScrollbarFadingEnabled(boolean fadeScrollbars) {
        scrollbarLens.setScrollbarFadingEnabled(fadeScrollbars);
    }

    @Override
    public int getScrollBarStyle() {
        return scrollbarLens.getScrollBarStyle();
    }

    @Override
    public void setScrollBarStyle(int style) {
        scrollbarLens.setScrollBarStyle(style);
    }

    @Override
    public boolean isHorizontalScrollBarEnabled() {
        return scrollbarLens.isHorizontalScrollBarEnabled();
    }

    @Override
    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        scrollbarLens.setHorizontalScrollBarEnabled(horizontalScrollBarEnabled);
    }

    @Override
    public boolean isVerticalScrollBarEnabled() {
        return scrollbarLens.isVerticalScrollBarEnabled();
    }

    @Override
    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        scrollbarLens.setVerticalScrollBarEnabled(verticalScrollBarEnabled);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public int getScrollBarDefaultDelayBeforeFade() {
        return scrollbarLens.getScrollBarDefaultDelayBeforeFade();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setScrollBarDefaultDelayBeforeFade(int scrollBarDefaultDelayBeforeFade) {
        scrollbarLens.setScrollBarDefaultDelayBeforeFade(scrollBarDefaultDelayBeforeFade);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public int getScrollBarFadeDuration() {
        return scrollbarLens.getScrollBarFadeDuration();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setScrollBarFadeDuration(int scrollBarFadeDuration) {
        scrollbarLens.setScrollBarFadeDuration(scrollBarFadeDuration);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public int getScrollBarSize() {
        return scrollbarLens.getScrollBarSize();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setScrollBarSize(int scrollBarSize) {
        scrollbarLens.setScrollBarSize(scrollBarSize);
    }

    public boolean isFillViewportWidth() {
        return fillViewportWidth;
    }

    public void setFillViewportWidth(boolean fillViewportWidth) {
        this.fillViewportWidth = fillViewportWidth;
        configure();
    }

    public boolean isFillViewportHeight() {
        return fillViewportHeight;
    }

    public void setFillViewportHeight(boolean fillViewportHeight) {
        this.fillViewportHeight = fillViewportHeight;
        configure();
    }

    public boolean isUseNativeSmoothScroll() {
        return useNativeSmoothScroll;
    }

    public void setUseNativeSmoothScroll(boolean useNativeSmoothScroll) {
        this.useNativeSmoothScroll = useNativeSmoothScroll;
        configure();
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

    public void addOnPanChangedListener(OnPanChangedListener listener) {
        panChangedListenerList.add(listener);
    }

    public void removeOnPanChangedListener(OnPanChangedListener listener) {
        panChangedListenerList.remove(listener);
    }

    public void addOnPanStoppedListener(OnPanStoppedListener listener) {
        panStoppedListenerList.add(listener);
    }

    public void removeOnPanStoppedListener(OnPanStoppedListener listener) {
        panStoppedListenerList.remove(listener);
    }

    public HorizontalScrollView getScrollViewX() {
        return scrollViewX;
    }

    public ScrollView getScrollViewY() {
        return scrollViewY;
    }

    public ScrollbarLens getScrollbarLens() {
        return scrollbarLens;
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

    public void fling(int velocityX, int velocityY) {
        scrollViewX.fling(velocityX);
        scrollViewY.fling(velocityY);
    }

    @Override
    public void addView(View child) {
        // Current child count
        int count;

        // Get child count
        if (spliced) {
            count = scrollViewY.getChildCount();
        } else {
            count = getChildCount();
        }

        // Enforce one child
        if (count != 0) {
            throw new IllegalStateException("PanView can only host one direct child");
        }

        // Add view to appropriate parent
        if (spliced) {
            scrollViewY.addView(child);
        } else {
            super.addView(child);
        }
    }

    @Override
    public void addView(View child, int index) {
        // Current child count
        int count;

        // Get child count
        if (spliced) {
            count = scrollViewY.getChildCount();
        } else {
            count = getChildCount();
        }

        // Enforce one child
        if (count != 0) {
            throw new IllegalStateException("PanView can only host one direct child");
        }

        // Add view to appropriate parent
        if (spliced) {
            scrollViewY.addView(child, index);
        } else {
            super.addView(child, index);
        }
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams layoutParams) {
        // Current child count
        int count;

        // Get child count
        if (spliced) {
            count = scrollViewY.getChildCount();
        } else {
            count = getChildCount();
        }

        // Enforce one child
        if (count != 0) {
            throw new IllegalStateException("PanView can only host one direct child");
        }

        // Add view to appropriate parent
        if (spliced) {
            scrollViewY.addView(child, layoutParams);
        } else {
            super.addView(child, layoutParams);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams layoutParams) {
        // Current child count
        int count;

        // Get child count
        if (spliced) {
            count = scrollViewY.getChildCount();
        } else {
            count = getChildCount();
        }

        // Enforce one child
        if (count != 0) {
            throw new IllegalStateException("PanView can only host one direct child");
        }

        // Add view to appropriate parent
        if (spliced) {
            scrollViewY.addView(child, index, layoutParams);
        } else {
            super.addView(child, index, layoutParams);
        }
    }

    @Override
    public void addView(View child, int width, int height) {
        // Current child count
        int count;

        // Get child count
        if (spliced) {
            count = scrollViewY.getChildCount();
        } else {
            count = getChildCount();
        }

        // Enforce one child
        if (count != 0) {
            throw new IllegalStateException("PanView can only host one direct child");
        }

        // Add view to appropriate parent
        if (spliced) {
            scrollViewY.addView(child, width, height);
        } else {
            super.addView(child, width, height);
        }
    }

    @Override
    public boolean awakenScrollBars() {
        return scrollbarLens.awakenScrollBars();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Splice views
        splice();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        // Create a stateful object based on super state
        SavedState savedState = new SavedState(super.onSaveInstanceState());

        // Save pan position
        savedState.panX = getPanX();
        savedState.panY = getPanY();

        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check if state is ours
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;

            // Pass on super state
            super.onRestoreInstanceState(savedState.getSuperState());

            // Restore pan position
            setPanX(savedState.panX);
            setPanY(savedState.panY);
        } else {
            // Not for us, pass it on
            super.onRestoreInstanceState(state);
        }
    }

    private void splice() {
        // Do not continue if already spliced
        if (spliced) {
            return;
        }

        // Splice scroll views between this view and its child
        View child = getChildAt(0);
        removeAllViews();
        scrollViewY.addView(child);
        scrollViewX.addView(scrollViewY);
        super.addView(scrollViewX, 0, generateDefaultLayoutParams());

        // Add scrollbar lens
        super.addView(scrollbarLens, 1, generateDefaultLayoutParams());

        // Set spliced flag
        spliced = true;
    }

    private class SavedState extends BaseSavedState {

        private int panX;
        private int panY;

        public SavedState(Parcel source) {
            super(source);

            // Read pan position
            panX = source.readInt();
            panY = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);

            // Write pan position
            out.writeInt(panX);
            out.writeInt(panY);
        }

        private final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }

        };

    }

    public class ScrollbarLens extends ScrollView {

        private ScrollbarLens(Context context) {
            super(context);

            // Use a transparent background, lest we forfeit our lensiness
            setBackgroundColor(0);

            // Enable horizontal scrollbar by default
            setHorizontalScrollBarEnabled(true);
        }

        @Override
        protected boolean awakenScrollBars() {
            // Redefine to provide intra-PanView access
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
