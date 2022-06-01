package com.inanyan.winstylus1android;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private MyView mainView;
    private Button button;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainView = (MyView) findViewById(R.id.mainView);
        mainView.statusText = (TextView) findViewById(R.id.statusText);
        button = (Button) findViewById(R.id.connectButton);
        editText = (EditText) findViewById(R.id.serverEdit);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainView.connect(editText.getText().toString());
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        mainView.finish();
    }
}