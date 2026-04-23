package net.wanmine.Modjam.entities.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public abstract class FlyingAttachedComponent<T extends FlyingAttachedComponent<T>> implements Component<EntityStore> {

    protected Ref<EntityStore> flyerRef;
    protected Ref<EntityStore> playerRef;
    protected Vector3d localOffset;
    public AttachmentType attachmentType = AttachmentType.NONE;

    public FlyingAttachedComponent() {}

    public FlyingAttachedComponent(Ref<EntityStore> flyerRef) {
        this.flyerRef = flyerRef;
    }

    public FlyingAttachedComponent(Ref<EntityStore> flyerRef, Vector3d localOffset) {
        this.flyerRef = flyerRef;
        this.localOffset = localOffset != null ? localOffset : new Vector3d(0.0, 0.0, 0.0);
    }

    public T withPlayerRef(Ref<EntityStore> playerRef) {
        this.playerRef = playerRef;
        return self();
    }

    public T withLocalOffset(Vector3d localOffset) {
        this.localOffset = localOffset != null ? localOffset : new Vector3d(0.0, 0.0, 0.0);
        return self();
    }

    public Vector3d getLocalOffset() {
        return localOffset;
    }

    public void setLocalOffset(Vector3d localOffset) {
        this.localOffset = localOffset != null ? localOffset : new Vector3d(0.0, 0.0, 0.0);
    }

    @Nullable
    @Override
    public abstract Component<EntityStore> clone();

    protected abstract T self();

    public abstract ComponentType<EntityStore, T> getType();

    public Ref<EntityStore> getFlyerRef() { return flyerRef; }
    public void setFlyerRef(Ref<EntityStore> flyerRef) { this.flyerRef = flyerRef; }

    public Ref<EntityStore> getPlayerRef() { return playerRef; }
    public void setPlayerRef(Ref<EntityStore> playerRef) { this.playerRef = playerRef; }
}