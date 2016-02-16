package com.example.jogle.calendar.doim;

import android.content.Context;

import com.example.jogle.calendar.widget.JGCalendarView;
import com.example.jogle.calendar.widget.JGCalendarView.CallBack;

public class JGCalendarViewBuilder {
	private JGCalendarView[] calendarViews;

	public  JGCalendarView[] createMassCalendarViews(Context context,int count,int style,CallBack callBack){
		calendarViews = new JGCalendarView[count];
		for(int i = 0; i < count;i++){
			calendarViews[i] = new JGCalendarView(context, style,callBack);
		}
		return calendarViews;
	}

	public  JGCalendarView[] createMassCalendarViews(Context context,int count,CallBack callBack){
		return createMassCalendarViews(context, count, JGCalendarView.MONTH_STYLE,callBack);
	}

	/**
	 * 切换CandlendarView的样式
	 * @param style
	 */
	public void swtichCalendarViewsStyle(int style){
		if(calendarViews != null)
			for(int i = 0 ;i < calendarViews.length;i++){
				calendarViews[i].switchStyle(style);
			}
	}
	/**
	 * CandlendarView回到当前日期
	 */

	public void backTodayCalendarViews(){
		if(calendarViews != null)
			for(int i = 0 ;i < calendarViews.length;i++){
				calendarViews[i].backToday();
			}
	}
}
