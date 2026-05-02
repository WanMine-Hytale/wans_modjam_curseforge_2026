package net.wanmine.SkyAdventures.entities.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import net.wanmine.SkyAdventures.entities.managers.BossBarManager;

import javax.annotation.Nonnull;

/**
 * Scansiona tutti gli NPCEntity ogni tick.
 * Se trova Vorthrax → registra/aggiorna ref e posizione nel BossBarManager.
 * Se Vorthrax non è più valido nel manager ma esiste nel mondo → lo riregistra.
 */
public class VorthraxTrackerSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(new Query[]{
            NPCEntity.getComponentType(),
            TransformComponent.getComponentType()
    });

    private final BossBarManager bossBarManager;

    public VorthraxTrackerSystem(BossBarManager bossBarManager) {
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

        if (bossBarManager.isVorthraxAlive()) return;

        NPCEntity npc = (NPCEntity) archetypeChunk.getComponent(index, NPCEntity.getComponentType());
        if (npc == null) return;
        if (!"Vorthrax".equals(npc.getRoleName())) return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        TransformComponent transform = (TransformComponent) archetypeChunk.getComponent(index, TransformComponent.getComponentType());

        Vector3d position = transform != null ? transform.getPosition().clone() : new Vector3d();
        bossBarManager.registerVorthrax(ref, position);
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }
}