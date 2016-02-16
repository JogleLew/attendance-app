package com.example.jogle.calendar.widget;

import com.example.jogle.attendance.JGDBOperation;
import com.example.jogle.attendance.JGDataSet;
import com.example.jogle.calendar.JGCalendarActivity;
import com.example.jogle.calendar.util.JGDateUtil;
import com.example.jogle.calendar.doim.JGCustomDate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.Calendar;
import java.util.List;

public class JGCalendarView extends View {
	public static List<JGDataSet> list;
	private static final String TAG = "CalendarView";

	public static final int MONTH_STYLE = 0;
	public static final int WEEK_STYLE = 1;

	private static final int TOTAL_COL = 7;
	private static final int TOTAL_ROW = 6;

	private int passedDays;
	private int signedDays;
	private int outDays;
	private int inDays;
	private Paint mCirclePaint;
    private Paint mLinePaint;
	private Paint mTextPaint;
	private Paint mRecordPaint;
	private int mViewWidth;
	private int mViewHight;
	private int mCellSpace;
	private Row rows[] = new Row[TOTAL_ROW];
	private static JGCustomDate mShowDate;
	public static int style = MONTH_STYLE;
	private static final int WEEK = 7;
	private CallBack mCallBack;
	private int touchSlop;
	private boolean callBackCellSpace;

	public interface CallBack {
		void setStatistics(int passedDays, int signedDays, int outDays, int inDays);

		void clickDate(JGCustomDate date);

		void onMesureCellHeight(int cellSpace);

		void changeDate(JGCustomDate date);
	}

