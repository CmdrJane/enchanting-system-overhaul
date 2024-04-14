package aiefu.eso.data.itemdata;

import aiefu.eso.Utils;
import aiefu.eso.client.gui.EnchantingTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public class RecipeViewerData {
    protected ItemDataPrepared[] itemData = new ItemDataPrepared[]{};
    protected RecipeViewerItemData[] cachedStacks;
    protected Enchantment enchantment;
    protected ItemStack resultStack;
    protected int xp = 0;
    protected int lvl;
    protected boolean mode;
    protected Component desc;

    public RecipeViewerData(ItemDataPrepared[] itemData, int lvl, Enchantment enchantment, boolean mode) {
        this.itemData = itemData;
        this.lvl = lvl;
        this.enchantment = enchantment;
        this.mode = mode;
        this.cacheItemStacks(itemData);
        this.composeDescription();
    }

    public RecipeViewerData(int xp, int lvl, Enchantment enchantment, boolean mode) {
        this.xp = xp;
        this.lvl = lvl;
        this.enchantment = enchantment;
        this.mode = mode;
        this.composeDescription();
    }

    public RecipeViewerData(ItemDataPrepared[] itemData, int xp, int lvl, Enchantment enchantment, boolean mode) {
        this.itemData = itemData;
        this.xp = xp;
        this.lvl = lvl;
        this.enchantment = enchantment;
        this.mode = mode;
        this.cacheItemStacks(itemData);
        this.composeDescription();
    }

    public ItemDataPrepared[] getItemData() {
        return itemData;
    }

    public int getXp() {
        return xp;
    }

    public void setItemData(ItemDataPrepared[] itemData) {
        this.itemData = itemData;
        this.cacheItemStacks(itemData);
    }

    public void setXp(int xp) {
        this.xp = xp;
        this.composeDescription();
    }

    public void cacheItemStacks(ItemDataPrepared[] prepared){
        RecipeViewerItemData[] data = new RecipeViewerItemData[prepared.length];
        for (int i = 0; i < prepared.length; i++) {
            data[i] = new RecipeViewerItemData(prepared[i]);
        }
        this.cachedStacks = data;
        this.resultStack = new ItemStack(Items.ENCHANTED_BOOK, 1);
        EnchantedBookItem.addEnchantment(resultStack, new EnchantmentInstance(enchantment, lvl));
    }

    public void composeDescription(){
        LocalPlayer player = Minecraft.getInstance().player;
        MutableComponent c = Component.translatable("eso.rv.level", this.lvl);
        if(xp > 0){
            if(mode){
                int totalXP = Utils.getTotalAvailableXPPoints(player);
                c.append(CommonComponents.SPACE);
                c.append(Component.translatable("eso.rv.xpreql", xp, EnchantingTableScreen.getFormatter().format(Utils.getXPCostInLevels(player, xp, totalXP))));
            } else {
                c.append(CommonComponents.SPACE);
                c.append(Component.translatable("eso.rv.xpreqp", xp));
            }
        }
        this.desc = c;
    }

    public ItemStack getResultStack() {
        return resultStack;
    }

    public RecipeViewerItemData[] getCachedStacks() {
        return cachedStacks;
    }

    public int getLvl() {
        return lvl;
    }

    public boolean isMode() {
        return mode;
    }

    public Component getDesc() {
        return desc;
    }
}
