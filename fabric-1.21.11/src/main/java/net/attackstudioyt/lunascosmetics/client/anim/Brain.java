package net.attackstudioyt.lunascosmetics.client.anim;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;

/**
 * A pet's little mind: when to blink, where to look, when to knead, whether it has
 * dozed off. One per wearer, advanced by real time whenever the pet is drawn, so it
 * keeps living whether it's on your head, a friend's head or in the wardrobe.
 */
public final class Brain {
    private static final Map<Object, Brain> BRAINS = new HashMap<>();

    public static Brain of(Object owner) {
        return BRAINS.computeIfAbsent(owner, k -> new Brain(k.hashCode()));
    }

    public static void forget(Object owner) {
        BRAINS.remove(owner);
    }

    private final Random rng;
    private long lastNanos = -1;

    /** seconds this brain has been alive; drives every oscillation */
    public float time;
    public float dt;

    // blinking
    private float nextBlink = 1.5f;
    private float blinkEnd = -1;
    private boolean doubleBlink;

    // looking around
    public float lookYaw, lookPitch, tilt;
    private float targetYaw, targetPitch, targetTilt;
    private float nextLook = 0.5f;

    // ears
    public float earLeft, earRight;
    private float twitchL = -9, twitchR = -9;
    private float nextTwitch = 2f;

    // kneading ("making biscuits")
    private float kneadStart = -99, kneadEnd = -99;
    private float nextKnead = 14f;

    // sleep
    private float idleTime;
    public float sleep;        // 0 awake .. 1 asleep, eased
    private float wakeAt = -99;

    // flinch + bounce
    private float hurtAt = -99;
    private float bounceAt = -99;
    private float nextBounce = 9f;

    // tail
    public float tailPhase;

    private Brain(long seed) {
        this.rng = Random.create(seed * 31 + System.nanoTime());
        this.time = rng.nextFloat() * 3;
        this.tailPhase = rng.nextFloat() * 6;
    }

    /** Advance to "now". Safe to call several times a frame. */
    public void update(Inputs in) {
        long now = System.nanoTime();
        dt = lastNanos < 0 ? 0 : MathHelper.clamp((now - lastNanos) / 1e9f, 0, 0.1f);
        lastNanos = now;
        if (dt == 0) {
            return;
        }
        time += dt;

        // --- sleep: nod off after ~25 s of stillness, wake the instant anything happens
        boolean still = in.moving() < 0.02f && in.fallSpeed() < 0.05f && !in.hurt() && in.idleLook();
        idleTime = still ? idleTime + dt : 0;
        float wantSleep = idleTime > 25 ? 1 : 0;
        if (wantSleep == 0 && sleep > 0.5f) {
            wakeAt = time;
        }
        sleep = approach(sleep, wantSleep, dt * (wantSleep > 0 ? 0.35f : 3.5f));

        // --- blinking
        if (time >= nextBlink) {
            blinkEnd = time + 0.11f;
            if (!doubleBlink && rng.nextFloat() < 0.22f) {
                doubleBlink = true;            // a quick second blink follows
                nextBlink = time + 0.26f;
            } else {
                doubleBlink = false;
                nextBlink = time + 2.2f + rng.nextFloat() * 4.2f;
            }
        }

        // --- looking around
        if (time >= nextLook) {
            float r = rng.nextFloat();
            if (r < 0.3f) {        // straight ahead, the way she's facing
                targetYaw = 0;
                targetPitch = 0;
            } else {
                targetYaw = (rng.nextFloat() * 2 - 1) * 42;
                targetPitch = (rng.nextFloat() * 2 - 1) * 14;
            }
            targetTilt = rng.nextFloat() < 0.25f ? (rng.nextBoolean() ? 1 : -1) * (10 + rng.nextFloat() * 8) : 0;
            nextLook = time + 1.4f + rng.nextFloat() * 3.6f;
        }
        float lookRate = 1 - (float) Math.exp(-dt * 5.5f);
        lookYaw += (targetYaw * (1 - sleep) - lookYaw) * lookRate;
        lookPitch += (targetPitch * (1 - sleep) - lookPitch) * lookRate;
        tilt += (targetTilt * (1 - sleep) - tilt) * lookRate * 0.8f;

        // --- ear twitches
        if (time >= nextTwitch) {
            if (rng.nextBoolean()) {
                twitchL = time;
            } else {
                twitchR = time;
            }
            if (rng.nextFloat() < 0.25f) {
                twitchL = twitchR = time;
            }
            nextTwitch = time + 2.5f + rng.nextFloat() * 6f;
        }
        earLeft = twitchCurve(time - twitchL);
        earRight = twitchCurve(time - twitchR);

        // --- kneading
        if (time >= nextKnead && sleep < 0.1f && in.moving() < 0.1f) {
            kneadStart = time;
            kneadEnd = time + 2.5f + rng.nextFloat() * 2.5f;
            nextKnead = kneadEnd + 12 + rng.nextFloat() * 20;
        }

        // --- happy bounce (Mini Moosh mostly)
        if (time >= nextBounce && sleep < 0.1f) {
            bounceAt = time;
            nextBounce = time + 6 + rng.nextFloat() * 10;
        }

        if (in.hurt() && time - hurtAt > 0.5f) {
            hurtAt = time;
            idleTime = 0;
        }

        float speed = 1.6f + in.moving() * 4.5f;
        tailPhase += dt * speed * (1 - sleep * 0.8f);
    }

    public boolean eyesClosed() {
        return time < blinkEnd || sleep > 0.6f || kneading() > 0.5f && ((int) (time * 1.3f) % 3 == 0);
    }

    /** 0..1 envelope while making biscuits */
    public float kneading() {
        if (time < kneadStart || time > kneadEnd) {
            return 0;
        }
        return Math.min(1, Math.min(time - kneadStart, kneadEnd - time) * 3);
    }

    /** 1 right after being hurt, fading out over ~0.6 s */
    public float flinch() {
        float s = time - hurtAt;
        return s < 0 || s > 0.6f ? 0 : 1 - s / 0.6f;
    }

    /** a squash-and-stretch hop: 0 at rest, 0..1 during the 0.6 s hop */
    public float bounce() {
        float s = time - bounceAt;
        return s < 0 || s > 0.6f ? 0 : s / 0.6f;
    }

    /** a stretch just after waking up, 0..1 */
    public float stretch() {
        float s = time - wakeAt;
        return s < 0 || s > 1.2f ? 0 : MathHelper.sin(s / 1.2f * MathHelper.PI);
    }

    private static float twitchCurve(float s) {
        if (s < 0 || s > 0.3f) {
            return 0;
        }
        return MathHelper.sin(s / 0.3f * MathHelper.PI);
    }

    private static float approach(float v, float target, float step) {
        return v < target ? Math.min(target, v + step) : Math.max(target, v - step);
    }
}
