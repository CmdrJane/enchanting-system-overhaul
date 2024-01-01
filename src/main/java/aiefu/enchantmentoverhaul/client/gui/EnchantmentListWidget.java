package aiefu.enchantmentoverhaul.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.Button;
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
    protected double scrollRate() {
        return 16.0D;
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if(!withinContentAreaPoint(mouseX, mouseY) || overlayActive){
            mouseX = -1;
            mouseY = -1;
        }
        for (Button b : enchantments){
            b.render(guiGraphics, mouseX, (int) (mouseY + scrollAmount()), partialTick);
        }
    }

    protected void renderBackground(GuiGraphics guiGraphics) {
        this.renderBorder(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    protected void renderBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int i = this.isFocused() ? -1 : -6250336;
        guiGraphics.fill(x, y, x + width, y + height, -3159622);
        //guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, -16777216);
    }

    @Override
    protected void renderDecorations(GuiGraphics guiGraphics) {
        if (this.scrollbarVisible()) {
            this.renderScrollBar(guiGraphics);
        }

    }
    protected void renderScrollBar(GuiGraphics guiGraphics) {
        int i = this.getScrollBarHeight();
        int minX = this.getX() + this.width;
        int maxX = this.getX() + this.width + 8;
        int minY = Math.max(this.getY(), (int)this.scrollAmount() * (this.height - i) / this.getMaxScrollAmount() + this.getY());
        int maxY = minY + i;
        guiGraphics.fill(minX, minY, maxX, maxY, -345617);
        guiGraphics.fill(minX + 1, minY + 1, maxX - 1, maxY -1, -4814674); //-8241880 . -11789813 //og -8355712 . -4144960 //purple -4814674 . -345617
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
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public void switchOverlayState(boolean bl){
        this.enchantments.forEach(b -> b.active = bl);
    }

    public List<EnchButtonWithData> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(List<EnchButtonWithData> enchantments) {
        this.enchantments = enchantments;
    }
}
