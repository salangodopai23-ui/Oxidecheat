package com.oxidecheat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private ModMenuView modMenu;

    private static final String CHANNEL_ID = "oxide_cheat_channel";

    // Posição inicial do botão flutuante
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public void onCreate() {
        super.onCreate();
        criarNotificacao();
        criarOverlay();
    }

    private void criarNotificacao() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "Oxide Cheat",
            NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);

        Notification notif = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Oxide Cheat")
            .setContentText("Rodando em background")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build();

        startForeground(1, notif);
    }

    private void criarOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Botão flutuante para abrir/fechar o menu
        FrameLayout botaoFlutuante = new FrameLayout(this);
        View btnView = LayoutInflater.from(this).inflate(R.layout.overlay_button, botaoFlutuante, false);
        botaoFlutuante.addView(btnView);

        // Mod menu (inicialmente escondido)
        modMenu = new ModMenuView(this);
        modMenu.setVisibility(View.GONE);

        // Params do botão flutuante
        WindowManager.LayoutParams paramsBotao = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        paramsBotao.gravity = Gravity.TOP | Gravity.LEFT;
        paramsBotao.x = 0;
        paramsBotao.y = 300;

        // Params do mod menu
        WindowManager.LayoutParams paramsMenu = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        paramsMenu.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        paramsMenu.y = 100;

        windowManager.addView(botaoFlutuante, paramsBotao);
        windowManager.addView(modMenu, paramsMenu);

        // Arrasta o botão flutuante
        botaoFlutuante.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = paramsBotao.x;
                    initialY = paramsBotao.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    paramsBotao.x = initialX + (int)(event.getRawX() - initialTouchX);
                    paramsBotao.y = initialY + (int)(event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(botaoFlutuante, paramsBotao);
                    return true;

                case MotionEvent.ACTION_UP:
                    // Se foi um toque (não arrasto), toggle do menu
                    float dx = Math.abs(event.getRawX() - initialTouchX);
                    float dy = Math.abs(event.getRawY() - initialTouchY);
                    if (dx < 10 && dy < 10) {
                        toggleMenu();
                    }
                    return true;
            }
            return false;
        });

        overlayView = botaoFlutuante;
    }

    private void toggleMenu() {
        if (modMenu.getVisibility() == View.VISIBLE) {
            modMenu.setVisibility(View.GONE);
        } else {
            modMenu.setVisibility(View.VISIBLE);
            modMenu.atualizar();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) windowManager.removeView(overlayView);
        if (modMenu != null) windowManager.removeView(modMenu);
        HackEngine.getInstance().pararTudo();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
