package aiefu.eso.data.itemdata;

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

    public RecipeViewerData(ItemDataPrepared[] itemData, int lvl, Enchantment enchantment) {
        this.itemData = itemData;
        this.lvl = lvl;
        this.enchantment = enchantment;
        this.cacheItemStacks(itemData);
    }

    public RecipeViewerData(int xp, int lvl, Enchantment enchantment) {
        this.xp = xp;
        this.lvl = lvl;
        this.enchantment = enchantment;
    }

    public RecipeViewerData(ItemDataPrepared[] itemData, int xp, int lvl, Enchantment enchantment) {
        this.itemData = itemData;
        this.xp = xp;
        this.lvl = lvl;
        this.enchantment = enchantment;
        this.cacheItemStacks(itemData);
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

    public ItemStack getResultStack() {
        return resultStack;
    }

    public RecipeViewerItemData[] getCachedStacks() {
        return cachedStacks;
    }

    public int getLvl() {
        return lvl;
    }
}
