package net.wanmine.Modjam;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import net.wanmine.Modjam.commands.ExampleCommand;

import javax.annotation.Nonnull;

public class ModjamPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ModjamPlugin instance;

    public ModjamPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Plugin Loaded: %s (version: %s)", this.getName(), this.getManifest().getVersion().toString());
    }

    public static ModjamPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up %s", this.getName());
        this.getCommandRegistry().registerCommand(new ExampleCommand("test", "test command"));
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started %s", this.getName());
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutdown %s", this.getName());
    }
}