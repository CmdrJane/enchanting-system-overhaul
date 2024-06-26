package aiefu.eso.client.gui;

import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.itemdata.ItemDataPrepared;
import aiefu.eso.data.itemdata.RecipeViewerData;
import aiefu.eso.data.itemdata.RecipeViewerItemData;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RecipeListWidget extends AbstractScrollWidget {
    protected RecipeHolder recipe;

    protected List<RecipeViewerData> data;
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
        int yOffset = this.getY() + 12;
        List<RecipeViewerData> list = this.data;
        for (RecipeViewerData d : list){
            ItemDataPrepared[] itd = d.getItemData();
            if(itd.length < 1 && d.getXp() < 1){
                continue;
            }
            int wOffset = this.getX() + 2;
            Font font = screen.getFont();
            guiGraphics.drawString(font, d.getDesc(), wOffset + 2, yOffset - 9,4210752, false);
            if(itd.length > 0){
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
                yOffset += 20;
            }
            yOffset += 12;
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
            for (RecipeViewerData recipeViewerData : this.data){
                for (RecipeViewerItemData d : recipeViewerData.getCachedStacks()){
                    d.next();
                }
            }
        }
        this.tickCount++;
    }

    protected boolean currentItemIsHovered(int yOffset, int wOffset, int mouseX, int mouseY){
        int k = (int) (mouseY + scrollAmount());
        return mouseX > wOffset && mouseX < wOffset + 17 && k > yOffset && k < yOffset + 17;
    }

    public void updateRecipes(RecipeHolder holder, Enchantment enchantment){
        Objects.requireNonNull(holder);
        this.recipe = holder;
        this.prepareData(enchantment);
        this.innerHeight = this.data.size() * 32;
        this.setScrollAmount(0.0D);
    }

    protected void prepareData(Enchantment enchantment){
        Int2ObjectOpenHashMap<RecipeViewerData> map = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<ItemDataPrepared[]> set : this.recipe.levels.int2ObjectEntrySet()){
            int lvl = set.getIntKey();
            if(set.getValue() != null){
                map.put(lvl, new RecipeViewerData(set.getValue(), lvl, enchantment, this.recipe.mode));
            }
        }
        for (Int2IntMap.Entry set : this.recipe.xpMap.int2IntEntrySet()){
            int lvl = set.getIntKey();
            RecipeViewerData data = map.get(lvl);
            if(data != null){
                data.setXp(set.getIntValue());
            } else map.put(lvl, new RecipeViewerData(set.getIntValue(), lvl, enchantment, this.recipe.mode));
        }
        List<Integer> keyset = new ArrayList<>(map.keySet());
        List<RecipeViewerData> sortedData = new ArrayList<>();
        Collections.sort(keyset);
        for (int k : keyset){
            sortedData.add(map.get(k));
        }
        this.data = sortedData;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
