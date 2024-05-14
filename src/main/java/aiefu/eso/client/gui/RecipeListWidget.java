package aiefu.eso.client.gui;

import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.itemdata.ItemDataPrepared;
import aiefu.eso.data.itemdata.RecipeViewerData;
import aiefu.eso.data.itemdata.RecipeViewerItemData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
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
    protected boolean scrollbarVisible() {
        return this.getInnerHeight() > this.getHeight();
    }

    @Override
    protected double scrollRate() {
        return 20.0D;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (this.visible) {
            enableScissor(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1);
            poseStack.pushPose();
            poseStack.translate(0.0, -this.scrollAmount(), 0.0);
            this.renderContents(poseStack, mouseX, mouseY, partialTick);
            poseStack.popPose();
            disableScissor();
            this.renderDecorations(poseStack);
        }
    }

    @Override
    protected void renderContents(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int yOffset = this.y + 12;
        List<RecipeViewerData> list = this.data;
        ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
        ir.blitOffset = 100.0F;
        for (RecipeViewerData d : list){
            ItemDataPrepared[] itd = d.getItemData();
            int xp = d.getXp();
            if(itd.length < 1 && xp < 1) continue;
            int wOffset = this.x + 2;
            Font font = screen.getFont();
            font.draw(poseStack, d.getDesc(), wOffset + 2, yOffset - 9,4210752);

            for (RecipeViewerItemData i : d.getCachedStacks()){
                ItemStack stack = i.isAnimated() ? i.getNextStack() : i.getStack();
                if(withinContentAreaPoint(mouseX, mouseY) && currentItemIsHovered(yOffset, wOffset, mouseX, mouseY)){
                    this.setTooltipForNextPass(stack, font, poseStack, mouseX, mouseY);
                }
                ir.renderGuiItem(stack, wOffset, (int) (yOffset - scrollAmount()));
                ir.renderGuiItemDecorations(font, stack, wOffset, (int) (yOffset - scrollAmount()));
                wOffset += 20;
            }
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, EnchantingTableScreen.ENCHANTING_BACKGROUND_TEXTURE);
            RenderSystem.enableDepthTest();
            blit(poseStack, wOffset, yOffset + 2, 210, 197, 16, 14);
            wOffset +=20;
            ItemStack resultStack = d.getResultStack();
            if(withinContentAreaPoint(mouseX, mouseY) && currentItemIsHovered(yOffset, wOffset, mouseX, mouseY)){
                this.setTooltipForNextPass(resultStack, font, poseStack, mouseX, mouseY);
            }
            ir.renderGuiItem(resultStack, wOffset, (int) (yOffset - scrollAmount()));
            ir.renderGuiItemDecorations(font, resultStack, wOffset, (int) (yOffset - scrollAmount()));
            yOffset += 32;
        }
        ir.blitOffset = 0;
    }

    public void setTooltipForNextPass(ItemStack stack, Font font, PoseStack ps, int x, int y){
        List<FormattedCharSequence> fcsl = new ArrayList<>();
        for (Component c : screen.getTooltipFromItem(stack)){
            fcsl.addAll(font.split(c, 200));
        }
        this.screen.setTooltipForNextRenderPass(fcsl);
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

    private int getScrollBarHeight() {
        return Mth.clamp((int)((float)(this.height * this.height) / (float)this.getContentHeight()), 32, this.height);
    }

    private int getContentHeight() {
        return this.getInnerHeight() + 4;
    }

    public void tick(){
        if(this.tickCount % 60 == 0){
            this.data.forEach(recipeViewerData -> {
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
            map.put(lvl, new RecipeViewerData(set.getValue(), lvl, enchantment, this.recipe.mode));
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
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
    }
}
