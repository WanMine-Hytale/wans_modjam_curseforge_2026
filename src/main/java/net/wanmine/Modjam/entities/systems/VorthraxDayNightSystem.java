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
import com.hypixel.hytale.server.npc.entities.NPCEntity;
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

    private static final Query<EntityStore> QUERY = Query.and(new Query[]{
            NPCEntity.getComponentType(),
            ModelComponent.getComponentType()
    });

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        NPCEntity npc = (NPCEntity) chunk.getComponent(index, NPCEntity.getComponentType());
        if (npc == null) return;
        if (!VORTHRAX_ASSET_ID.equals(npc.getRoleName())) return;

        ModelComponent modelComponent = chunk.getComponent(index, ModelComponent.getComponentType());
        if (modelComponent == null) return;

        Model model = modelComponent.getModel();
        if (model == null) return;

        Ref<EntityStore> vorthraxRef = chunk.getReferenceTo(index);

        VorthraxDayNightComponent dayNight = chunk.getComponent(index, VorthraxDayNightComponent.TYPE);
        if (dayNight == null) {
            //LOGGER.atInfo().log("[VorthraxDayNight] index=%d -> registrazione component", index);
            commandBuffer.putComponent(vorthraxRef, VorthraxDayNightComponent.TYPE, new VorthraxDayNightComponent());
            return;
        }

        boolean isDayTime;
        try {
            isDayTime = TimeUtil.isDayTime(store);
        } catch (Exception e) {
            //LOGGER.atInfo().log("[VorthraxDayNight] index=%d -> errore lettura tempo: %s", index, e.toString());
            return;
        }

        if (isDayTime == dayNight.isDay) return;

        //LOGGER.atInfo().log("[VorthraxDayNight] index=%d -> cambio fase! %s -> %s", index,dayNight.isDay ? "DAY" : "NIGHT", isDayTime      ? "DAY" : "NIGHT");

        VorthraxDayNightComponent updated = new VorthraxDayNightComponent();
        updated.isDay = isDayTime;
        commandBuffer.putComponent(vorthraxRef, VorthraxDayNightComponent.TYPE, updated);

        swapModel(vorthraxRef, isDayTime, commandBuffer, model);
    }

    private void swapModel(@Nonnull Ref<EntityStore> ref,
                           boolean isDayTime,
                           @Nonnull CommandBuffer<EntityStore> commandBuffer,
                           @Nonnull Model old) {

        String newModelPath   = isDayTime ? MODEL_DAY   : MODEL_NIGHT;
        String newTexturePath = isDayTime ? TEXTURE_DAY : TEXTURE_NIGHT;

        //LOGGER.atInfo().log("[VorthraxDayNight] swapModel -> fase=%s, model=%s, texture=%s", isDayTime ? "DAY" : "NIGHT", newModelPath, newTexturePath);

        Model newModel;
        try {
            newModel = new Model(
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
        } catch (Exception e) {
            //LOGGER.atInfo().log("[VorthraxDayNight] swapModel -> errore nella costruzione del nuovo Model: %s", e.toString());
            return;
        }

        try {
            commandBuffer.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
            //LOGGER.atInfo().log("[VorthraxDayNight] swapModel -> ModelComponent aggiornato nel CommandBuffer");
        } catch (Exception e) {
            //LOGGER.atInfo().log("[VorthraxDayNight] swapModel -> errore nel putComponent: %s", e.toString());
        }
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }
}