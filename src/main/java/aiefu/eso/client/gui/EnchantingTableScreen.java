package aiefu.eso.client.gui;

import aiefu.eso.ConfigurationFile;
import aiefu.eso.ESOCommon;
import aiefu.eso.Utils;
import aiefu.eso.client.ESOClient;
import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.itemdata.ItemDataPrepared;
import aiefu.eso.data.materialoverrides.MaterialData;
import aiefu.eso.data.materialoverrides.MaterialOverrides;
import aiefu.eso.menu.OverhauledEnchantmentMenu;
import aiefu.eso.network.NetworkManager;
import aiefu.eso.network.packets.EnchantItemData;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiPredicate;

public class EnchantingTableScreen extends AbstractContainerScreen<OverhauledEnchantmentMenu> {
    public static final ResourceLocation ENCHANTING_BACKGROUND_TEXTURE = new ResourceLocation(ESOCommon.MOD_ID,"textures/gui/ench_screen.png");

    public static final ResourceLocation[] EMPTY_LOCATIONS = new ResourceLocation[]{
            new ResourceLocation(ESOCommon.MOD_ID, "textures/gui/empty_slot_sword.png"),
            new ResourceLocation(ESOCommon.MOD_ID, "textures/gui/empty_slot_lapis_lazuli.png"),
            new ResourceLocation(ESOCommon.MOD_ID, "textures/gui/empty_slot_ingot.png")};

    public static final Style STYLE = Style.EMPTY.withColor(TextColor.fromRgb(5636095));

    public static final List<FormattedCharSequence> emptyMsg = Minecraft.getInstance().font.split(Component.translatable("eso.enchantmentsempty"), 110);

    private static final Map<Enchantment, Integer> EMPTY_MAP = Maps.newLinkedHashMap();

    private static final DecimalFormat decimal_formatter = new DecimalFormat("#.##");

    protected EnchantmentListWidget enchantmentsScrollList;

    protected RecipeListWidget recipeViewer;
    protected EditBox searchFilter;

    protected CustomEnchantingButton confirmButton;
    protected CustomEnchantingButton cancelButton;
    protected Enchantment selectedEnchantment;

    protected int ordinal = -1;
    protected List<FormattedCharSequence> confirmMsg = new ArrayList<>();

    protected MutableComponent displayMsg;

    protected final MutableComponent searchHint =  Component.translatable("eso.search");

    protected boolean overlayActive = false;

    protected boolean viewingRecipes = false;

    protected boolean seekRecipe = false;

    protected HashSet<EnchButtonWithData> tickingButtons = new HashSet<>();

    protected List<FormattedCharSequence> recipeViewerTooltip;

    protected int ticks = 0;

