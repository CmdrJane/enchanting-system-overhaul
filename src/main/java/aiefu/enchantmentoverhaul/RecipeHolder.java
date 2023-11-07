package aiefu.enchantmentoverhaul;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class RecipeHolder {
    public transient ResourceLocation ench_location;
    public static final ItemData EMPTY = new ItemData(null, 0);
    public static final String lapizLazuliSlot = "lapisLazuli";
    public static final String slot1 = "slot1";
    public static final String slot2 = "slot2";
    public static final String slot3 = "slot3";
    public String enchantment_id;
    public HashMap<String, Int2ObjectOpenHashMap<ItemData>> slots = new HashMap<>();

    public void processData(){
        this.ench_location = new ResourceLocation(enchantment_id);
    }

    public boolean checkRequirements(String slot, ItemStack stack, int targetLevel){
        ItemData data;
        Int2ObjectOpenHashMap<ItemData> map = this.slots.get(slot);
        if(map == null){
            data = EMPTY;
        } else {
            data = map.get(targetLevel);
        }
        if(data == null){
            data = EMPTY;
        }
        return data.isEmpty() || data.isSameStack(stack) && data.isEnough(stack);
    }

    public static class ItemData{
        protected transient boolean isEmpty = false;
        @Nullable
        public transient ResourceLocation item_id;
        @Nullable
        public String id;
        public int amount;

        public ItemData() {
        }

        public ItemData(@Nullable String id, int amount) {
            this.id = id;
            this.amount = amount;
        }

        public void makeId(){
            if(id == null){
                this.isEmpty = true;
            } else {
                if(this.id.isEmpty() || this.id.equalsIgnoreCase("empty")){
                    this.isEmpty = true;
                } else this.item_id = new ResourceLocation(this.id);
            }
        }

        public boolean isSameStack(ItemStack stack){
           return stack.is(BuiltInRegistries.ITEM.get(this.item_id));
        }

        public boolean isEnough(ItemStack stack){
            return stack.getCount() >= amount;
        }

        public boolean isEmpty() {
            return this.isEmpty;
        }
    }
}
