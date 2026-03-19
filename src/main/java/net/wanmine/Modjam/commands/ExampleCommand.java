package net.wanmine.Modjam.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;

public class ExampleCommand extends AbstractPlayerCommand {

    public ExampleCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
    }
    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        var packetHandler = playerRef.getPacketHandler();
        var title = Message.raw("Example Command").color("#ffc86e").bold(true);
        var description = Message.raw("Hello world!");
        NotificationUtil.sendNotification(packetHandler, title, description);
    }
}