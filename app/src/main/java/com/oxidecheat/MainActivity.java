package com.oxidecheat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView status = findViewById(R.id.tv_status);
        Button btnAtivar = findViewById(R.id.btn_ativar);
        Button btnDesativar = findViewById(R.id.btn_desativar);

        btnAtivar.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                // Pede permissão de overlay
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivityForResult(intent, REQ_OVERLAY);
            } else {
                iniciarCheat();
                status.setText("Status: ATIVO ✓");
                Toast.makeText(this, "Cheat ativado — abre o Oxide", Toast.LENGTH_LONG).show();
            }
        });

        btnDesativar.setOnClickListener(v -> {
            pararCheat();
            status.setText("Status: INATIVO");
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                iniciarCheat();
                TextView status = findViewById(R.id.tv_status);
                status.setText("Status: ATIVO ✓");
                Toast.makeText(this, "Permissão concedida — abre o Oxide", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permissão negada — necessária para o overlay", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void iniciarCheat() {
        Intent intent = new Intent(this, OverlayService.class);
        startForegroundService(intent);
    }

    private void pararCheat() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
    }
}
