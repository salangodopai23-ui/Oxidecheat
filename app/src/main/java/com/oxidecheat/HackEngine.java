package com.oxidecheat;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class HackEngine {

    // ─────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────
    private static HackEngine instance;

    public static HackEngine getInstance() {
        if (instance == null) instance = new HackEngine();
        return instance;
    }

    // ─────────────────────────────────────────────
    // Estado dos hacks
    // ─────────────────────────────────────────────
    private boolean esp      = false;
    private boolean aimbot   = false;
    private boolean godMode  = false;
    private boolean speed    = false;
    private boolean noRecoil = false;

    // PID do Oxide Mobile
    private int oxidePid = -1;

    // Endereços encontrados via scan
    private long addrHPLocal    = 0;
    private long addrSpeedMult  = 0;
    private List<Long> addrInimigos = new ArrayList<>();

    // Info ESP para exibir no menu
    private String espInfo = "";

    // Loop de hacks em background
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable loopRunnable;
    private boolean rodando = false;

    // Package do Oxide Mobile (ajusta se necessário)
    private static final String OXIDE_PACKAGE = "com.corrodinggames.rwa"; // verifique o package real

    // ─────────────────────────────────────────────
    // Toggle público
    // ─────────────────────────────────────────────
    public void toggleESP()      { esp      = !esp;      if (esp)      iniciarLoop(); }
    public void toggleAimbot()   { aimbot   = !aimbot;   if (aimbot)   iniciarLoop(); }
    public void toggleGodMode()  { godMode  = !godMode;  if (godMode)  iniciarLoop(); }
    public void toggleSpeed()    { speed    = !speed;    if (speed)    iniciarLoop(); }
    public void toggleNoRecoil() { noRecoil = !noRecoil; if (noRecoil) iniciarLoop(); }

    public boolean espAtivo()      { return esp; }
    public boolean aimbotAtivo()   { return aimbot; }
    public boolean godModeAtivo()  { return godMode; }
    public boolean speedAtivo()    { return speed; }
    public boolean noRecoilAtivo() { return noRecoil; }
    public String  getESPInfo()    { return espInfo; }

    // ─────────────────────────────────────────────
    // Encontra PID do Oxide
    // ─────────────────────────────────────────────
    public void inicializar(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.processName.contains("oxide") ||
                info.processName.contains("rwa") ||
                info.processName.equalsIgnoreCase(OXIDE_PACKAGE)) {
                oxidePid = info.pid;
                break;
            }
        }
    }

    // ─────────────────────────────────────────────
    // Leitura / escrita de memória via /proc/pid/mem
    // ─────────────────────────────────────────────
    private byte[] lerMemoria(long endereco, int tamanho) {
        if (oxidePid < 0) return null;
        try {
            RandomAccessFile mem = new RandomAccessFile(
                "/proc/" + oxidePid + "/mem", "r"
            );
            mem.seek(endereco);
            byte[] buffer = new byte[tamanho];
            mem.readFully(buffer);
            mem.close();
            return buffer;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean escreverMemoria(long endereco, byte[] dados) {
        if (oxidePid < 0) return false;
        try {
            RandomAccessFile mem = new RandomAccessFile(
                "/proc/" + oxidePid + "/mem", "rw"
            );
            mem.seek(endereco);
            mem.write(dados);
            mem.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private float lerFloat(long endereco) {
        byte[] raw = lerMemoria(endereco, 4);
        if (raw == null) return 0f;
        return ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    private void escreverFloat(long endereco, float valor) {
        byte[] raw = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putFloat(valor)
            .array();
        escreverMemoria(endereco, raw);
    }

    // ─────────────────────────────────────────────
    // Scan de memória — percorre /proc/pid/maps
    // e busca por padrão de float
    // ─────────────────────────────────────────────
    private List<Long> scanFloat(float alvo, float tolerancia) {
        List<Long> resultados = new ArrayList<>();
        if (oxidePid < 0) return resultados;

        try {
            RandomAccessFile maps = new RandomAccessFile(
                "/proc/" + oxidePid + "/maps", "r"
            );
            String linha;
            while ((linha = maps.readLine()) != null) {
                // Formato: start-end perms offset dev inode pathname
                // Só scanneia regiões rw (heap, data)
                if (!linha.contains("rw") || linha.contains("vdso")) continue;

                String[] partes = linha.split("[-\\s]+");
                if (partes.length < 2) continue;

                long start, end;
                try {
                    start = Long.parseLong(partes[0], 16);
                    end   = Long.parseLong(partes[1], 16);
                } catch (NumberFormatException e) { continue; }

                long tamanho = end - start;
                // Limita o scan a regiões razoáveis (< 50MB)
                if (tamanho <= 0 || tamanho > 50 * 1024 * 1024) continue;

                try {
                    RandomAccessFile mem = new RandomAccessFile(
                        "/proc/" + oxidePid + "/mem", "r"
                    );
                    mem.seek(start);
                    byte[] bloco = new byte[(int) tamanho];
                    mem.readFully(bloco);
                    mem.close();

                    // Percorre o bloco de 4 em 4 bytes
                    for (int i = 0; i <= bloco.length - 4; i += 4) {
                        float val = ByteBuffer.wrap(bloco, i, 4)
                            .order(ByteOrder.LITTLE_ENDIAN).getFloat();
                        if (Math.abs(val - alvo) <= tolerancia) {
                            resultados.add(start + i);
                        }
                    }
                } catch (Exception ignored) {}
            }
            maps.close();
        } catch (Exception e) {
            espInfo = "Sem acesso à memória: " + e.getMessage();
        }
        return resultados;
    }

    // ─────────────────────────────────────────────
    // Lógica de hacks
    // ─────────────────────────────────────────────

    // God Mode: mantém HP do jogador em 100
    private void aplicarGodMode() {
        if (addrHPLocal == 0) {
            // Tenta encontrar HP = 100.0
            List<Long> res = scanFloat(100f, 0.5f);
            if (!res.isEmpty()) addrHPLocal = res.get(0);
        }
        if (addrHPLocal != 0) {
            escreverFloat(addrHPLocal, 100f);
        }
    }

    // Speed: multiplica velocidade de movimento
    // Unity armazena TimeScale como float 1.0
    private void aplicarSpeed() {
        if (addrSpeedMult == 0) {
            List<Long> res = scanFloat(1.0f, 0.01f);
            if (!res.isEmpty()) addrSpeedMult = res.get(0);
        }
        if (addrSpeedMult != 0) {
            escreverFloat(addrSpeedMult, 2.0f);
        }
    }

    private void restaurarSpeed() {
        if (addrSpeedMult != 0) escreverFloat(addrSpeedMult, 1.0f);
    }

    // No Recoil: zera aceleração de recuo
    // Busca por padrão de recuo (float entre 2.0 e 10.0 típico em Unity)
    private void aplicarNoRecoil() {
        // Scan por valores de recuo e zera
        List<Long> candidatos = scanFloat(5.0f, 3.0f);
        for (Long addr : candidatos) {
            escreverFloat(addr, 0f);
        }
    }

    // ESP: lê posições de inimigos e monta string de info
    private void atualizarESP() {
        if (addrInimigos.isEmpty()) {
            // Busca HP de outros jogadores (entre 1 e 99 — exclui 100 que é o local)
            addrInimigos = scanFloat(50f, 49f);
            if (addrHPLocal != 0) addrInimigos.remove(addrHPLocal);
        }

        StringBuilder sb = new StringBuilder();
        int count = Math.min(addrInimigos.size(), 8);
        for (int i = 0; i < count; i++) {
            float hp = lerFloat(addrInimigos.get(i));
            if (hp > 0 && hp <= 100) {
                sb.append(String.format("Jogador %d | HP: %.0f\n", i + 1, hp));
            }
        }
        espInfo = sb.length() > 0 ? sb.toString() : "Nenhum inimigo detectado";
    }

    // Aimbot: zera HP do primeiro inimigo detectado
    private void aplicarAimbot() {
        if (addrInimigos.isEmpty()) return;
        for (Long addr : addrInimigos) {
            float hp = lerFloat(addr);
            if (hp > 0 && hp <= 100) {
                escreverFloat(addr, 0f);
                addrInimigos.remove(addr);
                break;
            }
        }
    }

    // ─────────────────────────────────────────────
    // Loop principal de hacks
    // ─────────────────────────────────────────────
    private void iniciarLoop() {
        if (rodando) return;
        rodando = true;

        loopRunnable = new Runnable() {
            @Override
            public void run() {
                if (!esp && !aimbot && !godMode && !speed && !noRecoil) {
                    rodando = false;
                    return;
                }

                if (godMode)  aplicarGodMode();
                if (speed)    aplicarSpeed();
                else          restaurarSpeed();
                if (noRecoil) aplicarNoRecoil();
                if (esp || aimbot) atualizarESP();
                if (aimbot)   aplicarAimbot();

                handler.postDelayed(this, 500); // tick a cada 500ms
            }
        };
        handler.post(loopRunnable);
    }

    public void pararTudo() {
        rodando = false;
        esp = aimbot = godMode = speed = noRecoil = false;
        if (loopRunnable != null) handler.removeCallbacks(loopRunnable);
        restaurarSpeed();
    }
}
