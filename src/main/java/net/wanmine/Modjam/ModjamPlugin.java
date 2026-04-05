package net.wanmine.Modjam;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import net.wanmine.Modjam.commands.ExampleCommand;
import net.wanmine.Modjam.managers.BossBarManager;
import net.wanmine.Modjam.systems.PlayerRangeTickSystem;
import net.wanmine.Modjam.systems.VorthraxDamageSystem;
import net.wanmine.Modjam.systems.VorthraxTrackerSystem;

import javax.annotation.Nonnull;

public class ModjamPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ModjamPlugin instance;
    private BossBarManager bossBarManager;

    public ModjamPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Plugin Loaded: %s (version: %s)", this.getName(), this.getManifest().getVersion().toString());
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public static ModjamPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up %s", this.getName());
        this.getCommandRegistry().registerCommand(new ExampleCommand("test", "test command"));

        if (NPCEntity.getComponentType() == null) {
            LOGGER.atWarning().log("MODERROR: NPCEntity component type not found!");
            return;
        }

        bossBarManager = new BossBarManager();

        getEntityStoreRegistry().registerSystem(new VorthraxTrackerSystem(bossBarManager));
        getEntityStoreRegistry().registerSystem(new VorthraxDamageSystem(bossBarManager));
        getEntityStoreRegistry().registerSystem(new PlayerRangeTickSystem(bossBarManager));
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
    }
}