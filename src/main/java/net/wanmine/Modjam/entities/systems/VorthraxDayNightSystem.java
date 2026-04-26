package net.wanmine.Modjam.entities.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.Modjam.entities.components.VorthraxDayNightComponent;
import net.wanmine.Modjam.utils.TimeUtil;

import com.hypixel.hytale.logger.HytaleLogger;
import javax.annotation.Nonnull;

public class VorthraxDayNightSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String VORTHRAX_ASSET_ID = "Vorthrax";

    private static final String MODEL_DAY     = "NPC/Boss/Vorthrax/Models/Vorthrax_Day.blockymodel";
    private static final String MODEL_NIGHT   = "NPC/Boss/Vorthrax/Models/Vorthrax_Night.blockymodel";
    private static final String TEXTURE_DAY   = "NPC/Boss/Vorthrax/Models/Model_Textures/Vorthrax_Texture.png";
    private static final String TEXTURE_NIGHT = "NPC/Boss/Vorthrax/Models/Model_Textures/Vorthrax_NightTexture.png";

    private static final long TICK_INTERVAL_MS = 1000L;
    private long lastTickMs = 0;

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        long now = System.currentTimeMillis();
        if (now - lastTickMs < TICK_INTERVAL_MS) return;
        lastTickMs = now;

        ModelComponent modelComponent = chunk.getComponent(index, ModelComponent.getComponentType());
        if (modelComponent == null) return;

        Model model = modelComponent.getModel();
        if (model == null || !VORTHRAX_ASSET_ID.equals(model.getModelAssetId())) return;

        Ref<EntityStore> vorthraxRef = chunk.getReferenceTo(index);

        VorthraxDayNightComponent dayNight = chunk.getComponent(index, VorthraxDayNightComponent.TYPE);
        if (dayNight == null) {
            commandBuffer.putComponent(vorthraxRef, VorthraxDayNightComponent.TYPE, new VorthraxDayNightComponent());
            return;
        }

        boolean isDayTime = TimeUtil.isDayTime(store);
        if (isDayTime == dayNight.isDay) return;

        dayNight.isDay = isDayTime;
        swapModel(vorthraxRef, isDayTime, commandBuffer, model);
    }

    private void swapModel(@Nonnull Ref<EntityStore> ref,
                           boolean isDayTime,
                           @Nonnull CommandBuffer<EntityStore> commandBuffer,
                           @Nonnull Model old) {

        String newModelPath   = isDayTime ? MODEL_DAY   : MODEL_NIGHT;
        String newTexturePath = isDayTime ? TEXTURE_DAY : TEXTURE_NIGHT;

        Model newModel = new Model(
                old.getModelAssetId(),
                old.getScale(),
                old.getRandomAttachmentIds(),
                old.getAttachments(),
                old.getBoundingBox(),
                newModelPath,
                newTexturePath,
                old.getGradientSet(),
                old.getGradientId(),
                old.getEyeHeight(),
                old.getCrouchOffset(),
                old.getSittingOffset(),
                old.getSleepingOffset(),
                old.getAnimationSetMap().isEmpty() ? null : old.getAnimationSetMap(),
                old.getCamera(),
                old.getLight(),
                old.getParticles(),
                old.getTrails(),
                old.getPhysicsValues(),
                old.getDetailBoxes(),
                old.getPhobia(),
                old.getPhobiaModelAssetId()
        );

        commandBuffer.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
        LOGGER.atInfo().log("[VorthraxDayNight] Swap -> model=%s, texture=%s", newModelPath, newTexturePath);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return ModelComponent.getComponentType();
    }
}