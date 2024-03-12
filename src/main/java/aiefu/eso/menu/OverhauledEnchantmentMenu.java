package aiefu.eso.menu;

import aiefu.eso.ConfigurationFile;
import aiefu.eso.ESOCommon;
import aiefu.eso.IServerPlayerAcc;
import aiefu.eso.client.ESOClient;
import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.materialoverrides.MaterialData;
import aiefu.eso.data.materialoverrides.MaterialOverrides;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverhauledEnchantmentMenu extends AbstractContainerMenu {
    public static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
    protected static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};;

    public static final ResourceLocation LAZURITE_EMPTY_ICON = new ResourceLocation(ESOCommon.MOD_ID, "textures/gui/empty_slot_lapis_lazuli.png");
    public static final ResourceLocation SWORD_EMPTY_ICON = new ResourceLocation(ESOCommon.MOD_ID, "textures/gui/empty_slot_sword.png");
    public static final ResourceLocation INGOT_EMPTY_ICON = new ResourceLocation(ESOCommon.MOD_ID, "textures/gui/empty_slot_ingot.png");
    private final ContainerLevelAccess access;
    public Object2IntOpenHashMap<Enchantment> enchantments = new Object2IntOpenHashMap<>();

    public Object2IntOpenHashMap<Enchantment> curses = new Object2IntOpenHashMap<>();

    public boolean isClientSide = false;

    protected SimpleContainer tableInv;
    public OverhauledEnchantmentMenu(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, ContainerLevelAccess.NULL, ESOClient.getClientPlayer());
        this.isClientSide = true;
        int r = buf.readVarInt();
        for (int i = 0; i < r; i++) {
            String s = buf.readUtf();
            int l = buf.readVarInt();
            Enchantment e = Registry.ENCHANTMENT.get(new ResourceLocation(s));
            if(e != null){
                if(e.isCurse()){
                    curses.put(e, l);
                } else enchantments.put(e, l);
            }
        }

    }
    public OverhauledEnchantmentMenu(int syncId, Inventory inventory, ContainerLevelAccess access, Player owner) {
        super(ESOCommon.enchantment_menu_ovr, syncId);
        this.access = access;
        this.tableInv = new SimpleContainer(5){
            @Override
            public void setChanged() {
                super.setChanged();
                OverhauledEnchantmentMenu.this.slotsChanged(this);
            }
        };
        //Inventory Slots
        int i;
        for(i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 9 + j * 18, 99 + i * 18));
            }
        }
        //Hot-bar
        for(i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 9 + i * 18, 157));
        }
        //Armor
        int slotId = 0;
        for (int j = 0; j < 2; j++) {
            for(i = 0; i < 2; ++i) {
                final EquipmentSlot equipmentSlot = SLOT_IDS[slotId];
                this.addSlot(new Slot(inventory, 39 - slotId, 175 + i * 18, 108 + j * 18) {
                    public void set(ItemStack stack) {
                        ItemStack oldStack = this.getItem();
                        super.set(stack);
                        OverhauledEnchantmentMenu.onEquipItem(owner, equipmentSlot, stack, oldStack);
                    }

                    public int getMaxStackSize() {
                        return 1;
                    }

                    public boolean mayPlace(ItemStack stack) {
                        return equipmentSlot == Mob.getEquipmentSlotForItem(stack);
                    }

                    public boolean mayPickup(Player player) {
                        ItemStack itemStack = this.getItem();
                        return (player.isCreative() || !EnchantmentHelper.hasBindingCurse(itemStack)) && super.mayPickup(player);
                    }

                    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                        return Pair.of(InventoryMenu.BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[equipmentSlot.getIndex()]);
                    }
                });
                slotId++;
            }
        }
        //Offhand
        this.addSlot(new Slot(inventory, 40, 175, 157) {
            public void set(ItemStack stack) {
                ItemStack oldStack = this.getItem();
                super.set(stack);
                OverhauledEnchantmentMenu.onEquipItem(owner, EquipmentSlot.OFFHAND, stack, oldStack);
            }

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        this.addSlot(new Slot(this.tableInv, 0, 24,31){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem().isEnchantable(stack) || stack.is(Items.BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(this.tableInv, 1, 42,31));
        for (int j = 0; j < 3; j++) {
            this.addSlot(new Slot(this.tableInv, j + 2, 15 + j * 18, 49));
        }
    }

    public void checkRequirementsAndConsume(ResourceLocation location, Player player, int ordinal){
        this.access.execute((level, blockPos) -> {
            boolean instabuild = player.getAbilities().instabuild;
            ItemStack stack = this.tableInv.getItem(0);
            if(!stack.isEmpty() && stack.getItem().isEnchantable(stack)) {
                if(stack.is(Items.BOOK)) {
                    this.enchantBook(location, player, ordinal);
                } else {
                    Enchantment target = Registry.ENCHANTMENT.get(location);
                    if (target != null) {
                        Object2IntOpenHashMap<Enchantment> learnedEnchantments = ((IServerPlayerAcc)player).enchantment_overhaul$getUnlockedEnchantments();
                        int learnedLevel = learnedEnchantments.getInt(target);
                        if(learnedLevel == 0 && !ESOCommon.config.disableDiscoverySystem && !instabuild){
                            return;
                        }
                        if(target.isCurse() && !ESOCommon.config.enableCursesAmplifier){
                            return;
                        }

                        stack.getOrCreateTag();
                        Map<Enchantment, Integer> enchs = stack.isEnchanted() ? EnchantmentHelper.getEnchantments(stack) : new HashMap<>();
                        int curses = 0;
                        for (Enchantment e : enchs.keySet()){
                            if(e.isCurse()) curses++;
                        }
                        MaterialData data = this.getMatData(stack.getItem());
                        if(target.canEnchant(stack) && (enchs.containsKey(target) || target.isCurse() && curses < data.getMaxCurses()
                                || this.getCurrentLimit(enchs.keySet().size(), curses) < this.getEnchantmentsLimit(curses, data))) {

                            for (Enchantment e : enchs.keySet()) {
                                if (e != target && !e.isCompatibleWith(target)) {
                                    return;
                                }
                            }
                            int targetLevel = 1;
                            Integer l = enchs.get(target);
                            if (l != null) {
                                targetLevel = l + 1;
                            }
                            if(targetLevel > learnedLevel && ESOCommon.config.enableEnchantmentsLeveling && !instabuild){
                                return;
                            }
                            List<RecipeHolder> holders = ESOCommon.getRecipeHolders(location);
                            if (holders != null && !holders.isEmpty() && ordinal != -1 && ordinal < holders.size()) {
                                RecipeHolder holder = holders.get(ordinal);
                                if (instabuild || targetLevel <= holder.getMaxLevel(target) && holder.checkAndConsume(this.tableInv, targetLevel, player)) {
                                    enchs.put(target, targetLevel);
                                    this.applyAndBroadcast(player, enchs, stack);
                                }
                            } else if (instabuild && targetLevel <= target.getMaxLevel()) {
                                enchs.put(target, targetLevel);
                                this.applyAndBroadcast(player, enchs, stack);
                            }
                        }
                    }
                }
            }
        });
    }

    public int getEnchantmentsLimit(int curses, MaterialData data){
        ConfigurationFile cfg = ESOCommon.config;
        return cfg.enableCursesAmplifier ? data.getMaxEnchantments() + Math.min(curses, data.getMaxCurses()) * data.getCurseMultiplier() : data.getMaxEnchantments();
    }

    public MaterialData getMatData(Item item){
        return ESOCommon.config.enableEnchantability ? ESOCommon.mat_config.getMaterialData(item) : MaterialOverrides.defaultMatData;
    }

    public int getCurrentLimit(int appliedEnchantments, int curses){
        ConfigurationFile cfg = ESOCommon.config;
        return cfg.enableCursesAmplifier ? appliedEnchantments - curses : appliedEnchantments;
    }

    public void applyAndBroadcast(Player player, Map<Enchantment, Integer> map, ItemStack stack){
        EnchantmentHelper.setEnchantments(map, stack);
        player.onEnchantmentPerformed(stack, 0);
        this.tableInv.setChanged();
        this.broadcastChanges();
    }

    public void enchantBook(ResourceLocation location, Player player, int ordinal){
        Enchantment target = Registry.ENCHANTMENT.get(location);
        if(target != null){
            ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
            stack.getOrCreateTag();
            List<RecipeHolder> holders = ESOCommon.getRecipeHolders(location);
            if(holders != null && !holders.isEmpty() && ordinal != -1 && ordinal < holders.size()){
                RecipeHolder holder = holders.get(ordinal);
                if(holder.checkAndConsume(this.tableInv, 1, player)){
                    EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(target, 1));
                    this.tableInv.setItem(0, stack);
                    player.onEnchantmentPerformed(stack, 0);
                    this.broadcastChanges();
                }
            } else if(player.getAbilities().instabuild){
                EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(target, 1));
                this.tableInv.setItem(0, stack);
                player.onEnchantmentPerformed(stack, 0);
                this.broadcastChanges();
            }
        }
    }

    public SimpleContainer getTableInv() {
        return tableInv;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, blockPos) -> this.clearContainer(player, this.tableInv));
    }

    @Override
    protected void clearContainer(Player player, Container container) {
        super.clearContainer(player, container);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
    }

    public static void onEquipItem(Player player, EquipmentSlot slot, ItemStack newItem, ItemStack oldItem) {
        player.onEquipItem(slot, oldItem, newItem);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = slots.get(i);
        if(slot.hasItem()){
            ItemStack stack = slot.getItem();
            returnStack = stack.copy();
            if(i == 41){
                EquipmentSlot eqs = Mob.getEquipmentSlotForItem(stack);
                if(eqs.getType() == EquipmentSlot.Type.ARMOR){
                    int o = 39 - eqs.getIndex();
                    if(!moveItemStackTo(stack, o, o + 1, true) && !moveItemStackTo(stack, 0, 36, true)){
                        return ItemStack.EMPTY;
                    }
                } else if(eqs == EquipmentSlot.OFFHAND){
                    if(!moveItemStackTo(stack, 40 , 41 , true) && !moveItemStackTo(stack, 0, 36, true)){
                        return ItemStack.EMPTY;
                    }
                } else {
                    if(!moveItemStackTo(stack, 0, 36, true)){
                        return ItemStack.EMPTY;
                    }
                }
            } else if(i > 41 && i < 46){
                if(!moveItemStackTo(stack, 0 , 36, true)){
                    return ItemStack.EMPTY;
                }
            } else {
                ItemStack stack2 = stack.copy();
                stack2.setCount(1);
                if(!this.slots.get(41).hasItem() && this.slots.get(41).mayPlace(stack2)){
                    stack.shrink(1);
                    this.slots.get(41).set(stack2);
                    returnStack = ItemStack.EMPTY;
                } else if(!moveItemStackTo(stack, 42, 46, false)){
                    return ItemStack.EMPTY;
                } else return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == returnStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return returnStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
    }
}
