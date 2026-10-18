package com.trevorschoeny.headcount.hud;

import com.trevlar.menukit.api.element.AbstractPanelElement;
import com.trevlar.menukit.api.element.RenderContext;

import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * A row of vanilla HUD icons, the way the hearts and armor bars draw: 9px icons 8px
 * apart, ten to a row, each icon a stack of sprites (a heart's container under its
 * fill). Hidden while it has nothing to show.
 */
final class SpriteRow extends AbstractPanelElement {

    private static final int SIZE = 9, STEP = 8, PER_ROW = 10, ROW_STEP = 10;

    private final IntSupplier count;
    private final IntFunction<Identifier[]> layers;
    private final BooleanSupplier shown;

    /**
     * @param count  how many icons
     * @param layers the sprites for icon i, bottom first
     * @param shown  whether the row shows at all
     */
    SpriteRow(IntSupplier count, IntFunction<Identifier[]> layers, BooleanSupplier shown) {
        this.count = count;
        this.layers = layers;
        this.shown = shown;
    }

    @Override public boolean isVisible() { return shown.getAsBoolean() && count.getAsInt() > 0; }
    @Override public int getWidth() { return Math.min(count.getAsInt(), PER_ROW) * STEP + (SIZE - STEP); }
    @Override public int getHeight() { return rows() * ROW_STEP - (ROW_STEP - SIZE); }
    @Override public int naturalWidth() { return getWidth(); }
    @Override public void layoutWithin(int budget) {}
    @Override public void fillWidth(int width) {}

    private int rows() { return (count.getAsInt() + PER_ROW - 1) / PER_ROW; }

    @Override
    public void render(RenderContext ctx) {
        int n = count.getAsInt();
        for (int i = 0; i < n; i++) {
            int x = ctx.originX() + childX + (i % PER_ROW) * STEP;
            int y = ctx.originY() + childY + (i / PER_ROW) * ROW_STEP;
            for (Identifier sprite : layers.apply(i)) {
                if (sprite != null) ctx.graphics().blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, SIZE, SIZE);
            }
        }
    }
}
