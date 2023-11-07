package aiefu.enchantmentoverhaul.client.gui;

import aiefu.enchantmentoverhaul.EnchantmentOverhaul;
import aiefu.enchantmentoverhaul.OverhauledEnchantmentMenu;
import aiefu.enchantmentoverhaul.client.EnchantmentOverhaulClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class EnchantingTableScreen extends AbstractContainerScreen<OverhauledEnchantmentMenu> {
    public static final ResourceLocation ENCHANTING_BACKGROUND_TEXTURE = new ResourceLocation(EnchantmentOverhaul.MOD_ID,"textures/gui/ench_screen.png");

    protected ScrollListWidget enchantmentsScrollList;
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
            //Todo:send enchant packet
            this.switchOverlayState(true);
        }));
        this.cancelButton = this.addWidget(new CustomEnchantingButton(leftPos + 130, topPos + 72, 30, 12, Component.translatable("gui.no"), button -> {
            this.switchOverlayState(true);
        }));

        this.confirmButton.active = overlayActive;
        this.confirmButton.visible = overlayActive;
        this.cancelButton.active = overlayActive;
        this.cancelButton.visible = overlayActive;

        this.enchantmentsScrollList = this.addRenderableWidget(new ScrollListWidget(this.leftPos + 79, this.topPos + 24, 125 , 48, Component.literal(""), this.craftEnchantmentsButtons()));
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
        this.enchantmentsScrollList.switchButtonsState(bl);
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

    public List<Button> craftEnchantmentsButtons(){
        List<Button> list = new ArrayList<>();
        for (int i = 0; i < enchMenu.enchantments.size(); i++) {
            Enchantment enchantment = enchMenu.enchantments.get(i);
            MutableComponent translatable = Component.translatable(enchantment.getDescriptionId());
            CustomEnchantingButton b = new CustomEnchantingButton(leftPos + 80, (this.topPos + 25) + 16 * i, 123, 14, translatable, button -> {
                this.switchOverlayState(false);
                this.selectedEnchantment = enchantment;
            });
            MutableComponent c = translatable.copy();
            c.append("\n");
            c.append(EnchantmentOverhaulClient.getEnchantmentDescription(enchantment));
            b.setTooltip(Tooltip.create(c));
            list.add(b);

            /*
            list.add(CustomEnchantingButton.builder(Component.translatable(s), button -> {
                System.out.println(s);
                System.out.println(FastColor.ARGB32.color(255, 250, 185, 239));
            }).bounds(this.leftPos + 60, (this.topPos + 16) + 16 * i,98, 14).build());
             */
        }

        return list;
    }
}
