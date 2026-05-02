package net.wanmine.SkyAdventures.entities.components.attachments;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.SkyAdventures.SkyAdventuresPlugin;
import net.wanmine.SkyAdventures.entities.components.AttachmentType;
import net.wanmine.SkyAdventures.entities.components.FlyingAttachedComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FlyingSeatComponent extends FlyingAttachedComponent<FlyingSeatComponent> {

    public FlyingSeatComponent() {
        this.attachmentType = AttachmentType.SEAT;
        this.localOffset = new Vector3d(0.0, 1.5, 0.0);
    }

    public FlyingSeatComponent(Ref<EntityStore> flyerRef) {
        super(flyerRef);
        this.attachmentType = AttachmentType.SEAT;
        this.localOffset = new Vector3d(0.0, 1.5, 0.0);
    }

    public FlyingSeatComponent(Ref<EntityStore> flyerRef, Vector3d localOffset) {
        super(flyerRef, localOffset);
        this.attachmentType = AttachmentType.SEAT;
    }

    @Override
    protected FlyingSeatComponent self() {
        return this;
    }

    @Override
    public ComponentType<EntityStore, FlyingSeatComponent> getType() {
        return SkyAdventuresPlugin.flyingSeatComponent;
    }

    public static ComponentType<EntityStore, FlyingSeatComponent> getComponentType() {
        return SkyAdventuresPlugin.flyingSeatComponent;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        FlyingSeatComponent cloned = new FlyingSeatComponent();
        cloned.flyerRef = this.flyerRef;
        cloned.playerRef = this.playerRef;
        cloned.attachmentType = this.attachmentType;
        cloned.localOffset = this.localOffset != null
                ? this.localOffset.clone()
                : new Vector3d(0.0, 1.5, 0.0);
        return cloned;
    }

    //region CODEC
    @Nonnull
    public static final BuilderCodec<FlyingSeatComponent> CODEC = BuilderCodec
            .builder(FlyingSeatComponent.class, FlyingSeatComponent::new)
            .build();
    //endregion
}