package aiefu.eso.menu;

import aiefu.eso.ESOCommon;
import aiefu.eso.IServerPlayerAcc;
import aiefu.eso.Utils;
import aiefu.eso.client.ESOClient;
import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.materialoverrides.MaterialData;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.Equipable;
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

    public static final ResourceLocation LAZURITE_EMPTY_ICON = new ResourceLocation("item/empty_slot_lapis_lazuli");
    public static final ResourceLocation SWORD_EMPTY_ICON = new ResourceLocation("item/empty_slot_sword");
    public static final ResourceLocation INGOT_EMPTY_ICON = new ResourceLocation("item/empty_slot_ingot");
    private final ContainerLevelAccess access;
    public Object2IntOpenHashMap<Enchantment> allEnchantments = new Object2IntOpenHashMap<>();
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
            Enchantment e = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(s));
            if(e != null){
                allEnchantments.put(e, l);
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
                    public void setByPlayer(ItemStack stack) {
                        OverhauledEnchantmentMenu.onEquipItem(owner, equipmentSlot, stack, this.getItem());
                        super.setByPlayer(stack);
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
            public void setByPlayer(ItemStack stack) {
                OverhauledEnchantmentMenu.onEquipItem(owner, EquipmentSlot.OFFHAND, stack, this.getItem());
                super.setByPlayer(stack);
            }

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        this.addSlot(new Slot(this.tableInv, 0, 24,31){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem().isEnchantable(stack) || stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, SWORD_EMPTY_ICON);
            }
        });
        this.addSlot(new Slot(this.tableInv, 1, 42,31){
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, LAZURITE_EMPTY_ICON);
            }
        });
        for (int j = 0; j < 3; j++) {
            this.addSlot(new Slot(this.tableInv, j + 2, 15 + j * 18, 49){
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, INGOT_EMPTY_ICON);
                }
            });
        }
    }

    public void checkRequirementsAndConsume(ResourceLocation location, Player player, int ordinal){
        this.access.execute((level, blockPos) -> {
            boolean instabuild = player.getAbilities().instabuild;
            ItemStack stack = this.tableInv.getItem(0);
            boolean isBook = stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
            if(!stack.isEmpty() && (isBook || stack.getItem().isEnchantable(stack))) {
                Enchantment target = BuiltInRegistries.ENCHANTMENT.get(location);
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
                    Map<Enchantment, Integer> enchs = Utils.containsEnchantments(stack) ? EnchantmentHelper.getEnchantments(stack) : new HashMap<>();
                    int curses = 0;
                    for (Enchantment e : enchs.keySet()){
                        if(e.isCurse()) curses++;
                    }
                    MaterialData data = Utils.getMatData(stack.getItem());
                    if((isBook || target.canEnchant(stack)) && (enchs.containsKey(target) || target.isCurse() && curses < data.getMaxCurses()
                            || Utils.getCurrentLimit(enchs.keySet().size(), curses) < Utils.getEnchantmentsLimit(curses, data))) {

                        if(!isBook){
                            for (Enchantment e : enchs.keySet()) {
                                if (e != target && !e.isCompatibleWith(target)) {
                                    return;
                                }
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
                                this.applyAndBroadcast(player, enchs, stack, isBook);
                            }
                        } else if (instabuild && targetLevel <= target.getMaxLevel()) {
                            enchs.put(target, targetLevel);
                            this.applyAndBroadcast(player, enchs, stack, isBook);
                        }
                    }
                }
            }
        });
    }

    public void applyAndBroadcast(Player player, Map<Enchantment, Integer> map, ItemStack stack, boolean isBook){
        if(isBook){
            if(stack.is(Items.BOOK)){
                stack = new ItemStack(Items.ENCHANTED_BOOK);
                this.tableInv.setItem(0, stack);
            }
            for (Map.Entry<Enchantment, Integer> e : map.entrySet()){
                EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(e.getKey(), e.getValue()));
            }
        } else {
            EnchantmentHelper.setEnchantments(map, stack);
        }
        player.onEnchantmentPerformed(stack, 0);
        this.tableInv.setChanged();
        this.broadcastChanges();
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
        Equipable equipable = Equipable.get(newItem);
        if (equipable != null) {
            player.onEquipItem(slot, oldItem, newItem);
        }
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
                if(eqs.isArmor()){
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
                ItemStack stack2 = stack.copyWithCount(1);
                if(!this.slots.get(41).hasItem() && this.slots.get(41).mayPlace(stack2)){
                    stack.shrink(1);
                    this.slots.get(41).setByPlayer(stack2);
                    returnStack = ItemStack.EMPTY;
                } else if(!moveItemStackTo(stack, 42, 46, false)){
                    return ItemStack.EMPTY;
                } else return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
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
