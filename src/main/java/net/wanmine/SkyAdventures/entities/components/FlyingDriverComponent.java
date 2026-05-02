package net.wanmine.SkyAdventures.entities.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.wanmine.SkyAdventures.SkyAdventuresPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FlyingDriverComponent implements Component<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private MovementStates lastStates;


    public MovementStates getLastStates() {
        return lastStates;
    }
    private Ref<EntityStore> flyerRef;

    public void setFlyerRef(Ref<EntityStore> flyerRef) {
        this.flyerRef = flyerRef;
    }

    private Ref<EntityStore> seatRef;

    public void setSeatRef(Ref<EntityStore> seatRef) {
        this.seatRef = seatRef;
    }
    public Ref<EntityStore> getSeatRef() {
        return seatRef;
    }

    public Ref<EntityStore> getFlyerRef() {
        return flyerRef;
    }

    private int activeCamera = 0;


    private Vector3f attachmentOffset = new Vector3f(0f, 0f, 0f);
    private MountController controllerType = MountController.Minecart;
    private boolean networkOutdated = true;

    private boolean walking;
    private boolean running;
    private boolean jumping;
    private boolean crouching;
    private boolean idle;
    private boolean prevJumping;

    private float moveForward;
    private float moveStrafe;
    private float verticalInput;
    private float bodyYaw;
    private float bodyPitch;
    private float bodyRoll;
    private float headYaw;
    private float headPitch;
    private float headRoll;

    public Vector3f getAttachmentOffset() { return attachmentOffset; }
    public void setAttachmentOffset(Vector3f attachmentOffset) {
        this.attachmentOffset = attachmentOffset != null ? attachmentOffset : new Vector3f(0f, 0f, 0f);
        markNetworkOutdated();
    }

    public MountController getControllerType() { return controllerType; }
    public void setControllerType(MountController controllerType) {
        this.controllerType = controllerType != null ? controllerType : MountController.Minecart;
        markNetworkOutdated();
    }

    public void markNetworkOutdated() {
        this.networkOutdated = true;
    }

    public boolean consumeNetworkOutdated() {
        boolean value = this.networkOutdated;
        this.networkOutdated = false;
        return value;
    }
    
    public boolean isWalking() { return walking; }
    public boolean isRunning() { return running; }
    public boolean isJumping() { return jumping; }
    public boolean isCrouching() { return crouching; }
    public boolean isIdle() { return idle; }
    public boolean wasJumping() { return prevJumping; }

    public void updateLastStates(MovementStates states) {
        if (states == null) return;
        lastStates = states;

/*        LOGGER.atInfo().log("hIdle: %s, crouch: %s, jump: %s, running: %s, walking: %s, idle: %s",
                states.horizontalIdle,
                states.crouching,
                states.jumping,
                states.running,
                states.walking,
                states.idle
        );*/

        prevJumping = jumping;

        walking = states.walking;
        running = states.running || states.sprinting;
        jumping = states.jumping;
        crouching = states.crouching;
        idle = states.horizontalIdle;
    }

    @Nonnull
    public static final BuilderCodec<FlyingDriverComponent> CODEC = BuilderCodec
            .builder(FlyingDriverComponent.class, FlyingDriverComponent::new)
            .build();

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        FlyingDriverComponent cloned = new FlyingDriverComponent();
        cloned.walking = this.walking;
        cloned.running = this.running;
        cloned.jumping = this.jumping;
        cloned.crouching = this.crouching;
        cloned.idle = this.idle;
        cloned.prevJumping = this.prevJumping;
        return cloned;
    }

    public static ComponentType<EntityStore, FlyingDriverComponent> getComponentType() {
        return SkyAdventuresPlugin.flyingDriverComponent;
    }

    public void setMoveForward(float moveForward) {
        this.moveForward = moveForward;
    }

    public float getMoveForward() {
        return moveForward;
    }

    public void setMoveStrafe(float moveStrafe) {
        this.moveStrafe = moveStrafe;
    }

    public float getMoveStrafe() {
        return moveStrafe;
    }

    public void setVerticalInput(float verticalInput) {
        this.verticalInput = verticalInput;
    }

    public float getVerticalInput() {
        return verticalInput;
    }

    public void setBodyYaw(float bodyYaw) {
        this.bodyYaw = bodyYaw;
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public void setBodyPitch(float bodyPitch) {
        this.bodyPitch = bodyPitch;
    }

    public float getBodyPitch() {
        return bodyPitch;
    }

    public void setBodyRoll(float bodyRoll) {
        this.bodyRoll = bodyRoll;
    }

    public float getBodyRoll() {
        return bodyRoll;
    }

    public void setHeadYaw(float headYaw) {
        this.headYaw = headYaw;
    }

    public float getHeadYaw() {
        return headYaw;
    }

    public void setHeadPitch(float headPitch) {
        this.headPitch = headPitch;
    }

    public float getHeadPitch() {
        return headPitch;
    }

    public void setHeadRoll(float headRoll) {
        this.headRoll = headRoll;
    }

    public float getHeadRoll() {
        return headRoll;
    }

    public int getActiveCamera() {
        return activeCamera;
    }

    public void setActiveCamera(int activeCamera) {
        this.activeCamera = activeCamera;
    }
}