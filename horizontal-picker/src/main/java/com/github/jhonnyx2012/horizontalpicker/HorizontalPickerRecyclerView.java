package com.github.jhonnyx2012.horizontalpicker;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoUnit;

/**
 * Created by Jhonny Barrios on 22/02/2017.
 */

public class HorizontalPickerRecyclerView extends RecyclerView implements OnItemClickedListener, View.OnClickListener {

  //This is the number of empty views we need to put at the beginning and at the end of the adapter
  //in order to have the left most item or right most item centered when reaching the end of the list
  private final static int DUMMY_VIEW_OFFSET = 3;
  private HorizontalPickerAdapter adapter;
  private LinearLayoutManager layoutManager;
  private int itemWidth;
  private HorizontalPickerListener listener;
  private boolean scrollToSelectedPosition;
  private LinearSnapHelper snapHelper = new LinearSnapHelper();

  public HorizontalPickerRecyclerView(Context context) {
    super(context);
  }

  public HorizontalPickerRecyclerView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public HorizontalPickerRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void init(Context context,
                   final int daysToPlus,
                   final int initialOffset,
                   final boolean scrollToSelectedPosition,
                   final int mBackgroundColor,
                   final int mDateSelectedColor,
                   final int mDateSelectedTextColor,
                   final int mTodayDateTextColor,
                   final int mTodayDateBackgroundColor,
                   final int mDayOfWeekTextColor,
                   final int mUnselectedDayTextColor) {
    this.scrollToSelectedPosition = scrollToSelectedPosition;
    layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
    setLayoutManager(layoutManager);
    adapter = new HorizontalPickerAdapter(HorizontalPickerRecyclerView.this,
        getContext(),
        daysToPlus,
        initialOffset,
        mBackgroundColor,
        mDateSelectedColor,
        mDateSelectedTextColor,
        mTodayDateTextColor,
        mTodayDateBackgroundColor,
        mDayOfWeekTextColor,
        mUnselectedDayTextColor,
        DUMMY_VIEW_OFFSET);
    setAdapter(adapter);

    snapHelper.attachToRecyclerView(HorizontalPickerRecyclerView.this);
    removeOnScrollListener(onScrollListener);
    ViewTreeObserver vto = getViewTreeObserver();
    vto.addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        if (getMeasuredWidth() != 0) {
          getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
        itemWidth = (int) Math.floor(getMeasuredWidth() / 7D);
        adapter.setItemWidth(itemWidth);
        addOnScrollListener(onScrollListener);
      }
    });
  }

  private OnScrollListener onScrollListener = new OnScrollListener() {
    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
      super.onScrollStateChanged(recyclerView, newState);
      switch (newState) {
        case RecyclerView.SCROLL_STATE_IDLE:
          listener.onStopDraggingPicker();
          final View snapView = snapHelper.findSnapView(layoutManager);
          if (snapView == null) {
            return;
          }
          final ViewHolder viewHolder = recyclerView.findContainingViewHolder(snapView);
          if (viewHolder == null) {
            return;
          }
          int position = viewHolder.getAdapterPosition();
          if (position != -1 && position != adapter.getSelectedPosition()) {
            adapter.getSelectedPosition();
            Log.d("selection", "Scroll Idle select " + position);
            selectItem(position, true);
          }
          break;
        case SCROLL_STATE_DRAGGING:
          listener.onDraggingPicker();
          break;
      }
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
      super.onScrolled(recyclerView, dx, dy);
    }
  };

  private void selectItem(int position, boolean userAction) {
    if (position == NO_POSITION) {
      return;
    }
    if (position != adapter.getSelectedPosition()) {
      int unselectPosition = adapter.getSelectedPosition();
      adapter.setSelectedPosition(position);
      adapter.notifyItemChanged(unselectPosition);
      adapter.notifyItemChanged(position);
      listener.onDateSelected(adapter.getItem(position), userAction);
    }
  }

  public void setListener(HorizontalPickerListener listener) {
    this.listener = listener;
  }

  @Override
  public void onClickView(View v, int adapterPosition) {
    if (adapterPosition != adapter.getSelectedPosition()
        && adapter.getItemViewType(adapterPosition) != R.layout.item_placeholder) {
      Log.d("selection", "onClickView " + adapterPosition);
      selectItem(adapterPosition, true);
      if (scrollToSelectedPosition) {
        smoothScrollToPosition(adapterPosition);
      }
    }
  }

  @Override
  public void onClick(View v) {

  }

  @Override
  public void smoothScrollToPosition(int position) {
    final RecyclerView.SmoothScroller smoothScroller = new CenterSmoothScroller(getContext());
    smoothScroller.setTargetPosition(position);
    layoutManager.startSmoothScroll(smoothScroller);
  }

  public void setDate(LocalDate date) {
    Log.d("selection", "set date " + date);
    Day firstDay = adapter.getItem(DUMMY_VIEW_OFFSET);
    final int offset = (int) ChronoUnit.DAYS.between(firstDay.getDate(), date);
    selectItem(offset + DUMMY_VIEW_OFFSET, false);
    ((LinearLayoutManager) getLayoutManager()).scrollToPositionWithOffset(offset, 0);
  }

  public @Nullable LocalDate getSelectedDate() {
    if (adapter.getSelectedPosition() != -1) {
      return adapter.getItem(adapter.getSelectedPosition()).getDate();
    } else {
      return null;
    }
  }

  private static class CenterSmoothScroller extends LinearSmoothScroller {

    CenterSmoothScroller(Context context) {
      super(context);
    }

    @Override
    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
      return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
    }
  }
}
