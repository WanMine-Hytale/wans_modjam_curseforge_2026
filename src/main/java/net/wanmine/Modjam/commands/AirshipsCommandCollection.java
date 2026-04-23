package net.wanmine.Modjam.commands;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.Modjam.entities.components.FlyingDriverComponent;
import net.wanmine.Modjam.entities.components.FlyingEntityComponent;
import net.wanmine.Modjam.utils.AirshipFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
public class AirshipsCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public AirshipsCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("No Player Component?");
            return;
        }

        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            LOGGER.atWarning().log("No playerEntityRef?");
            return;
        }

        TransformComponent playerTransform = store.getComponent(
                playerEntityRef,
                EntityModule.get().getTransformComponentType()
        );
        if (playerTransform == null) {
            LOGGER.atWarning().log("No player transform?");
            return;
        }

        store.tryRemoveComponent(playerEntityRef, FlyingDriverComponent.getComponentType());
        store.tryRemoveComponent(playerEntityRef, MountedComponent.getComponentType());

        world.execute(() -> {
            FlyingDriverComponent existingDriver = store.getComponent(
                    playerEntityRef,
                    FlyingDriverComponent.getComponentType()
            );
            if (existingDriver != null) {
                LOGGER.atWarning().log("[TestMountsCommand] Player already has FlyingDriverComponent!");
                return;
            }

            Vector3d basePos = playerTransform.getPosition().clone();
            Vector3f baseRot = playerTransform.getRotation().clone();

            // #region Airship
            TransformComponent mountTransform = new TransformComponent();
            mountTransform.setPosition(basePos.clone());
            mountTransform.getRotation().assign(baseRot);

            Holder<EntityStore> mountHolder = EntityStore.REGISTRY.newHolder();

            ModelAsset mountModelAsset = ModelAsset.getAssetMap().getAsset("Airship_Two");
            if (mountModelAsset == null) {
                LOGGER.atWarning().log("No mount model asset?");
                return;
            }

            Model mountModel = Model.createScaledModel(mountModelAsset, 2.0f);

            mountHolder.addComponent(FlyingEntityComponent.getComponentType(), new FlyingEntityComponent());
            mountHolder.addComponent(TransformComponent.getComponentType(), mountTransform);
            mountHolder.addComponent(PersistentModel.getComponentType(), new PersistentModel(mountModel.toReference()));
            mountHolder.addComponent(ModelComponent.getComponentType(), new ModelComponent(mountModel));
            mountHolder.addComponent(BoundingBox.getComponentType(), new BoundingBox(mountModel.getBoundingBox()));
            mountHolder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            mountHolder.addComponent(Nameplate.getComponentType(), new Nameplate("Mount"));
            mountHolder.ensureComponent(UUIDComponent.getComponentType());
            mountHolder.addComponent(HeadRotation.getComponentType(), new HeadRotation(baseRot));

            Ref<EntityStore> mountableEntity = store.addEntity(mountHolder, AddReason.SPAWN);
            if (mountableEntity == null || !mountableEntity.isValid()) {
                LOGGER.atWarning().log("Failed to spawn mountableEntity");
                return;
            }

            FlyingEntityComponent flyingEntity = store.getComponent(
                    mountableEntity,
                    FlyingEntityComponent.getComponentType()
            );
            if (flyingEntity == null) {
                LOGGER.atWarning().log("No FlyingEntityComponent on mountableEntity?");
                return;
            }

            flyingEntity.ySpeed = 0.0;
            flyingEntity.currentSpeed = 0.0;
            flyingEntity.hoverTargetY = basePos.y;
            // #endregion

            // #region Seat entity
            TransformComponent seatTransform = new TransformComponent();
            seatTransform.setPosition(basePos.clone().add(new Vector3d(0.0, 1.0, 0.0)));
            seatTransform.getRotation().assign(baseRot);

            Holder<EntityStore> riderHolder = EntityStore.REGISTRY.newHolder();

            ModelAsset seatModelAsset = ModelAsset.getAssetMap().getAsset("Airship_Cube");
            if (seatModelAsset == null) {
                LOGGER.atWarning().log("No seat model asset?");
                return;
            }

            Model seatModel = Model.createScaledModel(seatModelAsset, 1.0f);

            FlyingSeatComponent seatComponent = new FlyingSeatComponent(mountableEntity)
                    .withPlayerRef(playerEntityRef)
                    .withLocalOffset(new Vector3d(0.0, 1.0, 0.0));

            riderHolder.addComponent(TransformComponent.getComponentType(), seatTransform);
            riderHolder.addComponent(PersistentModel.getComponentType(), new PersistentModel(seatModel.toReference()));
            riderHolder.addComponent(ModelComponent.getComponentType(), new ModelComponent(seatModel));
            riderHolder.addComponent(BoundingBox.getComponentType(), new BoundingBox(seatModel.getBoundingBox()));
            riderHolder.addComponent(FlyingSeatComponent.getComponentType(), seatComponent);
            riderHolder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            riderHolder.addComponent(Nameplate.getComponentType(), new Nameplate("Rider"));
            riderHolder.ensureComponent(UUIDComponent.getComponentType());

            Ref<EntityStore> riderEntity = store.addEntity(riderHolder, AddReason.SPAWN);
            if (riderEntity == null || !riderEntity.isValid()) {
                LOGGER.atWarning().log("Failed to spawn riderEntity");
                return;
            }

            UUIDComponent uuidComponent = store.getComponent(riderEntity, UUIDComponent.getComponentType());
            if (uuidComponent == null) {
                LOGGER.atWarning().log("No UUIDComponent on riderEntity?");
                return;
            }

            flyingEntity.addAttachment(
                    uuidComponent.getUuid(),
                    riderEntity,
                    mountableEntity,
                    store,
                    FlyingSeatComponent.getComponentType()
            );
            flyingEntity.setDriverSeat(riderEntity);
            // #endregion

            // #region Player mount
            NetworkId riderNetId = store.getComponent(riderEntity, NetworkId.getComponentType());
            if (riderNetId == null) {
                LOGGER.atWarning().log("No NetworkId on riderEntity?");
                return;
            }

            Vector3f attachmentOffset = new Vector3f(0.0F, 0.5F, 0.0F);

            FlyingDriverComponent driver = new FlyingDriverComponent();
            driver.setFlyerRef(mountableEntity);
            driver.setSeatRef(riderEntity);
            driver.setAttachmentOffset(attachmentOffset);
            driver.setControllerType(MountController.Minecart);
            driver.markNetworkOutdated();

            store.addComponent(
                    playerEntityRef,
                    FlyingDriverComponent.getComponentType(),
                    driver
            );

            flyingEntity.setDriver(playerEntityRef);

            LOGGER.atInfo().log(
                    "[TestMount] mounted player=%s seat=%s flyer=%s seatNetId=%s offset=%s,%s,%s",
                    playerEntityRef,
                    riderEntity,
                    mountableEntity,
                    riderNetId.getId(),
                    attachmentOffset.x,
                    attachmentOffset.y,
                    attachmentOffset.z
            );
            // #endregion
        });
    }
}*/

class AirshipSpawnCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public AirshipSpawnCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
        requirePermission("airships.spawn");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("No Player Component?");
            return;
        }

        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            LOGGER.atWarning().log("No playerEntityRef?");
            return;
        }

        TransformComponent playerTransform = store.getComponent(
                playerEntityRef,
                EntityModule.get().getTransformComponentType()
        );
        if (playerTransform == null) {
            LOGGER.atWarning().log("No player transform?");
            return;
        }

        store.tryRemoveComponent(playerEntityRef, FlyingDriverComponent.getComponentType());

        world.execute(() -> {
            if (store.getComponent(playerEntityRef, FlyingDriverComponent.getComponentType()) != null) {
                LOGGER.atWarning().log("Player already has FlyingDriverComponent!");
                return;
            }

            Vector3d basePos = playerTransform.getPosition().clone();
            Vector3f baseRot = playerTransform.getRotation().clone();

            AirshipFactory.spawnMount(store, basePos, baseRot);
        });
    }
}

class AirshipMountSubCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public AirshipMountSubCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
        requirePermission("airships.mount");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("No Player Component?");
            return;
        }

        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            LOGGER.atWarning().log("No playerEntityRef?");
            return;
        }

        TransformComponent playerTransform = store.getComponent(
                playerEntityRef,
                EntityModule.get().getTransformComponentType()
        );
        if (playerTransform == null) {
            LOGGER.atWarning().log("No player transform?");
            return;
        }

        store.tryRemoveComponent(playerEntityRef, FlyingDriverComponent.getComponentType());
        store.tryRemoveComponent(playerEntityRef, MountedComponent.getComponentType());

        world.execute(() -> {
            if (store.getComponent(playerEntityRef, FlyingDriverComponent.getComponentType()) != null) {
                LOGGER.atWarning().log("Player already has FlyingDriverComponent!");
                return;
            }

            Vector3d basePos = playerTransform.getPosition().clone();
            Vector3f baseRot = playerTransform.getRotation().clone();

            Ref<EntityStore> mountRef = AirshipFactory.spawnMount(store, basePos, baseRot);
            if (mountRef == null) return;

            AirshipFactory.mountPlayer(store, mountRef, playerEntityRef, basePos, baseRot);
        });
    }
}

class AirshipDismountSubCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public AirshipDismountSubCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("No Player Component?");
            return;
        }

        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            LOGGER.atWarning().log("No playerEntityRef?");
            return;
        }

        world.execute(() -> {
            FlyingDriverComponent driver = store.getComponent(playerEntityRef, FlyingDriverComponent.getComponentType());
            if (driver == null) {
                LOGGER.atWarning().log("Player doesn't have FlyingDriverComponent!");
                return;
            }

            Ref<EntityStore> flyerRef = driver.getFlyerRef();
            if (flyerRef == null) {
                LOGGER.atWarning().log("Player doesn't have Flyer ref!");
                return;
            }

            AirshipFactory.dismountPlayer(store, flyerRef, playerEntityRef);
        });
    }
}

class AirshipDestroySubCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final FlagArg destroyAllArg;


    public AirshipDestroySubCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
        this.destroyAllArg = this.withFlagArg("all", "Destroy all Airships");
        requirePermission("airships.destroy");
    }

    public record DestroyFindResult(Ref<EntityStore> entityRef, TransformComponent mountTransform) {
        public void destroy(Store<EntityStore> store) {
            AirshipFactory.destroyMount(store, this.entityRef);
            LOGGER.atWarning().log("Removed Airship at position (%s,%s,%s)", mountTransform.getPosition().x, mountTransform.getPosition().y, mountTransform.getPosition().z);
        }
    }

    private void executeDestroy(CommandContext ctx, Store<EntityStore> store, TransformComponent playerTransform) {
        List<DestroyFindResult> entities = new ArrayList<>();

        store.forEachEntityParallel((int index, ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> buffer) -> {
            Ref<EntityStore> entityRef = chunk.getReferenceTo(index);
            TransformComponent mountTransform = chunk.getComponent(index, TransformComponent.getComponentType());
            FlyingEntityComponent flyingEntity = chunk.getComponent(index, FlyingEntityComponent.getComponentType());
            if (mountTransform != null && flyingEntity != null) {
                entities.add(new DestroyFindResult(entityRef, mountTransform));
            }
        });

        entities.forEach((entity) -> {
            if (this.destroyAllArg.get(ctx) == true) {
                entity.destroy(store);
            } else {
                double dist = playerTransform.getPosition().distanceTo(entity.mountTransform.getPosition());
                if (dist <= 10.0) {
                    entity.destroy(store);
                }
            }
        });

    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("No Player Component?");
            return;
        }

        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            LOGGER.atWarning().log("No playerEntityRef?");
            return;
        }

        world.execute(() -> {
            TransformComponent playerTransform = store.getComponent(playerEntityRef, EntityModule.get().getTransformComponentType());
            if (playerTransform == null) return;
            FlyingDriverComponent driver = store.getComponent(playerEntityRef, FlyingDriverComponent.getComponentType());
            if (driver == null) {
                this.executeDestroy(ctx, store, playerTransform);
                return;
            }
            Ref<EntityStore> flyerRef = driver.getFlyerRef();
            if (flyerRef == null || !flyerRef.isValid()) {
                this.executeDestroy(ctx, store, playerTransform);
                return;
            }
            AirshipFactory.destroyMount(store, flyerRef);
        });
    }
}

public class AirshipsCommandCollection extends AbstractCommandCollection {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public AirshipsCommandCollection(@Nonnull String name, @Nonnull String description) {
        super(name, description);
        addSubCommand(new AirshipSpawnCommand("spawn", "Spawn an Airship"));
        addSubCommand(new AirshipMountSubCommand("mount", "Spawn an Airship and mount directly"));
        addSubCommand(new AirshipDismountSubCommand("dismount", "Dismount from the current Airship"));
        addSubCommand(new AirshipDestroySubCommand("destroy", "Remove last spawned Airship"));
    }
}