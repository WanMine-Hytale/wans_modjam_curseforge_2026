package net.wanmine.Modjam.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import net.wanmine.Modjam.managers.BossBarManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VorthraxDamageSystem extends DamageEventSystem {

    private static final Query<EntityStore> QUERY = Query.and(new Query[]{
            NPCEntity.getComponentType(),
            EntityStatMap.getComponentType()
    });

    private final BossBarManager bossBarManager;

    public VorthraxDamageSystem(@Nonnull BossBarManager bossBarManager) {
        this.bossBarManager = bossBarManager;
    }

    @Nullable
    @Override
    public com.hypixel.hytale.component.SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {

        NPCEntity npc = (NPCEntity) archetypeChunk.getComponent(
                index, NPCEntity.getComponentType());

        if (npc == null || !BossBarManager.BOSS_NAME.equals(npc.getRoleName())) return;

        EntityStatMap statMap = (EntityStatMap) archetypeChunk.getComponent(
                index, EntityStatMap.getComponentType());

        if (statMap == null) return;

        EntityStatValue health = statMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null) return;

        float percent = health.asPercentage();
        Ref<EntityStore> vorthraxRef = archetypeChunk.getReferenceTo(index);

        if (percent <= 0f) {
            bossBarManager.onVorthraxDeath(vorthraxRef, store);
            return;
        }

        bossBarManager.onVorthraxDamaged(vorthraxRef, percent, store);
    }
}