    public EnchantingTableScreen(OverhauledEnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 181;
        this.imageWidth = 218;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.confirmButton = this.addWidget(new CustomEnchantingButton(leftPos + 60, topPos + 92, 30, 12, CommonComponents.GUI_YES, button -> {
            this.switchOverlayState(true);
            this.switchButtonsState(false);
            NetworkManager.sendToServer(new EnchantItemData(Objects.requireNonNull(ForgeRegistries.ENCHANTMENTS.getKey(selectedEnchantment)).toString(), ordinal));
        }));
        this.cancelButton = this.addWidget(new CustomEnchantingButton(leftPos + 130, topPos + 92, 30, 12, CommonComponents.GUI_NO, button -> {
            this.switchOverlayState(true);
            this.enchantmentsScrollList.setOverlayActive(false);
            this.recalculateAvailability(this.menu.getTableInv());
        }));

        this.confirmButton.active = overlayActive;
        this.confirmButton.visible = overlayActive;
        this.cancelButton.active = overlayActive;
        this.cancelButton.visible = overlayActive;
        this.searchFilter = this.addWidget(new EditBox(this.font, leftPos + 81, topPos + 9, 123, 10, searchHint));
        this.searchFilter.setBordered(false);
        List<EnchButtonWithData> list = this.craftEnchantmentsButtons(this.searchFilter.getValue());
        this.enchantmentsScrollList = this.addWidget(new EnchantmentListWidget(this.leftPos + 79, this.topPos + 24, 125 , 48, Component.literal(""), list));

        this.viewingRecipes = false;
        this.seekRecipe = false;
        this.recipeViewer = new RecipeListWidget(this.leftPos + 79, this.topPos + 24, 125 , 48, Component.literal(""), this);
        this.setInitialFocus(enchantmentsScrollList);
        this.recalculateAvailability(menu.getTableInv());
        this.menu.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu containerToSend, int dataSlotIndex, ItemStack stack) {
                if(dataSlotIndex == 41){
                    EnchantingTableScreen.this.updateButtons();
                }
                if(dataSlotIndex > 40 && dataSlotIndex < 46){
                    EnchantingTableScreen.this.recalculateAvailability(menu.getTableInv());
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {

            }
        });
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        this.switchOverlayState(!overlayActive);
    }

    public void switchOverlayState(boolean bl){ //False to activate overlay, true to disable
        if(selectedEnchantment != null && !bl){
            MutableComponent msg = Component.translatable("eso.applyench.1",
                    Component.translatable(this.selectedEnchantment.getDescriptionId()).withStyle(STYLE),
                    ((MutableComponent)this.menu.getTableInv().getItem(0).getDisplayName()).withStyle(STYLE));
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
        this.enchantmentsScrollList.switchButtonsState(bl);
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
        if(ticks % 60 == 0){
            Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(this.menu.getTableInv().getItem(0));
            for (EnchButtonWithData b : this.tickingButtons){
                if(b.isHoveredOrFocused()){
                    Enchantment enchantment = b.getEnchantment();

                    RecipeHolder holder = b.getRecipe();
                    Integer l = enchs.get(enchantment);
                    int maxLevel = holder.getMaxLevel(enchantment);
                    int targetLevel = l != null ? Math.min(maxLevel, l + 1) : 1;

                    MutableComponent translatable = RecipeHolder.getFullName(enchantment, targetLevel, maxLevel);
                    this.composeTooltipAndApply(translatable, enchantment, holder, targetLevel, b, false);
                }
            }
        }
        if(viewingRecipes){
            recipeViewer.tick();
        }
        this.ticks++;
    }

    public void setTooltipForNextRenderPass(List<FormattedCharSequence> seq){
        this.recipeViewerTooltip = seq;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.recipeViewerTooltip = null;
        this.renderBackground(poseStack);
        if(viewingRecipes){
            poseStack.pushPose();
            poseStack.translate(0.0, 0.0, 100.0);
            this.recipeViewer.render(poseStack, mouseX, mouseY, partialTick);
            poseStack.popPose();
        }
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.searchFilter.render(poseStack, mouseX, mouseY, partialTick);
        if(!viewingRecipes) this.enchantmentsScrollList.render(poseStack, mouseX, mouseY, partialTick);
        else if(recipeViewerTooltip != null){
            renderTooltip(poseStack, recipeViewerTooltip, mouseX, mouseY);
        }
        if(!overlayActive) this.renderTooltip(poseStack, mouseX, mouseY);
        if(displayMsg != null){
            int x = leftPos + 79;
            this.drawCenteredStringNoShadow(poseStack, this.font, displayMsg, x + 124 / 2, topPos + 75, 4210752);
        }
        if(this.searchFilter.getValue().isEmpty() && !this.searchFilter.isFocused()){
            this.font.drawShadow(poseStack, searchHint, leftPos + 81, topPos + 9, 16777215);
        }
        if(menu.enchantments.isEmpty() && menu.curses.isEmpty()){
            int g = 0;
            int h = (48 - (8 * emptyMsg.size() + (emptyMsg.size() - 1) * 6)) / 2;
            for (FormattedCharSequence cs : emptyMsg){
                this.drawCenteredStringNoShadow(poseStack, this.font, cs,leftPos + 79 + 124 / 2, topPos + 25 + h + 14 * g, 4210752);
                g++;
            }
        }

        if(seekRecipe && !viewingRecipes){
            seekRecipe = false;
            for (EnchButtonWithData e : this.enchantmentsScrollList.enchantments){
                if(e.isHoveredOrFocused()){
                    RecipeHolder holder = e.getRecipe();
                    if(holder != null && !(holder.levels.isEmpty() && holder.xpMap.isEmpty())){
                        this.recipeViewer.updateRecipes(holder, e.getEnchantment());
                        this.recipeViewer.setFocused(true);
                        this.viewingRecipes = true;
                    }
                    break;
                }
            }
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 1000.0F);
        this.renderConfirmOverlay(poseStack, mouseX, mouseY, partialTick);
        this.confirmButton.render(poseStack, mouseX, mouseY, partialTick);
        this.cancelButton.render(poseStack, mouseX, mouseY, partialTick);
        poseStack.popPose();

    }

    public void renderConfirmOverlay(PoseStack stack, int mx, int my, float pt){
        if(overlayActive){
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, ENCHANTING_BACKGROUND_TEXTURE);
            blit(stack, leftPos + 10, topPos + 48, 0,196,200, 60);
            int h = (42 - (8* this.confirmMsg.size() + (this.confirmMsg.size() - 1) * 6)) / 2;
            for (int i = 0; i < this.confirmMsg.size(); i++) {
                this.drawCenteredStringNoShadow(stack, font, this.confirmMsg.get(i), leftPos + 109, topPos + 50 + h + 14 * i,4210752);
            }
        }
    }

    protected void drawCenteredStringNoShadow(PoseStack stack, Font font, Component text, int x, int y, int color){
        font.draw(stack, text, x - (float) font.width(text) / 2, y, color);
    }

    protected void drawCenteredStringNoShadow(PoseStack stack, Font font, FormattedCharSequence text, int x, int y, int color){
        font.draw(stack, text, x - (float) font.width(text) / 2, y, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyMapping key = ESOClient.recipeKey;
        if(this.searchFilter.isFocused()){
            if(keyCode == 256) this.searchFilter.setFocus(false);
            else if(keyCode == 257){
                this.enchantmentsScrollList.setEnchantments(this.craftEnchantmentsButtons(this.searchFilter.getValue()));
                this.recalculateAvailability(this.menu.getTableInv());
            }
           return this.searchFilter.keyPressed(keyCode, scanCode, modifiers);
        }
        else if(viewingRecipes && (key.matches(keyCode, scanCode) || keyCode == 256)){
            this.viewingRecipes = false;
            this.recipeViewer.setFocused(false);
            return true;
        } else if(!overlayActive && !viewingRecipes && key.matches(keyCode, scanCode)){
            this.seekRecipe = true;
            return true;
        }else return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if(overlayActive) return false;
        else {
            if(viewingRecipes){
                this.recipeViewer.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            } else this.enchantmentsScrollList.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(viewingRecipes){
            recipeViewer.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if(viewingRecipes){
            this.recipeViewer.mouseScrolled(mouseX, mouseY, delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(viewingRecipes){
            this.recipeViewer.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float f, int i, int j) {
        int x = (this.width - this.imageWidth) / 2;
        int g = (this.height - this.imageHeight) / 2;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, ENCHANTING_BACKGROUND_TEXTURE);
        blit(poseStack, x, g, 0, 0, this.imageWidth, this.imageHeight);

        int q = 0;
        for (int r = 41; r < 46; r++) {
            Slot s = menu.slots.get(r);
            if(s.getItem().isEmpty()){
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, EMPTY_LOCATIONS[q]);
                blit(poseStack, leftPos + s.x,  topPos + s.y, 0, 0, 16, 16, 16, 16);
            }
            if(q < 2){
                q++;
            }
        }
    }

    public List<EnchButtonWithData> craftEnchantmentsButtons(String filter){
        tickingButtons.clear();
        List<EnchButtonWithData> list = new ArrayList<>();
        ItemStack stack = this.menu.getTableInv().getItem(0);
        boolean stackIsEmpty = stack.isEmpty();
        boolean bl = stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
        Map<Enchantment, Integer> enchs = stackIsEmpty ? EMPTY_MAP : EnchantmentHelper.getEnchantments(stack);
        Object2IntOpenHashMap<Enchantment> availableEnchantments = stackIsEmpty || bl ? menu.enchantments : this.filterToNewSet(menu.enchantments, (enchantment, level) -> enchantment.canEnchant(stack));
        Object2IntOpenHashMap<Enchantment> curses = this.filterToNewSet(enchs, (enchantment, integer) -> enchantment.isCurse());
        MaterialData matData = this.getMatData(stack.getItem());
        int currentEnchantmentsCount = this.getCurrentEnchantmentsCount(enchs.size(), curses.size());
        int enchantmentsLimit = this.getEnchantmentsLimit(curses.size(), matData);
        if(stackIsEmpty){
            this.displayMsg = null;
        } else {
            int i = Math.max(enchantmentsLimit - currentEnchantmentsCount, 0);
            this.displayMsg = Component.translatable("eso.enchantmentsleft", i);
        }

        ConfigurationFile cfg = ESOCommon.config;

        Object2IntOpenHashMap<Enchantment> applicableCurses = bl ? menu.curses : this.filterToNewSet(menu.curses, (e, l) -> e.canEnchant(stack));
        if(currentEnchantmentsCount >= enchantmentsLimit){
            availableEnchantments = new Object2IntOpenHashMap<>(enchs);
            if(cfg.enableCursesAmplifier){
                if(curses.size() < matData.getMaxCurses()){
                    availableEnchantments.putAll(applicableCurses);
                } else {
                    for (Object2IntMap.Entry<Enchantment> c : curses.object2IntEntrySet()){
                        availableEnchantments.put(c.getKey(), c.getIntValue());
                    }
                }
            }
        } else if(cfg.enableCursesAmplifier && curses.size() < matData.getMaxCurses()){
            availableEnchantments.putAll(applicableCurses);
        }
        int offset = 0;
        LocalPlayer player = Minecraft.getInstance().player;

        for (Object2IntMap.Entry<Enchantment> entry : availableEnchantments.object2IntEntrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getIntValue();
            String name = I18n.get(enchantment.getDescriptionId());
            if(filter.isEmpty() || filter.isBlank() || name.toLowerCase().contains(filter.toLowerCase())){
                List<RecipeHolder> holders = ESOCommon.getRecipeHolders(ForgeRegistries.ENCHANTMENTS.getKey(enchantment));
                boolean hide = ESOCommon.config.hideEnchantmentsWithoutRecipe;
                if(holders != null){
                    int ordinal = 0;
                    for (RecipeHolder holder : holders){
                        Integer l = enchs.get(enchantment);
                        int maxLevel = holder.getMaxLevel(enchantment);
                        int targetLevel = l != null ? Math.min(maxLevel, l + 1) : 1;

                        if(hide && !player.getAbilities().instabuild && !holder.levels.containsKey(targetLevel) && holder.xpMap.get(targetLevel) < 1){
                            continue;
                        }

                        MutableComponent translatable = RecipeHolder.getFullName(enchantment, targetLevel, maxLevel);
                        EnchButtonWithData b = new EnchButtonWithData(leftPos + 80, (this.topPos + 25) + 16 * offset, 123, 14, translatable, button -> {
                            this.selectedEnchantment = enchantment;
                            this.ordinal = ((EnchButtonWithData)button).getOrdinal();
                            this.switchOverlayState(false);
                            this.switchButtonsState(false);
                            this.enchantmentsScrollList.setOverlayActive(true);
                        }, holder, enchantment, level, ordinal);
                        this.composeTooltipAndApply(translatable, enchantment, holder, targetLevel, b, true);
                        list.add(b);
                        offset++;
                        ordinal++;
                    }
                } else if(player.getAbilities().instabuild || !hide){
                    Integer l = enchs.get(enchantment);
                    int maxLevel = enchantment.getMaxLevel();
                    int targetLevel = l != null ? Math.min(maxLevel, l + 1) : 1;

                    MutableComponent translatable = RecipeHolder.getFullName(enchantment, targetLevel, maxLevel);
                    EnchButtonWithData b = new EnchButtonWithData(leftPos + 80, (this.topPos + 25) + 16 * offset, 123, 14, translatable, button -> {
                        this.selectedEnchantment = enchantment;
                        this.ordinal = -1;
                        this.switchOverlayState(false);
                        this.switchButtonsState(false);
                        this.enchantmentsScrollList.setOverlayActive(true);
                    }, null, enchantment, level, -1);
                    this.composeTooltipAndApply(translatable, enchantment, null, targetLevel, b, true);
                    list.add(b);
                    offset++;
                }
            }
        }

        return list;
    }

    public void composeTooltipAndApply(MutableComponent translatable, Enchantment enchantment, RecipeHolder holder, int targetLevel, EnchButtonWithData button, boolean bl){
        MutableComponent c = translatable.copy();
        c.withStyle(ChatFormatting.AQUA);
        c.append(CommonComponents.NEW_LINE);
        c.append(ESOClient.getEnchantmentDescription(enchantment));
        if(ESOCommon.config.enableEnchantmentsLeveling && !Minecraft.getInstance().player.getAbilities().instabuild && targetLevel > button.enchantmentInstance.level){
            c.append(CommonComponents.NEW_LINE);
            c.append(Component.translatable("eso.knowledgerequired", enchantment.getFullname(targetLevel)).withStyle(ChatFormatting.DARK_RED));
        }
        if(holder != null){
            ItemDataPrepared[] d = holder.levels.get(targetLevel);
            if(d != null){
                c.append(CommonComponents.NEW_LINE);
                c.append(Component.translatable("eso.requires").withStyle(ChatFormatting.GRAY));
                for (ItemDataPrepared data : holder.levels.get(targetLevel)) {
                    MutableComponent itemName;
                    if(data.isEmpty()){
                        itemName = Component.translatable("eso.emptyitem").withStyle(ChatFormatting.DARK_GRAY);
                    } else {
                        Item item;
                        CompoundTag tag = null;
                        int amount;
                        if(data.itemList != null){
                            if(data.applicableItems.isEmpty()) continue;
                            if(bl){
                                this.tickingButtons.add(button);
                                data.resetPos();
                            }
                            ItemDataPrepared ids = data.getNotNestedData();
                            item = ids.item;
                            tag = ids.compoundTag;
                            amount = ids.amount;
                            data.next();
                        } else if(data.tagKey != null){
                            if(data.applicableItems.isEmpty()) continue;
                            if(bl){
                                this.tickingButtons.add(button);
                                data.resetPos();
                            }
                            item = data.getApplicableItem();
                            amount = data.amount;
                            data.next();
                        } else {
                            item = data.item;
                            tag = data.compoundTag;
                            amount = data.amount;
                        }
                        if(tag != null){
                            ItemStack refStack = new ItemStack(item, 1);
                            refStack.setTag(tag);
                            itemName = Component.translatable(refStack.getDescriptionId());
                            if(item instanceof PotionItem){
                                String potion_id = tag.getString("Potion");
                                if(potion_id.contains("strong")){
                                    itemName.append(Component.translatable("eso.potionstrong"));
                                } else if(potion_id.contains("long")){
                                    itemName.append(Component.translatable("eso.potionlong"));
                                }

                            }
                        } else itemName = Component.translatable(item.getDescriptionId());
                        itemName.append(": ").append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GOLD);
                    }
                    c.append(CommonComponents.NEW_LINE);
                    c.append(itemName);

                }
            }
            int cost = holder.xpMap.get(targetLevel);
            if(cost > 0){
                Player player = Minecraft.getInstance().player;
                MutableComponent costMsg;
                if(holder.mode){
                    int totalXP = Utils.getTotalAvailableXPPoints(player);
                    costMsg = Component.translatable("eso.xprequirementpoints", cost, decimal_formatter.format(Utils.getXPCostInLevels(player, cost, totalXP)));
                    if(cost > totalXP){
                        costMsg.withStyle(ChatFormatting.DARK_RED);
                    } else costMsg.withStyle(ChatFormatting.DARK_GREEN);
                } else {
                    costMsg = Component.translatable("eso.xprequirementlevels", cost);
                    if(cost > player.experienceLevel){
                        costMsg.withStyle(ChatFormatting.DARK_RED);
                    } else costMsg.withStyle(ChatFormatting.DARK_GREEN);
                }
                c.append(CommonComponents.NEW_LINE);
                c.append(costMsg);
            }
            if(!(holder.levels.isEmpty() && holder.xpMap.isEmpty())){
                c.append(CommonComponents.NEW_LINE);
                c.append(Component.translatable("eso.tooltip.recipekey", ESOClient.recipeKey.getTranslatedKeyMessage().getString().toUpperCase()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        List<FormattedCharSequence> seq = font.split(c, 190);
        button.setTooltip((stack, x, y) -> EnchantingTableScreen.this.renderTooltip(stack, seq, x, y));
    }

    public Object2IntOpenHashMap<Enchantment> filterToNewSet(Object2IntOpenHashMap<Enchantment> map, BiPredicate<Enchantment, Integer> predicate){
        Object2IntOpenHashMap<Enchantment> enchs = new Object2IntOpenHashMap<>();
        for (Object2IntMap.Entry<Enchantment> e : map.object2IntEntrySet()){
            if(predicate.test(e.getKey(), e.getIntValue())){
                enchs.put(e.getKey(), e.getIntValue());
            }
        }
        return enchs;
    }

    public Object2IntOpenHashMap<Enchantment> filterToNewSet(Map<Enchantment, Integer> map, BiPredicate<Enchantment, Integer> predicate){
        Object2IntOpenHashMap<Enchantment> enchs = new Object2IntOpenHashMap<>();
        for (Map.Entry<Enchantment, Integer> e : map.entrySet()){
            if(predicate.test(e.getKey(), e.getValue())){
                enchs.put(e.getKey(), e.getValue().intValue());
            }
        }
        return enchs;
    }

    public EditBox getSearchFilter() {
        return searchFilter;
    }

    public int getEnchantmentsLimit(int curses, MaterialData data){
        ConfigurationFile cfg = ESOCommon.config;
        return cfg.enableCursesAmplifier ? data.getMaxEnchantments() + Math.min(curses, data.getMaxCurses()) * data.getCurseMultiplier() : data.getMaxEnchantments();
    }

    public MaterialData getMatData(Item item){
        return ESOCommon.config.enableEnchantability ? ESOCommon.mat_config.getMaterialData(item) : MaterialOverrides.defaultMatData;
    }

    public int getCurrentEnchantmentsCount(int appliedEnchantments, int curses){
        ConfigurationFile cfg = ESOCommon.config;
        return cfg.enableCursesAmplifier ? appliedEnchantments - curses : appliedEnchantments;
    }

    public void updateButtons(){
        this.enchantmentsScrollList.resetScrollAmount();
        this.enchantmentsScrollList.setEnchantments(craftEnchantmentsButtons(this.searchFilter.getValue()));
    }

    public void recalculateAvailability(SimpleContainer container){
        ItemStack stack = container.getItem(0);
        Player player = Minecraft.getInstance().player;
        if(!stack.isEmpty() && (stack.is(Items.ENCHANTED_BOOK) || stack.getItem().isEnchantable(stack))){
            Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
            label1:
            for (EnchButtonWithData b : this.enchantmentsScrollList.getEnchantments()) {
                Integer targetLevel = enchs.get(b.getEnchantment());
                targetLevel = targetLevel == null ? 1 : targetLevel + 1;

                if(ESOCommon.config.enableEnchantmentsLeveling && targetLevel > b.enchantmentInstance.level){
                    b.active = false;
                    continue;
                }

                if(!stack.is(Items.BOOK) || !stack.is(Items.ENCHANTED_BOOK)) {
                    for (Enchantment e : enchs.keySet()) {
                        if (e != b.getEnchantment() && !e.isCompatibleWith(b.getEnchantment())) {
                            b.active = false;
                            continue label1;
                        }
                    }
                }
                RecipeHolder holder = b.getRecipe();
                if (holder != null) {
                    b.active = targetLevel <= holder.getMaxLevel(b.getEnchantment()) && (player.getAbilities().instabuild || holder.check(container, targetLevel, player));
                } else b.active = player.getAbilities().instabuild && targetLevel <= b.getEnchantment().getMaxLevel();
            }
        } else this.enchantmentsScrollList.enchantments.forEach(b -> b.active = false);
    }

    public EnchantmentListWidget getEnchantmentsScrollList() {
        return enchantmentsScrollList;
    }

    public Font getFont(){
        return font;
    }

    public static DecimalFormat getFormatter(){
        return decimal_formatter;
    }
}
