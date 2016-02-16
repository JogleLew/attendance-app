package com.example.jogle.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.example.jogle.calendar.JGCalendarActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class JGSignActivity extends Activity implements Runnable, JGDownloadCallBack, JGUploadCallBack {
    private int type; // OUT_ATTENDANCE or IN_ATTENDANCE
    private int option; // 0 for tabLeft, 1 for tabRight
    public final static int OUT_ATTENDANCE = -1;
    public final static int IN_ATTENDANCE = -2;
    public final static int CUSTOMER_PICK = 103;
    private static final int CAPTURE_REQUEST_CODE = 100;

    public static JGDataSet dataSet;
    private int uid;

    private boolean isFirstTime;
    private String outSignInTimes;
    private String outSignOutTimes;
    private String tabLeftStatus;
    private String tabRightStatus;
    private String actualTime;

    private MyHandler handler = new MyHandler(this);
    private Thread thread; // background thread for fetching web time

    private RelativeLayout editPos;
    private RelativeLayout editCustomer;
    private RelativeLayout backButton;
    private LinearLayout block1;
    private LinearLayout block2;
    private EditText note;
    private TextView title;
    private TextView tabLeft;
    private TextView tabRight;
    private TextView ruleTimeText;
    private TextView ruleTimeTime;
    private TextView time;
    private TextView outSignTimes;
    private ImageView picView;
    private ImageView shotButton;
    private Button button;

    private LocationClient mLocationClient;
    private BDLocationListener myListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        SDKInitializer.initialize(getApplicationContext()); // Register Baidu Map SDK
        setContentView(R.layout.jg_activity_sign);

        outSignInTimes = "";
        outSignOutTimes = "";
        tabLeftStatus = "";
        tabRightStatus = "";
        isFirstTime = true;

        findViewsById();
        adjustViewByType();
        initDataSet();
        fetchWebTime();
        enableLocationSevices();

        setOnTabClick();
        setOnSignButtonClick();
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLeaveDialog();
            }
        });
        editCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intentToCustomer();
            }
        });
        note.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                dataSet.setContent(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    protected void onStop() {
        thread.interrupt();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mLocationClient.stop();
        thread.interrupt();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        time = (TextView) findViewById(R.id.time);
        if (dataSet.getTime() != null)
            time.setText(getDateAndMinuteTime(dataSet.getTime()));
        TextView addr = (TextView) findViewById(R.id.address);
        if (dataSet.getPosition() != null)
            addr.setText(dataSet.getPosition());
        TextView customers = (TextView) findViewById(R.id.customers);
        if (dataSet.getCustomers() != null)
            customers.setText(dataSet.getCustomers());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    dataSet.generateThumbnail();
                    button.setClickable(true);
                    picView.setVisibility(View.VISIBLE);
                    picView.setImageBitmap(dataSet.getThumbnail());
                    picView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(JGSignActivity.this, JGShowActivity.class);
                            intent.putExtra("pic_path", dataSet.getPicPath());
                            startActivity(intent);
                        }
                    });
                    shotButton.setVisibility(View.GONE);
                    break;
                case Activity.RESULT_CANCELED:
                    dataSet.setTimeStamp(null);
                    break;
            }
        }
        else if (requestCode == CUSTOMER_PICK) {
            TextView customers = (TextView) findViewById(R.id.customers);
            if (dataSet.getCustomers() != null)
                customers.setText(dataSet.getCustomers());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            showLeaveDialog();
            return false;
        }
        return false;
    }

    public void run() {
        try {
            while (true) {
                try {
                    URL url = new URL("http://www.baidu.com");// 取得资源对象 
                    URLConnection uc = url.openConnection();// 生成连接对象    
                    uc.connect(); // 发出连接 
                    long ldate = uc.getDate(); // 取得网站日期时间（时间戳）
                    Date date = new Date(ldate);
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                    String dateStr = sdf.format(c.getTime());
                    Message message = new Message();
                    message.obj = dateStr;
                    handler.sendMessage(message);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findViewsById() {
        button = (Button) findViewById(R.id.button);
        picView = (ImageView) findViewById(R.id.pic_view);
        shotButton = (ImageView) findViewById(R.id.shot);
        backButton = (RelativeLayout) findViewById(R.id.back);
        time = (TextView) findViewById(R.id.time);
        editPos = (RelativeLayout) findViewById(R.id.editpos);
        editCustomer = (RelativeLayout) findViewById(R.id.editcustomer);
        note = (EditText) findViewById(R.id.note);
        title = (TextView) findViewById(R.id.title);
        block1 = (LinearLayout) findViewById(R.id.block1);
        block2 = (LinearLayout) findViewById(R.id.block2);
        tabLeft = (TextView) findViewById(R.id.tab_left);
        tabRight = (TextView) findViewById(R.id.tab_right);
        outSignTimes = (TextView) findViewById(R.id.out_sign_times);
        ruleTimeText = (TextView) findViewById(R.id.text_ruletime);
        ruleTimeTime = (TextView) findViewById(R.id.time_ruletime);
    }

    private void adjustViewByType() {
        option = 0;
        type = getIntent().getIntExtra("type", 0);
        if (type == OUT_ATTENDANCE) {
            block1.setVisibility(View.GONE);
            block2.setVisibility(View.VISIBLE);
            title.setText("外勤签到");
            tabLeft.setText("往程签到");
            tabRight.setText("返程签退");
        }
        else if (type == IN_ATTENDANCE) {
            block1.setVisibility(View.VISIBLE);
            block2.setVisibility(View.GONE);
            editCustomer.setVisibility(View.GONE);
            note.setVisibility(View.GONE);
        }
        else {
            showUnknownErrorDialog();
            finish();
        }
    }

    private void initDataSet() {
        // get UID and name from intent
        /*****************************************************
         CenterDatabase cd = new CenterDatabase(this, null);
         String UID = cd.getUID();
         int uid = Integer.parseInt(UID);
         String name = cd.getNameByUID(UID);
         cd.close();
         *****************************************************/
        uid = getIntent().getIntExtra("uid", -1);
        String name = getIntent().getStringExtra("name");

        // refresh dataSet
        dataSet = new JGDataSet();
        if (type == OUT_ATTENDANCE)
            dataSet.setType(0);
        else if (type == IN_ATTENDANCE)
            dataSet.setType(1);
        else
            showUnknownErrorDialog();
        dataSet.setUserID(uid);
        dataSet.setUserName(name);
    }

    private void fetchWebTime() {
        thread = new Thread(this);
        thread.start();
    }

    private void enableLocationSevices() {
        myListener = new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation location) {
                if (location == null)
                    return;
                if (location.getLocType() == BDLocation.TypeGpsLocation ||
                        location.getLocType() == BDLocation.TypeNetWorkLocation ||
                        location.getLocType() == BDLocation.TypeOffLineLocation) {

                    dataSet.setLatitude(location.getLatitude());
                    dataSet.setLongitude(location.getLongitude());
                    if (dataSet.getPosition() == null)
                        dataSet.setPosition(location.getAddrStr());
                    TextView addr = (TextView) findViewById(R.id.address);
                    addr.setText(dataSet.getPosition());
                    editPos.setClickable(true);
                    editPos.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            intentToLocate();
                        }
                    });
                }
            }
        };
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(myListener);
        initLocation();
        mLocationClient.start();
    }

    private void intentToLocate() {
        Intent intent = new Intent(JGSignActivity.this, JGLocateActivity.class);
        startActivity(intent);
    }

    private void intentToCustomer() {
        Intent intent = new Intent(JGSignActivity.this, JGCustomerPickerActivity.class);
        startActivityForResult(intent, CUSTOMER_PICK);
//        startActivity(intent);
    }

    private void intentToSystemShot() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 调用系统相机
        Uri fileUri = getOutputMediaFileUri(); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_REQUEST_CODE);
    }

    public Uri getOutputMediaFileUri() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Attendance");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        dataSet.setTimeStamp(timeStamp);
        mediaFile = new File(dataSet.getPicPath());
        return Uri.fromFile(mediaFile);
    }

    private void setOnTabClick() {
        tabLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                option = 0;
                if (type == OUT_ATTENDANCE)
                    dataSet.setType(0);
                else if (type == IN_ATTENDANCE)
                    dataSet.setType(1);
                outSignTimes.setText(outSignInTimes);
                tabLeft.setTextColor(getResources().getColor(R.color.my_tab_sel_color));
                tabLeft.setBackgroundResource(R.drawable.my_tab_sel);
                tabRight.setTextColor(getResources().getColor(android.R.color.black));
                tabRight.setBackgroundColor(getResources().getColor(android.R.color.white));
                ruleTimeText.setText("上班时间");
                ruleTimeTime.setText("8:30 / 14:00");
                checkButtonClickable();
            }
        });
        tabRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                option = 1;
                if (type == OUT_ATTENDANCE)
                    dataSet.setType(3);
                else if (type == IN_ATTENDANCE)
                    dataSet.setType(2);
                outSignTimes.setText(outSignOutTimes);
                tabRight.setTextColor(getResources().getColor(R.color.my_tab_sel_color));
                tabRight.setBackgroundResource(R.drawable.my_tab_sel);
                tabLeft.setTextColor(getResources().getColor(android.R.color.black));
                tabLeft.setBackgroundColor(getResources().getColor(android.R.color.white));
                ruleTimeText.setText("下班时间");
                ruleTimeTime.setText("12:00 / 17:30");
                checkButtonClickable();
            }
        });
    }

    private void setOnSignButtonClick() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int t;
                if ((t = checkSatisfaction()) > 0) {
                    if (type == OUT_ATTENDANCE && t == 3)
                        showUnsatisfiedDialog();
                    if (t == 4)
                        showSignInDialog();
                    return;
                }
                uploadToServer();
                button.setClickable(false);
                button.setBackgroundColor(getResources().getColor(R.color.button_pressed_color));
            }
        });

        if (dataSet.getTimeStamp() == null) {
            shotButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intentToSystemShot();
                }
            });
        }
    }

    private int checkSatisfaction() {
        if (dataSet.getTime() == null)
            return 1;
        if (dataSet.getPosition() == null)
            return 2;
        if (type == OUT_ATTENDANCE && dataSet.getContent().length() == 0 && dataSet.getTimeStamp() == null)
            return 3;
        if (option == 1 && tabLeftStatus.length() == 0)
            return 4;
        return 0;
    }

    private void solveConflict(String time) {
        if (!isFirstTime)
            return;
        isFirstTime = false;
        JGDBOperation operation = new JGDBOperation(getApplicationContext());
        List<JGDataSet> list = operation.getByDateAndUid(time.split(" ")[0], uid);
        boolean inSignIn = false, inSignOut = false, outSignIn = false, outSignOut = false;
        for (int i = 0; i < list.size(); i++) {
            JGDataSet item = list.get(i);
            switch (item.getType()) {
                case 0:
                    outSignIn = true;
                    if (type == OUT_ATTENDANCE) {
                        tabLeftStatus = item.getTime();
                        outSignInTimes += getMinuteTime(item.getTime()) + " ";
                    }
                    break;
                case 1:
                    inSignIn = true;
                    if (type == IN_ATTENDANCE)
                        tabLeftStatus = item.getTime();
                    break;
                case 2:
                    inSignOut = true;
                    if (type == IN_ATTENDANCE)
                        tabRightStatus = item.getTime();
                    break;
                case 3:
                    outSignOut = true;
                    if (type == OUT_ATTENDANCE) {
                        tabLeftStatus = "";
                        outSignOutTimes += getMinuteTime(item.getTime()) + " ";
                    }
                    break;
            }
        }
        if (option == 0)
            outSignTimes.setText(outSignInTimes);
        else
            outSignTimes.setText(outSignOutTimes);
        checkButtonClickable();
        if (tabLeftStatus.length() > 0) {
            tabRight.performClick();
        }
        if (type == OUT_ATTENDANCE && inSignIn && !inSignOut
                || type == IN_ATTENDANCE && outSignIn && !outSignOut) {
            showLockDialog();
        }
    }

    private void checkButtonClickable() {
        if (tabLeftStatus.length() > 0 && option == 0) {
            button.setClickable(false);
            button.setBackgroundColor(getResources().getColor(R.color.button_pressed_color));
            if (type == OUT_ATTENDANCE)
                button.setText(getMinuteTime(tabLeftStatus) + " 往程已签");
            else if (type == IN_ATTENDANCE)
                button.setText(getMinuteTime(tabLeftStatus) + " 已签到");
            else
                showUnknownErrorDialog();
            button.setTextColor(0xff01aff4);
            if (dataSet.getTimeStamp() != null) {
                picView.setVisibility(View.VISIBLE);
                picView.setImageBitmap(dataSet.getThumbnail());
                picView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(JGSignActivity.this, JGShowActivity.class);
                        intent.putExtra("pic_path", dataSet.getPicPath());
                        startActivity(intent);
                    }
                });
            }
        }
        else if (tabRightStatus.length() > 0 && option == 1){
            button.setClickable(false);
            button.setBackgroundColor(getResources().getColor(R.color.button_pressed_color));
            if (type == OUT_ATTENDANCE)
                button.setText(getMinuteTime(tabRightStatus) + " 返程已签");
            else if (type == IN_ATTENDANCE)
                button.setText(getMinuteTime(tabRightStatus) + " 已签退");
            else
                showUnknownErrorDialog();
            button.setTextColor(0xff01aff4);
            if (dataSet.getTimeStamp() != null) {
                picView.setVisibility(View.VISIBLE);
                picView.setImageBitmap(dataSet.getThumbnail());
                picView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(JGSignActivity.this, JGShowActivity.class);
                        intent.putExtra("pic_path", dataSet.getPicPath());
                        startActivity(intent);
                    }
                });
            }
        } else {
            button.setClickable(true);
            button.setBackgroundResource(R.drawable.my_button_ok);
            if (type == OUT_ATTENDANCE) {
                if (option == 0)
                    button.setText("确认往程签到");
                else if (option == 1)
                    button.setText("确认返程签退");
                else
                    showUnknownErrorDialog();
            }
            else if (type == IN_ATTENDANCE) {
                if (option == 0)
                    button.setText("确认考勤签到");
                else if (option == 1)
                    button.setText("确认考勤签退");
                else
                    showUnknownErrorDialog();
            }
            else
                showUnknownErrorDialog();
            button.setTextColor(0xffffffff);
        }
    }

    private void uploadToServer() {
        // upload dataset
        JGUpload upload = new JGUpload(this);
        String JSONString = upload.changeArrayDateToJson(dataSet);
        upload.up(JSONString);

        // upload picture
        if (dataSet.getTimeStamp() != null) {
            JGUploadPic uploadThread = new JGUploadPic(dataSet);
            uploadThread.start();
        }
    }

    public void uploadCallBack(String payload) {
        if (payload.equals("1")) {
            JGDBOperation operation = new JGDBOperation(getApplicationContext());
            operation.save(dataSet);
            button.setText(getMinuteTime(dataSet.getTime()) + "已签到");
            showSuccessDialog();
        } else if (payload.equals("2")) {
            showFailedDialog();
        }
    }

    private void receiveTime(String t) {
        actualTime = t;
        if (isFirstTime)
            syncDatabase();
    }

    private void syncDatabase() {
        String day = getDateTimeStamp(actualTime);
        JGDownload download1 = new JGDownload(this);
        download1.down(JGCalendarActivity.ONLY_USER, uid,
                day.substring(0, day.length() - 2) + "01_000000", day+"_235959");
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
        syncLocalDatabase(newList);
        solveConflict(actualTime);
    }

    private void syncLocalDatabase(List<JGDataSet> array) {
        JGDBOperation operation = new JGDBOperation(this);
        operation.deleteAll();

        for (JGDataSet dataSet : array) {
            operation.save(dataSet);
        }
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this).setTitle("提示")
                .setMessage((type == OUT_ATTENDANCE) ? ((option == 0) ? "外勤往程签到成功" : "外勤返程签退成功")
                        : ((option == 0) ? "考勤签到成功" : "考勤签退成功"))
                .setPositiveButton("确" +
                        "" +
                        "定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showFailedDialog() {
        new AlertDialog.Builder(this).setTitle("提示")
                .setMessage("上传数据失败，请检查网络。")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).show();
    }

    private void showLeaveDialog() {
//        if (tabLeftStatus.length() > 0 && tabRightStatus.length() > 0) {
//            finish();
//            return;
//        }
//        new AlertDialog.Builder(this).setTitle("提示")
//                .setMessage((type == OUT_ATTENDANCE) ? ((option == 0) ? "是否取消外勤签到？" : "是否取消外勤签退？")
//                        : ((option == 0) ? "是否取消考勤签到？" : "是否取消考勤签退？"))
//                .setPositiveButton("否", null)
//                .setNegativeButton("是", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (dataSet.getTimeStamp() != null && !button.getText().toString().equals("已签到")) {
                            File pic = new File(dataSet.getPicPath());
                            if (pic.exists()) {
                                pic.delete();
                            }
                            File thumbnail = new File(dataSet.getThumbnailPath());
                            if (thumbnail.exists()) {
                                thumbnail.delete();
                            }
                        }
                        finish();
