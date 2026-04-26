package net.wanmine.Modjam.entities.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class VorthraxDayNightComponent implements Component<EntityStore> {
    public static ComponentType<EntityStore, VorthraxDayNightComponent> TYPE;

    public static final BuilderCodec<VorthraxDayNightComponent> CODEC =
            BuilderCodec.builder(VorthraxDayNightComponent.class, VorthraxDayNightComponent::new)
                    .build();

    public boolean isDay = false;

    public VorthraxDayNightComponent() {}

    @Override
    public Component<EntityStore> clone() {
        VorthraxDayNightComponent clone = new VorthraxDayNightComponent();
        clone.isDay = this.isDay;
        return clone;
    }
}