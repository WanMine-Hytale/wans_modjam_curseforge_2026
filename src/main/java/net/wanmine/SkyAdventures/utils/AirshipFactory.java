package net.wanmine.SkyAdventures.utils;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.SkyAdventures.entities.components.FlyingDriverComponent;
import net.wanmine.SkyAdventures.entities.components.FlyingEntityComponent;
import net.wanmine.SkyAdventures.entities.components.attachments.FlyingSeatComponent;

public class AirshipFactory {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static Ref<EntityStore> spawnMount(Store<EntityStore> store, Vector3d position, Vector3f rotation) {
        ModelAsset mountModelAsset = ModelAsset.getAssetMap().getAsset("Airship");
        if (mountModelAsset == null) {
            LOGGER.atWarning().log("No mount model asset?");
            return null;
        }

        Model mountModel = Model.createScaledModel(mountModelAsset, 2.0f);

        TransformComponent mountTransform = new TransformComponent();
        mountTransform.setPosition(position.clone());
        mountTransform.getRotation().assign(rotation);

        FlyingEntityComponent flyingEntity = new FlyingEntityComponent();
        flyingEntity.ySpeed = 0.0;
        flyingEntity.currentSpeed = 0.0;
        flyingEntity.hoverTargetY = position.y;

        Holder<EntityStore> mountHolder = EntityStore.REGISTRY.newHolder();
        mountHolder.addComponent(FlyingEntityComponent.getComponentType(), flyingEntity);
        mountHolder.addComponent(TransformComponent.getComponentType(), mountTransform);
        mountHolder.addComponent(PersistentModel.getComponentType(), new PersistentModel(mountModel.toReference()));
        mountHolder.addComponent(ModelComponent.getComponentType(), new ModelComponent(mountModel));
        mountHolder.addComponent(BoundingBox.getComponentType(), new BoundingBox(mountModel.getBoundingBox()));
        mountHolder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
        /*mountHolder.addComponent(Nameplate.getComponentType(), new Nameplate("Mount"));*/
        mountHolder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        mountHolder.ensureComponent(UUIDComponent.getComponentType());

        mountHolder.ensureComponent(Interactable.getComponentType());
        Interactions interactions = new Interactions();
        interactions.setInteractionId(InteractionType.Use, "FlyMountInteraction");
        mountHolder.addComponent(Interactions.getComponentType(), interactions);

        Ref<EntityStore> mountRef = store.addEntity(mountHolder, AddReason.SPAWN);
        if (mountRef == null || !mountRef.isValid()) {
            LOGGER.atWarning().log("Failed to spawn mount");
            return null;
        }

        flyingEntity.setReadyToFly(true);
        return mountRef;
    }

    public static Ref<EntityStore> spawnMount(Store<EntityStore> store, Vector3d position, Vector3f rotation, float initialScale) {
        ModelAsset mountModelAsset = ModelAsset.getAssetMap().getAsset("Airship");
        if (mountModelAsset == null) {
            LOGGER.atWarning().log("No mount model asset?");
            return null;
        }

        float scale = Math.clamp(initialScale, 0.0f, FlyingEntityComponent.DEFAULT_MODEL_SCALE);
        Model mountModel = Model.createScaledModel(mountModelAsset, scale);

        FlyingEntityComponent flyingEntity = new FlyingEntityComponent();
        flyingEntity.ySpeed = 0.0;
        flyingEntity.currentSpeed = 0.0;
        flyingEntity.hoverTargetY = position.y;
        flyingEntity.setModelScale(scale);
        if (scale < FlyingEntityComponent.DEFAULT_MODEL_SCALE) {
            flyingEntity.setReadyToFly(false);
        }

        TransformComponent mountTransform = new TransformComponent();
        mountTransform.setPosition(position.clone());
        mountTransform.getRotation().assign(rotation);


        Holder<EntityStore> mountHolder = EntityStore.REGISTRY.newHolder();
        mountHolder.addComponent(FlyingEntityComponent.getComponentType(), flyingEntity);
        mountHolder.addComponent(TransformComponent.getComponentType(), mountTransform);
        mountHolder.addComponent(PersistentModel.getComponentType(), new PersistentModel(mountModel.toReference()));
        mountHolder.addComponent(ModelComponent.getComponentType(), new ModelComponent(mountModel));
        mountHolder.addComponent(BoundingBox.getComponentType(), new BoundingBox(mountModel.getBoundingBox()));
        mountHolder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
        /*mountHolder.addComponent(Nameplate.getComponentType(), new Nameplate("Mount"));*/
        mountHolder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        mountHolder.ensureComponent(UUIDComponent.getComponentType());

        mountHolder.ensureComponent(Interactable.getComponentType());
        Interactions interactions = new Interactions();
        interactions.setInteractionId(InteractionType.Use, "FlyMountInteraction");
        mountHolder.addComponent(Interactions.getComponentType(), interactions);

        Ref<EntityStore> mountRef = store.addEntity(mountHolder, AddReason.SPAWN);
        if (mountRef == null || !mountRef.isValid()) {
            LOGGER.atWarning().log("Failed to spawn mount");
            return null;
        }

        return mountRef;
    }

