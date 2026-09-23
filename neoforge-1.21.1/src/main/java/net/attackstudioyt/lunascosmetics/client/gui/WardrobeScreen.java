package net.attackstudioyt.lunascosmetics.client.gui;

import net.attackstudioyt.lunascosmetics.client.ClientConfig;
import net.attackstudioyt.lunascosmetics.client.Wardrobe;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetic;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetics;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Loadout;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Slot;
import net.attackstudioyt.lunascosmetics.client.custom.CustomLibrary;
import net.attackstudioyt.lunascosmetics.client.sync.ClientSync;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Luna's Cosmetics: a free wardrobe. Everything is unlocked, nothing costs anything.
 *
 * <p>Left: Luna herself, live, wearing the outfit (drag to spin her). Right: tabs of
 * cosmetics as little living cards; click one to wear it, click again to take it off.
 */
public class WardrobeScreen extends Screen {
    private enum Tab {
        PETS("Pets", "icon_pets", Slot.PET),
        HATS("Hats", "icon_hats", Slot.HAT),
        BACK("Back", "icon_back", Slot.BACK),
        MINE("My Own", "icon_custom", null),
        SETTINGS("Settings", "icon_theme", null);

        final String label;
        final String icon;
        final Slot slot;

        Tab(String label, String icon, Slot slot) {
            this.label = label;
            this.icon = icon;
            this.slot = slot;
        }
    }

    private static Tab lastTab = Tab.PETS;

    private final Screen parent;
    private Tab tab = lastTab;
    private final Petals petals = new Petals(0.8f);

    // layout
    private int leftX1, leftX2, top, bottom, rightX1, rightX2, gridY1, gridY2;
    private static final int CARD_W = 64, CARD_H = 78, GAP = 6;

    // preview spin
    private float yaw = 20, yawVel, pitch = 4;
    private boolean dragging;
    private float lastInteract;

    private float scroll, scrollTarget;
    private final List<Card> cards = new ArrayList<>();
    private final List<Toggle> toggles = new ArrayList<>();
    private final List<Btn> buttons = new ArrayList<>();
    private EditBox relayField;
    private String hoverDesc;
    private float t;
    private long last;
    private float tabAnim;
    private float tabAnimTarget;

    private record Card(Cosmetic cosmetic, Slot slot, int x, int y) {
    }

    private record Toggle(String label, String hint, int x, int y, int w, java.util.function.BooleanSupplier get,
                          Runnable flip) {
    }

    private record Btn(String label, int x, int y, int w, int h, Runnable action) {
    }

    public WardrobeScreen(Screen parent) {
        super(Component.translatable("lunascosmetics.title"));
        this.parent = parent;
        CustomLibrary.rescan(true);
    }

    @Override
    protected void init() {
        int margin = 10;
        top = 30;
        bottom = height - margin;
        leftX1 = margin;
        leftX2 = margin + Math.max(120, Math.min(190, (int) (width * 0.34f)));
        rightX1 = leftX2 + 8;
        rightX2 = width - margin;
        gridY1 = top + 30;
        gridY2 = bottom - 24;
        tabAnim = tabAnimTarget = tab.ordinal();
        relayField = new EditBox(font, 0, 0, 10, 18, Component.literal("Relay"));
        relayField.setMaxLength(200);
        relayField.setValue(ClientConfig.get().relayUrl);
        relayField.setHint(Component.literal("wss://... (optional)"));
        relayField.setResponder(s -> {
            ClientConfig.get().relayUrl = s.trim();
            ClientConfig.save();
        });
        addWidget(relayField);
        layoutTab();
    }

