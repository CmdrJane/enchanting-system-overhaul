package aiefu.eso.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class EnchantmentListWidget extends AbstractScrollWidget {

    public List<EnchButtonWithData> enchantments;
    protected boolean overlayActive = false;

    public EnchantmentListWidget(int x, int y, int width, int height, Component message, List<EnchButtonWithData> enchantments) {
        super(x, y, width, height, message);
        this.enchantments = enchantments;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(this.withinContentAreaPoint(mouseX, mouseY)){
            this.enchantments.forEach(b -> b.mouseClicked(mouseX, mouseY + scrollAmount(), button));
        } else this.setFocused(false);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected int getInnerHeight() {
        return this.enchantments.size() * 16;
    }

    @Override
    protected boolean scrollbarVisible() {
        return this.getInnerHeight() > this.getHeight();
    }

    @Override
    protected double scrollRate() {
        return 16.0D;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (this.visible) {
            this.renderBackground(poseStack);
            enableScissor(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1);
            poseStack.pushPose();
            poseStack.translate(0.0, -this.scrollAmount(), 0.0);
            this.renderContents(poseStack, mouseX, mouseY, partialTick);
            poseStack.popPose();
            disableScissor();
            this.renderDecorations(poseStack);
            if(withinContentAreaPoint(mouseX, mouseY) && !overlayActive){
                for (EnchButtonWithData b : enchantments){
                    if(b.isHoveredOrFocused()){
                        b.renderToolTip(poseStack, mouseX, mouseY);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void renderContents(PoseStack stack, int mouseX, int mouseY, float partialTick) {
        for (EnchButtonWithData b : enchantments){
            b.render(stack, mouseX, (int) (mouseY + scrollAmount()), partialTick);
        }
    }

    protected void renderBackground(PoseStack stack) {
        this.renderBorder(stack, this.x, this.y, this.getWidth(), this.getHeight());
    }

    protected void renderBorder(PoseStack stack, int x, int y, int width, int height) {
        int i = this.isFocused() ? -1 : -6250336;
        fill(stack, x, y, x + width, y + height, -3159622);
        //guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, -16777216);
    }

    @Override
    protected void renderDecorations(PoseStack stack) {
        if (this.scrollbarVisible()) {
            this.renderScrollBar(stack);
        }

    }
    protected void renderScrollBar(PoseStack stack) {
        int i = this.getScrollBarHeight();
        int minX = this.x + this.width;
        int maxX = this.x + this.width + 8;
        int minY = Math.max(this.y, (int)this.scrollAmount() * (this.height - i) / this.getMaxScrollAmount() + this.y);
        int maxY = minY + i;
        fill(stack, minX, minY, maxX, maxY, -345617);
        fill(stack, minX + 1, minY + 1, maxX - 1, maxY -1, -4814674); //-8241880 . -11789813 //og -8355712 . -4144960 //purple -4814674 . -345617
    }

    public void resetScrollAmount(){
        this.setScrollAmount(0);
    }

    public int getScrollBarHeight() {
        return Mth.clamp((int)((float)(this.height * this.height) / (float)this.getContentHeight()), 32, this.height);
    }

    public int getContentHeight() {
        return this.getInnerHeight() + 4;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }

    public void switchButtonsState(boolean bl){
        this.enchantments.forEach(b -> b.active = bl);
    }

    public void setOverlayActive(boolean bl){
        this.overlayActive = bl;
    }

    public List<EnchButtonWithData> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(List<EnchButtonWithData> enchantments) {
        this.enchantments = enchantments;
        this.resetScrollAmount();
    }
}