	public JGCalendarView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);

	}

	public JGCalendarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);

	}

	public JGCalendarView(Context context) {
		super(context);
		init(context);
	}

	public JGCalendarView(Context context, int style, CallBack mCallBack) {
		super(context);
		JGCalendarView.style = style;
		this.mCallBack = mCallBack;
		init(context);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		passedDays = 0;
		signedDays = 0;
		outDays = 0;
		inDays = 0;
		for (int i = 0; i < TOTAL_ROW; i++) {
			if (rows[i] != null)
				rows[i].drawCells(canvas);
		}
		mCallBack.setStatistics(passedDays, signedDays, outDays, inDays);
	}

	private void init(Context context) {
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mCirclePaint.setStyle(Paint.Style.FILL);
		mCirclePaint.setColor(Color.parseColor("#01aff4"));
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setColor(Color.parseColor("#217ac1"));
        mLinePaint.setStrokeWidth((float) 3.0);
		mRecordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mRecordPaint.setStyle(Paint.Style.FILL);
		touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		initDate();

	}

	private void initDate() {
		if (style == MONTH_STYLE) {
			mShowDate = new JGCustomDate();
		} else if(style == WEEK_STYLE ) {
			mShowDate = JGDateUtil.getNextSunday();
		}
		fillDate();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mViewWidth = w;
		mViewHight = h;
		mCellSpace = Math.min(mViewHight / TOTAL_ROW, mViewWidth / TOTAL_COL);
		if (!callBackCellSpace) {
			mCallBack.onMesureCellHeight(mCellSpace);
			callBackCellSpace = true;
		}
		mTextPaint.setTextSize(mCellSpace / 3);
	}

	private Cell mClickCell;
	private float mDownX;
	private float mDownY;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDownX = event.getX();
			mDownY = event.getY();
			break;
		case MotionEvent.ACTION_UP:
			float disX = event.getX() - mDownX;
			float disY = event.getY() - mDownY;
			if (Math.abs(disX) < touchSlop && Math.abs(disY) < touchSlop) {
				int col = (int) (mDownX / mCellSpace);
				int row = (int) (mDownY / mCellSpace);
				measureClickCell(col, row);
			}
			break;
		}
		return true;
	}

	private void measureClickCell(int col, int row) {
		if (col >= TOTAL_COL || row >= TOTAL_ROW)
			return;
		if (mClickCell != null) {
			rows[mClickCell.j].cells[mClickCell.i] = mClickCell;
		}
		if (rows[row] != null) {
			mClickCell = new Cell(rows[row].cells[col].date,
					rows[row].cells[col].state, rows[row].cells[col].i,
					rows[row].cells[col].j);
			rows[row].cells[col].state = State.CLICK_DAY;
			JGCustomDate date = rows[row].cells[col].date;
			date.week = col;
			mCallBack.clickDate(date);
			invalidate();
		}
	}

	class Row {
		public int j;

		Row(int j) {
			this.j = j;
		}

		public Cell[] cells = new Cell[TOTAL_COL];

		public void drawCells(Canvas canvas) {
			for (int i = 0; i < cells.length; i++) {
				if (cells[i] != null)
					cells[i].drawSelf(canvas);
			}

		}
	}

	class Cell {
		public JGCustomDate date;
		public State state;
		public int i;
		public int j;

		public Cell(JGCustomDate date, State state, int i, int j) {
			super();
			this.date = date;
			this.state = state;
			this.i = i;
			this.j = j;
		}

		public void drawSelf(Canvas canvas) {
			Calendar now = Calendar.getInstance();
			Calendar calendar = date.toCalendar();
			boolean isIn = false, isOut = false;
			double count = 0, countIn = 0, countOut = 0;
            int inSignIn = 0, inSignOut = 0, outSignIn = 0, outSignOut = 0;
			for (int i = 0; i < list.size(); i++) {
                JGDataSet item = list.get(i);
                String timeString = item.getTime().split(" ")[1];
				if (item.hasSameDate(calendar)) {
                    switch (item.getType()) {
                        case 0:
                            outSignIn = toSecond(timeString);
                            break;
                        case 1:
                            inSignIn = toSecond(timeString);
                            break;
                        case 2:
                            inSignOut = toSecond(timeString);
                            // check validation
                            if (inSignOut - inSignIn >= JGCalendarActivity.WHOLE_WORK_HOUR * 3600)
                                countIn = 1;
                            else if (inSignOut - inSignIn >= JGCalendarActivity.HALF_WORK_HOUR * 3600)
                                countIn = 0.5;
                            else
                                countIn = 0;
                            // check finish
                            if (inSignIn > 0 && inSignOut > 0)
                                isIn = true;
                            else
                                isIn = false;
                            break;
                        case 3:
                            outSignOut = toSecond(timeString);
                            // check validation
                            if (outSignOut - outSignIn >= JGCalendarActivity.HALF_WORK_HOUR * 3600)
                                countOut += 1;
                            else
                                countOut += 0.5;
                            // check finish
                            if (outSignIn > 0 && outSignOut > 0)
                                isOut = true;
                            else
                                isOut = false;
                            break;
                    }
				}
			}
            count = countIn + countOut;

			if (state == State.CURRENT_MONTH_DAY || state == State.CLICK_DAY) {
                if (isOut)
                    outDays++;
                if (isIn)
                    inDays++;
                if (count >= 1)
                    signedDays++;
                if (calendar.compareTo(now) < 0)
				    passedDays++;
            }

            if (count >= 1) {
                mTextPaint.setColor(Color.parseColor("#FF16ED00")); //green
            }
            else
                mTextPaint.setColor(Color.parseColor("#B6FF5E3A")); //red

			switch (state) {
			case CURRENT_MONTH_DAY:
				if (calendar.compareTo(now) < 0) {
                    if (isIn) {
                        mRecordPaint.setColor(Color.parseColor("#FFFFCE3A"));
                        canvas.drawCircle((float) (mCellSpace * (i + 0.8)),
                                (float) ((j + 0.4) * mCellSpace), mCellSpace / 12,
                                mRecordPaint);
                    }
                    if (isOut) {
                        mRecordPaint.setColor(Color.parseColor("#FF10CF9B"));
                        canvas.drawCircle((float) (mCellSpace * (i + 0.95)),
                                (float) ((j + 0.4) * mCellSpace), mCellSpace / 12,
                                mRecordPaint);
                    }
				}
				else {
					mTextPaint.setColor(Color.parseColor("#ffcfcfcf"));  //grey
				}
				break;
			case NEXT_MONTH_DAY:
			case PAST_MONTH_DAY:
				mTextPaint.setColor(Color.parseColor("#ffbbbbbb"));
				break;
			case TODAY:
				mTextPaint.setColor(Color.parseColor("#FFF24949"));
				break;
			case CLICK_DAY:
                if (calendar.compareTo(now) < 0) {
                    if (isIn) {
                        mRecordPaint.setColor(Color.parseColor("#FFFFCE3A"));
                        canvas.drawCircle((float) (mCellSpace * (i + 0.8)),
                                (float) ((j + 0.4) * mCellSpace), mCellSpace / 12,
                                mRecordPaint);
                    }
                    if (isOut) {
                        mRecordPaint.setColor(Color.parseColor("#FF10CF9B"));
                        canvas.drawCircle((float) (mCellSpace * (i + 0.95)),
                                (float) ((j + 0.4) * mCellSpace), mCellSpace / 12,
                                mRecordPaint);
                    }
                }
				else
					mTextPaint.setColor(Color.parseColor("#ffcfcfcf"));

				canvas.drawLine((float) (mCellSpace * (i + 0.25)), (float) (mCellSpace * (j + 0.75)),
						(float) (mCellSpace * (i + 0.75)), (float) (mCellSpace * (j + 0.75)), mLinePaint);
				break;
			}

            String content = date.day + "";
			canvas.drawText(content,
					(float) ((i + 0.5) * mCellSpace - mTextPaint.measureText(content) / 2),
					(float) ((j + 0.7) * mCellSpace - mTextPaint.measureText(
							content, 0, 1) / 2), mTextPaint);
		}

        private int toSecond(String time) {
            int hour, minute, second;
            String[] x = time.split(":");
            if (x.length < 3)
                return 0;
            hour = Integer.parseInt(x[0]);
            minute = Integer.parseInt(x[1]);
            second = Integer.parseInt(x[2]);
            return hour * 3600 + minute * 60 + second;
        }
	}

	enum State {
		CURRENT_MONTH_DAY, PAST_MONTH_DAY, NEXT_MONTH_DAY, TODAY, CLICK_DAY;
	}

	private void fillDate() {
		if (style == MONTH_STYLE) {
			fillMonthDate();
		} else if(style == WEEK_STYLE) {
			fillWeekDate();
		}
		mCallBack.changeDate(mShowDate);
	}

	private void fillWeekDate() {
		int lastMonthDays = JGDateUtil.getMonthDays(mShowDate.year, mShowDate.month - 1);
		rows[0] = new Row(0);
		int day = mShowDate.day;
		for (int i = TOTAL_COL -1; i >= 0 ; i--) {
			day -= 1;
			if (day < 1) {
				day = lastMonthDays;
			}
			JGCustomDate date = JGCustomDate.modifiDayForObject(mShowDate, day);
			if (JGDateUtil.isToday(date)) {
				mClickCell = new Cell(date, State.TODAY, i, 0);
				date.week = i;
				mCallBack.clickDate(date);
				rows[0].cells[i] =  new Cell(date, State.CLICK_DAY, i, 0);
				continue;
			}
			rows[0].cells[i] = new Cell(date, State.CURRENT_MONTH_DAY,i, 0);
		}
	}

	private void fillMonthDate() {
		int monthDay = JGDateUtil.getCurrentMonthDay();
		int lastMonthDays = JGDateUtil.getMonthDays(mShowDate.year, mShowDate.month - 1);
		int currentMonthDays = JGDateUtil.getMonthDays(mShowDate.year, mShowDate.month);
		int firstDayWeek = JGDateUtil.getWeekDayFromDate(mShowDate.year, mShowDate.month);
		boolean isCurrentMonth = false;
		if (JGDateUtil.isCurrentMonth(mShowDate)) {
			isCurrentMonth = true;
		}
		int day = 0;
		for (int j = 0; j < TOTAL_ROW; j++) {
			rows[j] = new Row(j);
			for (int i = 0; i < TOTAL_COL; i++) {
				int postion = i + j * TOTAL_COL;
				if (postion >= firstDayWeek
						&& postion < firstDayWeek + currentMonthDays) {
					day++;
					if (isCurrentMonth && day == monthDay) {
						JGCustomDate date = JGCustomDate.modifiDayForObject(mShowDate, day);
						//mClickCell = new Cell(date,State.TODAY, i,j);
						date.week = i;
						//mCallBack.clickDate(date);
						//rows[j].cells[i] = new Cell(date,State.CLICK_DAY, i,j);
						rows[j].cells[i] = new Cell(date,State.CURRENT_MONTH_DAY, i,j);
						continue;
					}
					rows[j].cells[i] = new Cell(JGCustomDate.modifiDayForObject(mShowDate, day),
							State.CURRENT_MONTH_DAY, i, j);
				} else if (postion < firstDayWeek) {
					rows[j].cells[i] = new Cell(new JGCustomDate(mShowDate.year, mShowDate.month-1, lastMonthDays - (firstDayWeek- postion - 1)), State.PAST_MONTH_DAY, i, j);
				} else if (postion >= firstDayWeek + currentMonthDays) {
					rows[j].cells[i] = new Cell((new JGCustomDate(mShowDate.year, mShowDate.month+1, postion - firstDayWeek - currentMonthDays + 1)), State.NEXT_MONTH_DAY, i, j);
				}
			}
		}
	}

	public void update() {
		fillDate();
		invalidate();
	}
	
	public void backToday(){
		initDate();
		invalidate();
	}

	public void switchStyle(int style) {
		JGCalendarView.style = style;
		if (style == MONTH_STYLE) {
			update();
		} else if (style == WEEK_STYLE) {
			int firstDayWeek = JGDateUtil.getWeekDayFromDate(mShowDate.year,
					mShowDate.month);
			int day =  1 + WEEK - firstDayWeek;
			mShowDate.day = day;
			
			update();
		}
		
	}

	public void rightSilde() {
		if (style == MONTH_STYLE) {
			
			if (mShowDate.month == 12) {
				mShowDate.month = 1;
				mShowDate.year += 1;
			} else {
				mShowDate.month += 1;
			}
			
		} else if (style == WEEK_STYLE) {
			int currentMonthDays = JGDateUtil.getMonthDays(mShowDate.year, mShowDate.month);
			if (mShowDate.day + WEEK > currentMonthDays) {
				if (mShowDate.month == 12) {
					mShowDate.month = 1;
					mShowDate.year += 1;
				} else {
					mShowDate.month += 1;
				}
				mShowDate.day = WEEK - currentMonthDays + mShowDate.day;	
			}else{
				mShowDate.day += WEEK;
			
			}
		}
		update();
	}

	public void leftSilde() {
		
		if (style == MONTH_STYLE) {
			if (mShowDate.month == 1) {
				mShowDate.month = 12;
				mShowDate.year -= 1;
			} else {
				mShowDate.month -= 1;
			}
			
		} else if (style == WEEK_STYLE) {
			int lastMonthDays = JGDateUtil.getMonthDays(mShowDate.year, mShowDate.month);
			if (mShowDate.day - WEEK < 1) {
				if (mShowDate.month == 1) {
					mShowDate.month = 12;
					mShowDate.year -= 1;
				} else {
					mShowDate.month -= 1;
				}
				mShowDate.day = lastMonthDays - WEEK + mShowDate.day;
				
			}else{
				mShowDate.day -= WEEK;
			}
			Log.i(TAG, "leftSilde"+mShowDate.toString());
		}
		update();
	}
}