    private void layoutTab() {
        cards.clear();
        toggles.clear();
        buttons.clear();
        relayField.visible = false;
        relayField.active = false;
        int gridW = rightX2 - rightX1 - 16;
        int cols = Math.max(1, (gridW + GAP) / (CARD_W + GAP));
        int startX = rightX1 + 8 + (gridW - (cols * (CARD_W + GAP) - GAP)) / 2;

        if (tab.slot != null) {
            List<Cosmetic> list = new ArrayList<>();
            list.add(null); // "none"
            list.addAll(Cosmetics.forSlot(tab.slot));
            for (Cosmetic c : Cosmetics.custom()) {
                if (c.slot == tab.slot) {
                    list.add(c);
                }
            }
            placeCards(list, tab.slot, cols, startX);
        } else if (tab == Tab.MINE) {
            List<Cosmetic> list = new ArrayList<>(Cosmetics.custom());
            placeCards(list, null, cols, startX);
            int by = gridY2 - 2;
            buttons.add(new Btn("Open my folder", rightX1 + 8, by - 20, 100, 18,
                    () -> Util.getPlatform().openFile(CustomLibrary.dir().toFile())));
            buttons.add(new Btn("Reload", rightX1 + 114, by - 20, 60, 18, () -> {
                CustomLibrary.rescan(true);
                layoutTab();
            }));
        } else {
            int x = rightX1 + 14;
            int y = gridY1 + 4;
            int w = rightX2 - rightX1 - 28;
            ClientConfig c = ClientConfig.get();
            toggles.add(new Toggle("Cherry Cat theme", "Pink menus, falling petals, paw cursor, a cat on the title screen",
                    x, y, w, () -> c.theme, () -> c.theme = !c.theme));
            toggles.add(new Toggle("Meow when clicking", "Buttons go mew (only with the theme on)",
                    x, y + 30, w, () -> c.meowClicks, () -> c.meowClicks = !c.meowClicks));
            toggles.add(new Toggle("Pet sounds", "Sleepy purrs, and Mini Moosh squeaks when it bounces",
                    x, y + 60, w, () -> c.petSounds, () -> c.petSounds = !c.petSounds));
            toggles.add(new Toggle("See friends' cosmetics", "Show what other players with the mod are wearing",
                    x, y + 90, w, () -> c.showOthers, () -> c.showOthers = !c.showOthers));
            toggles.add(new Toggle("Auto-update", net.attackstudioyt.lunascosmetics.update.Updater.status(),
                    x, y + 120, w, () -> c.autoUpdate, () -> c.autoUpdate = !c.autoUpdate));
            relayField.visible = true;
            relayField.active = true;
            relayField.setX(x);
            relayField.setY(y + 162);
            relayField.setWidth(Math.min(w, 260));
        }
        scroll = scrollTarget = 0;
    }

    private void placeCards(List<Cosmetic> list, Slot slot, int cols, int startX) {
        for (int i = 0; i < list.size(); i++) {
            int col = i % cols, row = i / cols;
            Cosmetic c = list.get(i);
            cards.add(new Card(c, c == null ? slot : c.slot, startX + col * (CARD_W + GAP), gridY1 + 4 + row * (CARD_H + GAP)));
        }
    }

    private int contentHeight() {
        int max = 0;
        for (Card c : cards) {
            max = Math.max(max, c.y + CARD_H - gridY1 + 6);
        }
        return max;
    }

    // ---- rendering -------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fillGradient(0, 0, width, height, 0xCC2A0F20, 0xCC5A2242);
        petals.render(ctx, width, height, mouseX, mouseY, 0.9f);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : Mth.clamp((now - last) / 1e9f, 0, 0.1f);
        last = now;
        t += dt;
        CustomLibrary.rescan(false);
        tabAnim += (tabAnimTarget - tabAnim) * Math.min(1, dt * 14);
        scroll += (scrollTarget - scroll) * Math.min(1, dt * 16);
        hoverDesc = null;

