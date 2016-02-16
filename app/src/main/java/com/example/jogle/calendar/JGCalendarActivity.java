package com.example.jogle.calendar;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jogle.attendance.JGDBOperation;
import com.example.jogle.attendance.JGDataSet;
import com.example.jogle.attendance.JGDownload;
import com.example.jogle.attendance.JGDownloadCallBack;
import com.example.jogle.attendance.JGPeopleListAdapter;
import com.example.jogle.attendance.R;
import com.example.jogle.calendar.doim.JGCalendarViewBuilder;
import com.example.jogle.calendar.doim.JGCustomDate;
import com.example.jogle.calendar.widget.JGCalendarView;
import com.example.jogle.calendar.widget.JGCalendarView.CallBack;
import com.example.jogle.calendar.widget.JGCalendarViewPagerLisenter;
import com.example.jogle.calendar.widget.JGCustomViewPagerAdapter;
import com.ricky.database.CenterDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JGCalendarActivity extends Activity implements OnClickListener, CallBack, JGDownloadCallBack {
	public static final int SELF = -128;
	public static final int SUBORDINATE = -129;
    public static final int ONLY_USER = 0;
    public static final int ONLY_SUBORDINATE = 1;
    public static final int MAX_USER_NUM = 1000;

    public static String B3AF26AADF83CDDD;
    public static boolean F21893C82B19C60B = false;

    public static final int WHOLE_WORK_HOUR = 9;
    public static final int HALF_WORK_HOUR  = 4;

    private int type;

    private String nowString;
    private String firstString;

	private ViewPager viewPager;
	private JGCalendarView[] views;
	private TextView showYearView;
	private TextView showMonthView;
//	private TextView showWeekView;
	private JGCalendarViewBuilder builder = new JGCalendarViewBuilder();
	private View mContentPager;
	private JGCustomDate mClickDate = null;

	private LinearLayout myStatistics;
	private TabHost tabHost;

	private Spinner spinner;
	private String[] mData = {"我的签到统计", "下属签到统计"};
	private TextView inSignInTime;
    private TextView inSignOutTime;
//    private TextView outSignInTime;   // LRQ
//    private TextView outSignOutTime;  // LRQ
    private TextView inSignInLocation;
    private TextView inSignOutLocation;
//    private TextView outSignInLocation;
//    private TextView outSignOutLocation;

	private TextView noSelection;
	private TextView passedDays;
	private TextView outDays;
	private TextView inDays;
	private TextView signedRate;
    private JGArcView arcView;
    private JGArcView arcView2;

    private TextView todayDate;
    private TextView weekDates;
    private TextView monthDates;
	public static final String MAIN_ACTIVITY_CLICK_DATE = "main_click_date";

	private ListView peopleListweek;
	private ListView peopleListmonth;
	private JGPeopleListAdapter peopleListWeekAdapter;
	private JGPeopleListAdapter peopleListMonthAdapter;
	private List<JGDataSet> depListItems;
	private List<Map<String, Object>> depListWeek;
	private List<Map<String, Object>> depListMonth;

	private int uid;

    /* LRQ start */
    private LinearLayout clickme;
    private LinearLayout outAttendance;
    private LinearLayout outSignContent;
    /* LRQ end */

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.jg_activity_calendar);

		uid = getIntent().getIntExtra("uid", -1);
		String name = getIntent().getStringExtra("name");
        type = getIntent().getIntExtra("type", SELF);
        if (type != SUBORDINATE)
            type = SELF;

		JGDBOperation operation = new JGDBOperation(this);
		JGCalendarView.list = operation.getByUID(String.valueOf(uid));

		Calendar now = Calendar.getInstance();
		Calendar firstDay = Calendar.getInstance();
		firstDay.set(firstDay.get(Calendar.YEAR), firstDay.get(Calendar.MONTH), 1, 0, 0, 0);
        firstDay.add(Calendar.MONTH, -1);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		nowString = sdf.format(now.getTime()) + "_235959";
		firstString = sdf.format(firstDay.getTime()) + "_000000";

		JGDownload download1 = new JGDownload(this);
		download1.down(ONLY_USER, uid, firstString, nowString);

		findViewbyId();

        setUpTimeRange();

		tabHost.setup();
		tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("日统计").setContent(R.id.tab1));
		tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("周统计").setContent(R.id.tab2));
		tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator("月统计").setContent(R.id.tab3));
        for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextColor(0xFFFFFFFF);
        }

        if (type == SUBORDINATE)
            mData = new String[]{name + "的签到统计"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.jg_myspinner_att, mData);
		adapter.setDropDownViewResource(R.layout.jg_myspinner_dropdown_att);
		spinner = (Spinner) findViewById(R.id.spinner);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    myStatistics.bringToFront();
                } else if (position == 1) {
                    tabHost.bringToFront();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
		spinner.setVisibility(View.VISIBLE);

		depListWeek = new ArrayList<Map<String, Object>>();
		peopleListweek = (ListView) findViewById(R.id.peoplelistweek);
		peopleListWeekAdapter = new JGPeopleListAdapter(getApplicationContext(), depListWeek);
		peopleListweek.setAdapter(peopleListWeekAdapter);
        peopleListweek.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Map<String, Object> map = depListWeek.get(i);
                int uid = (int) map.get("uid");
                String name = (String) map.get("name");
                Intent intent = new Intent(JGCalendarActivity.this, JGCalendarActivity.class);
                intent.putExtra("uid", uid);
                intent.putExtra("name", name);
                intent.putExtra("type", JGCalendarActivity.SUBORDINATE);
                startActivity(intent);
            }
        });

		depListMonth = new ArrayList<Map<String, Object>>();
		peopleListmonth = (ListView) findViewById(R.id.peoplelistmonth);
		peopleListMonthAdapter = new JGPeopleListAdapter(getApplicationContext(), depListMonth);
		peopleListmonth.setAdapter(peopleListMonthAdapter);
        peopleListmonth.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Map<String, Object> map = depListMonth.get(i);
                int uid = (int) map.get("uid");
                String name = (String) map.get("name");
                Intent intent = new Intent(JGCalendarActivity.this, JGCalendarActivity.class);
                intent.putExtra("uid", uid);
                intent.putExtra("name", name);
                intent.putExtra("type", JGCalendarActivity.SUBORDINATE);
                startActivity(intent);
            }
        });

		RelativeLayout back = (RelativeLayout) findViewById(R.id.back);
		back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        /* LRQ start */
        final RelativeLayout changer = (RelativeLayout) findViewById(R.id.changer);
