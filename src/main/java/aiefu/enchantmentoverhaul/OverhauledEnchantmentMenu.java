package aiefu.enchantmentoverhaul;

import aiefu.enchantmentoverhaul.client.EnchantmentOverhaulClient;
import com.mojang.datafixers.util.Pair;
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
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverhauledEnchantmentMenu extends AbstractContainerMenu {
    public static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
    protected static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};;
    private final ContainerLevelAccess access;
    public List<Enchantment> enchantments = new ArrayList<>();

    public boolean isClientSide = false;

    protected SimpleContainer tableInv;
    public OverhauledEnchantmentMenu(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, ContainerLevelAccess.NULL, EnchantmentOverhaulClient.getClientPlayer());
        this.isClientSide = true;
        int r = buf.readInt();
        for (int i = 0; i < r; i++) {
            String s = buf.readUtf();
            ResourceLocation loc = new ResourceLocation(s);
            this.enchantments.add(BuiltInRegistries.ENCHANTMENT.get(loc));
        }
    }
    public OverhauledEnchantmentMenu(int syncId, Inventory inventory, ContainerLevelAccess access, Player owner) {
        super(EnchantmentOverhaul.enchantment_menu_ovr, syncId);
        this.access = access;
        this.tableInv = new SimpleContainer(5);
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
                        return !itemStack.isEmpty() && !player.isCreative() && EnchantmentHelper.hasBindingCurse(itemStack) ? false : super.mayPickup(player);
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
                return stack.getItem().isEnchantable(stack) || stack.is(Items.BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                if(OverhauledEnchantmentMenu.this.isClientSide){
                    EnchantmentOverhaulClient.updateEnchantmentsCriteria(OverhauledEnchantmentMenu.this.tableInv);
                }
            }
        });
        this.addSlot(new Slot(this.tableInv, 1, 42,31){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true; //TODO: make filters
            }

            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                if(OverhauledEnchantmentMenu.this.isClientSide){
                    EnchantmentOverhaulClient.updateEnchantmentsCriteria(OverhauledEnchantmentMenu.this.tableInv);
                }
            }
        });
        for (int j = 0; j < 3; j++) { //15 49
            this.addSlot(new Slot(this.tableInv, j + 2, 15 + j * 18, 49){
                @Override
                public void set(ItemStack stack) {
                    super.set(stack);
                    if(OverhauledEnchantmentMenu.this.isClientSide){
                        EnchantmentOverhaulClient.updateEnchantmentsCriteria(OverhauledEnchantmentMenu.this.tableInv);
                    }
                }
            });
        }
    }

    public void checkRequirementsAndConsume(ResourceLocation location, Player player){
        this.access.execute((level, blockPos) -> {
            ItemStack stack = this.tableInv.getItem(0);
            if(!stack.isEmpty() && stack.getItem().isEnchantable(stack)) {
                if(stack.is(Items.BOOK)) {
                    this.enchantBook(location, player);
                } else {
                    Enchantment target = BuiltInRegistries.ENCHANTMENT.get(location);
                    if (target != null) {
                        stack.getOrCreateTag();
                        Map<Enchantment, Integer> enchs = stack.isEnchanted() ? EnchantmentHelper.getEnchantments(stack) : new HashMap<>();
                        if(!target.canEnchant(stack) || enchs.keySet().size() >= EnchantmentOverhaul.config.getMaxEnchantments()){
                            return;
                        }
                        for (Enchantment e : enchs.keySet()){
                            if(!e.isCompatibleWith(target)){
                                return;
                            }
                        }
                        int targetLevel = 1;
                        Integer l = enchs.get(target);
                        if (l != null) {
                            targetLevel = l + 1;
                        }
                        RecipeHolder holder = EnchantmentOverhaul.recipeMap.get(location);
                        if (holder != null && targetLevel < holder.getMaxLevel(target) && holder.checkAndConsume(this.tableInv, targetLevel)) {
                            enchs.put(target, targetLevel);
                            EnchantmentHelper.setEnchantments(enchs, stack);
                            player.onEnchantmentPerformed(stack, 0);
                            this.tableInv.setChanged();
                            this.broadcastChanges();
                        }
                    }
                }
            }
        });
    }

    public void enchantBook(ResourceLocation location, Player player){
        Enchantment target = BuiltInRegistries.ENCHANTMENT.get(location);
        if(target != null){
            ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
            stack.getOrCreateTag();
            RecipeHolder holder = EnchantmentOverhaul.recipeMap.get(location);
            if(holder != null && holder.checkAndConsume(this.tableInv, 1)){
                stack.enchant(target, 1);
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
                    if(!moveItemStackTo(stack, o, o + 1, true)){
                        return ItemStack.EMPTY;
                    }
                } else if(eqs == EquipmentSlot.OFFHAND){
                    if(!moveItemStackTo(stack, 40 , 41 , true)){
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
