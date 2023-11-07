package aiefu.enchantmentoverhaul.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.Objects;

public class CustomEnchantingButton extends Button {
    public CustomEnchantingButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.blitNineSliced(EnchantingTableScreen.ENCHANTING_BACKGROUND_TEXTURE, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 110, 20, 146, this.getTextureY());
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.active ? 4210752 : 10526880;
        this.drawCenteredString(guiGraphics, minecraft.font, this.getMessage(), i | Mth.ceil(this.alpha * 255.0F) << 24);
        //this.renderString(guiGraphics, minecraft.font, i | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    public void drawCenteredString(GuiGraphics graphics, Font font, Component text, int color){
        Objects.requireNonNull(font);

        int l = font.width(text);
        if(l > this.width - 5){
            String s = Language.getInstance().getOrDefault(text.getString());
            int length = s.length();
            text = Component.literal(s.substring(0, Math.min(14, length)) + "...");
        }
        int minX = this.getX() + width;

        int r = (this.getY() + this.getY() + this.getHeight() - 9) / 2 + 1;

        this.drawCenteredString(graphics, font, text, (minX + this.getX()) / 2, r, color, false);
    }

    public void drawCenteredString(GuiGraphics graphics, Font font, Component text, int x, int y, int color, boolean shadow){
        FormattedCharSequence formattedCharSequence = text.getVisualOrderText();
        graphics.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color, shadow);
    }

    public int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused()) { //W148 H197
            i = 2;
        }

        return 196 + i * 20;
    }

}
