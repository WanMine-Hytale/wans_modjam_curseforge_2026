package net.wanmine.Modjam;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.Modjam.commands.AirshipsCommandCollection;
import net.wanmine.Modjam.entities.components.*;
import net.wanmine.Modjam.entities.components.attachments.FlyingSeatComponent;
import net.wanmine.Modjam.entities.systems.FlyMountInteractionsSystems;
import net.wanmine.Modjam.entities.systems.FlyMountMovementSystem;
import net.wanmine.Modjam.entities.systems.FlyMountSystems;


import javax.annotation.Nonnull;

public class ModjamPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static ComponentType<EntityStore, FlyingEntityComponent> flyingEntityComponent;
    public static ComponentType<EntityStore, FlyingSeatComponent> flyingSeatComponent;
    public static ComponentType<EntityStore, FlyingDriverComponent> flyingDriverComponent;

    private static ModjamPlugin instance;

    public ModjamPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Plugin Loaded: %s (version: %s)", this.getName(), this.getManifest().getVersion().toString());
    }

    private PacketFilter inboundFilter;

    public static ModjamPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up %s", this.getName());

        flyingSeatComponent = this.getEntityStoreRegistry().registerComponent(FlyingSeatComponent.class, "FlyingSeat", FlyingSeatComponent.CODEC);
        flyingDriverComponent = this.getEntityStoreRegistry().registerComponent(FlyingDriverComponent.class, "FlyingDriver", FlyingDriverComponent.CODEC);
        flyingEntityComponent = this.getEntityStoreRegistry().registerComponent(FlyingEntityComponent.class, "FlyingEntity", FlyingEntityComponent.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("FlyMountInteraction", FlyMountInteractionsSystems.MountInteraction.class, FlyMountInteractionsSystems.MountInteraction.CODEC);

        this.getCommandRegistry().registerCommand(new AirshipsCommandCollection("airships", "Airships command"));

        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.InputSystem());
        this.getEntityStoreRegistry().registerSystem(new FlyMountMovementSystem());
        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.ValidationSystem());
        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.SeatFollowFlyerSystem());
        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.PlayerMount());
        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.SyncSystem());
        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.RemovalSystem());
        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.RemoveDriver());
        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.RemoveSeat());
        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.AddNetworkIdToFlyingEntitySystem());

        inboundFilter = PacketAdapters.registerInbound(new FlyMountInteractionsSystems.DismountPacketWatcher());
    }


    @Override
    protected void start() {
        LOGGER.atInfo().log("Started %s", this.getName());
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutdown %s", this.getName());
        if (inboundFilter != null) {
            PacketAdapters.deregisterInbound(inboundFilter);
        }
    }
}