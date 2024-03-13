package aiefu.eso.client.gui;

import aiefu.eso.ESOCommon;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Objects;

public class CustomEnchantingButton extends Button {
    public static final ResourceLocation ench_buttons = new ResourceLocation(ESOCommon.MOD_ID, "textures/gui/ench_buttons.png");

    protected TriConsumer<PoseStack, Integer, Integer> tooltip;
    public CustomEnchantingButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress);
    }

    @Override
    public void renderButton(PoseStack poseStack, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, ench_buttons);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        int k = this.active ? 4210752 : 10526880;
        blit4Sliced(poseStack, this.x, this.y, this.getWidth(), this.getHeight(), getTextureY(), 200, 20, 256, 256);
        this.drawCenteredStringNoShadow(poseStack, minecraft.font, this.getMessage(), k | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    public static void blit4Sliced(PoseStack stack, int x, int y, int w, int h, int yOffset, int activeZoneW, int activeZoneH, int textureW, int textureH){
        blit(stack, x, y, 0, yOffset, w / 2, h / 2, textureW, textureH);
        blit(stack, x, y + h / 2, 0, (activeZoneH - (float) h / 2) + yOffset, w / 2, h / 2, textureW, textureH);
        blit(stack, x + w / 2, y, activeZoneW - (float) w / 2, yOffset, (int)Math.ceil(w / 2.0D), h / 2, textureW, textureH);
        blit(stack, x + w / 2, y + h / 2, activeZoneW - (float) w / 2, (activeZoneH - (float) h / 2) + yOffset, (int) Math.ceil(w / 2.0D), h / 2 , textureW, textureH);
    }

    public void drawCenteredStringNoShadow(PoseStack stack, Font font, Component text, int color){
        Objects.requireNonNull(font);

        int l = font.width(text);
        if(l > this.width - 5){
            String s = Language.getInstance().getOrDefault(text.getString());
            int length = s.length();
            text = Component.literal(s.substring(0, Math.min(14, length)) + "...");
        }
        drawStringNoShadow(stack, font, text, this.x + this.width / 2, this.y + this.height / 4, color);
    }

    @Override
    public void renderToolTip(PoseStack poseStack, int i, int j) {
        if(tooltip != null){
            this.tooltip.accept(poseStack, i, j);
        }
    }

    protected void drawStringNoShadow(PoseStack poseStack, Font font, Component component, int x, int y, int k) {
        FormattedCharSequence formattedCharSequence = component.getVisualOrderText();
        font.draw(poseStack, formattedCharSequence, (float)(x - font.width(formattedCharSequence) / 2), (float)y, k);
    }

    public void setTooltip(TriConsumer<PoseStack, Integer, Integer> tooltip){
        this.tooltip = tooltip;
    }


    public int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused()) { //W148 H197
            i = 2;
        }

        return i * 20;
    }

}