//        changer.measure(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
//        final int changerHeight2 = changer.getMeasuredHeight();

        final TextView clickTip = (TextView) findViewById(R.id.click_tip);
        clickme = (LinearLayout) findViewById(R.id.clickme);
        clickme.setOnClickListener(new OnClickListener() {
            private boolean needToExpand = true;
//            private boolean canDoIt = true;
            private int changerHeight1;

            @Override
            public void onClick(View v) {
//                if (canDoIt) {
//                    canDoIt = false;
                    if (needToExpand) {
                        changerHeight1 = changer.getHeight();
//                        Animation toExpand = new Animation() {
//                            @Override
//                            protected void applyTransformation(float interpolatedTime, Transformation t) {
//                                if (interpolatedTime == 1) {
//                                    canDoIt = true;
//                                    changer.getLayoutParams().height = RelativeLayout.LayoutParams.MATCH_PARENT;
//                                } else {
//                                    changer.getLayoutParams().height =
//                                            (int) ((changerHeight2 - changerHeight1) * interpolatedTime) + changerHeight1;
//                                    Log.d("ricky", "height:" + changer.getLayoutParams().height);
//                                }
//                                changer.requestLayout();
//                            }
//                        };
//                        toExpand.setDuration(500);
//                        changer.startAnimation(toExpand);
                        changer.getLayoutParams().height = RelativeLayout.LayoutParams.MATCH_PARENT;
                        changer.requestLayout();
                        needToExpand = false;
                        clickTip.setText("提示:点击本区域折起");
                    } else {
//                        final Animation toCollapse = new Animation() {
//                            @Override
//                            protected void applyTransformation(float interpolatedTime, Transformation t) {
//                                if (interpolatedTime == 1) {
//                                    canDoIt = true;
//                                }
//                                changer.getLayoutParams().height =
//                                        (int) ((changerHeight1 - changerHeight2) * interpolatedTime) + changerHeight2;
//                                changer.requestLayout();
//                                Log.d("ricky", "height:" + changer.getLayoutParams().height);
//                            }
//                        };
//                        toCollapse.setDuration(500);
//                        changer.startAnimation(toCollapse);
                        changer.getLayoutParams().height = changerHeight1;
                        changer.requestLayout();
                        needToExpand = true;
                        clickTip.setText("提示:点击本区域展开");
                    }
//                }
            }
        });
        clickme.setClickable(false);
        /* LRQ end */
	}

	private void findViewbyId() {
		inSignInTime = (TextView) findViewById(R.id.in_sign_in_time);
		inSignOutTime = (TextView) findViewById(R.id.in_sign_out_time);
//        outSignInTime = (TextView) findViewById(R.id.out_sign_in_time);     // LRQ
//        outSignOutTime = (TextView) findViewById(R.id.out_sign_out_time);   // LRQ
        inSignInLocation = (TextView) findViewById(R.id.in_sign_in_location);
        inSignOutLocation = (TextView) findViewById(R.id.in_sign_out_location);
//        outSignInLocation = (TextView) findViewById(R.id.out_sign_in_location);
//        outSignOutLocation = (TextView) findViewById(R.id.out_sign_out_location);
		noSelection = (TextView) findViewById(R.id.no_selection);
		passedDays = (TextView) findViewById(R.id.passed_days);
		signedRate = (TextView) findViewById(R.id.signed_rate);
		outDays = (TextView) findViewById(R.id.out_days);
		inDays = (TextView) findViewById(R.id.in_days);
        arcView = (JGArcView) findViewById(R.id.arc_view);
        arcView2 = (JGArcView) findViewById(R.id.arc_view_2);
//		signedBar = (TextView) findViewById(R.id.signed_bar);
//		unsignedBar = (TextView) findViewById(R.id.unsigned_bar);
        todayDate = (TextView) findViewById(R.id.today_date);
        weekDates = (TextView) findViewById(R.id.week_dates);
        monthDates = (TextView) findViewById(R.id.month_dates);

		myStatistics = (LinearLayout) findViewById(R.id.my_statistics);
		tabHost = (TabHost) findViewById(R.id.tabHost);

		viewPager = (ViewPager) this.findViewById(R.id.viewpager);
		showMonthView = (TextView)this.findViewById(R.id.show_month_view);
		showYearView = (TextView)this.findViewById(R.id.show_year_view);
		views = builder.createMassCalendarViews(this, 5, this);
		mContentPager = this.findViewById(R.id.contentPager);
		setViewPager();

        /* LRQ start */
        outAttendance = (LinearLayout) findViewById(R.id.out_attendance);
        outSignContent = (LinearLayout) findViewById(R.id.out_sign_content);
        /* LRQ end */
	}


    private void setUpTimeRange() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        String nowString = sdf.format(now.getTime());
        todayDate.setText(nowString);

        Calendar week = Calendar.getInstance();
        week.add(Calendar.DAY_OF_MONTH, -7);
        String weekString = sdf.format(week.getTime());
        weekDates.setText(weekString + " 至 " + nowString);

        Calendar month = Calendar.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1);
        String monthString = sdf.format(month.getTime());
        monthDates.setText(monthString + " 至 " + nowString);
    }

	public void setViewPager() {
		JGCustomViewPagerAdapter<JGCalendarView> viewPagerAdapter = new JGCustomViewPagerAdapter<JGCalendarView>(views);
		viewPager.setAdapter(viewPagerAdapter);
		viewPager.setCurrentItem(498); 
		viewPager.setOnPageChangeListener(new JGCalendarViewPagerLisenter(viewPagerAdapter));
	}

    @Override
    protected void onDestroy() {
     super.onDestroy();  
 }  

    public void setShowDateViewText(int year ,int month){
        showYearView.setText(year+"");
        showMonthView.setText(month + "月");
    }

	@Override
	public void onMesureCellHeight(int cellSpace) {}

	@Override
	public void setStatistics(int passedDays, int signedDays, int outDays, int inDays) {
        int rate;
        rate = (int)(signedDays * 100.0 / passedDays);
		this.passedDays.setText(String.valueOf(passedDays));
		this.signedRate.setText(String.valueOf(rate));
		this.outDays.setText(String.valueOf(outDays));
		this.inDays.setText(String.valueOf(inDays));
        arcView.setPercent(rate);
	}

	@Override
	public void clickDate(JGCustomDate date) {
		mClickDate = date;
		noSelection.setVisibility(View.GONE);
        /* LRQ start */
        clickme.setClickable(true);
        outAttendance.setVisibility(View.GONE);
        outSignContent.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        View outSignView;
        /* LRQ end */

        inSignInTime.setText("暂无数据");
		inSignOutTime.setText("暂无数据");
        inSignInLocation.setText("");
        inSignOutLocation.setText("");
//        outSignInTime.setText("暂无数据");  // LRQ
//        outSignOutTime.setText("暂无数据"); // LRQ
//        outSignInLocation.setText("");
//        outSignOutLocation.setText("");

		for (int i = 0; i < JGCalendarView.list.size(); i++) {
			JGDataSet item = JGCalendarView.list.get(i);
			if (item.hasSameDate(mClickDate.toCalendar())) {
				switch (item.getType()) {
				    /* LRQ start */
//					case 0: if (outSignInTime.getText().equals("暂无数据"))
//                                outSignInTime.setText("");
//                            outSignInTime.setText(outSignInTime.getText() +
//                                getMinuteTime(item.getTime()) + " ");
//						break;
                    case 0:
                        if (outAttendance.getVisibility() == View.GONE) {
                            outAttendance.setVisibility(View.VISIBLE);
                        }
                        outSignView = inflater.inflate(R.layout.out_sign_view, null);
                        ((TextView) outSignView.findViewById(R.id.out_sign_time)).setText(getMinuteTime(item.getTime()));
                        ((TextView) outSignView.findViewById(R.id.out_sign_location)).setText(item.getPosition());
                        ((TextView) outSignView.findViewById(R.id.tv)).setText("外勤往程");
                        outSignContent.addView(outSignView);
                        break;
                    /* LRQ end */
                    case 1: inSignInTime.setText(getMinuteTime(item.getTime()));
                            inSignInLocation.setText(item.getPosition());
						break;
					case 2: inSignOutTime.setText(getMinuteTime(item.getTime()));
                            inSignOutLocation.setText(item.getPosition());
                        break;
				    /* LRQ start */
//                    case 3: if (outSignOutTime.getText().equals("暂无数据"))
//                        outSignOutTime.setText("");
//                        outSignOutTime.setText(outSignOutTime.getText() +
//                                getMinuteTime(item.getTime()) + " ");
//                        break;
                    case 3:
                        if (outAttendance.getVisibility() == View.GONE) {
                            outAttendance.setVisibility(View.VISIBLE);
                        }
                        outSignView = inflater.inflate(R.layout.out_sign_view, null);
                        ((TextView) outSignView.findViewById(R.id.out_sign_time)).setText(getMinuteTime(item.getTime()));
                        ((TextView) outSignView.findViewById(R.id.out_sign_location)).setText(item.getPosition());
                        ((TextView) outSignView.findViewById(R.id.tv)).setText("外勤返程");
                        outSignContent.addView(outSignView);
                    /* LRQ end */
				}
			}
		}
	}

	@Override
	public void changeDate(JGCustomDate date) {
		setShowDateViewText(date.year, date.month);
	}

	@Override
	public void onClick(View v) {}

