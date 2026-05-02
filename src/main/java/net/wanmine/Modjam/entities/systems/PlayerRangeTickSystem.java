package net.wanmine.Modjam.entities.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.Modjam.entities.managers.BossBarManager;

import javax.annotation.Nonnull;

/**
 * Sistema tick che gira su tutti i Player ogni frame.
 * Controlla se ogni player è nel range di Vorthrax:
 *   - entrato nel range → mostra bossbar
 *   - uscito dal range  → nascondi bossbar
 *
 * Richiede sia Player che TransformComponent per leggere la posizione.
 */
public class PlayerRangeTickSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    private static final Query<EntityStore> QUERY = Query.and(new Query[]{
            Player.getComponentType(),
            TransformComponent.getComponentType()
    });

    private final BossBarManager bossBarManager;

    public PlayerRangeTickSystem(BossBarManager bossBarManager) {
        this.bossBarManager = bossBarManager;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        if (!bossBarManager.isVorthraxAlive()) {}

        TransformComponent playerTransform = (TransformComponent) archetypeChunk.getComponent(
                index, TransformComponent.getComponentType());
        if (playerTransform == null) return;

        Player playerComponent = (Player) archetypeChunk.getComponent(
                index, Player.getComponentType());
        if (playerComponent == null) return;

        var playerRef = archetypeChunk.getReferenceTo(index);
        Vector3d playerPos = playerTransform.getPosition();

        bossBarManager.tickPlayer(playerRef, playerPos, playerComponent, store);
    }
}