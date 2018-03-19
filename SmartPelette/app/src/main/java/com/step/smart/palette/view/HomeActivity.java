package com.step.smart.palette.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.step.smart.palette.R;
import com.step.smart.palette.widget.PaletteView;


public class HomeActivity extends AppCompatActivity {

    PaletteView mPaletteView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Button clear = findViewById(R.id.clear);
        mPaletteView = findViewById(R.id.palette);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("Home", "onClick");
                mPaletteView.clear();
            }
        });
    }
}