//	public List<Map<String, Object>> getData() {
//		listItems = new ArrayList<Map<String, Object>>();

//		Map<String, Object> map = new HashMap<String, Object>();
//		map.put("name", "员工1");
//		map.put("department", "部门1");
//		map.put("signedDays", 5);
//		map.put("passedDays", 16);
//		map.put("uid", 1);
//		listItems.add(map);
//
//		return listItems;
//	}

	private void getData() {
        int total = 0;
        int[] uids = new int[MAX_USER_NUM];
        int[] monthSigned = new int[MAX_USER_NUM]; // 月统计情况
        int[] weekSigned = new int[MAX_USER_NUM]; // 周统计情况
        boolean[] daySigned = new boolean[MAX_USER_NUM]; // 日统计情况
        double[] record = new double[32];

        int passedDays = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        Calendar sevenDaysAgo = Calendar.getInstance();
        sevenDaysAgo.add(Calendar.DAY_OF_MONTH, -7);

        Calendar firstDay = Calendar.getInstance();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        String sevenDayString = sdf.format(sevenDaysAgo.getTime());
        String firstDayString = sdf.format(firstDay.getTime());

        // get all uids
        for (JGDataSet ds : depListItems) {
            int targetUid = ds.getUserID();
            boolean isFound = false;
            for (int i = 0; i < total; i++) {
                int uid = uids[i];
                if (uid == targetUid) {
                    isFound = true;
                    break;
                }
            }
            if (!isFound) {
                uids[total++] = targetUid;
            }
        }

        int numOfSignedToday = 0;

        // calculate month statistics
        for (int i = 0; i < total; i++) {
            int uid = uids[i];
            int inSignIn = 0, inSignOut = 0, outSignIn = 0, outSignOut = 0;

            // initialize record array
            for (int j = 1; j <= passedDays; j++)
                record[j] = 0;

            for (JGDataSet ds : depListItems) {
                if (ds.getTime().compareTo(firstDayString) < 0 ||
                        ds.getUserID() != uid)
                    continue;
                String dayString = ds.getTime().split("月")[1].split("日")[0];
                int day = Integer.parseInt(dayString);
                String timeString = ds.getTime().split(" ")[1];

                switch (ds.getType()) {
                    case 0:
                        outSignIn = toSecond(timeString);
                        break;
                    case 1:
                        inSignIn = toSecond(timeString);
                        break;
                    case 2:
                        inSignOut = toSecond(timeString);
                        if (inSignOut - inSignIn >= JGCalendarActivity.WHOLE_WORK_HOUR * 3600)
                            record[day] += 1;
                        else if (inSignOut - inSignIn >= JGCalendarActivity.HALF_WORK_HOUR * 3600)
                            record[day] += 0.5;
                        inSignIn = 0; inSignOut = 0;
                        break;
                    case 3:
                        outSignOut = toSecond(timeString);
                        if (outSignOut > 0 && outSignOut - outSignIn >= JGCalendarActivity.HALF_WORK_HOUR * 3600)
                            record[day] += 1;
                        else if (outSignOut > 0)
                            record[day] += 0.5;
                        outSignIn = 0; outSignOut = 0;
                        break;
                }
            }
            int count = 0;
            daySigned[i] = false;
            for (int j = 1; j <= passedDays; j++)
                if (record[j] >= 1) {
                    count++;
                    if (j == passedDays) {
                        numOfSignedToday++;
                        daySigned[i] = true;
                    }
                }
            monthSigned[i] = count;
        }

        // calculate week statistics
        for (int i = 0; i < total; i++) {
            int uid = uids[i];
            int inSignIn = 0, inSignOut = 0, outSignIn = 0, outSignOut = 0;

            // initialize record array
            for (int j = 1; j <= 31; j++)
                record[j] = 0;

            for (JGDataSet ds : depListItems) {
                if (ds.getTime().compareTo(sevenDayString) < 0 ||
                        ds.getUserID() != uid)
                    continue;
                String dayString = ds.getTime().split("月")[1].split("日")[0];
                int day = Integer.parseInt(dayString);
                String timeString = ds.getTime().split(" ")[1];

                switch (ds.getType()) {
                    case 0:
                        outSignIn = toSecond(timeString);
                        break;
                    case 1:
                        inSignIn = toSecond(timeString);
                        break;
                    case 2:
                        inSignOut = toSecond(timeString);
                        if (inSignOut - inSignIn >= JGCalendarActivity.WHOLE_WORK_HOUR * 3600)
                            record[day] += 1;
                        else if (inSignOut - inSignIn >= JGCalendarActivity.HALF_WORK_HOUR * 3600)
                            record[day] += 0.5;
                        inSignIn = 0; inSignOut = 0;
                        break;
                    case 3:
                        outSignOut = toSecond(timeString);
                        if (outSignOut > 0 && outSignOut - outSignIn >= JGCalendarActivity.HALF_WORK_HOUR * 3600)
                            record[day] += 1;
                        else if (outSignOut > 0)
                            record[day] += 0.5;
                        outSignIn = 0; outSignOut = 0;
                        break;
                }
            }
            int count = 0;
            for (int j = 1; j <= 31; j++)
                if (record[j] >= 1)
                    count++;
            weekSigned[i] = count;
        }

        TextView dayNames = (TextView) findViewById(R.id.signed_day_names);
        TextView dayNames2 = (TextView) findViewById(R.id.unsigned_day_names);
        CenterDatabase cd = new CenterDatabase(this, null);
        SQLiteDatabase db = cd.getWritableDatabase();

        for (int i = 0; i < total; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            String name = "";
            Cursor cursor = db.rawQuery("select name from " + CenterDatabase.USER + " where uid = ?", new String[]{String.valueOf(uids[i])});
            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
            cursor.close();
            if (daySigned[i]) {
                dayNames.setText(dayNames.getText().toString() + " " /*+ uids[i]*/ + name);
            }
            else {
                dayNames2.setText(dayNames2.getText().toString() + " " /*+ uids[i]*/ + name);
            }
        }

        depListMonth.clear();
        for (int i = 0; i < total; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            String name = "";
            Cursor cursor = db.rawQuery("select name from " + CenterDatabase.USER + " where uid = ?", new String[]{String.valueOf(uids[i])});
            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
            cursor.close();
            map.put("name", name);
            map.put("department", String.valueOf(uids[i]));
            map.put("signedDays", monthSigned[i]);
            map.put("passedDays", passedDays);
            map.put("uid", uids[i]);
            depListMonth.add(map);
        }

        depListWeek.clear();
        for (int i = 0; i < total; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            String name = "";
            Cursor cursor = db.rawQuery("select name from " + CenterDatabase.USER + " where uid = ?", new String[]{String.valueOf(uids[i])});
            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
            cursor.close();
            map.put("name", name);
            map.put("department", String.valueOf(uids[i]));
            map.put("signedDays", weekSigned[i]);
            map.put("passedDays", 7);
            map.put("uid", uids[i]);
            depListWeek.add(map);
        }

        db.close();
        cd.close();

        TextView allPeople = (TextView) findViewById(R.id.total_people);
        allPeople.setText(String.valueOf(total));
        TextView signedToday = (TextView) findViewById(R.id.signed_today);
        signedToday.setText(String.valueOf(numOfSignedToday));
        TextView unsignedToday = (TextView) findViewById(R.id.unsigned_today);
        unsignedToday.setText(String.valueOf(total - numOfSignedToday));
        TextView signedRate = (TextView) findViewById(R.id.signed_rate2);

        int rate = (total != 0) ? numOfSignedToday * 100 / total : 0;
        arcView2.setPercent(rate);
        signedRate.setText(String.valueOf(rate));

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

	@Override
	public void downloadCallBack(int type, String msg) {
		//Log.e("callback: ", msg);
		List<JGDataSet> newList = new ArrayList<JGDataSet>();
		if (msg == null) {
			Toast.makeText(getApplicationContext(), "网络连接失败", Toast.LENGTH_SHORT).show();
			return;
		}

		try {
			JSONObject jsonObject = new JSONObject(msg);
			int error = jsonObject.getInt("error");
			if (error == 1) {
				JSONArray array = jsonObject.getJSONArray("result");
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = (JSONObject) array.get(i);
					JGDataSet dataSet = new JGDataSet();
					dataSet.setType(obj.getInt("type"));
					if (obj.has("uid"))
						dataSet.setUserID(obj.getInt("uid"));
					else
						dataSet.setUserID(uid);
					dataSet.setTime(obj.getString("time"));
					dataSet.setPosition(obj.getString("position"));
					dataSet.setContent(obj.getString("content"));
					newList.add(dataSet);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (type == ONLY_USER) {
//			JGCalendarView.list = newList;
            if (this.type == SELF)
                syncLocalDatabase(newList);
            JGDownload download2 = new JGDownload(this);
		    download2.down(ONLY_SUBORDINATE, uid, firstString, nowString);
		}
		else if (type == ONLY_SUBORDINATE) {
			depListItems = newList;
            if (this.type == SELF)
                appendDatabase(newList);

            getData();
            peopleListMonthAdapter.notifyDataSetChanged();
            peopleListWeekAdapter.notifyDataSetChanged();
		}
	}

    private void syncLocalDatabase(List<JGDataSet> array) {
        JGDBOperation operation = new JGDBOperation(this);
        operation.deleteAll();

        for (JGDataSet dataSet : array) {
            operation.save(dataSet);
        }
    }

    private void appendDatabase(List<JGDataSet> array) {
        JGDBOperation operation = new JGDBOperation(this);

        for (JGDataSet dataSet : array) {
            operation.save(dataSet);
        }
    }

    private String getMinuteTime(String s) {
        String t = s.split(" ")[1];
        return t.substring(0, t.length()- 3);
    }

}