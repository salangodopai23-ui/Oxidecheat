package com.oxidecheat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ModMenuView extends LinearLayout {

    private final HackEngine engine = HackEngine.getInstance();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Button btnESP, btnAimbot, btnGodMode, btnSpeed, btnNoRecoil;
    private TextView tvInfo;

    // Cores
    private static final int COR_ON  = Color.parseColor("#22CC44");
    private static final int COR_OFF = Color.parseColor("#CC2222");
    private static final int COR_BG  = Color.parseColor("#CC0A0A0A");
    private static final int COR_BTN = Color.parseColor("#FF1A1A1A");

    public ModMenuView(Context ctx) {
        super(ctx);
        construirUI();
        iniciarLoopESP();
    }

    private void construirUI() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(12), dp(12), dp(12), dp(12));

        // Fundo arredondado
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COR_BG);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), Color.parseColor("#FF333333"));
        setBackground(bg);

        // Título
        TextView titulo = new TextView(getContext());
        titulo.setText("[ OXIDE CHEAT ]");
        titulo.setTextColor(Color.parseColor("#FFDD0000"));
        titulo.setTextSize(14);
        titulo.setGravity(Gravity.CENTER);
        titulo.setPadding(0, 0, 0, dp(8));
        addView(titulo);

        // Info ESP
        tvInfo = new TextView(getContext());
        tvInfo.setTextColor(Color.parseColor("#FF00FF00"));
        tvInfo.setTextSize(10);
        tvInfo.setGravity(Gravity.CENTER);
        tvInfo.setPadding(0, 0, 0, dp(6));
        addView(tvInfo);

        // Botões
        btnESP      = criarBotao("ESP");
        btnAimbot   = criarBotao("AIMBOT");
        btnGodMode  = criarBotao("GOD MODE");
        btnSpeed    = criarBotao("SPEED HACK");
        btnNoRecoil = criarBotao("SEM RECUO");

        addView(btnESP);
        addView(btnAimbot);
        addView(btnGodMode);
        addView(btnSpeed);
        addView(btnNoRecoil);

        // Listeners
        btnESP.setOnClickListener(v -> {
            engine.toggleESP();
            atualizar();
        });

        btnAimbot.setOnClickListener(v -> {
            engine.toggleAimbot();
            atualizar();
        });

        btnGodMode.setOnClickListener(v -> {
            engine.toggleGodMode();
            atualizar();
        });

        btnSpeed.setOnClickListener(v -> {
            engine.toggleSpeed();
            atualizar();
        });

        btnNoRecoil.setOnClickListener(v -> {
            engine.toggleNoRecoil();
            atualizar();
        });

        atualizar();
    }

    private Button criarBotao(String nome) {
        Button btn = new Button(getContext());
        btn.setText(nome + "  [OFF]");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            dp(180), dp(38)
        );
        params.setMargins(0, dp(3), 0, dp(3));
        btn.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COR_BTN);
        bg.setCornerRadius(dp(6));
        btn.setBackground(bg);
        return btn;
    }

    // Atualiza visual dos botões conforme estado
    public void atualizar() {
        atualizarBotao(btnESP,      "ESP",        engine.espAtivo());
        atualizarBotao(btnAimbot,   "AIMBOT",     engine.aimbotAtivo());
        atualizarBotao(btnGodMode,  "GOD MODE",   engine.godModeAtivo());
        atualizarBotao(btnSpeed,    "SPEED HACK", engine.speedAtivo());
        atualizarBotao(btnNoRecoil, "SEM RECUO",  engine.noRecoilAtivo());
    }

    private void atualizarBotao(Button btn, String nome, boolean ativo) {
        btn.setText(nome + (ativo ? "  [ON]" : "  [OFF]"));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ativo ? COR_ON : COR_BTN);
        bg.setCornerRadius(dp(6));
        if (ativo) bg.setAlpha(180);
        btn.setBackground(bg);
    }

    // Loop que atualiza o tvInfo com dados ESP a cada 1s
    private void iniciarLoopESP() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (engine.espAtivo()) {
                    String info = engine.getESPInfo();
                    tvInfo.setText(info);
                } else {
                    tvInfo.setText("");
                }
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private int dp(int valor) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int)(valor * density);
    }
}