    public static void destroyMount(Store<EntityStore> store, Ref<EntityStore> mountRef) {
        FlyingEntityComponent flyingEntity = store.getComponent(mountRef, FlyingEntityComponent.getComponentType());
        if (flyingEntity == null) return;

        Ref<EntityStore> driverRef = flyingEntity.getDriver();
        if (driverRef != null && driverRef.isValid()) {
            dismountPlayer(store, mountRef, driverRef);
        }

        Ref<EntityStore> seatRef = flyingEntity.getDriverSeat();
        if (seatRef != null && seatRef.isValid()) {
            store.removeEntity(seatRef, RemoveReason.REMOVE);
        }

        flyingEntity.getSeats().forEach((_, seat) -> {
            if (seat != null && seat.isValid()) store.removeEntity(seat, RemoveReason.REMOVE);
        });

        store.removeEntity(mountRef, RemoveReason.REMOVE);
        LOGGER.atInfo().log("Mount destroyed | mountRef=%s | seatRef=%s", mountRef, seatRef);
    }

    public static Ref<EntityStore> spawnSeat(Store<EntityStore> store,
                                             Ref<EntityStore> mountRef,
                                             Ref<EntityStore> playerRef,
                                             Vector3d position,
                                             Vector3f rotation) {
        ModelAsset seatModelAsset = ModelAsset.getAssetMap().getAsset("Airship_DriverSeat");
        if (seatModelAsset == null) {
            LOGGER.atWarning().log("No seat model asset?");
            return null;
        }

        Model seatModel = Model.createScaledModel(seatModelAsset, 2f);

        TransformComponent seatTransform = new TransformComponent();
        seatTransform.setPosition(position.clone().add(new Vector3d(0.0, 1.0, 0.0)));
        seatTransform.getRotation().assign(rotation);

        FlyingSeatComponent seatComponent = new FlyingSeatComponent(mountRef)
                .withPlayerRef(playerRef)
                .withLocalOffset(new Vector3d(0.0, -0.1, -0.2));

        Holder<EntityStore> seatHolder = EntityStore.REGISTRY.newHolder();
        seatHolder.addComponent(TransformComponent.getComponentType(), seatTransform);
        seatHolder.addComponent(PersistentModel.getComponentType(), new PersistentModel(seatModel.toReference()));
        seatHolder.addComponent(ModelComponent.getComponentType(), new ModelComponent(seatModel));
        seatHolder.addComponent(BoundingBox.getComponentType(), new BoundingBox(seatModel.getBoundingBox()));
        seatHolder.addComponent(FlyingSeatComponent.getComponentType(), seatComponent);
        seatHolder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
        /*seatHolder.addComponent(Nameplate.getComponentType(), new Nameplate("Rider"));*/
        seatHolder.ensureComponent(UUIDComponent.getComponentType());

        Ref<EntityStore> seatRef = store.addEntity(seatHolder, AddReason.SPAWN);
        if (seatRef == null || !seatRef.isValid()) {
            LOGGER.atWarning().log("Failed to spawn seat");
            return null;
        }

        FlyingEntityComponent flyingEntity = store.getComponent(mountRef, FlyingEntityComponent.getComponentType());
        if (flyingEntity == null) {
            LOGGER.atWarning().log("No FlyingEntityComponent on mount");
            return null;
        }

        UUIDComponent uuidComponent = store.getComponent(seatRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            LOGGER.atWarning().log("No UUIDComponent on seat");
            return null;
        }

        flyingEntity.addAttachment(uuidComponent.getUuid(), seatRef, mountRef, store, FlyingSeatComponent.getComponentType());
        flyingEntity.setDriverSeat(seatRef);



        return seatRef;
    }



