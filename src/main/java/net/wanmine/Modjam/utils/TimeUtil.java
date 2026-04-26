package net.wanmine.Modjam.utils;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.time.ZoneOffset;

public final class TimeUtil {

    private static final float DAY_START_HOUR   = 4.8f;
    private static final float NIGHT_START_HOUR = 19.2f;

    private TimeUtil() {}

    public static boolean isDayTime(@Nonnull Store<EntityStore> store) {
        var wtr = store.getResource(WorldTimeResource.getResourceType());
        var zonedDateTime = wtr.getGameTime().atZone(ZoneOffset.UTC);
        float currentTime = zonedDateTime.getHour()
                + (zonedDateTime.getMinute() / 60.0f)
                + (zonedDateTime.getSecond() / 3600.0f);
        return currentTime >= DAY_START_HOUR && currentTime < NIGHT_START_HOUR;
    }
}