package net.wanmine.Modjam.entities.systems;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.Modjam.entities.components.FlyingDriverComponent;
import net.wanmine.Modjam.entities.components.FlyingEntityComponent;
import net.wanmine.Modjam.utils.AirshipFactory;

import javax.annotation.Nonnull;
import java.util.UUID;

public class FlyMountInteractionsSystems {

    public static class MountInteraction extends SimpleInstantInteraction {
        private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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

            if (player == null) { LOGGER.atWarning().log("No Player component on playerRef=%s", playerRef); return; }
            if (playerRefComponent == null) { LOGGER.atWarning().log("No PlayerRef component on playerRef=%s", playerRef); return; }
            if (mountTransform == null) { LOGGER.atWarning().log("No TransformComponent on mountRef=%s", mountRef); return; }

            World world = player.getWorld();
            if (world == null) {
                LOGGER.atWarning().log("Player has no world");
                return;
            }

            LOGGER.atInfo().log("Scheduling mount/dismount on world thread | player=%s mount=%s", playerRef, mountRef);

            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();

                FlyingEntityComponent flyingEntity = store.getComponent(mountRef, FlyingEntityComponent.getComponentType());
                if (flyingEntity == null) {
                    LOGGER.atWarning().log("No FlyingEntityComponent on mountRef=%s", mountRef);
                    return;
                }

                Ref<EntityStore> currentDriver = flyingEntity.getDriver();
                boolean isDriver = playerRef.equals(currentDriver);

                LOGGER.atInfo().log("Mount state | hasDriver=%s currentDriver=%s isDriver=%s",
                        flyingEntity.hasDriver(), currentDriver, isDriver);
                if (isDriver) {
                    LOGGER.atInfo().log("Already the driver!");
                } else {
                    if (flyingEntity.hasDriver()) {
                        LOGGER.atInfo().log("Mount=%s already occupied by driver=%s, ignoring", mountRef, currentDriver);
                        return;
                    }

                    Vector3d mountPos = mountTransform.getPosition().clone();
                    Vector3f mountRot = mountTransform.getRotation().clone();

                    LOGGER.atInfo().log("Mounting player=%s on mount=%s | pos=%s rot=%s", playerRef, mountRef, mountPos, mountRot);
                    AirshipFactory.mountPlayer(store, mountRef, playerRef, mountPos, mountRot);
                    LOGGER.atInfo().log("Mount complete");
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
}