    public static void mountPlayer(Store<EntityStore> store,
                                   Ref<EntityStore> mountRef,
                                   Ref<EntityStore> playerRef,
                                   Vector3d position,
                                   Vector3f rotation) {
        Ref<EntityStore> seatRef = spawnSeat(store, mountRef, playerRef, position, rotation);
        if (seatRef == null) return;

        NetworkId seatNetId = store.getComponent(seatRef, NetworkId.getComponentType());
        if (seatNetId == null) {
            LOGGER.atWarning().log("No NetworkId on seat");
            return;
        }

        Vector3f attachmentOffset = new Vector3f(0.0F, 0.3F, 0.0F);

        FlyingDriverComponent driver = new FlyingDriverComponent();
        driver.setFlyerRef(mountRef);
        driver.setSeatRef(seatRef);
        driver.setAttachmentOffset(attachmentOffset);
        driver.setControllerType(MountController.Minecart);
        driver.markNetworkOutdated();

        store.addComponent(playerRef, FlyingDriverComponent.getComponentType(), driver);

        FlyingEntityComponent flyingEntity = store.getComponent(mountRef, FlyingEntityComponent.getComponentType());
        if (flyingEntity != null) flyingEntity.setDriver(playerRef);

        AnimationUtils.playAnimation(mountRef, AnimationSlot.Movement, null, "Forward", true, store);
    }

    public static void dismountPlayer(Store<EntityStore> store,
                                      Ref<EntityStore> mountRef,
                                      Ref<EntityStore> playerEntityRef) {

        MovementManager movementManager = store.getComponent(playerEntityRef, MovementManager.getComponentType());
        if (movementManager == null) return;

        MovementStatesComponent movementStatesComponent = store.getComponent(playerEntityRef, MovementStatesComponent.getComponentType());
        if (movementStatesComponent == null) return;

        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) return;

        PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (playerRef == null) return;

        FlyingDriverComponent driver = store.getComponent(playerEntityRef, FlyingDriverComponent.getComponentType());
        if (driver == null) return;

        FlyingEntityComponent flyingEntity = store.getComponent(mountRef, FlyingEntityComponent.getComponentType());
        if (flyingEntity != null) flyingEntity.clearDriver();

        store.tryRemoveComponent(playerEntityRef, FlyingDriverComponent.getComponentType());
        Ref<EntityStore> seatRef = driver.getSeatRef();
        if (seatRef != null && seatRef.isValid()) {
            store.removeEntity(seatRef, RemoveReason.REMOVE);
        }

        boolean isCreative = player.getGameMode() == GameMode.Creative;

        AnimationUtils.stopAnimation(playerEntityRef, AnimationSlot.Movement, true, store);

        movementManager.getSettings().canFly = isCreative;
        movementManager.update(playerRef.getPacketHandler());

        movementStatesComponent.getMovementStates().flying = false;
        movementStatesComponent.setSentMovementStates(movementStatesComponent.getMovementStates());
        store.putComponent(playerEntityRef, MovementStatesComponent.getComponentType(), movementStatesComponent);
        player.applyMovementStates(playerEntityRef, new SavedMovementStates(false), movementStatesComponent.getMovementStates(), store);

        player.setMountEntityId(-1);
    }
}