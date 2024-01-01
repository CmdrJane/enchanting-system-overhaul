package aiefu.enchantmentoverhaul.client.gui;

import aiefu.enchantmentoverhaul.ConfigurationFile;
import aiefu.enchantmentoverhaul.EnchantmentOverhaul;
import aiefu.enchantmentoverhaul.OverhauledEnchantmentMenu;
import aiefu.enchantmentoverhaul.RecipeHolder;
import aiefu.enchantmentoverhaul.client.EnchantmentOverhaulClient;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class EnchantingTableScreen extends AbstractContainerScreen<OverhauledEnchantmentMenu> {
    public static final ResourceLocation ENCHANTING_BACKGROUND_TEXTURE = new ResourceLocation(EnchantmentOverhaul.MOD_ID,"textures/gui/ench_screen.png");

    public static final Style STYLE = Style.EMPTY.withColor(TextColor.fromRgb(5636095));

    public static final List<FormattedCharSequence> emptyMsg = Minecraft.getInstance().font.split(Component.translatable("enchantmentoverhaul.enchantmentsempty"), 110);

    protected EnchantmentListWidget enchantmentsScrollList;
    protected EditBox searchFilter;

    protected CustomEnchantingButton confirmButton;
    protected CustomEnchantingButton cancelButton;
    protected Enchantment selectedEnchantment;
    protected List<FormattedCharSequence> confirmMsg = new ArrayList<>();
    protected OverhauledEnchantmentMenu enchMenu;

    protected MutableComponent displayMsg;

    protected boolean overlayActive = false;

    protected boolean firstInit = true;
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
        this.confirmButton = this.addWidget(new CustomEnchantingButton(leftPos + 60, topPos + 92, 30, 12, Component.translatable("gui.yes"), button -> {
            this.switchOverlayState(true);
            this.switchButtonsState(false);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(Objects.requireNonNull(BuiltInRegistries.ENCHANTMENT.getKey(selectedEnchantment)).toString());
            ClientPlayNetworking.send(EnchantmentOverhaul.c2s_enchant_item, buf);
        }));
        this.cancelButton = this.addWidget(new CustomEnchantingButton(leftPos + 130, topPos + 92, 30, 12, Component.translatable("gui.no"), button -> {
            this.switchOverlayState(true);
            this.recalculateAvailability(this.enchMenu.getTableInv());
        }));

        this.confirmButton.active = overlayActive;
        this.confirmButton.visible = overlayActive;
        this.cancelButton.active = overlayActive;
        this.cancelButton.visible = overlayActive;
        Component searchHint = Component.translatable("enchantmentoverhaul.search");
        this.searchFilter = this.addWidget(new EditBox(this.font, leftPos + 81, topPos + 9, 123, 10, searchHint));
        this.searchFilter.setBordered(false);
        this.searchFilter.setHint(searchHint);
        List<EnchButtonWithData> list = this.firstInit ? new ArrayList<>() : this.craftEnchantmentsButtons(this.searchFilter.getValue());
        this.firstInit = false;
        this.enchantmentsScrollList = this.addRenderableWidget(new EnchantmentListWidget(this.leftPos + 79, this.topPos + 24, 125 , 48, Component.literal(""), list));
        this.setInitialFocus(enchantmentsScrollList);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        this.switchOverlayState(!overlayActive);
        this.recalculateAvailability(this.enchMenu.getTableInv());
    }

    public void switchOverlayState(boolean bl){ //False to activate overlay, true to disable
        if(selectedEnchantment != null && !bl){
            MutableComponent msg = Component.translatable("enchantmentoverhaul.applyench.1");
            msg.append(Component.translatable(this.selectedEnchantment.getDescriptionId()).withStyle(STYLE));
            msg.append(Component.translatable("enchantmentoverhaul.applyench.2"));
            msg.append(((MutableComponent)this.enchMenu.getTableInv().getItem(0).getDisplayName()).withStyle(STYLE));
            this.confirmMsg = font.split(msg, 190);
        }
        this.searchFilter.active = bl;
        this.enchantmentsScrollList.active = bl;
        this.overlayActive = !bl;
        this.confirmButton.active = !bl;
        this.confirmButton.visible = !bl;
        this.cancelButton.active = !bl;
        this.cancelButton.visible = !bl;
    }

    public void switchButtonsState(boolean bl){
        this.enchantmentsScrollList.switchOverlayState(bl);
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
        if(displayMsg != null){
            int x = leftPos + 79;
            this.drawCenteredString(guiGraphics, this.font, displayMsg, x + 124 / 2, topPos + 75, 4210752, false);
        }
        if(enchantmentsScrollList.getEnchantments().isEmpty()){
            int i = 0;
            int h = (48 - (font.lineHeight * emptyMsg.size() + (emptyMsg.size() - 1) * 7)) / 2;
            for (FormattedCharSequence cs : emptyMsg){
                this.drawCenteredString(guiGraphics, this.font, cs,leftPos + 79 + 124 / 2, topPos + 25 + h + 14 * i, 4210752, false);
                i++;
            }
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 1000.0F);
        this.renderConfirmOverlay(guiGraphics, mouseX, mouseY, partialTick);
        this.confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();
    }

    public void renderConfirmOverlay(GuiGraphics graphics, int mx, int my, float pt){
        if(overlayActive){
            graphics.blitNineSliced(ENCHANTING_BACKGROUND_TEXTURE, leftPos + 10, topPos + 48, 200, 60, 20, 20, 140, 60, 0, 196);
            int h = (50 - (font.lineHeight * this.confirmMsg.size() + (this.confirmMsg.size() - 1) * 7)) / 2;
            for (int i = 0; i < this.confirmMsg.size(); i++) {
                this.drawCenteredString(graphics, font, this.confirmMsg.get(i), leftPos + 109, topPos + 48 + h + 14 * i,4210752, false);
            }
        }
    }

    protected void drawCenteredString(GuiGraphics graphics, Font font, Component text, int x, int y, int color, boolean dropShadow){
        graphics.drawString(font, text, x - font.width(text) / 2, y, color, dropShadow);
    }

    protected void drawCenteredString(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color, boolean dropShadow){
        graphics.drawString(font, text, x - font.width(text) / 2, y, color, dropShadow);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(this.searchFilter.isFocused()){
            if(keyCode == 256) this.searchFilter.setFocused(false);
            else if(keyCode == 257){
                this.enchantmentsScrollList.setEnchantments(this.craftEnchantmentsButtons(this.searchFilter.getValue()));
                this.recalculateAvailability(this.enchMenu.getTableInv());
            }
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

    public List<EnchButtonWithData> craftEnchantmentsButtons(String filter){
        List<EnchButtonWithData> list = new ArrayList<>();
        ItemStack stack = this.enchMenu.getTableInv().getItem(0);
        boolean bl = stack.is(Items.BOOK);
        Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
        HashSet<Enchantment> el = stack.isEmpty() || bl ? enchMenu.enchantments : this.filterToNewSet(enchMenu.enchantments, enchantment -> enchantment.canEnchant(stack));
        HashSet<Enchantment> curses = this.filterToNewSet(enchs.keySet(), Enchantment::isCurse);
        int ec = this.getCurrentEnchantmentsCount(enchs.size(), curses.size());
        int ml = this.getEnchantmentsLimit(curses.size());
        if(stack.isEmpty()){
            this.displayMsg = null;
        } else {
            int i = Math.max(ml - ec, 0);
            this.displayMsg = Component.translatable("enchantmentoverhaul.enchantmentsleft", i);
        }

        ConfigurationFile cfg = EnchantmentOverhaul.config;

        HashSet<Enchantment> applicableCurses = bl ? enchMenu.curses : this.filterToNewSet(enchMenu.curses, e -> e.canEnchant(stack));
        if(ec >= ml){
            el = new HashSet<>(enchs.keySet());
            if(cfg.enableCursesAmplifier){
                if(curses.size() < cfg.maxCurses){
                    el.addAll(applicableCurses);
                } else {
                    for (Enchantment c : curses){
                        if(c.getMaxLevel() > 1){
                            el.add(c);
                        }
                    }
                }
            }
        } else if(cfg.enableCursesAmplifier && curses.size() < cfg.maxCurses){
            el.addAll(applicableCurses);
        }
        int offset = 0;

        for (Enchantment enchantment : el) {
            String name = I18n.get(enchantment.getDescriptionId());
            if(filter.isEmpty() || filter.isBlank() || name.toLowerCase().contains(filter.toLowerCase())){
                @Nullable RecipeHolder holder = EnchantmentOverhaul.recipeMap.get(BuiltInRegistries.ENCHANTMENT.getKey(enchantment));
                Integer l = enchs.get(enchantment);
                int maxLevel = RecipeHolder.getMaxLevel(holder, enchantment);
                int targetLevel = l != null ? Math.min(maxLevel, l + 1) : 1;

                MutableComponent translatable = RecipeHolder.getFullName(enchantment, targetLevel, maxLevel);
                EnchButtonWithData b = new EnchButtonWithData(leftPos + 80, (this.topPos + 25) + 16 * offset, 123, 14, translatable, button -> {
                    this.selectedEnchantment = enchantment;
                    this.switchOverlayState(false);
                    this.switchButtonsState(false);
                }, holder, enchantment);

                MutableComponent c = translatable.copy();
                c.withStyle(ChatFormatting.AQUA);
                c.append("\n");
                c.append(EnchantmentOverhaulClient.getEnchantmentDescription(enchantment));
                if(holder != null){
                    c.append("\n");
                    c.append(Component.translatable("enchantmentoverhaul.requires").withStyle(ChatFormatting.GRAY));
                    for (RecipeHolder.ItemData data : holder.levels.get(targetLevel)) {
                        MutableComponent itemName;
                        if(data.isEmpty()){
                            itemName = Component.translatable("enchantmentoverhaul.emptyitem").withStyle(ChatFormatting.DARK_GRAY);
                        } else {
                            if(data.compoundTag != null){
                                ItemStack refStack = new ItemStack(data.item, 1);
                                refStack.setTag(data.compoundTag);
                                itemName = Component.translatable(refStack.getDescriptionId());
                            } else itemName = Component.translatable(data.item.getDescriptionId());
                            itemName.append(": ").append(Component.literal(String.valueOf(data.amount)).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GOLD);
                        }
                        c.append("\n");
                        c.append(itemName);

                    }
                }
                b.setTooltip(Tooltip.create(c));
                list.add(b);
                offset++;
            }
        }

        return list;
    }

    public HashSet<Enchantment> filterToNewSet(Set<Enchantment> set , Predicate<Enchantment> predicate){
        HashSet<Enchantment> enchs = new HashSet<>();
        for (Enchantment e : set){
            if(predicate.test(e)){
                enchs.add(e);
            }
        }
        return enchs;
    }

    public int getEnchantmentsLimit(int curses){
        ConfigurationFile cfg = EnchantmentOverhaul.config;
        return cfg.enableCursesAmplifier ? cfg.maxEnchantments + Math.min(curses, cfg.maxCurses) * cfg.enchantmentLimitIncreasePerCurse : cfg.maxEnchantments;
    }

    public int getCurrentEnchantmentsCount(int appliedEnchantments, int curses){
        ConfigurationFile cfg = EnchantmentOverhaul.config;
        return cfg.enableCursesAmplifier ? appliedEnchantments - curses : appliedEnchantments;
    }

    public void updateButtons(){
        this.enchantmentsScrollList.resetScrollAmount();
        this.enchantmentsScrollList.setEnchantments(craftEnchantmentsButtons(this.getFilterString()));
    }

    public String getFilterString(){
        return this.searchFilter.getValue();
    }

    public void recalculateAvailability(SimpleContainer container){
        ItemStack stack = container.getItem(0);
        Player player = Minecraft.getInstance().player;
        if(!stack.isEmpty() && stack.getItem().isEnchantable(stack)){
            Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
            label1:
            for (EnchButtonWithData b : this.enchantmentsScrollList.getEnchantments()) {
                if(!stack.is(Items.BOOK)) {
                    for (Enchantment e : enchs.keySet()) {
                        if (e != b.enchantment && !e.isCompatibleWith(b.enchantment)) {
                            b.active = false;
                            continue label1;
                        }
                    }
                }

                Integer targetLevel = enchs.get(b.getEnchantment());
                targetLevel = targetLevel == null ? 1 : targetLevel + 1;
                RecipeHolder holder = b.getRecipe();
                if (holder != null) {
                    b.active = targetLevel <= holder.getMaxLevel(b.getEnchantment()) && (player.getAbilities().instabuild || holder.check(container, targetLevel));
                } else b.active = player.getAbilities().instabuild && targetLevel <= b.getEnchantment().getMaxLevel();
            }
        } else this.enchantmentsScrollList.enchantments.forEach(b -> b.active = false);
    }

    public EnchantmentListWidget getEnchantmentsScrollList() {
        return enchantmentsScrollList;
    }
}
