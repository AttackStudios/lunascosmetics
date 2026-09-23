package net.attackstudioyt.lunascosmetics.client.anim;

/**
 * What the wearer is doing this frame, boiled down to what pets react to.
 *
 * @param moving       0 (still) .. 1 (running)
 * @param walkPhase    limb swing phase, for bobbing in step
 * @param headPitch    wearer's head pitch in degrees (+ = looking down)
 * @param fallSpeed    blocks/tick downward, 0 when not falling
 * @param idleLook     true when the wearer's view hasn't moved for a while (for sleep)
 */
public record Inputs(float moving, float walkPhase, float headPitch, float fallSpeed,
                     boolean sneaking, boolean hurt, boolean idleLook) {
    public static final Inputs STILL = new Inputs(0, 0, 0, 0, false, false, false);
}
