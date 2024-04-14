package aiefu.eso.client.gui;

import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.itemdata.ItemDataPrepared;
import aiefu.eso.data.itemdata.RecipeViewerData;
import aiefu.eso.data.itemdata.RecipeViewerItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecipeListWidget extends AbstractScrollWidget {
    protected RecipeHolder recipe;
    protected int innerHeight = 0;

    protected EnchantingTableScreen screen;

    protected int tickCount = 0;

    public RecipeListWidget(int x, int y, int width, int height, Component message, EnchantingTableScreen screen) {
        super(x, y, width, height, message);
        this.screen = screen;
    }

    @Override
    protected int getInnerHeight() {
        return innerHeight;
    }

    @Override
    protected double scrollRate() {
        return 20.0D;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        int yOffset = this.getY() + 12;
        List<RecipeViewerData> list = this.recipe.getRecipeViewerData();
        for (RecipeViewerData d : list){
            ItemDataPrepared[] itd = d.getItemData();
            int xp = d.getXp();
            if(itd.length < 1 && xp < 1) continue;
            int wOffset = this.getX() + 2;
            Font font = screen.getFont();
            guiGraphics.drawString(font, d.getDesc(), wOffset + 2, yOffset - 9,4210752, false);

            for (RecipeViewerItemData i : d.getCachedStacks()){
                ItemStack stack = i.isAnimated() ? i.getNextStack() : i.getStack();
                if(withinContentAreaPoint(mouseX, mouseY) && currentItemIsHovered(yOffset, wOffset, mouseX, mouseY)){
                    this.setTooltipForNextPass(stack, font);
                }
                guiGraphics.renderFakeItem(stack, wOffset, yOffset);
                guiGraphics.renderItemDecorations(font, stack, wOffset, yOffset);
                wOffset += 20;
            }
            guiGraphics.blit(EnchantingTableScreen.ENCHANTING_BACKGROUND_TEXTURE, wOffset, yOffset + 2, 210, 197, 16, 14);
            wOffset +=20;
            ItemStack resultStack = d.getResultStack();
            if(withinContentAreaPoint(mouseX, mouseY) && currentItemIsHovered(yOffset, wOffset, mouseX, mouseY)){
                this.setTooltipForNextPass(resultStack, font);
            }
            guiGraphics.renderFakeItem(resultStack, wOffset, yOffset);
            guiGraphics.renderItemDecorations(font, resultStack, wOffset, yOffset);
            yOffset += 32;
        }
    }

    public void setTooltipForNextPass(ItemStack stack, Font font){
        List<FormattedCharSequence> fcsl = new ArrayList<>();
        for (Component c : Screen.getTooltipFromItem(Minecraft.getInstance(), stack)){
            fcsl.addAll(font.split(c, 200));
        }
        this.screen.setTooltipForNextRenderPass(fcsl);
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {

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

    private int getScrollBarHeight() {
        return Mth.clamp((int)((float)(this.height * this.height) / (float)this.getContentHeight()), 32, this.height);
    }

    private int getContentHeight() {
        return this.getInnerHeight() + 4;
    }

    public void tick(){
        if(this.tickCount % 60 == 0){
            this.recipe.recipeViewerData.forEach(recipeViewerData -> {
                for (RecipeViewerItemData d : recipeViewerData.getCachedStacks()){
                    d.next();
                }
            });
        }
        this.tickCount++;
    }

    protected boolean currentItemIsHovered(int yOffset, int wOffset, int mouseX, int mouseY){
        int k = (int) (mouseY + scrollAmount());
        return mouseX > wOffset && mouseX < wOffset + 17 && k > yOffset && k < yOffset + 17;
    }

    public void updateRecipes(RecipeHolder holder){
        Objects.requireNonNull(holder);
        this.recipe = holder;
        this.innerHeight = holder.mergeAndGet().size() * 32;
        this.setScrollAmount(0.0D);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
