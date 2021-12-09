package com.palamartech.agent;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    public static final String webToken = "b1ywggz78hp0si7bawoam6uy0lyuj21i2vatarnborcdt3e8paxhlqdpc4tbk7yeteio0u1diregaojc3vqhjsoiw7f67gw69kqqu7poi2savq07iatwf45e4hzyxfmr";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*PalamarAgent agent = new PalamarAgent(getApplicationContext(), webToken);
        agent.createNewSession();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                agent.talk("Naber");
            }
        }, 5000);   //5 seconds*/
    }
}