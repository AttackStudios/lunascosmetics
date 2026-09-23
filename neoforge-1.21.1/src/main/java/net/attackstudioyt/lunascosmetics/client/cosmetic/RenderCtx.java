package net.attackstudioyt.lunascosmetics.client.cosmetic;

import net.attackstudioyt.lunascosmetics.client.anim.Brain;
import net.attackstudioyt.lunascosmetics.client.anim.Inputs;
import net.attackstudioyt.lunascosmetics.client.render.CosmeticSink;

/**
 * Everything a cosmetic needs to draw one frame. The matrices in {@code sink} are
 * already at the cosmetic's anchor (top of the head, the shoulder, or the neck on the
 * back) in entity model space: pixels/16, -y up, -z forward.
 */
public record RenderCtx(CosmeticSink sink, Brain brain, Inputs inputs, Perch perch, boolean gui) {
}
