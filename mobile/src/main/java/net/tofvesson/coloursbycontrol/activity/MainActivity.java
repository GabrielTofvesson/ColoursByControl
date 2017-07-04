package net.tofvesson.coloursbycontrol.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import net.tofvesson.coloursbycontrol.R;
import net.tofvesson.coloursbycontrol.view.WelcomeView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);
        ((WelcomeView) findViewById(R.id.welcome)).setOnFinishedListener(new WelcomeView.OnWelcomeFinishedListener() {
            @Override
            public void onTriggerFinished(WelcomeView view) {
                view.triggerDraw();
            }
        }).triggerDraw();
        findViewById(R.id.newProj).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ScrollingActivity.class));
            }
        });
    }

}
