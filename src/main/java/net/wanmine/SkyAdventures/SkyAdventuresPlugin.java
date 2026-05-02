package net.wanmine.SkyAdventures;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.SkyAdventures.commands.AirshipsCommandCollection;
import net.wanmine.SkyAdventures.entities.components.*;
import net.wanmine.SkyAdventures.entities.components.attachments.FlyingSeatComponent;
import net.wanmine.SkyAdventures.entities.components.VorthraxDayNightComponent;
import net.wanmine.SkyAdventures.entities.systems.FlyMountInteractionsSystems;
import net.wanmine.SkyAdventures.entities.systems.FlyMountMovementSystem;
import net.wanmine.SkyAdventures.entities.systems.FlyMountSystems;
import net.wanmine.SkyAdventures.entities.systems.VorthraxDayNightSystem;

import com.hypixel.hytale.server.npc.entities.NPCEntity;
import net.wanmine.SkyAdventures.entities.managers.BossBarManager;
import net.wanmine.SkyAdventures.entities.systems.PlayerRangeTickSystem;
import net.wanmine.SkyAdventures.entities.systems.VorthraxDamageSystem;
import net.wanmine.SkyAdventures.entities.systems.VorthraxTrackerSystem;

import javax.annotation.Nonnull;

public class SkyAdventuresPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static ComponentType<EntityStore, FlyingEntityComponent> flyingEntityComponent;
    public static ComponentType<EntityStore, FlyingSeatComponent> flyingSeatComponent;
    public static ComponentType<EntityStore, FlyingDriverComponent> flyingDriverComponent;
    public static ComponentType<EntityStore, VorthraxDayNightComponent> vorthraxDayNightComponent;

    private static SkyAdventuresPlugin instance;
    private BossBarManager bossBarManager;

    public SkyAdventuresPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Plugin Loaded: %s (version: %s)", this.getName(), this.getManifest().getVersion().toString());
    }

    private PacketFilter dismountInboundFilter;
    private PacketFilter keybindsInboundFilter;

    public BossBarManager getBossBarManager() { return bossBarManager; }
    public static SkyAdventuresPlugin getInstance() { return instance; }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up %s", this.getName());

        flyingSeatComponent = this.getEntityStoreRegistry().registerComponent(FlyingSeatComponent.class, "FlyingSeat", FlyingSeatComponent.CODEC);
        flyingDriverComponent = this.getEntityStoreRegistry().registerComponent(FlyingDriverComponent.class, "FlyingDriver", FlyingDriverComponent.CODEC);
        flyingEntityComponent = this.getEntityStoreRegistry().registerComponent(FlyingEntityComponent.class, "FlyingEntity", FlyingEntityComponent.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("FlyMountInteraction", FlyMountInteractionsSystems.MountInteraction.class, FlyMountInteractionsSystems.MountInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("AirshipCrateInteraction", FlyMountInteractionsSystems.AirshipCrateInteraction.class, FlyMountInteractionsSystems.AirshipCrateInteraction.CODEC);

        vorthraxDayNightComponent = this.getEntityStoreRegistry().registerComponent(
                VorthraxDayNightComponent.class,
                "VorthraxDayNight",
                VorthraxDayNightComponent.CODEC
        );
        VorthraxDayNightComponent.TYPE = vorthraxDayNightComponent;

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
        this.getEntityStoreRegistry().registerSystem(new FlyMountSystems.InitialScaleUpSystem());

        dismountInboundFilter = PacketAdapters.registerInbound(new FlyMountInteractionsSystems.DismountPacketWatcher());
        keybindsInboundFilter = PacketAdapters.registerInbound(new FlyMountInteractionsSystems.AirshipKeybindsHandler());

        if (NPCEntity.getComponentType() == null) {
            LOGGER.atWarning().log("MODERROR: NPCEntity component type not found!");
            return;
        }

        bossBarManager = new BossBarManager();

        getEntityStoreRegistry().registerSystem(new VorthraxTrackerSystem(bossBarManager));
        getEntityStoreRegistry().registerSystem(new VorthraxDamageSystem(bossBarManager));
        getEntityStoreRegistry().registerSystem(new PlayerRangeTickSystem(bossBarManager));
        getEntityStoreRegistry().registerSystem(new VorthraxDayNightSystem());
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started %s", this.getName());
    }

    @Override
    protected void shutdown() {
        if (bossBarManager != null) {
            bossBarManager.clearAll();
        }
        LOGGER.atInfo().log("Shutdown %s", this.getName());
        if (dismountInboundFilter != null) {
            PacketAdapters.deregisterInbound(dismountInboundFilter);
        }
        if (keybindsInboundFilter != null) {
            PacketAdapters.deregisterInbound(keybindsInboundFilter);
        }
    }
}