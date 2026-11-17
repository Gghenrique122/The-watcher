package com.stalkerhorror.entity;

/**
 * Estilos de aparicao. O Diretor escolhe o estilo com base no que aprendeu sobre o jogador.
 * minR/maxR: distancia (blocos). minA/maxA: angulo relativo ao olhar do jogador (0 = de frente, 180 = atras).
 */
public enum StalkerStyle {
    WATCHER(20, 30, 25, 70, 50, 1800, false),     // parado, na borda da visao
    PERIPHERAL(16, 24, 75, 150, 24, 1000, false), // canto do olho / atras de voce
    TUNNEL(16, 30, 0, 180, 34, 1200, false),      // fim de tuneis e cavernas escuras
    PURSUIT(24, 32, 20, 60, 30, 2600, true),      // te acompanha a distancia (so anda quando voce nao olha)
    FAR(30, 40, 0, 80, 90, 1400, false),          // silhueta distante em terreno alto, so os olhos na neblina
    WINDOW(15, 22, 20, 160, 40, 1000, false);     // do lado de fora da sua casa ao acordar

    public final int minR, maxR, minA, maxA, maxStare, life;
    public final boolean walker;

    StalkerStyle(int minR, int maxR, int minA, int maxA, int maxStare, int life, boolean walker) {
        this.minR = minR;
        this.maxR = maxR;
        this.minA = minA;
        this.maxA = maxA;
        this.maxStare = maxStare;
        this.life = life;
        this.walker = walker;
    }

    public static StalkerStyle byName(String s) {
        for (StalkerStyle v : values()) {
            if (v.name().equalsIgnoreCase(s)) return v;
        }
        return null;
    }
}
