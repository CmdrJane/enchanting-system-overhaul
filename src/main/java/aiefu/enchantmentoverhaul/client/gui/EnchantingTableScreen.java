package aiefu.enchantmentoverhaul.client.gui;

import aiefu.enchantmentoverhaul.EnchantmentOverhaul;
import aiefu.enchantmentoverhaul.OverhauledEnchantmentMenu;
import aiefu.enchantmentoverhaul.RecipeHolder;
import aiefu.enchantmentoverhaul.client.EnchantmentOverhaulClient;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnchantingTableScreen extends AbstractContainerScreen<OverhauledEnchantmentMenu> {
    public static final ResourceLocation ENCHANTING_BACKGROUND_TEXTURE = new ResourceLocation(EnchantmentOverhaul.MOD_ID,"textures/gui/ench_screen.png");

    protected EnchantmentListWidget enchantmentsScrollList;
    protected EditBox searchFilter;

    protected CustomEnchantingButton confirmButton;
    protected CustomEnchantingButton cancelButton;
    protected Enchantment selectedEnchantment;
    protected OverhauledEnchantmentMenu enchMenu;

    protected boolean overlayActive = false;
    public EnchantingTableScreen(OverhauledEnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 181;
        this.imageWidth = 218;
        this.inventoryLabelY = this.imageHeight - 94;
        this.enchMenu = menu;
    }

    @Override
    protected void init() {
        super.init();
        this.confirmButton = this.addWidget(new CustomEnchantingButton(leftPos + 60, topPos + 72, 30, 12, Component.translatable("gui.yes"), button -> {
            this.switchOverlayState(true);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(BuiltInRegistries.ENCHANTMENT.getKey(selectedEnchantment).toString());
            ClientPlayNetworking.send(EnchantmentOverhaul.c2s_enchant_item, buf);
        }));
        this.cancelButton = this.addWidget(new CustomEnchantingButton(leftPos + 130, topPos + 72, 30, 12, Component.translatable("gui.no"), button -> {
            this.switchOverlayState(true);
        }));

        this.confirmButton.active = overlayActive;
        this.confirmButton.visible = overlayActive;
        this.cancelButton.active = overlayActive;
        this.cancelButton.visible = overlayActive;

        this.enchantmentsScrollList = this.addRenderableWidget(new EnchantmentListWidget(this.leftPos + 79, this.topPos + 24, 125 , 48, Component.literal(""), this.craftEnchantmentsButtons()));
        this.searchFilter = this.addWidget(new EditBox(this.font, leftPos + 80, topPos + 8, 123, 10, Component.literal("Search...")));
        this.setInitialFocus(enchantmentsScrollList);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        this.switchOverlayState(!overlayActive);
    }

    public void switchOverlayState(boolean bl){ //False to activate overlay, true to disable
        this.searchFilter.active = bl;
        this.enchantmentsScrollList.active = bl;
        this.enchantmentsScrollList.switchOverlayState(bl);
        this.overlayActive = !bl;
        this.confirmButton.active = !bl;
        this.confirmButton.visible = !bl;
        this.cancelButton.active = !bl;
        this.cancelButton.visible = !bl;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if(!overlayActive) {
            super.slotClicked(slot, slotId, mouseButton, type);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.searchFilter.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.searchFilter.render(guiGraphics, mouseX, mouseY, partialTick);
        if(!overlayActive) this.renderTooltip(guiGraphics, mouseX, mouseY);
        int x = leftPos + 79;
        Component text = Component.literal("Enchantments left: 5");
        this.drawCenteredString(guiGraphics, this.font, text, ((x + 123 + x) /2), topPos + 75, 4210752, false);
        this.renderConfirmOverlay(guiGraphics, mouseX, mouseY, partialTick);
        this.confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void renderConfirmOverlay(GuiGraphics graphics, int mx, int my, float pt){
        if(overlayActive){
            graphics.blit(ENCHANTING_BACKGROUND_TEXTURE, leftPos + 40, topPos + 48, 0, 196, 140, 40);
            int x = leftPos + 40;
            this.drawCenteredString(graphics, font, Component.literal("Test String"), (x + 140 + x) / 2, topPos + 58,4210752, false);
        }
    }

    protected void drawCenteredString(GuiGraphics graphics, Font font, Component text, int x, int y, int color, boolean dropShadow){
        graphics.drawString(font, text, x - font.width(text) / 2, y, color, dropShadow);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(this.searchFilter.isFocused()){
            if(keyCode == 256) this.searchFilter.setFocused(false);
           return this.searchFilter.keyPressed(keyCode, scanCode, modifiers);
        }
        else return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if(overlayActive) return false;
        else {
            this.enchantmentsScrollList.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(ENCHANTING_BACKGROUND_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    public List<EnchButtonWithData> craftEnchantmentsButtons(){
        List<EnchButtonWithData> list = new ArrayList<>();
        for (int i = 0; i < enchMenu.enchantments.size(); i++) {
            Enchantment enchantment = enchMenu.enchantments.get(i);
            MutableComponent translatable = Component.translatable(enchantment.getDescriptionId());
            EnchButtonWithData b = new EnchButtonWithData(leftPos + 80, (this.topPos + 25) + 16 * i, 123, 14, translatable, button -> {
                this.switchOverlayState(false);
                this.selectedEnchantment = enchantment;
            }, EnchantmentOverhaul.recipeMap.get(BuiltInRegistries.ENCHANTMENT.getKey(enchantment)), enchantment);
            MutableComponent c = translatable.copy();
            c.append("\n");
            c.append(EnchantmentOverhaulClient.getEnchantmentDescription(enchantment));
            b.setTooltip(Tooltip.create(c));
            list.add(b);
        }

        return list;
    }

    public void recalculateAvailability(SimpleContainer container){
        ItemStack stack = container.getItem(0);
        if(!stack.isEmpty() && stack.getItem().isEnchantable(stack)){
            Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
            if(enchs.keySet().size() < EnchantmentOverhaul.config.getMaxEnchantments()) {
                label1:
                for (EnchButtonWithData b : enchantmentsScrollList.getEnchantments()) {
                    if(!b.enchantment.canEnchant(stack)){
                        continue;
                    }
                    for (Enchantment e : enchs.keySet()) {
                        if (!e.isCompatibleWith(b.enchantment)) {
                            b.active = false;
                            continue label1;
                        }
                    }
                    RecipeHolder holder = b.getRecipe();
                    if (holder != null) {
                        Integer targetLevel = enchs.get(b.getEnchantment());
                        targetLevel = targetLevel == null ? 1 : targetLevel + 1;
                        b.active = targetLevel < holder.getMaxLevel(b.getEnchantment()) && holder.check(container, targetLevel);
                    } else b.active = false;
                }
            } else this.enchantmentsScrollList.enchantments.forEach(b -> b.active = false);
        } else this.enchantmentsScrollList.enchantments.forEach(b -> b.active = false);
    }
}
