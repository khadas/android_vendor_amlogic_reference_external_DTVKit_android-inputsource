package com.droidlogic.dtvkit.inputsource;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Setup extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup);

        findViewById(R.id.btn_cable_manual_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), DtvkitDvbcSetup.class));
                finish();
            }
        });

        findViewById(R.id.btn_satellite_manual_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), DtvkitDvbsSetup.class));
                finish();
            }
        });

        findViewById(R.id.btn_terrestrial_auto_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), DtvkitDvbtSetup.class));
                finish();
            }
        });
    }


}