        // 1.21.1: Screen.render paints the background (and petals) itself, so it goes first
        super.render(ctx, mouseX, mouseY, delta);
        drawTitle(ctx);
        drawPreviewPanel(ctx, mouseX, mouseY, dt);
        drawRightPanel(ctx, mouseX, mouseY);
    }

    private void drawTitle(GuiGraphics ctx) {
        PoseStack m = ctx.pose();
        String title = "Luna's Cosmetics";
        int tw = font.width(title);
        float s = 1.6f;
        float bob = Mth.sin(t * 2) * 1.2f;
        m.pushPose();
        m.translate(width / 2f - tw * s / 2, 8 + bob, 0);
        m.scale(s, s, 1);
        ctx.drawString(font, title, 0, 0, 0xFFFFE3EE, true);
        m.popPose();
        String face = ((int) (t * 10) % 37 == 0) ? "cat_cherry_blink" : "cat_cherry_open";
        Ui.sprite(ctx, face, (int) (width / 2f - tw * s / 2) - 22, 6, 16, 16);
        Ui.sprite(ctx, "icon_heart", (int) (width / 2f + tw * s / 2) + 6, 8 + (int) (Mth.sin(t * 3) * 2), 9, 9);
    }

    private void drawPreviewPanel(GuiGraphics ctx, int mouseX, int mouseY, float dt) {
        Ui.panel(ctx, leftX1, top, leftX2, bottom, 6, 0xFFF7A1C0, 0xEE3A1428);
        // spotlight
        int cx = (leftX1 + leftX2) / 2;
        Ui.round(ctx, cx - 44, bottom - 64, cx + 44, bottom - 50, 7, 0x40FFD6E5);

        if (!dragging) {
            yaw += yawVel * dt;
            yawVel *= (float) Math.exp(-dt * 3);
            if (t - lastInteract > 3) {
                float target = 20 + Mth.sin(t * 0.4f) * 25;
                yaw += (target - yaw) * Math.min(1, dt * 0.8f);
            }
        }
        Minecraft client = Minecraft.getInstance();
        PlayerSkin skin = client.player != null ? client.player.getSkin()
                : client.getSkinManager().getInsecureSkin(client.getGameProfile());
        int px1 = leftX1 + 6, px2 = leftX2 - 6, py1 = top + 6, py2 = bottom - 46;
        float scale = Math.min((py2 - py1) / 2.95f, (px2 - px1) / 1.3f);
        Preview.draw(ctx, null, Wardrobe.mine(), "wardrobe-preview", yaw, pitch,
                skin.texture(), skin.model() == PlayerSkin.Model.SLIM,
                px1, py1, px2, py2, scale);

        // perch + take-off buttons under her
        Loadout mine = Wardrobe.mine();
        int by = bottom - 42;
        if (mine.get(Slot.PET) != null) {
            drawButton(ctx, "Pet: " + mine.perch().label, leftX1 + 6, by, leftX2 - leftX1 - 12, 17, mouseX, mouseY);
        }
        drawButton(ctx, "Take everything off", leftX1 + 6, by + 20, leftX2 - leftX1 - 12, 17, mouseX, mouseY);
        String status = ClientSync.status();
        if (status.length() > 0 && client.getConnection() != null) {
            int w = font.width(status);
            float sc = Math.min(1, (leftX2 - leftX1 - 10) / (float) w);
            PoseStack m = ctx.pose();
            m.pushPose();
            m.translate(leftX1 + 6, top + 6, 0);
            m.scale(sc * 0.75f, sc * 0.75f, 1);
            ctx.drawString(font, status, 0, 0, 0xFFFFC1D6, false);
            m.popPose();
        }
    }

    private boolean drawButton(GuiGraphics ctx, String label, int x, int y, int w, int h, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        Ui.panel(ctx, x, y, x + w, y + h, 4, hover ? 0xFFFFFFFF : 0xFFF7A1C0, hover ? 0xFFE0588A : 0xFFB83A66);
        ctx.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        return hover;
    }

    private void drawRightPanel(GuiGraphics ctx, int mouseX, int mouseY) {
        Ui.panel(ctx, rightX1, top, rightX2, bottom, 6, 0xFFF7A1C0, 0xEE3A1428);

        // tabs, with a pink pill that slides between them
        Tab[] tabs = Tab.values();
        int tabW = (rightX2 - rightX1 - 12) / tabs.length;
        int ty = top + 6;
        float pillX = rightX1 + 6 + tabAnim * tabW;
        Ui.round(ctx, (int) pillX + 1, ty, (int) pillX + tabW - 1, ty + 18, 6, 0xFFE0588A);
        for (int i = 0; i < tabs.length; i++) {
            Tab tb = tabs[i];
            int x = rightX1 + 6 + i * tabW;
            boolean hover = mouseX >= x && mouseX < x + tabW && mouseY >= ty && mouseY < ty + 18;
            if (hover && tb != tab) {
                Ui.round(ctx, x + 1, ty, x + tabW - 1, ty + 18, 6, 0x40FFD6E5);
            }
            int lw = font.width(tb.label);
            boolean showText = tabW > lw + 18;
            int contentW = showText ? 12 + lw : 12;
            int sx = x + (tabW - contentW) / 2;
            Ui.sprite(ctx, tb.icon, sx, ty + 3, 12, 12);
            if (showText) {
                ctx.drawString(font, tb.label, sx + 14, ty + 5, tb == tab ? 0xFFFFFFFF : 0xFFFFC1D6, true);
            }
        }

        if (tab == Tab.SETTINGS) {
            drawSettings(ctx, mouseX, mouseY);
            return;
        }

        int maxScroll = Math.max(0, contentHeight() - (gridY2 - gridY1));
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        ctx.enableScissor(rightX1 + 2, gridY1, rightX2 - 2, gridY2);
        Loadout mine = Wardrobe.mine();
        boolean inGrid = mouseY >= gridY1 && mouseY < gridY2;
        for (Card card : cards) {
            int x = card.x;
            int y = (int) (card.y - scroll);
            if (y + CARD_H < gridY1 || y > gridY2) {
                continue;
            }
            boolean hover = inGrid && mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
            String id = card.cosmetic == null ? null : card.cosmetic.id;
            boolean selected = card.cosmetic == null ? mine.get(card.slot) == null
                    : id.equals(mine.get(card.cosmetic.slot));
            int lift = hover ? 2 : 0;
            int border = selected ? 0xFFFF6FA0 : hover ? 0xFFFFD6E5 : 0xFF7A3A5A;
            int fill = selected ? 0xFF6A2448 : hover ? 0xFF55203C : 0xFF4A1A34;
            Ui.panel(ctx, x, y - lift, x + CARD_W, y + CARD_H - lift, 5, border, fill);
            if (selected) {
                Ui.panel(ctx, x - 1, y - lift - 1, x + CARD_W + 1, y + CARD_H - lift + 1, 6, 0xFFFF6FA0, 0x00000000);
            }
            // accent swatch behind the model
            if (card.cosmetic != null) {
                Ui.round(ctx, x + 10, y - lift + 50, x + CARD_W - 10, y - lift + 56, 3,
                        Ui.alpha(card.cosmetic.accent, 0.55f));
                float spin = (hover ? t * 70 : t * 22) + card.x * 0.7f;
                Preview.draw(ctx, card.cosmetic.id, mine, "card:" + card.cosmetic.id,
                        -25 + Mth.sin(spin * 0.017f) * 35 + (hover ? t * 40 % 360 : 0), 18, null, false,
                        x + 2, y - lift + 4, x + CARD_W - 2, y - lift + 58, 34);
            } else {
                Ui.sprite(ctx, "icon_none", x + CARD_W / 2 - 12, y - lift + 16, 24, 24);
            }
            String name = card.cosmetic == null ? "None" : card.cosmetic.name;
            drawFitted(ctx, name, x + CARD_W / 2, y - lift + 62, CARD_W - 6, selected ? 0xFFFFFFFF : 0xFFFFD6E5);
            if (selected) {
                float beat = 1 + Mth.sin(t * 5) * 0.08f;
                PoseStack m = ctx.pose();
                m.pushPose();
                m.translate(x + CARD_W - 6, y - lift + 6, 200); // in front of the 3D card model
                m.scale(beat, beat, 1);
                Ui.sprite(ctx, "icon_heart", -5, -5, 9, 9);
                m.popPose();
            }
            if (hover) {
                hoverDesc = card.cosmetic == null ? "Take it off" : card.cosmetic.description;
            }
        }
        ctx.disableScissor();

        if (tab == Tab.MINE) {
            if (cards.isEmpty()) {
                int cx = (rightX1 + rightX2) / 2;
                ctx.drawCenteredString(font, "Make your own cosmetics!", cx, gridY1 + 20, 0xFFFFE3EE);
                ctx.drawCenteredString(font, "Build one in Blockbench, save the .bbmodel", cx, gridY1 + 36, 0xFFFFC1D6);
                ctx.drawCenteredString(font, "into your folder, and it shows up here.", cx, gridY1 + 48, 0xFFFFC1D6);
            }
            for (Btn b : buttons) {
                drawButton(ctx, b.label, b.x, b.y, b.w, b.h, mouseX, mouseY);
            }
        }

        // scrollbar
        if (maxScroll > 0) {
            int trackH = gridY2 - gridY1;
            int barH = Math.max(16, trackH * trackH / (trackH + maxScroll));
            int barY = gridY1 + (int) ((trackH - barH) * (scroll / maxScroll));
            Ui.round(ctx, rightX2 - 6, barY, rightX2 - 3, barY + barH, 1, 0xAAF7A1C0);
        }

        String desc = hoverDesc != null ? hoverDesc : describeWorn(mine);
        drawFitted(ctx, desc, (rightX1 + rightX2) / 2, bottom - 16, rightX2 - rightX1 - 16, 0xFFFFD6E5);
    }

    private String describeWorn(Loadout mine) {
        if (tab.slot == null) {
            return tab == Tab.MINE ? "Hats: hat_name.bbmodel  Pets: pet_name  Back: back_name" : "";
        }
        Cosmetic c = Cosmetics.get(mine.get(tab.slot));
        return c == null ? "Everything here is free. Click to wear!" : "Wearing: " + c.name;
    }

    private void drawFitted(GuiGraphics ctx, String s, int cx, int y, int maxW, int color) {
        int w = font.width(s);
        if (w <= maxW) {
            ctx.drawString(font, s, cx - w / 2, y, color, true);
            return;
        }
        float sc = maxW / (float) w;
        PoseStack m = ctx.pose();
        m.pushPose();
        m.translate(cx - w * sc / 2, y + (1 - sc) * 4, 0);
        m.scale(sc, sc, 1);
        ctx.drawString(font, s, 0, 0, color, true);
        m.popPose();
    }

    private void drawSettings(GuiGraphics ctx, int mouseX, int mouseY) {
        for (Toggle tg : toggles) {
            boolean on = tg.get.getAsBoolean();
            boolean hover = mouseX >= tg.x && mouseX < tg.x + tg.w && mouseY >= tg.y && mouseY < tg.y + 24;
            if (hover) {
                Ui.round(ctx, tg.x - 4, tg.y - 3, tg.x + tg.w + 4, tg.y + 25, 5, 0x30FFD6E5);
            }
            // switch: a pill with a cat-paw knob that slides
            int sx = tg.x, sy = tg.y + 3;
            Ui.round(ctx, sx, sy, sx + 26, sy + 14, 7, on ? 0xFFE0588A : 0xFF6A4A5C);
            int knob = on ? sx + 13 : sx + 1;
            Ui.round(ctx, knob, sy + 1, knob + 12, sy + 13, 6, 0xFFFFF4F8);
            if (on) {
                Ui.sprite(ctx, "icon_heart", knob + 2, sy + 3, 8, 8);
            }
            ctx.drawString(font, tg.label, sx + 32, tg.y + 1, 0xFFFFFFFF, true);
            drawFittedLeft(ctx, tg.hint, sx + 32, tg.y + 12, tg.w - 34, 0xFFFFC1D6);
        }
        int x = toggles.isEmpty() ? rightX1 + 14 : toggles.get(0).x;
        int y = relayField.getY() - 12;
        drawFittedLeft(ctx, "Sync relay (optional, for servers without the mod)", x, y, rightX2 - x - 10, 0xFFFFD6E5);
        relayField.render(ctx, mouseX, mouseY, 0);
        drawFittedLeft(ctx, ClientSync.status(), x, relayField.getY() + 22, rightX2 - x - 10, 0xFFFFC1D6);
    }

    private void drawFittedLeft(GuiGraphics ctx, String s, int x, int y, int maxW, int color) {
        int w = font.width(s);
        float sc = w <= maxW ? 0.85f : maxW / (float) w * 0.85f;
        PoseStack m = ctx.pose();
        m.pushPose();
        m.translate(x, y, 0);
        m.scale(Math.min(sc, 0.85f), Math.min(sc, 0.85f), 1);
        ctx.drawString(font, s, 0, 0, color, false);
        m.popPose();
    }

    // ---- input -------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        // tabs
        Tab[] tabs = Tab.values();
        int tabW = (rightX2 - rightX1 - 12) / tabs.length;
        int ty = top + 6;
        if (my >= ty && my < ty + 18 && mx >= rightX1 + 6 && mx < rightX1 + 6 + tabW * tabs.length) {
            Tab picked = tabs[(int) ((mx - rightX1 - 6) / tabW)];
            if (picked != tab) {
                tab = lastTab = picked;
                tabAnimTarget = picked.ordinal();
                Ui.play(SoundEvents.CAT_AMBIENT, 1.8f, 0.25f);
                layoutTab();
            }
            return true;
        }
        // preview: start dragging
        if (mx >= leftX1 && mx < leftX2 && my >= top && my < bottom - 46) {
            dragging = true;
            lastInteract = t;
            return true;
        }
        // perch / take off
        Loadout mine = Wardrobe.mine();
        int by = bottom - 42;
        if (mx >= leftX1 + 6 && mx < leftX2 - 6) {
            if (mine.get(Slot.PET) != null && my >= by && my < by + 17) {
                Wardrobe.setMine(mine.withPerch(mine.perch().next()));
                Ui.play(SoundEvents.CAT_PURREOW, 1.3f, 0.35f);
                return true;
            }
            if (my >= by + 20 && my < by + 37) {
                Loadout none = Loadout.EMPTY.withPerch(mine.perch());
                Wardrobe.setMine(none);
                Ui.play(SoundEvents.CAT_HISS, 1.6f, 0.2f);
                return true;
            }
        }
        if (tab == Tab.SETTINGS) {
            for (Toggle tg : toggles) {
                if (mx >= tg.x && mx < tg.x + tg.w && my >= tg.y && my < tg.y + 24) {
                    tg.flip.run();
                    ClientConfig.save();
                    Ui.play(SoundEvents.CAT_AMBIENT, tg.get.getAsBoolean() ? 1.6f : 1.1f, 0.3f);
                    return true;
                }
            }
            return false;
        }
        for (Btn b : buttons) {
            if (mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h) {
                Ui.play(SoundEvents.CAT_AMBIENT, 1.5f, 0.3f);
                b.action.run();
                return true;
            }
        }
        if (my >= gridY1 && my < gridY2) {
            for (Card card : cards) {
                int y = (int) (card.y - scroll);
                if (mx >= card.x && mx < card.x + CARD_W && my >= y && my < y + CARD_H) {
                    equip(card);
                    return true;
                }
            }
        }
        return false;
    }

    private void equip(Card card) {
        Loadout mine = Wardrobe.mine();
        if (card.cosmetic == null) {
            Wardrobe.setMine(mine.with(card.slot, null));
            Ui.play(SoundEvents.CAT_AMBIENT, 1.0f, 0.2f);
            return;
        }
        Slot slot = card.cosmetic.slot;
        boolean wearing = card.cosmetic.id.equals(mine.get(slot));
        Wardrobe.setMine(mine.with(slot, wearing ? null : card.cosmetic.id));
        if (!wearing) {
            Ui.play(slot == Slot.PET ? SoundEvents.CAT_PURREOW : SoundEvents.CAT_AMBIENT,
                    1.3f + Ui.rand() * 0.3f, 0.45f);
            petals.burst(card.x + CARD_W / 2f, card.y - scroll + CARD_H / 2f, 12);
        } else {
            Ui.play(SoundEvents.CAT_AMBIENT, 0.9f, 0.25f);
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            yaw -= (float) dx * 1.6f;
            pitch = Mth.clamp(pitch + (float) dy * 0.8f, -25, 35);
            yawVel = -(float) dx * 40;
            lastInteract = t;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= rightX1 && mouseY >= gridY1 && mouseY < gridY2) {
            scrollTarget -= (float) vertical * 30;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    /** dev harness only: switch tab as if clicked */
    public static void devTab(int i) {
        if (Minecraft.getInstance().screen instanceof WardrobeScreen w) {
            w.tab = lastTab = Tab.values()[i];
            w.tabAnimTarget = i;
            w.layoutTab();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
