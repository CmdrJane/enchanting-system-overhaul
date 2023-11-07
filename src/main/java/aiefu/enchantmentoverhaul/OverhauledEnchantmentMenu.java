package aiefu.enchantmentoverhaul;

import aiefu.enchantmentoverhaul.client.EnchantmentOverhaulClient;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OverhauledEnchantmentMenu extends AbstractContainerMenu {
    public static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
    protected static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};;
    private final ContainerLevelAccess access;
    public List<Enchantment> enchantments = new ArrayList<>();

    protected SimpleContainer tableInv;
    public OverhauledEnchantmentMenu(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, ContainerLevelAccess.NULL, EnchantmentOverhaulClient.getClientPlayer());
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
                return stack.isEnchantable();
            }
        });
        this.addSlot(new Slot(this.tableInv, 1, 42,31){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true; //TODO: make filters
            }
        });
        for (int j = 0; j < 3; j++) { //15 49
            this.addSlot(new Slot(this.tableInv, j + 2, 15 + j * 18, 49));
        }
    }

    public void enchant(Player player, String enchantmentLocation){
        this.access.execute((level, blockPos) -> {
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(enchantmentLocation));
            ItemStack stack = this.tableInv.getItem(0);
            if(stack.isEnchanted()){
                Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
                if(enchs.containsKey(enchantment)){
                    int l = enchs.get(enchantment);
                    if(l + 1 <= EnchantmentOverhaul.config.maxEnchantmentLevel()){
                        player.onEnchantmentPerformed(stack, 0);
                        enchs.put(enchantment, l + 1);
                        EnchantmentHelper.setEnchantments(enchs, stack);
                    }
                }
            } else stack.enchant(enchantment, enchantment.getMinLevel());
        });
    }

    public boolean checkRecipeRequirements(ItemStack stack, Enchantment enchantment, int targetLvl){
        return true;
    }

    public static void onEquipItem(Player player, EquipmentSlot slot, ItemStack newItem, ItemStack oldItem) {
        Equipable equipable = Equipable.get(newItem);
        if (equipable != null) {
            player.onEquipItem(slot, oldItem, newItem);
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
    }
}
