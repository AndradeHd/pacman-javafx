/*
Copyright (c) 2021-2026 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.pacmanfx.arcade.pacman.rendering;

import de.amr.pacmanfx.lib.RectShort;
import de.amr.pacmanfx.model.actors.Actor;
import de.amr.pacmanfx.model.actors.Bonus;
import de.amr.pacmanfx.model.actors.Ghost;
import de.amr.pacmanfx.model.actors.Pac;
import de.amr.pacmanfx.uilib.assets.SpriteSheet;
import de.amr.pacmanfx.uilib.rendering.ActorRenderer;
import de.amr.pacmanfx.uilib.rendering.BaseSpriteRenderer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import org.tinylog.Logger;

import static java.util.Objects.requireNonNull;

public class ArcadePacMan_Actor_Renderer extends BaseSpriteRenderer implements ActorRenderer {

    private static final long GHOST_FLASH_MS = 300;

    public ArcadePacMan_Actor_Renderer(Canvas canvas, SpriteSheet<?> spriteSheet) {
        super(canvas, spriteSheet);
    }

    @Override
    public ArcadePacMan_SpriteSheet spriteSheet() {
        return (ArcadePacMan_SpriteSheet) super.spriteSheet();
    }

    @Override
    public void drawActor(Actor actor) {
        requireNonNull(actor);
        if (!actor.isVisible()) return;

        GraphicsContext gc = ctx();  // <-- AQUI ESTÁ A CORREÇÃO

        boolean isGhost = actor instanceof Ghost;
        boolean isPac = actor instanceof Pac;

        // semi transparência fantasma
        if (isGhost) {
            gc.save();
            gc.setGlobalAlpha(0.5);
        }

        // Pac-Man azul
        if (isPac) {
            gc.save();
            ColorAdjust adj = new ColorAdjust();
            adj.setHue(0.55);
            adj.setSaturation(0.5);
            adj.setBrightness(0.1);
            gc.setEffect(adj);
        }

        if (actor instanceof Bonus bonus) {
            drawBonus(bonus);
        }
        else if (isGhost) {
            long t = System.currentTimeMillis();
            boolean flashOn = (t / GHOST_FLASH_MS) % 2 == 0;

            actor.optAnimationManager()
                .map(am -> am.currentSprite(actor))
                .ifPresentOrElse(
                    sprite -> {
                        if (flashOn) {
                            RectShort[] flashing = spriteSheet().spriteSequence(SpriteID.GHOST_FLASHING);
                            if (flashing != null && flashing.length > 0) {
                                int idx = (int) ((t / 100) % flashing.length);
                                drawSpriteCentered(actor.center(), flashing[idx]);
                            } else {
                                drawSpriteCentered(actor.center(), sprite);
                            }
                        } else {
                            drawSpriteCentered(actor.center(), sprite);
                        }
                    },
                    () -> {
                        RectShort[] normal = spriteSheet().spriteSequence(SpriteID.RED_GHOST_LEFT);
                        if (normal != null && normal.length > 0) {
                            drawSpriteCentered(actor.center(), normal[0]);
                        }
                    }
                );
        }
        else {
            actor.optAnimationManager()
                .map(am -> am.currentSprite(actor))
                .ifPresent(sprite -> drawSpriteCentered(actor.center(), sprite));
        }

        if (isPac) {
            gc.setEffect(null);
            gc.restore();
        }
        if (isGhost) {
            gc.setGlobalAlpha(1.0);
            gc.restore();
        }
    }

    private void drawBonus(Bonus bonus) {
        switch (bonus.state()) {
            case EDIBLE ->
                drawBonusSprite(bonus, spriteSheet().spriteSequence(SpriteID.BONUS_SYMBOLS), bonus.symbol());
            case EATEN ->
                drawBonusSprite(bonus, spriteSheet().spriteSequence(SpriteID.BONUS_VALUES), bonus.symbol());
            case INACTIVE -> {}
        }
    }

    private void drawBonusSprite(Bonus bonus, RectShort[] sprites, int index) {
        if (0 <= index && index < sprites.length) {
            drawSpriteCentered(bonus.center(), sprites[index]);
        } else {
            Logger.error("Cannot render bonus with symbol code {}", index);
        }
    }
}