//                    }
//                }).show();
    }

    private void showLockDialog() {
        new AlertDialog.Builder(this).setTitle("提示")
                .setMessage((type == OUT_ATTENDANCE) ? "请先完成考勤签退" : "请先完成外勤返程签退")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showUnsatisfiedDialog() {
        new AlertDialog.Builder(this).setTitle("提示")
                .setMessage("请填写备注或拍照")
                .setPositiveButton("确定", null)
                .show();
    }

    private void showSignInDialog() {
        new AlertDialog.Builder(this).setTitle("提示")
                .setMessage((type == OUT_ATTENDANCE) ? "请先完成往程签到" : "请先完成考勤签到")
                .setPositiveButton("确定", null)
                .show();
    }

    private void showUnknownErrorDialog() {
        new AlertDialog.Builder(this).setTitle("提示")
                .setMessage("签到界面出现未知错误")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系
        option.setScanSpan(1000);
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps
        option.setLocationNotify(true);
        //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIgnoreKillProcess(false);
        //可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        mLocationClient.setLocOption(option);
    }

    private String getDateAndMinuteTime(String s) {
        return s.substring(0, s.length()- 3);
    }

    private String getDateTimeStamp(String s) {
        String r = s.split(" ")[0];
        r = r.replace("年", "");
        r = r.replace("月", "");
        r = r.replace("日", "");
        return r;
    }

    private String getMinuteTime(String s) {
        String t = s.split(" ")[1];
        return t.substring(0, t.length()- 3);
    }

    private static class MyHandler extends Handler {
        private final WeakReference<JGSignActivity> mActivity;

        public MyHandler(JGSignActivity activity) {
            mActivity = new WeakReference<JGSignActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            JGSignActivity activity = mActivity.get();
            if (activity != null) {
                String t = (String) msg.obj;
                if (JGCalendarActivity.F21893C82B19C60B)
                    t = JGCalendarActivity.B3AF26AADF83CDDD;
                activity.receiveTime(t);
                activity.dataSet.setTime(t);
                activity.time.setText(activity.getDateAndMinuteTime(t));

            }
        }
    }
}