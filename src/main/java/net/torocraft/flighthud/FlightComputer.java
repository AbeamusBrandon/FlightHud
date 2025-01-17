package net.torocraft.flighthud;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.torocraft.flighthud.sounds.ModSounds;

public class FlightComputer {
    private static final float TICKS_PER_SECOND = 20;

    private float previousRollAngle = 0.0f;

    public Vec3d velocity;
    public float speed;
    public float pitch;
    public float heading;
    public Vec3d flightPath;
    public float flightPitch;
    public float flightHeading;
    public float roll;
    public float altitude;
    public Integer groundLevel;
    public Float distanceFromGround;
    public Float elytraHealth;

    // Sounds
    final byte altitudeWarningCooldown = 100;
    byte timeSinceLastAltitudeWarning = altitudeWarningCooldown;

    final byte terrainWarningCooldown = 100;
    byte timeSinceLastTerrainWarning = terrainWarningCooldown;

    public void update(MinecraftClient client, float partial) {
        velocity = client.player.getVelocity();
        pitch = computePitch(client, partial);
        speed = computeSpeed(client);
        roll = computeRoll(client, partial);
        heading = computeHeading(client);
        altitude = computeAltitude(client);
        groundLevel = computeGroundLevel(client);
        distanceFromGround = computeDistanceFromGround(client, altitude, groundLevel);
        flightPitch = computeFlightPitch(velocity, pitch);
        flightHeading = computeFlightHeading(velocity, heading);
        elytraHealth = computeElytraHealth(client);

        // Sounds
        if (timeSinceLastAltitudeWarning < altitudeWarningCooldown) {
            timeSinceLastAltitudeWarning++;
        }
        if (altitude < 0 && timeSinceLastAltitudeWarning == altitudeWarningCooldown) {
            client.player.getWorld().playSound(client.player, client.player.getBlockPos(),
                    ModSounds.ALTITUDE_WARNING, SoundCategory.MASTER, 1f, 1f);
            timeSinceLastAltitudeWarning = 0;
        }

        /*
        if (timeSinceLastTerrainWarning < terrainWarningCooldown) {
            timeSinceLastTerrainWarning++;
        }
        if (timeSinceLastTerrainWarning == terrainWarningCooldown) {
            client.player.getWorld().playSound(client.player, client.player.getBlockPos(),
                    ModSounds.TERRAIN_WARNING, SoundCategory.MASTER, 1f, 1f);
        }
        */
    }

    private Float computeElytraHealth(MinecraftClient client) {
        ItemStack stack = client.player.getEquippedStack(EquipmentSlot.CHEST);
        if (stack != null && stack.getItem() == Items.ELYTRA) {
            float remain = ((float) stack.getMaxDamage() - (float) stack.getDamage()) / (float) stack.getMaxDamage();
            return remain * 100f;
        }
        return null;
    }

    private float computeFlightPitch(Vec3d velocity, float pitch) {
        if (velocity.length() < 0.01) {
            return pitch;
        }
        Vec3d n = velocity.normalize();
        return (float) (90 - Math.toDegrees(Math.acos(n.y)));
    }

    private float computeFlightHeading(Vec3d velocity, float heading) {
        if (velocity.length() < 0.01) {
            return heading;
        }
        return toHeading((float) Math.toDegrees(-Math.atan2(velocity.x, velocity.z)));
    }

    /**
     * Roll logic is from:
     * https://github.com/Jorbon/cool_elytra/blob/main/src/main/java/edu/jorbonism/cool_elytra/mixin/GameRendererMixin.java
     * to enable both mods will sync up when used together.
     */
    private float computeRoll(MinecraftClient client, float partial) {
        if (!FlightHud.CONFIG_SETTINGS.calculateRoll) {
            return 0;
        }

        float wingPower = FlightHud.CONFIG_SETTINGS.rollTurningForce;
        float rollSmoothing = FlightHud.CONFIG_SETTINGS.rollSmoothing;
        Vec3d facing = client.player.getRotationVecClient();
        Vec3d velocity = client.player.getVelocity();
        double horizontalFacing2 = facing.horizontalLengthSquared();
        double horizontalSpeed2 = velocity.horizontalLengthSquared();

        float rollAngle = 0.0f;

        if (horizontalFacing2 > 0.0D && horizontalSpeed2 > 0.0D) {
            double dot = (velocity.x * facing.x + velocity.z * facing.z) / Math.sqrt(
                    horizontalFacing2 * horizontalSpeed2);
            dot = MathHelper.clamp(dot, -1, 1);
            double direction = Math.signum(velocity.x * facing.z - velocity.z * facing.x);
            rollAngle = (float) (
                    Math.atan(Math.sqrt(horizontalSpeed2) * Math.acos(dot) * wingPower) * direction * 57.29577951308);
        }

        rollAngle = (float) ((1.0 - rollSmoothing) * rollAngle + rollSmoothing * previousRollAngle);
        previousRollAngle = rollAngle;

        return rollAngle;
    }

    private float computePitch(MinecraftClient client, float parital) {
        return client.player.getPitch(parital) * -1;
    }

    private boolean isGround(BlockPos pos, MinecraftClient client) {
        BlockState block = client.world.getBlockState(pos);
        return !block.isAir();
    }

    public BlockPos findGround(MinecraftClient client) {
        BlockPos pos = client.player.getBlockPos();
        while (pos.getY() >= 0) {
            pos = pos.down();
            if (isGround(pos, client)) {
                return pos;
            }
        }
        return null;
    }

    private Integer computeGroundLevel(MinecraftClient client) {
        BlockPos ground = findGround(client);
        return ground == null ? null : ground.getY();
    }

    private Float computeDistanceFromGround(MinecraftClient client, float altitude, Integer groundLevel) {
        if (groundLevel == null) {
            return null;
        }
        return Math.max(0f, altitude - groundLevel);
    }

    private float computeAltitude(MinecraftClient client) {
        return (float) client.player.getPos().y - 1;
    }

    private float computeHeading(MinecraftClient client) {
        return toHeading(client.player.getYaw());
    }

    private float computeSpeed(MinecraftClient client) {
        float speed = 0;
        var player = client.player;
        if (player.hasVehicle()) {
            Entity entity = player.getVehicle();
            speed = (float) entity.getVelocity().length() * TICKS_PER_SECOND;
        } else {
            speed = (float) client.player.getVelocity().length() * TICKS_PER_SECOND;
        }
        return speed;
    }

    private float toHeading(float yawDegrees) {
        return (yawDegrees + 180) % 360;
    }
}
