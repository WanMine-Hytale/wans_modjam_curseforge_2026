package net.wanmine.SkyAdventures.entities.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.Opacity;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import net.wanmine.SkyAdventures.entities.components.*;

import javax.annotation.Nonnull;
import java.util.*;

public class FlyMountMovementSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final double THRUST          = 8.0;
    private static final double VERTICAL_THRUST = 4.0;
    private static final double DRAG            = 0.8;
    private static final double VERTICAL_DRAG   = 2.0;

    private static final float TURN_SPEED   = 1.2f;
    private static final float TURN_ACCEL   = 2.5f;
    private static final float TURN_DAMPING = 2.0f;

    private static final float PITCH_LERP       = 0.08f;
    private static final float MAX_VISUAL_PITCH = (float) (Math.PI / 22.5);

    private static final float VELOCITY_MAX = 5.0f;

    private static final double GROUND_OFFSET = 2.0;

    private final Set<Dependency<EntityStore>> dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, FlyMountSystems.InputSystem.class)
    );

    private final Query<EntityStore> query = Archetype.of(
            FlyingEntityComponent.getComponentType(),
            TransformComponent.getComponentType()
    );

    private void bringToTheGround(float dt, TransformComponent transform, World world, Store<EntityStore> store) {
        Vector3d location = transform.getPosition();

        Vector3d groundHit = TargetUtil.getTargetLocation(
                world,
                blockId -> {
                    if (blockId == BlockType.EMPTY_ID) return false;
                    BlockType type = BlockType.getAssetMap().getAsset(blockId);
                    return type != null && type.getOpacity() != Opacity.Transparent;
                },
                location.x, location.y + 1.0, location.z,
                0, -1, 0,
                256.0
        );


        if (groundHit != null) {
            double currentY = location.getY();
            double landingY = groundHit.getY() + GROUND_OFFSET;

            if (currentY > landingY + 0.05) {
                float distanceToGround = (float)(currentY - landingY);
                float descentSpeed = Math.min(2.0f, distanceToGround * 0.5f);
                transform.setPosition(transform.getPosition().clone().add(0, -descentSpeed * dt, 0));
                transform.markChunkDirty(store);
            }
        }
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> chunkStore,
                     @Nonnull CommandBuffer<EntityStore> cb) {

        World world = chunkStore.getExternalData().getWorld();
        Store<EntityStore> store = world.getEntityStore().getStore();

        Ref<EntityStore> flyingEntityRef = chunk.getReferenceTo(index);

        FlyingEntityComponent flying = chunk.getComponent(index, FlyingEntityComponent.getComponentType());
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());

        if (flying == null || transform == null) return;

        Ref<EntityStore> driverRef = flying.getDriver();
        if (driverRef == null || !driverRef.isValid() || !flying.hasDriver()) {
            bringToTheGround(dt, transform, world, store);
            return;
        }
        FlyingDriverComponent driver = store.getComponent(driverRef, FlyingDriverComponent.getComponentType());
        if (driver == null) return;

        float currentYaw = transform.getRotation().getYaw();
        float sinYaw = (float) Math.sin(currentYaw);
        float cosYaw = (float) Math.cos(currentYaw);

        float rawForward = driver.getMoveForward();
        float rawStrafe  = driver.getMoveStrafe();
        float moveForward = rawForward * cosYaw + rawStrafe * sinYaw;
        float moveStrafe  = rawStrafe  * cosYaw - rawForward * sinYaw;
        double moveVertical = driver.getVerticalInput();

        double horizontalSpeed = Math.sqrt(
                flying.velocity.x * flying.velocity.x + flying.velocity.z * flying.velocity.z
        );

        float speedNorm = (float) Math.min(1.0, horizontalSpeed / 10.0);

        float yaw = updateVisualRotation(flying, moveStrafe, moveForward, speedNorm, currentYaw, dt);

        flying.velocity.x += moveForward * sinYaw * THRUST * dt;
        flying.velocity.z += moveForward * cosYaw * THRUST * dt;
        flying.velocity.y += moveVertical * VERTICAL_THRUST * dt;

        flying.velocity.x *= Math.max(0.0, 1.0 - DRAG * dt);
        flying.velocity.z *= Math.max(0.0, 1.0 - DRAG * dt);
        flying.velocity.y *= Math.max(0.0, 1.0 - VERTICAL_DRAG * dt);

        float turnInput = Math.max(-1.0f, Math.min(1.0f, moveStrafe / VELOCITY_MAX));
        String wheelAnimation;
        if (Math.abs(turnInput) > 0.01f) {
            wheelAnimation = turnInput < 0 ? "WheelLeft" : "WheelRight";
        } else {
            wheelAnimation = "";
        }

        if (!wheelAnimation.equals(flying.lastWheelAnimation)) {
            flying.lastWheelAnimation = wheelAnimation;
            if (!wheelAnimation.isEmpty()) {
                AnimationUtils.playAnimation(flyingEntityRef, AnimationSlot.Action, null, wheelAnimation, true, store);
            }
        }

        transform.getRotation().setYaw(yaw);
        transform.getRotation().setPitch(flying.smoothVisualPitch);

        transform.setPosition(transform.getPosition().clone().add(flying.velocity.clone().scale(dt)));
        transform.markChunkDirty(store);
    }

    private float updateVisualRotation(FlyingEntityComponent flying, float moveStrafe, float moveForward, float speedNorm, float currentYaw, float dt) {
        float turnInput = Math.max(-1.0f, Math.min(1.0f, moveStrafe / VELOCITY_MAX));

        float targetTurnSpeed = turnInput != 0.0f ? -turnInput * TURN_SPEED : 0.0f;

        flying.currentTurnSpeed = approachF(
                flying.currentTurnSpeed,
                targetTurnSpeed,
                (Math.abs(turnInput) > 0.01f ? TURN_ACCEL : TURN_DAMPING) * dt
        );

        float yaw = normalizeAngle(currentYaw + flying.currentTurnSpeed * dt);

        flying.smoothVisualPitch = approachF(
                flying.smoothVisualPitch,
                -speedNorm * MAX_VISUAL_PITCH,
                PITCH_LERP * 60.0f * dt
        );

        return yaw;
    }

    private static float approachF(float current, float target, float amount) {
        if (current < target) return Math.min(current + amount, target);
        if (current > target) return Math.max(current - amount, target);
        return current;
    }

    private static float normalizeAngle(float angle) {
        while (angle >  Math.PI) angle -= (float) (Math.PI * 2.0);
        while (angle < -Math.PI) angle += (float) (Math.PI * 2.0);
        return angle;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() { return dependencies; }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() { return query; }
}