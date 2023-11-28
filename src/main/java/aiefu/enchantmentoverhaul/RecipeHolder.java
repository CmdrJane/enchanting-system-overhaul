package aiefu.enchantmentoverhaul;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

public class RecipeHolder {
    public transient ResourceLocation ench_location;
    public static final ItemData EMPTY = new ItemData(null);
    public String enchantment_id;
    public int maxLevel;
    public Int2ObjectOpenHashMap<ItemData[]> levels = new Int2ObjectOpenHashMap<>();

    public void processData(){
        this.levels.values().forEach(m -> {
            for (int i = 0; i < m.length; i++) {
                ItemData data = m[i];
                if(data.id == null || data.id.isEmpty() || data.id.isBlank() || data.id.equalsIgnoreCase("empty")){
                    m[i] = EMPTY;
                } else {
                    data.makeId();
                }
            }
        });
        this.ench_location = new ResourceLocation(enchantment_id);
        EnchantmentOverhaul.recipeMap.put(this.ench_location, this);
    }

    public boolean check(SimpleContainer container, int targetLevel){
        int x = 0;
        ItemData[] array = this.levels.get(targetLevel);
        if(array != null) {
            for (int i = 1; i < 5; i++) {
                ItemStack stack = container.getItem(i);
                ItemData data = array[i - 1];
                if (data != null) {
                    if (data.isEmpty() || stack.getItem() == data.item && data.isEnough(stack)) {
                        x++;
                    }
                } else {
                    x++;
                }
            }
        }
        return x > 3;
    }

    public boolean checkAndConsume(SimpleContainer container, int targetLevel){
        int x = 0;
        ItemData[] arr = this.levels.get(targetLevel);
        if(arr != null) {
            for (int i = 1; i < 5; i++) {
                ItemStack stack = container.getItem(i);
                ItemData data = arr[i - 1];
                if (data != null) {
                    if (data.isEmpty() || stack.getItem() == data.item && data.isEnough(stack)) {
                        x++;
                    }
                } else {
                    x++;
                    arr[i - 1] = EMPTY;
                }
            }
        }
        if(x > 3){
            for (int i = 0; i < arr.length; i++) {
                container.getItem(i + 1).shrink(arr[i].amount);
            }
            return true;
        } else return false;
    }

    public int getMaxLevel(Enchantment enchantment){
        return this.maxLevel < 1 ? enchantment.getMaxLevel() : this.maxLevel;
    }

    public static class ItemData{
        protected transient boolean isEmpty = false;
        @Nullable
        public transient ResourceLocation item_id;
        public transient Item item = Items.AIR;
        @Nullable
        public String id;
        public int amount;

        public ItemData() {
        }

        public ItemData(Void v) {
            this.makeId();
        }

        public ItemData(@Nullable String id, int amount) {
            this.id = id;
            this.amount = amount;
        }

        public void makeId(){
            this.item_id = new ResourceLocation(this.id);
            this.item = BuiltInRegistries.ITEM.get(item_id);
        }

        public boolean isSameStack(ItemStack stack){
           return stack.is(item);
        }

        public boolean isEnough(ItemStack stack){
            return stack.getCount() >= amount;
        }

        public boolean isEmpty() {
            return this.isEmpty;
        }
    }
}
