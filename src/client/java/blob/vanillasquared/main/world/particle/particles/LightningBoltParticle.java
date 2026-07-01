package blob.vanillasquared.main.world.particle.particles;

import blob.vanillasquared.main.world.particle.VSQParticleRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LightningBoltParticle extends Particle {
    private static final float LIGHTNING_SCALE = 1.0F / 16.0F;
    private static final double BOUNDS_RADIUS = 0.75D;
    private static final int LAYER_COUNT = 4;
    private static final int SPINE_POINTS = 8;
    private static final int SPINE_SEGMENTS = SPINE_POINTS - 1;
    private static final float CORE_START_Y = -7.45F;
    private static final float CORE_END_Y = 7.45F;
    private static final float CORE_LENGTH = CORE_END_Y - CORE_START_Y;
    private static final float BASE_RED = 0.45F;
    private static final float BASE_GREEN = 0.45F;
    private static final float BASE_BLUE = 0.5F;
    private static final float BASE_ALPHA = 0.3F;
    private static final int EXPECTED_VARIANT_PRESET_COUNT = LightningBoltParticleOptions.MAX_VARIANT - LightningBoltParticleOptions.MIN_VARIANT + 1;
    private static final LightningShapePreset[] VARIANT_PRESETS = new LightningShapePreset[]{
            new LightningShapePreset(1.18F, 4.6F, 0.14F, 0.24F, 1.0F),
            new LightningShapePreset(1.3F, 4.95F, 0.13F, 0.25F, 0.98F),
            new LightningShapePreset(1.08F, 4.3F, 0.16F, 0.22F, 1.08F),
            new LightningShapePreset(1.42F, 5.15F, 0.12F, 0.23F, 0.94F)
    };

    static {
        if (VARIANT_PRESETS.length != EXPECTED_VARIANT_PRESET_COUNT) {
            throw new IllegalStateException("Lightning bolt particle needs " + EXPECTED_VARIANT_PRESET_COUNT + " variant presets, got " + VARIANT_PRESETS.length);
        }
    }

    private final long seed;
    private final float yaw;
    private final float pitch;
    private final int variant;
    private final float length;

    protected LightningBoltParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, LightningBoltParticleOptions options) {
        super(level, x, y, z, xd, yd, zd);
        this.seed = this.random.nextLong();
        this.yaw = options.yaw();
        this.pitch = options.pitch();
        this.variant = options.variant();
        this.length = options.length();
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.lifetime = 2;
        this.setBoundingBox(new AABB(x - BOUNDS_RADIUS, y - BOUNDS_RADIUS, z - BOUNDS_RADIUS, x + BOUNDS_RADIUS, y + BOUNDS_RADIUS, z + BOUNDS_RADIUS));
    }

    @Override
    public ParticleRenderType getGroup() {
        return VSQParticleRenderTypes.LIGHTNING_BOLT;
    }

    public ParticleGroupRenderState createRenderState(Camera camera, float tickDelta) {
        var cameraPosition = camera.position();
        float renderX = (float) (Mth.lerp(tickDelta, this.xo, this.x) - cameraPosition.x);
        float renderY = (float) (Mth.lerp(tickDelta, this.yo, this.y) - cameraPosition.y);
        float renderZ = (float) (Mth.lerp(tickDelta, this.zo, this.z) - cameraPosition.z);

        PoseStack poseStack = new PoseStack();
        poseStack.translate(renderX, renderY, renderZ);
        poseStack.mulPose(pathRotation(this.yaw, this.pitch));
        poseStack.scale(LIGHTNING_SCALE, this.length / CORE_LENGTH, LIGHTNING_SCALE);
        return new LightningBoltParticleRenderState(poseStack, this.seed, this.variant);
    }

    private static Quaternionf pathRotation(float yaw, float pitch) {
        float yawRadians = yaw * Mth.DEG_TO_RAD;
        float pitchRadians = pitch * Mth.DEG_TO_RAD;
        float horizontal = Mth.cos(pitchRadians);
        Vector3f direction = new Vector3f(
                horizontal * Mth.cos(yawRadians),
                Mth.sin(pitchRadians),
                horizontal * Mth.sin(yawRadians)
        ).normalize();
        return new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), direction);
    }

    private static void renderBolt(Matrix4f matrix, VertexConsumer consumer, long seed, int variant) {
        LightningShapePreset preset = VARIANT_PRESETS[Mth.clamp(variant, 0, VARIANT_PRESETS.length - 1)];
        float[] xs = new float[SPINE_POINTS];
        float[] zs = new float[SPINE_POINTS];
        buildSpine(seed + variant * 31L, preset, xs, zs);

        for (int layer = 0; layer < LAYER_COUNT; layer++) {
            for (int segment = SPINE_SEGMENTS - 1; segment >= 0; segment--) {
                float lowerX = xs[segment + 1];
                float lowerZ = zs[segment + 1];
                float upperX = xs[segment];
                float upperZ = zs[segment];
                float lowerY = segmentY(segment + 1);
                float upperY = segmentY(segment);
                float lowerRadius = layerRadius(layer, segment + 1, preset);
                float upperRadius = layerRadius(layer, segment, preset);

                emitVanillaSegment(matrix, consumer, lowerX, lowerY, lowerZ, upperX, upperY, upperZ, lowerRadius, upperRadius);
            }
        }
    }

    private static void buildSpine(long seed, LightningShapePreset preset, float[] xs, float[] zs) {
        RandomSource random = RandomSource.create(seed);
        float x = 0.0F;
        float z = 0.0F;

        for (int index = SPINE_POINTS - 1; index >= 0; index--) {
            xs[index] = x;
            zs[index] = z;
            if (index == 0) {
                break;
            }
            float t = 1.0F - index / (float) SPINE_SEGMENTS;
            float profile = 0.45F + (float) Math.sin(t * Math.PI) * 0.55F;
            x += (random.nextInt(11) - 5) * preset.jitterScale() * profile;
            z += (random.nextInt(11) - 5) * preset.jitterScale() * profile;
            x = Mth.clamp(x, -preset.maxHorizontalExtent(), preset.maxHorizontalExtent());
            z = Mth.clamp(z, -preset.maxHorizontalExtent(), preset.maxHorizontalExtent());
        }

        centerSpine(xs);
        centerSpine(zs);
    }

    private static void centerSpine(float[] values) {
        float min = values[0];
        float max = values[0];
        for (int index = 1; index < values.length; index++) {
            min = Math.min(min, values[index]);
            max = Math.max(max, values[index]);
        }

        float center = (min + max) * 0.5F;
        for (int index = 0; index < values.length; index++) {
            values[index] = values[index] - center;
        }
    }

    private static float segmentY(int pointIndex) {
        return Mth.lerp(pointIndex / (float) SPINE_SEGMENTS, CORE_END_Y, CORE_START_Y);
    }

    private static float layerRadius(int layer, int pointIndex, LightningShapePreset preset) {
        float t = 1.0F - pointIndex / (float) SPINE_SEGMENTS;
        float taper = 0.52F + (float) Math.sin(t * Math.PI) * 0.48F;
        float vanillaRadius = (preset.baseRadius() + layer * preset.layerStep()) * (1.0F + pointIndex * 0.065F);
        return vanillaRadius * taper * preset.radiusScale();
    }

    private static void emitVanillaSegment(Matrix4f matrix, VertexConsumer consumer, float lowerX, float lowerY, float lowerZ, float upperX, float upperY, float upperZ, float lowerRadius, float upperRadius) {
        quad(matrix, consumer, lowerX, lowerY, lowerZ, upperX, upperY, upperZ, lowerRadius, upperRadius, false, false, true, false);
        quad(matrix, consumer, lowerX, lowerY, lowerZ, upperX, upperY, upperZ, lowerRadius, upperRadius, true, false, true, true);
        quad(matrix, consumer, lowerX, lowerY, lowerZ, upperX, upperY, upperZ, lowerRadius, upperRadius, true, true, false, true);
        quad(matrix, consumer, lowerX, lowerY, lowerZ, upperX, upperY, upperZ, lowerRadius, upperRadius, false, true, false, false);
    }

    private static void quad(Matrix4f matrix, VertexConsumer consumer, float lowerX, float lowerY, float lowerZ, float upperX, float upperY, float upperZ, float lowerRadius, float upperRadius, boolean lowerXPositive, boolean lowerZPositive, boolean upperXPositive, boolean upperZPositive) {
        addVertex(
                matrix,
                consumer,
                lowerX + signedRadius(lowerRadius, lowerXPositive),
                lowerY,
                lowerZ + signedRadius(lowerRadius, lowerZPositive)
        );
        addVertex(
                matrix,
                consumer,
                upperX + signedRadius(upperRadius, lowerXPositive),
                upperY,
                upperZ + signedRadius(upperRadius, lowerZPositive)
        );
        addVertex(
                matrix,
                consumer,
                upperX + signedRadius(upperRadius, upperXPositive),
                upperY,
                upperZ + signedRadius(upperRadius, upperZPositive)
        );
        addVertex(
                matrix,
                consumer,
                lowerX + signedRadius(lowerRadius, upperXPositive),
                lowerY,
                lowerZ + signedRadius(lowerRadius, upperZPositive)
        );
    }

    private static float signedRadius(float radius, boolean positive) {
        return positive ? radius : -radius;
    }

    private static void addVertex(Matrix4f matrix, VertexConsumer consumer, float x, float y, float z) {
        consumer.addVertex(matrix, x, y, z).setColor(BASE_RED, BASE_GREEN, BASE_BLUE, BASE_ALPHA);
    }

    private record LightningShapePreset(
            float jitterScale,
            float maxHorizontalExtent,
            float baseRadius,
            float layerStep,
            float radiusScale
    ) {
    }

    public static class Provider implements ParticleProvider<LightningBoltParticleOptions> {
        @Override
        public Particle createParticle(LightningBoltParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new LightningBoltParticle(level, x, y, z, xd, yd, zd, options);
        }
    }

    record LightningBoltParticleRenderState(PoseStack poseStack, long seed, int variant) implements ParticleGroupRenderState {
        @Override
        public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
            submitNodeCollector.submitCustomGeometry(this.poseStack, RenderTypes.lightning(), (pose, consumer) -> renderBolt(pose.pose(), consumer, this.seed, this.variant));
        }
    }
}
