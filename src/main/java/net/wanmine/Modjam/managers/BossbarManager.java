package net.wanmine.Modjam.managers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.Modjam.ui.BossHealthHUD;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class BossBarManager {

    public static final double BOSS_RANGE = 100.0;
    public static final String BOSS_NAME  = "Vorthrax";

    // ── Stato Vorthrax ────────────────────────────────────────────────────────

    @Nullable private Ref<EntityStore> vorthraxRef;
    @Nullable private Vector3d         vorthraxPosition;

    private float lastHealthPercent = 1.0f;

    private static final class HudEntry {
        final BossHealthHUD hud;
        final com.hypixel.hytale.server.core.universe.PlayerRef playerRefComponent;
        final Player playerComponent;

        HudEntry(BossHealthHUD hud,
                 com.hypixel.hytale.server.core.universe.PlayerRef playerRefComponent,
                 Player playerComponent) {
            this.hud                = hud;
            this.playerRefComponent = playerRefComponent;
            this.playerComponent    = playerComponent;
        }
    }

    private final Map<Ref<EntityStore>, HudEntry> activeHuds = new HashMap<>();


    public void registerVorthrax(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull Vector3d initialPosition) {
        this.vorthraxRef       = ref;
        this.vorthraxPosition  = initialPosition.clone();
        this.lastHealthPercent = 1.0f;
    }

    public boolean isVorthraxAlive() {
        return vorthraxRef != null && vorthraxRef.isValid();
    }

    public void updateVorthraxPosition(@Nonnull Vector3d pos) {
        if (vorthraxPosition == null) vorthraxPosition = new Vector3d();
        vorthraxPosition.assign(pos);
    }

    public void tickPlayer(@Nonnull Ref<EntityStore>  playerRef,
                           @Nonnull Vector3d           playerPos,
                           @Nonnull Player             playerComponent,
                           @Nonnull Store<EntityStore> store) {

        if (!isVorthraxAlive() || vorthraxPosition == null) {
            if (activeHuds.containsKey(playerRef)) {
                hideBossBar(playerRef);
            }
            return;
        }

        boolean inRange   = playerPos.distanceTo(vorthraxPosition) <= BOSS_RANGE;
        boolean hudActive = activeHuds.containsKey(playerRef);

        if (inRange && !hudActive) {
            showBossBar(playerRef, playerComponent, store);
        } else if (!inRange && hudActive) {
            hideBossBar(playerRef);
        }
    }

    // ── Aggiornamento HP (VorthraxDamageSystem) ───────────────────────────────

    public void onVorthraxDamaged(@Nonnull Ref<EntityStore> vorthraxEntityRef,
                                  float healthPercent,
                                  @Nonnull Store<EntityStore> store) {
        this.lastHealthPercent = healthPercent;

        for (HudEntry entry : activeHuds.values()) {
            entry.hud.updateHealth(healthPercent);
        }

        TransformComponent tc = (TransformComponent) store.getComponent(
                vorthraxEntityRef, TransformComponent.getComponentType());
        if (tc != null) {
            updateVorthraxPosition(tc.getPosition());
        }
    }

    public void onVorthraxDied() {
        for (HudEntry entry : activeHuds.values()) {
            entry.hud.hide();
            entry.playerComponent.getHudManager().resetHud(entry.playerRefComponent);
        }
        activeHuds.clear();
        vorthraxRef       = null;
        vorthraxPosition  = null;
        lastHealthPercent = 1.0f;
    }

    public void clearAll() {
        for (HudEntry entry : activeHuds.values()) {
            entry.hud.hide();
            entry.playerComponent.getHudManager().resetHud(entry.playerRefComponent);
        }
        activeHuds.clear();
        vorthraxRef       = null;
        vorthraxPosition  = null;
        lastHealthPercent = 1.0f;
    }

    private void showBossBar(@Nonnull Ref<EntityStore>  playerRef,
                             @Nonnull Player             playerComponent,
                             @Nonnull Store<EntityStore> store) {

        com.hypixel.hytale.server.core.universe.PlayerRef pRefComponent =
                (com.hypixel.hytale.server.core.universe.PlayerRef) store.getComponent(
                        playerRef,
                        com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
        if (pRefComponent == null) return;

        BossHealthHUD hud = new BossHealthHUD(pRefComponent, BOSS_NAME, lastHealthPercent);

        playerComponent.getHudManager().setCustomHud(pRefComponent, hud);
        hud.show();

        activeHuds.put(playerRef, new HudEntry(hud, pRefComponent, playerComponent));
    }

    private void hideBossBar(@Nonnull Ref<EntityStore> playerRef) {
        HudEntry entry = activeHuds.remove(playerRef);
        if (entry == null) return;

        entry.hud.hide();
        entry.playerComponent.getHudManager().resetHud(entry.playerRefComponent);
    }
}
