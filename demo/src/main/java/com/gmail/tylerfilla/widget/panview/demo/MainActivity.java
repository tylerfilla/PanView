package com.gmail.tylerfilla.widget.panview.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.gmail.tylerfilla.widget.panview.PanView;

public class MainActivity extends AppCompatActivity {

    private PanView panView;
    private TextView sampleText;
    private SwitchCompat wordWrapSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate main layout and set as content view
        setContentView(R.layout.activity_main);

        // Get stuff
        panView = (PanView) findViewById(R.id.panView);
        sampleText = (TextView) findViewById(R.id.sampleText);
        wordWrapSwitch = (SwitchCompat) findViewById(R.id.wordWrapSwitch);

        // Set sample text
        sampleText.setText(Html.fromHtml(getString(R.string.sample_text)));

        // Listen for word wrap switch toggles
        wordWrapSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setWordWrap(isChecked);
            }

        });
    }

    private void setWordWrap(boolean wordWrap) {
        // Get current layout parameters
        ViewGroup.LayoutParams layoutParams = sampleText.getLayoutParams();

        if (wordWrap) {
            // Remove the need to scroll horizontally
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            // Make it wide enough to demonstrate panning
            layoutParams.width = 2 * panView.getWidth();
        }

        // Set new layout parameters
        sampleText.setLayoutParams(layoutParams);
    }

}
