package net.wanmine.Modjam.entities.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import net.wanmine.Modjam.entities.components.FlyingDriverComponent;
import net.wanmine.Modjam.entities.components.FlyingEntityComponent;
import net.wanmine.Modjam.entities.components.attachments.FlyingSeatComponent;
import com.hypixel.hytale.component.system.RefSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public final class FlyMountSystems {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FlyMountSystems() {
    }

    private static void applyFlyMountCamera(@Nonnull Ref<EntityStore> playerRef, @Nonnull Ref<EntityStore> flyerRef, @Nonnull Store<EntityStore> store) {
        PlayerRef player = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (player == null) {
            return;
        }

        ServerCameraSettings settings = new ServerCameraSettings();
        settings.distance = 12.0f;
        settings.positionLerpSpeed = 0.15f;
        settings.rotationLerpSpeed = 0.2f;
        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffsetRaycast;
        settings.movementForceRotationType = MovementForceRotationType.AttachedToHead;
        settings.mouseInputType = MouseInputType.LookAtTarget;
        settings.displayCursor = false;
        settings.canMoveType = CanMoveType.Always;
        settings.positionOffset = new Position(0.0f, 5.0f, 0.0f);
        /*settings.attachedToType = AttachedToType.EntityId;*/
        settings.allowPitchControls = true;
        settings.isFirstPerson = false;
        settings.displayReticle = false;

        player.getPacketHandler().write(new SetServerCamera(ClientCameraView.Custom, true, settings));
    }

    private static void resetFlyMountCamera(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store) {
        PlayerRef player = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (player == null) {
            return;
        }

        player.getPacketHandler().writeNoCache(
                new SetServerCamera(ClientCameraView.Custom, false, null)
        );
    }

    private static void handleFlyingMountedRemoval(@Nonnull Ref<EntityStore> playerRef, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull FlyingDriverComponent driver) {
        Ref<EntityStore> seatRef = driver.getSeatRef();
        if (seatRef != null && seatRef.isValid()) {
            FlyingSeatComponent seat = commandBuffer.getComponent(seatRef, FlyingSeatComponent.getComponentType());
            if (seat != null && playerRef.equals(seat.getPlayerRef())) {
                seat.setPlayerRef(null);
            }
        }
    }

    public static final class SeatFollowFlyerSystem extends EntityTickingSystem<EntityStore> {

        private final ComponentType<EntityStore, FlyingSeatComponent> flyingSeatComponentType =
                FlyingSeatComponent.getComponentType();
        private final ComponentType<EntityStore, TransformComponent> transformComponentType =
                TransformComponent.getComponentType();

        private final Query<EntityStore> query = Archetype.of(
                flyingSeatComponentType,
                transformComponentType
        );

        private final Set<Dependency<EntityStore>> dependencies = Set.of(
                new SystemDependency<>(Order.AFTER, FlyMountMovementSystem.class)
        );

        @Override
        public void tick(float dt,
                         int index,
                         @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                         @Nonnull Store<EntityStore> store,
                         @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            FlyingSeatComponent seat = archetypeChunk.getComponent(index, flyingSeatComponentType);
            TransformComponent seatTransform = archetypeChunk.getComponent(index, transformComponentType);

            if (seat == null || seatTransform == null) {
                return;
            }

            Ref<EntityStore> flyerRef = seat.getFlyerRef();
            if (flyerRef == null || !flyerRef.isValid()) {
                return;
            }

            TransformComponent flyerTransform = store.getComponent(flyerRef, transformComponentType);
            if (flyerTransform == null) {
                return;
            }

            Vector3d flyerPos = flyerTransform.getPosition().clone();
            Vector3f flyerRot = flyerTransform.getRotation().clone();

            Vector3d localOffset = seat.getLocalOffset();
            if (localOffset == null) {
                localOffset = new Vector3d(0.0, 0.0, 0.0);
            }

            Vector3d rotatedOffset = localOffset.clone().rotateY(flyerRot.getYaw());
            Vector3d seatPos = flyerPos.add(rotatedOffset);

            seatTransform.setPosition(seatPos);
            seatTransform.setRotation(flyerRot);
            seatTransform.markChunkDirty(store);
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return this.dependencies;
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return this.query;
        }
    }

    public static final class ValidationSystem extends EntityTickingSystem<EntityStore> {

        private final ComponentType<EntityStore, FlyingDriverComponent> flyingDriverComponentType = FlyingDriverComponent.getComponentType();
        private final ComponentType<EntityStore, Player> playerComponentType = Player.getComponentType();

        private final Query<EntityStore> query = Archetype.of(flyingDriverComponentType, playerComponentType);

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            FlyingDriverComponent driver = archetypeChunk.getComponent(index, flyingDriverComponentType);
            Player player = archetypeChunk.getComponent(index, playerComponentType);
            if (driver == null || player == null) {
                return;
            }

            Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
            Ref<EntityStore> flyerRef = driver.getFlyerRef();
            Ref<EntityStore> seatRef = driver.getSeatRef();

            if (flyerRef == null || !flyerRef.isValid() || seatRef == null || !seatRef.isValid()) {
                clearDriver(playerRef, player, driver, store, commandBuffer);
                return;
            }

            FlyingSeatComponent seat = store.getComponent(seatRef, FlyingSeatComponent.getComponentType());
            if (seat == null) {
                clearDriver(playerRef, player, driver, store, commandBuffer);
                return;
            }

            if (seat.getFlyerRef() == null || !flyerRef.equals(seat.getFlyerRef())) {
                clearDriver(playerRef, player, driver, store, commandBuffer);
                return;
            }

            if (seat.getPlayerRef() == null || !playerRef.equals(seat.getPlayerRef())) {
                clearDriver(playerRef, player, driver, store, commandBuffer);
            }
        }

        private void clearDriver(@Nonnull Ref<EntityStore> playerRef, @Nonnull Player player, @Nonnull FlyingDriverComponent driver, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            handleFlyingMountedRemoval(playerRef, commandBuffer, driver);
            resetFlyMountCamera(playerRef, store);
            player.setMountEntityId(0);
            driver.setMoveForward(0f);
            driver.setMoveStrafe(0f);
            driver.setVerticalInput(0f);
            commandBuffer.removeComponent(playerRef, FlyingDriverComponent.getComponentType());
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return query;
        }
    }

    public static final class PlayerMount extends RefChangeSystem<EntityStore, FlyingDriverComponent> {

        private final ComponentType<EntityStore, FlyingDriverComponent> flyingDriverComponentType = FlyingDriverComponent.getComponentType();
        private final ComponentType<EntityStore, PlayerInput> playerInputComponentType = PlayerInput.getComponentType();
        private final ComponentType<EntityStore, NetworkId> networkIdComponentType = NetworkId.getComponentType();

        private final Query<EntityStore> query = playerInputComponentType;

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return query;
        }

        @Nonnull
        @Override
        public ComponentType<EntityStore, FlyingDriverComponent> componentType() {
            return flyingDriverComponentType;
        }

        @Override
        public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull FlyingDriverComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            PlayerInput playerInput = commandBuffer.getComponent(ref, playerInputComponentType);
            if (playerInput == null) {
                return;
            }

            Ref<EntityStore> seatRef = component.getSeatRef();
            if (seatRef != null && seatRef.isValid()) {
                NetworkId seatNetworkId = commandBuffer.getComponent(seatRef, networkIdComponentType);
                if (seatNetworkId != null) {
                    playerInput.setMountId(seatNetworkId.getId());
                    playerInput.getMovementUpdateQueue().clear();
                }
            }

            Ref<EntityStore> flyerRef = component.getFlyerRef();
            if (flyerRef != null && flyerRef.isValid()) {
                applyFlyMountCamera(ref, flyerRef, store);
            }

            component.markNetworkOutdated();
        }

        @Override
        public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nullable FlyingDriverComponent oldComponent, @Nonnull FlyingDriverComponent newComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            newComponent.markNetworkOutdated();
        }

        @Override
        public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull FlyingDriverComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            PlayerInput playerInput = commandBuffer.getComponent(ref, playerInputComponentType);
            if (playerInput != null) {
                playerInput.setMountId(0);
            }

            resetFlyMountCamera(ref, store);
        }
    }

    public static final class InputSystem extends EntityTickingSystem<EntityStore> {

        private final ComponentType<EntityStore, PlayerInput> playerInputComponentType = PlayerInput.getComponentType();
        private final ComponentType<EntityStore, FlyingDriverComponent> flyingDriverComponentType = FlyingDriverComponent.getComponentType();

        private final Query<EntityStore> query = Archetype.of(playerInputComponentType, flyingDriverComponentType);

        private final Set<Dependency<EntityStore>> dependencies = Set.of(new SystemDependency<>(Order.BEFORE, PlayerSystems.ProcessPlayerInput.class));

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            FlyingDriverComponent driver = archetypeChunk.getComponent(index, flyingDriverComponentType);
            PlayerInput playerInput = archetypeChunk.getComponent(index, playerInputComponentType);
            Player player = archetypeChunk.getComponent(index, Player.getComponentType());

            if (driver == null || playerInput == null || player == null) {
                return;
            }

            List<PlayerInput.InputUpdate> queue = playerInput.getMovementUpdateQueue();

            float verticalInput = 0.0f;

            Vector3d lastClientVelocity = null;

            boolean consumedMovement = false;
            boolean consumedRiderStates = false;
            boolean sawBody = false;
            boolean sawHead = false;

            for (int i = queue.size() - 1; i >= 0; i--) {
                PlayerInput.InputUpdate update = queue.get(i);
                switch (update) {
                    case PlayerInput.RelativeMovement relative -> {
                        driver.setMoveStrafe((float) relative.getX());
                        driver.setMoveForward((float) relative.getZ());
                        consumedMovement = true;
                        queue.remove(i);
                    }

                    case PlayerInput.AbsoluteMovement absoluteMovement -> {
                        consumedMovement = true;
                        queue.remove(i);
                    }

                    case PlayerInput.SetClientVelocity clientVelocity -> {
                        lastClientVelocity = clientVelocity.getVelocity();
                        consumedMovement = true;
                        queue.remove(i);
                    }

                    case PlayerInput.SetMovementStates movementStates -> {
                        driver.updateLastStates(movementStates.movementStates());
                        consumedRiderStates = true;
                        queue.remove(i);
                    }

                    case PlayerInput.SetRiderMovementStates riderMovementStates -> {
                        driver.updateLastStates(riderMovementStates.movementStates());
                        consumedRiderStates = true;
                        queue.remove(i);
                    }

                    case PlayerInput.SetBody body -> {
                        var direction = body.direction();
                        driver.setBodyYaw(direction.yaw);
                        driver.setBodyPitch(direction.pitch);
                        driver.setBodyRoll(direction.roll);
                        sawBody = true;
                    }

                    case PlayerInput.SetHead head -> {
                        var direction = head.direction();
                        driver.setHeadYaw(direction.yaw);
                        driver.setHeadPitch(direction.pitch);
                        driver.setHeadRoll(direction.roll);
                        sawHead = true;
                    }

                    default -> {
                    }
                }
            }

            if (lastClientVelocity != null) {
                driver.setMoveForward((float) lastClientVelocity.z);
                driver.setMoveStrafe((float) lastClientVelocity.x);
            }

            if (driver.isJumping()) {
                verticalInput = 1.0f;
            } else if (driver.isCrouching()) {
                verticalInput = -1.0f;
            }
            driver.setVerticalInput(verticalInput);
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return this.dependencies;
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return this.query;
        }
    }

    public static final class SyncSystem extends EntityTickingSystem<EntityStore> {

        private final ComponentType<EntityStore, FlyingDriverComponent> flyingDriverComponentType =
                FlyingDriverComponent.getComponentType();
        private final ComponentType<EntityStore, TransformComponent> transformComponentType =
                TransformComponent.getComponentType();
        private final ComponentType<EntityStore, Player> playerComponentType =
                Player.getComponentType();

        private final Query<EntityStore> query = Archetype.of(
                flyingDriverComponentType,
                transformComponentType,
                playerComponentType
        );

        private final Set<Dependency<EntityStore>> dependencies = Set.of(
                new SystemDependency<>(Order.AFTER, SeatFollowFlyerSystem.class)
        );

        @Override
        public void tick(float dt,
                         int index,
                         @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                         @Nonnull Store<EntityStore> store,
                         @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            FlyingDriverComponent driver = archetypeChunk.getComponent(index, flyingDriverComponentType);
            TransformComponent playerTransform = archetypeChunk.getComponent(index, transformComponentType);
            Player player = archetypeChunk.getComponent(index, playerComponentType);
            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            MovementManager movementManager = archetypeChunk.getComponent(index, MovementManager.getComponentType());
            MovementStatesComponent movementStatesComponent = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());

            if (driver == null || playerTransform == null || player == null || playerRef == null || movementManager == null || movementStatesComponent == null) {
                return;
            }

            Ref<EntityStore> seatRef = driver.getSeatRef();
            if (seatRef == null || !seatRef.isValid()) {
                return;
            }

            Ref<EntityStore> flyerRef = driver.getFlyerRef();
            if (flyerRef == null || !flyerRef.isValid()) {
                return;
            }

            TransformComponent seatTransform = store.getComponent(seatRef, transformComponentType);
            TransformComponent flyerTransform = store.getComponent(flyerRef, transformComponentType);
            NetworkId seatNetworkId = store.getComponent(seatRef, NetworkId.getComponentType());

            if (seatTransform == null || flyerTransform == null || seatNetworkId == null) {
                return;
            }

            Vector3d targetPos = seatTransform.getPosition().add(new Vector3d(0.0f, 1.0f, 0.0f)).clone();
            Vector3f targetRot = flyerTransform.getRotation().clone();
            Vector3d shadowOffset = new Vector3d(0.0, 0.0, 0.0).rotateY(targetRot.getYaw());
            targetPos.add(shadowOffset);

            Vector3f attachmentOffset = driver.getAttachmentOffset();
            if (attachmentOffset != null) {
                Vector3d rotatedOffset = new Vector3d(attachmentOffset.x, attachmentOffset.y, attachmentOffset.z)
                        .rotateY(targetRot.getYaw());
                targetPos.add(rotatedOffset);
            }

            playerTransform.setPosition(targetPos);
            playerTransform.setRotation(targetRot);
            playerTransform.markChunkDirty(store);

            boolean currentFlyState = movementManager.getSettings().canFly;
            if (!currentFlyState) {
                movementManager.getSettings().canFly = true;
                movementManager.update(playerRef.getPacketHandler());
            }

            MovementStates movementStates = movementStatesComponent.getMovementStates().clone();
            if (!movementStates.flying) {
                movementStates.flying = true;
                movementStatesComponent.setMovementStates(movementStates);
            }

            if (player.getReference() != null) {
                AnimationUtils.playAnimation(player.getReference(), AnimationSlot.Movement, null, "Sit", true, store);
            }

            ClientTeleport clientTeleport = new ClientTeleport(
                    (byte)playerRef.getUuid().hashCode(),
                    new ModelTransform(
                            PositionUtil.toPositionPacket(targetPos),
                            null,
                            null
                    ),
                    true
            );

            playerRef.getPacketHandler().writeNoCache(clientTeleport);

            if (player.getMountEntityId() != seatNetworkId.getId()) {
                player.setMountEntityId(seatNetworkId.getId());
            }
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return this.dependencies;
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return this.query;
        }
    }

    public static final class RemoveDriver extends RefSystem<EntityStore> {

        private final ComponentType<EntityStore, FlyingDriverComponent> flyingDriverComponentType = FlyingDriverComponent.getComponentType();

        @Override
        public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        }

        @Override
        public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            FlyingDriverComponent driver = commandBuffer.getComponent(ref, flyingDriverComponentType);
            if (driver != null) {
                handleFlyingMountedRemoval(ref, commandBuffer, driver);
            }

            resetFlyMountCamera(ref, store);
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return flyingDriverComponentType;
        }
    }

    public static final class RemoveSeat extends RefSystem<EntityStore> {

        private final ComponentType<EntityStore, FlyingSeatComponent> flyingSeatComponentType = FlyingSeatComponent.getComponentType();
        private final ComponentType<EntityStore, FlyingDriverComponent> flyingDriverComponentType = FlyingDriverComponent.getComponentType();

        @Override
        public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        }

        @Override
        public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            FlyingSeatComponent seat = commandBuffer.getComponent(ref, flyingSeatComponentType);
            if (seat == null) {
                return;
            }

            Ref<EntityStore> playerRef = seat.getPlayerRef();
            if (playerRef != null && playerRef.isValid()) {
                FlyingDriverComponent driver = commandBuffer.getComponent(playerRef, flyingDriverComponentType);
                if (driver != null) {
                    resetFlyMountCamera(playerRef, store);
                    commandBuffer.removeComponent(playerRef, flyingDriverComponentType);
                }
            }
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return flyingSeatComponentType;
        }
    }

    public static class AddNetworkIdToFlyingEntitySystem extends HolderSystem<EntityStore> {
        private final ComponentType<EntityStore, FlyingEntityComponent> myEntityComponentType = FlyingEntityComponent.getComponentType();
        private final ComponentType<EntityStore, NetworkId> networkIdComponentType = NetworkId.getComponentType();
        private final Query<EntityStore> query = Query.and(this.myEntityComponentType, Query.not(this.networkIdComponentType));

        @Override
        public void onEntityAdd(@Nonnull Holder<EntityStore> holder, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store) {
            if (!holder.getArchetype().contains(NetworkId.getComponentType())) {
                holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            }
        }

        @Override
        public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store) {}

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return query;
        }
    }

    public static final class RemovalSystem extends EntityTickingSystem<EntityStore> {

        private final ComponentType<EntityStore, FlyingSeatComponent> flyingSeatComponentType = FlyingSeatComponent.getComponentType();
        private final ComponentType<EntityStore, FlyingDriverComponent> flyingDriverComponentType = FlyingDriverComponent.getComponentType();

        private final Query<EntityStore> query = Archetype.of(flyingSeatComponentType);

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            FlyingSeatComponent seat = archetypeChunk.getComponent(index, flyingSeatComponentType);
            if (seat == null) {
                return;
            }

            Ref<EntityStore> seatRef = archetypeChunk.getReferenceTo(index);
            Ref<EntityStore> playerRef = seat.getPlayerRef();

            if (playerRef == null || !playerRef.isValid()) {
                return;
            }

            FlyingDriverComponent driver = store.getComponent(playerRef, flyingDriverComponentType);
            if (driver == null) {
                seat.setPlayerRef(null);
                return;
            }


            Ref<EntityStore> driverSeatRef = driver.getSeatRef();
            Ref<EntityStore> driverFlyerRef = driver.getFlyerRef();
            Ref<EntityStore> seatFlyerRef = seat.getFlyerRef();

            if (!seatRef.equals(driverSeatRef) || seatFlyerRef == null || !seatFlyerRef.equals(driverFlyerRef)) {
                seat.setPlayerRef(null);
                resetFlyMountCamera(playerRef, store);
                commandBuffer.removeComponent(playerRef, flyingDriverComponentType);
            }
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return query;
        }
    }

    public static class InitialScaleUpSystem extends TickingSystem<EntityStore> {

        private final ComponentType<EntityStore, FlyingEntityComponent> flyingEntityComponent;
        private final ComponentType<EntityStore, ModelComponent> modelComponent;

        public InitialScaleUpSystem() {
            this.flyingEntityComponent = FlyingEntityComponent.getComponentType();
            this.modelComponent = ModelComponent.getComponentType();
        }

        @Nonnull
        public Query<EntityStore> getQuery() {
            return Query.and(
                    FlyingEntityComponent.getComponentType(),
                    ModelComponent.getComponentType()
            );
        }

        @Override
        public void tick(float dt, int tickCount, @Nonnull Store<EntityStore> store) {
            store.forEachChunk(this.getQuery(), ((chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    Ref<EntityStore> entityRef = chunk.getReferenceTo(i);
                    FlyingEntityComponent flyingEntity = chunk.getComponent(i, this.flyingEntityComponent);
                    if (flyingEntity != null && flyingEntity.getModelScale() < FlyingEntityComponent.DEFAULT_MODEL_SCALE && !flyingEntity.isReadyToFly()) {
                        ModelComponent modelComponent = chunk.getComponent(i, this.modelComponent);

                        float currentScale = flyingEntity.getModelScale();

                        if (modelComponent != null) {
                            ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelComponent.getModel().getModelAssetId());
                            float newScale = Math.clamp(currentScale + dt * 0.27f, 0.0f, FlyingEntityComponent.DEFAULT_MODEL_SCALE);
                            flyingEntity.setModelScale(newScale);
                            if (modelAsset != null) {
                                if (newScale >= FlyingEntityComponent.DEFAULT_MODEL_SCALE) {
                                    flyingEntity.setModelScale(FlyingEntityComponent.DEFAULT_MODEL_SCALE);
                                    flyingEntity.setReadyToFly(true);
                                    Model updatedModel = Model.createScaledModel(modelAsset, FlyingEntityComponent.DEFAULT_MODEL_SCALE);
                                    commandBuffer.putComponent(entityRef, this.modelComponent, new ModelComponent(updatedModel));
                                    commandBuffer.putComponent(entityRef, this.flyingEntityComponent, flyingEntity);
                                } else {
                                    Model updatedModel = Model.createScaledModel(modelAsset, newScale);
                                    commandBuffer.putComponent(entityRef, this.modelComponent, new ModelComponent(updatedModel));
                                }
                            }
                        }
                    }
                }
            }));
        }
    }
}