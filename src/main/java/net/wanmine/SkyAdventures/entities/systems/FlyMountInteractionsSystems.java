package net.wanmine.SkyAdventures.entities.systems;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.interaction.CancelInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;
import net.wanmine.SkyAdventures.entities.components.FlyingDriverComponent;
import net.wanmine.SkyAdventures.entities.components.FlyingEntityComponent;
import net.wanmine.SkyAdventures.utils.AirshipFactory;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlyMountInteractionsSystems {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static class MountInteraction extends SimpleInstantInteraction {

        public MountInteraction() {
            LOGGER.atInfo().log("MountInteraction Registered");
        }

        @Override
        protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldownHandler) {
            Ref<EntityStore> playerRef = ctx.getEntity();
            Ref<EntityStore> mountRef = ctx.getTargetEntity();

            LOGGER.atInfo().log("MountInteraction triggered | player=%s target=%s", playerRef, mountRef);

            if (mountRef == null) {
                LOGGER.atWarning().log("No target entity, interaction failed");
                ctx.getState().state = InteractionState.Failed;
                return;
            }

            CommandBuffer<EntityStore> commandBuffer = ctx.getCommandBuffer();
            if (commandBuffer == null) {
                LOGGER.atWarning().log("No CommandBuffer available");
                return;
            }

            Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
            PlayerRef playerRefComponent = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
            TransformComponent mountTransform = commandBuffer.getComponent(mountRef, TransformComponent.getComponentType());

            MovementStatesComponent movementStates = commandBuffer.getComponent(playerRef, MovementStatesComponent.getComponentType());

            if (player == null) { LOGGER.atWarning().log("No Player component on playerRef=%s", playerRef); return; }
            if (playerRefComponent == null) { LOGGER.atWarning().log("No PlayerRef component on playerRef=%s", playerRef); return; }
            if (mountTransform == null) { LOGGER.atWarning().log("No TransformComponent on mountRef=%s", mountRef); return; }
            if (movementStates == null) { LOGGER.atWarning().log("No MovementStatesComponent on mountRef=%s", mountRef); return; }



            World world = player.getWorld();
            if (world == null) {
                LOGGER.atWarning().log("Player has no world");
                return;
            }

            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();

                FlyingEntityComponent flyingEntity = store.getComponent(mountRef, FlyingEntityComponent.getComponentType());
                if (flyingEntity == null) {
                    LOGGER.atWarning().log("No FlyingEntityComponent on mountRef=%s", mountRef);
                    return;
                }

                if (!flyingEntity.isReadyToFly()) {
                    NotificationUtil.sendNotification(playerRefComponent.getPacketHandler(), Message.raw("Airship"), Message.raw("Airship still not ready!"), NotificationStyle.Warning);
                    return;
                }

                Ref<EntityStore> currentDriver = flyingEntity.getDriver();
                boolean isDriver = playerRef.equals(currentDriver);

                if (movementStates.getMovementStates().crouching && !isDriver) {
                    AirshipFactory.destroyMount(store, mountRef);
                    Vector3i position = mountTransform.getPosition().toVector3i();

                    Vector3d groundHit = TargetUtil.getTargetLocation(
                            world,
                            blockId -> {
                                if (blockId == com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.EMPTY_ID) return false;
                                com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType type = BlockType.getAssetMap().getAsset(blockId);
                                return type != null && type.getOpacity() != Opacity.Transparent;
                            },
                            position.x, position.y, position.z,
                            0, -1, 0,
                            256.0
                    );
                    if (groundHit != null) {
                        world.setBlock((int) groundHit.x, (int) groundHit.y, (int) groundHit.z, "Airship_Crate");
                    } else {
                        world.setBlock(position.x, position.y, position.z, "Airship_Crate");
                    }

                } else {
                    if (isDriver) {
                        NotificationUtil.sendNotification(playerRefComponent.getPacketHandler(), Message.raw("Airship"), Message.raw("Airship has already a driver!"), NotificationStyle.Warning);
                    } else {
                        if (flyingEntity.hasDriver()) {
                            LOGGER.atInfo().log("Mount=%s already occupied by driver=%s, ignoring", mountRef, currentDriver);
                            return;
                        }
                        Vector3d mountPos = mountTransform.getPosition().clone();
                        Vector3f mountRot = mountTransform.getRotation().clone();
                        AirshipFactory.mountPlayer(store, mountRef, playerRef, mountPos, mountRot);
                    }
                }


            });
        }

        public static final BuilderCodec<FlyMountInteractionsSystems.MountInteraction> CODEC = BuilderCodec.builder(
                FlyMountInteractionsSystems.MountInteraction.class, FlyMountInteractionsSystems.MountInteraction::new, SimpleInstantInteraction.CODEC
        ).build();
    }

    public static class DismountPacketWatcher implements PacketWatcher {
        @Override
        public void accept(PacketHandler packetHandler, Packet packet) {
            if (packet.getId() != 290) {
                return;
            }
            SyncInteractionChains interactionChains = (SyncInteractionChains) packet;
            SyncInteractionChain[] updates = interactionChains.updates;

            for (SyncInteractionChain item : updates) {
                PlayerAuthentication playerAuthentication = packetHandler.getAuth();
                if (playerAuthentication != null) {
                    UUID uuid = playerAuthentication.getUuid();
                    PlayerRef player = Universe.get().getPlayer(uuid);
                    InteractionType interactionType = item.interactionType;
                    World currentWorld = Universe.get().getWorld(player.getWorldUuid());
                    if (currentWorld == null) return;
                    Store<EntityStore> store = currentWorld.getEntityStore().getStore();
                    if(interactionType == InteractionType.Use){
                        currentWorld.execute(() -> {
                            FlyingDriverComponent driver = store.getComponent(player.getReference(), FlyingDriverComponent.getComponentType());
                            if (driver == null) return;
                            AirshipFactory.dismountPlayer(store, driver.getFlyerRef(), player.getReference());
                        });
                        return;
                    }
                }
            }
        }
    }


    public static class AirshipKeybindsHandler implements PlayerPacketFilter {

        @FunctionalInterface
        interface HotbarAction {
            void execute(PlayerRef playerRef, Ref<EntityStore> entityRef, Store<EntityStore> store, FlyingDriverComponent driver);
        }

        private static final Map<Integer, HotbarAction> ACTIONS = Map.of(
                8, (_, entityRef,store, driver) -> {
                    if (driver.getActiveCamera() == 0) {
                        FlyMountSystems.resetFlyMountCamera(entityRef, store);
                        driver.setActiveCamera(1);
                    } else {
                        FlyMountSystems.applyFlyMountCamera(entityRef, store);
                        driver.setActiveCamera(0);
                    }
                }
        );

        private void handleKeyTrigger(PlayerRef playerRef, Ref<EntityStore> entityRef, int originalSlot, int chainId, ForkedChainId forkedId, HotbarAction action) {
            Store<EntityStore> store = entityRef.getStore();
            store.getExternalData().getWorld().execute(() -> {
                Player playerComponent = store.getComponent(entityRef, Player.getComponentType());
                InventoryComponent.Hotbar playerHotbar = store.getComponent(entityRef, InventoryComponent.Hotbar.getComponentType());
                FlyingDriverComponent driver = store.getComponent(entityRef, FlyingDriverComponent.getComponentType());

                if (playerComponent == null || playerHotbar == null || driver == null) return;

                playerRef.getPacketHandler().writeNoCache(new CancelInteractionChain(chainId, forkedId));
                playerHotbar.setActiveSlot((byte) originalSlot);
                playerRef.getPacketHandler().writeNoCache(new SetActiveSlot(
                        InventoryComponent.HOTBAR_SECTION_ID, originalSlot
                ));

                action.execute(playerRef, entityRef, store, driver);
            });
        }

        @Override
        public boolean test(PlayerRef playerRef, Packet packet) {
            if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

            SyncInteractionChain abilityChain = null;
            HotbarAction action = null;
            List<SyncInteractionChain> keep = new ArrayList<>();

            for (SyncInteractionChain chain : syncPacket.updates) {
                HotbarAction candidate = ACTIONS.get(chain.data != null ? chain.data.targetSlot : -1);
                if (candidate != null
                        && chain.interactionType == InteractionType.SwapFrom
                        && chain.initial
                        && abilityChain == null) {
                    abilityChain = chain;
                    action = candidate;
                } else {
                    keep.add(chain);
                }
            }

            if (abilityChain == null) return false;

            final SyncInteractionChain finalChain = abilityChain;
            final HotbarAction finalAction = action;

            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null || !entityRef.isValid()) return false;

            Store<EntityStore> store = entityRef.getStore();
            World world = store.getExternalData().getWorld();

            world.execute(() -> {
                FlyingDriverComponent driver = store.getComponent(entityRef, FlyingDriverComponent.getComponentType());
                if (driver == null) return;

                handleKeyTrigger(playerRef, entityRef, finalChain.activeHotbarSlot, finalChain.chainId, finalChain.forkedId, finalAction);
            });

            if (!keep.isEmpty()) {
                syncPacket.updates = keep.toArray(new SyncInteractionChain[0]);
            }

            return false;
        }
    }


    public static class AirshipCrateInteraction extends SimpleBlockInteraction {

        public static final BuilderCodec<FlyMountInteractionsSystems.AirshipCrateInteraction> CODEC = BuilderCodec.builder(
                FlyMountInteractionsSystems.AirshipCrateInteraction.class, FlyMountInteractionsSystems.AirshipCrateInteraction::new, SimpleBlockInteraction.CODEC
        ).build();


        public AirshipCrateInteraction() {
            LOGGER.atInfo().log("AirshipCrateInteraction Registered");
        }

        @Override
        protected void interactWithBlock(@NonNullDecl World world, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext ctx, @Nullable ItemStack itemStack, @Nonnull Vector3i vector3i, @Nonnull CooldownHandler cooldownHandler) {
            if(interactionType != InteractionType.Use)
                return;

            Ref<EntityStore> targetRef = ctx.getTargetEntity();
            BlockPosition targetBlock = ctx.getTargetBlock();


            Ref<EntityStore> ref = ctx.getEntity();
            Store<EntityStore> store = ref.getStore();
            Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
            if(playerComponent == null)
                return;

            Ref<EntityStore> playerEntityRef = playerComponent.getReference();
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

            world.execute(() -> {
                store.tryRemoveComponent(playerEntityRef, FlyingDriverComponent.getComponentType());

                if (store.getComponent(playerEntityRef, FlyingDriverComponent.getComponentType()) != null) {
                    LOGGER.atWarning().log("Player already has FlyingDriverComponent!");
                    return;
                }

                Vector3f baseRot = playerTransform.getRotation().clone();

                Vector3d cratePosition = new Vector3d(targetBlock.x, targetBlock.y, targetBlock.z);

                AirshipFactory.spawnMount(store, cratePosition, baseRot, 0.1f);
            });

        }
        @Override
        protected void simulateInteractWithBlock(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl World world, @NonNullDecl Vector3i vector3i) {

        }

    }
}
