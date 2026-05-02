package net.wanmine.SkyAdventures.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class BossHealthHUD extends CustomUIHud {

    private final String bossName;
    private float healthPercent;
    private static final String LAYOUT = "Bossbar/boss_health.ui";

    public BossHealthHUD(@Nonnull PlayerRef playerRef,
                         @Nonnull String bossName,
                         float initialHealthPercent) {
        super(playerRef);
        this.bossName      = bossName;
        this.healthPercent = initialHealthPercent;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append(LAYOUT);
        builder.set("#boss.Text", bossName);
        builder.set("#health.Text", formatPercent(healthPercent));
    }

    public void updateHealth(float newPercent) {
        this.healthPercent = newPercent;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#health.Text", formatPercent(newPercent));
        update(false, builder);
    }

    /** Svuota il testo prima che HudManager faccia il reset completo. */
    public void hide() {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#health.Text", "");
        update(false, builder);
    }

    private static String formatPercent(float percent) {
        return Math.round(percent * 100) + "%";
    }
}
