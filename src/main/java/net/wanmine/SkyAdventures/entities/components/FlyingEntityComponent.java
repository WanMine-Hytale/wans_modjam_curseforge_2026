package net.wanmine.SkyAdventures.entities.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.SkyAdventures.SkyAdventuresPlugin;
import net.wanmine.SkyAdventures.entities.components.attachments.FlyingSeatComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FlyingEntityComponent implements Component<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private Map<UUID, Ref<EntityStore>> attachments = new ConcurrentHashMap<>();
    private final Map<UUID, Ref<EntityStore>> seats = new ConcurrentHashMap<>();



    public static final float DEFAULT_MODEL_SCALE = 2.0f;
    private float modelScale = 2.0f;
    public float getModelScale() {
        return modelScale;
    }
    public void setModelScale(float newScale) {
        this.modelScale = newScale;
    }

    private boolean readyToFly = false;
    public boolean isReadyToFly() {
        return readyToFly;
    }
    public void setReadyToFly(boolean newState) {
        this.readyToFly = newState;
    }


    public String lastAnimation = "";
    public String lastWheelAnimation = "";

    private Ref<EntityStore> driver = null;
    private Ref<EntityStore> driverSeat = null;

    public double currentSpeed = 0.0;
    public double ySpeed = 0.0;
    public double strafeSpeed = 0.0;
    public double hoverTargetY = Double.NaN;
    public float smoothVisualPitch = 0.0f;
    public float currentTurnSpeed = 0.0f;
    public Vector3d velocity = new Vector3d(0.0, 0.0, 0.0);


    //region CODEC
    @Nonnull
    public static final BuilderCodec<FlyingEntityComponent> CODEC = BuilderCodec
            .builder(FlyingEntityComponent.class, FlyingEntityComponent::new)
            .append(
                    new KeyedCodec<>("IsReadyToFly", BuilderCodec.BOOLEAN),
                    (comp, value) -> comp.readyToFly = value,
                    comp -> comp.readyToFly
            ).add()
            .build();
    //endregion

    public Map<UUID, Ref<EntityStore>> getAttachments() {
        return Collections.unmodifiableMap(attachments);
    }

    public void setAttachments(Map<UUID, Ref<EntityStore>> attachments) {
        this.attachments = attachments != null
                ? new ConcurrentHashMap<>(attachments)
                : new ConcurrentHashMap<>();
    }

    public Map<UUID, Ref<EntityStore>> getSeats() {
        return Collections.unmodifiableMap(seats);
    }

    public void setSeats(Map<UUID, Ref<EntityStore>> newSeats) {
        seats.clear();
        if (newSeats != null) {
            seats.putAll(newSeats);
        }
    }

    public <T extends FlyingAttachedComponent<T>> boolean addAttachment(
            UUID uuid,
            Ref<EntityStore> entity,
            Ref<EntityStore> flyerRef,
            Store<EntityStore> store,
            ComponentType<EntityStore, T> componentType) {

        if (uuid == null || entity == null || flyerRef == null || componentType == null) {
            LOGGER.atWarning().log("[addAttachment] FAILED - null argument");
            return false;
        }

        if (!entity.isValid() || !flyerRef.isValid()) {
            LOGGER.atWarning().log("[addAttachment] FAILED - invalid ref entity=%b flyer=%b",
                    entity.isValid(), flyerRef.isValid());
            return false;
        }

        T attachment = store.getComponent(entity, componentType);
        if (attachment == null) {
            LOGGER.atWarning().log("[addAttachment] FAILED - attachment component missing");
            return false;
        }

        if (attachments.containsKey(uuid)) {
            LOGGER.atWarning().log("[addAttachment] FAILED - uuid already exists %s", uuid);
            return false;
        }

        attachment.setFlyerRef(flyerRef);
        attachments.put(uuid, entity);

        if (attachment.attachmentType == AttachmentType.SEAT) {
            seats.put(uuid, entity);
        }

        return true;
    }

    public boolean addSeat(
            UUID uuid,
            Ref<EntityStore> seatEntity,
            Ref<EntityStore> flyerRef,
            Store<EntityStore> store) {
        return addAttachment(uuid, seatEntity, flyerRef, store, FlyingSeatComponent.getComponentType());
    }

    public boolean removeAttachment(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        Ref<EntityStore> removedAttachment = attachments.remove(uuid);
        Ref<EntityStore> removedSeat = seats.remove(uuid);

        if (driverSeat != null) {
            if ((driverSeat.equals(removedAttachment))
                    || (driverSeat.equals(removedSeat))) {
                driverSeat = null;
            }
        }

        return removedAttachment != null || removedSeat != null;
    }

    public void setDriver(Ref<EntityStore> driverRef) {
        this.driver = driverRef;
    }

    @Nullable
    public Ref<EntityStore> getDriver() {
        return driver;
    }

    public boolean hasDriver() {
        return driver != null && driver.isValid();
    }

    public void clearDriver() {
        this.driver = null;
        this.driverSeat = null;
    }

    public void setDriverSeat(Ref<EntityStore> driverSeatRef) {
        this.driverSeat = driverSeatRef;
    }

    public boolean isDriverSeat(Ref<EntityStore> ref) {
        return driverSeat != null && driverSeat.equals(ref);
    }

    @Nullable
    public Ref<EntityStore> getDriverSeat() {
        return this.driverSeat;
    }

    public boolean hasDriverSeat() {
        return driverSeat != null && driverSeat.isValid();
    }

    public Map<UUID, Ref<EntityStore>> getPassengers() {
        if (driverSeat == null) {
            return Collections.unmodifiableMap(seats);
        }

        return seats.entrySet().stream()
                .filter(e -> !driverSeat.equals(e.getValue()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        FlyingEntityComponent cloned = new FlyingEntityComponent();
        cloned.currentSpeed = this.currentSpeed;
        cloned.ySpeed = this.ySpeed;
        cloned.strafeSpeed = this.strafeSpeed;
        cloned.hoverTargetY = this.hoverTargetY;
        cloned.attachments = new ConcurrentHashMap<>(this.attachments);
        cloned.seats.putAll(this.seats);
        cloned.driver = this.driver;
        cloned.driverSeat = this.driverSeat;
        cloned.readyToFly = this.readyToFly;
        return cloned;
    }

    public static ComponentType<EntityStore, FlyingEntityComponent> getComponentType() {
        return SkyAdventuresPlugin.flyingEntityComponent;
    }
}
