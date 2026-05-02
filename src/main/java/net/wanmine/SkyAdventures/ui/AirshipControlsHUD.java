package net.wanmine.SkyAdventures.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class AirshipControlsHUD extends CustomUIHud {

    private static final String LAYOUT = "Airship/AirshipControls.ui";

    public AirshipControlsHUD(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append(LAYOUT);
    }
}
