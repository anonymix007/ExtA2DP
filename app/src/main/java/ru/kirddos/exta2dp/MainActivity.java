package ru.kirddos.exta2dp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;


import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        TextView main = findViewById(R.id.text);
        HiddenApiBypass.addHiddenApiExemptions("L");
        main.setText(Arrays.toString(SourceCodecType.getSourceCodecTypes()));
    }
}