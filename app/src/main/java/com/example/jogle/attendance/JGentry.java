package com.example.jogle.attendance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.jogle.calendar.JGCalendarActivity;


public class JGentry extends Activity {
    private Button b1;
    private Button b2;
    private Button b3;
    private EditText et2;
    private EditText et3;
    private LinearLayout dateSet;
    private CheckBox F21893C82B19C60B;
    private EditText B3AF26AADF83CDDD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jg_activity_entry);
        b1 = (Button) findViewById(R.id.button);
        b2 = (Button) findViewById(R.id.button2);
        b3 = (Button) findViewById(R.id.button3);
        et2 = (EditText) findViewById(R.id.editText2);
        et3 = (EditText) findViewById(R.id.editText3);
        dateSet = (LinearLayout) findViewById(R.id.dateSet);
        F21893C82B19C60B = (CheckBox) findViewById(R.id.F21893C82B19C60B);
        B3AF26AADF83CDDD = (EditText) findViewById(R.id.B3AF26AADF83CDDD);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(JGentry.this, JGSignActivity.class);
                intent.putExtra("uid", Integer.parseInt(et2.getText().toString()));
                intent.putExtra("name", et3.getText().toString());
                intent.putExtra("type", JGSignActivity.OUT_ATTENDANCE);
                startActivity(intent);
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(JGentry.this, JGSignActivity.class);
                intent.putExtra("uid", Integer.parseInt(et2.getText().toString()));
                intent.putExtra("name", et3.getText().toString());
                intent.putExtra("type", JGSignActivity.IN_ATTENDANCE);
                startActivity(intent);
            }
        });
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(JGentry.this, JGCalendarActivity.class);
                intent.putExtra("uid", Integer.parseInt(et2.getText().toString()));
                intent.putExtra("name", et3.getText().toString());
                startActivity(intent);
            }
        });

        F21893C82B19C60B.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //JGCalendarActivity.F21893C82B19C60B = b;
            }
        });

        B3AF26AADF83CDDD.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //JGCalendarActivity.B3AF26AADF83CDDD = editable.toString();
            }
        });

        b3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                F21893C82B19C60B.setVisibility(View.VISIBLE);
                dateSet.setVisibility(View.VISIBLE);
                return true;
            }
        });
    }
}
