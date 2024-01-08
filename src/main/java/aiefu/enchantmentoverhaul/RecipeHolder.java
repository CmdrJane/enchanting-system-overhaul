package aiefu.enchantmentoverhaul;

import aiefu.enchantmentoverhaul.exception.ItemDoesNotExistException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
                if(data == null || data.isEmpty()){
                    m[i] = EMPTY;
                } else {
                    try {
                        data.makeId(enchantment_id);
                    } catch (ItemDoesNotExistException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        this.ench_location = new ResourceLocation(enchantment_id);
        List<RecipeHolder> list = EnchantmentOverhaul.recipeMap.get(ench_location);
        if(list != null){
            list.add(this);
        } else {
            list = new ArrayList<>();
            list.add(this);
            EnchantmentOverhaul.recipeMap.put(ench_location, list);
        }
    }

    public boolean check(SimpleContainer container, int targetLevel){
        int x = 0;
        ItemData[] array = this.levels.get(targetLevel);
        if(array != null) {
            for (int i = 0; i < array.length; i++) {
                ItemStack stack = container.getItem(i + 1);
                ItemData data = array[i];
                if (data != null) {
                    if (data.isEmpty() || data.testItemStackMatch(stack)) {
                        x++;
                    }
                } else {
                    x++;
                }
            }
            return x > array.length - 1;
        }
        return false;
    }

    public boolean checkAndConsume(SimpleContainer container, int targetLevel){
        int x = 0;
        ItemData[] arr = this.levels.get(targetLevel);
        if(arr != null) {
            for (int i = 0; i < arr.length; i++) {
                ItemStack stack = container.getItem(i + 1);
                ItemData data = arr[i];
                if (data != null) {
                    if (data.isEmpty() || data.testItemStackMatch(stack)) {
                        x++;
                    }
                } else {
                    x++;
                    arr[i] = EMPTY;
                }
            }

            if(x >= arr.length){
                for (int i = 0; i < arr.length; i++) {
                    ItemStack stack = container.getItem(i + 1);
                    ItemData data = arr[i];
                    ItemStack remainder = data.getRemainderForStack(stack);
                    if(stack.getCount() == 1 && remainder != null){
                        container.setItem(i + 1, remainder);
                    } else stack.shrink(data.amount);
                }
                return true;
            }
        }
        return false;
    }

    public void processTags(){
        for (Int2ObjectMap.Entry<ItemData[]> e : levels.int2ObjectEntrySet()){
            for (ItemData data : e.getValue()){
                data.processTags();
            }
        }
    }

    public int getMaxLevel(Enchantment enchantment){
        return this.maxLevel < 1 ? enchantment.getMaxLevel() : this.maxLevel;
    }

    public static MutableComponent getFullName(Enchantment e, int level, int maxLevel){
        MutableComponent mutableComponent = Component.translatable(e.getDescriptionId());
        if (level != 1 || maxLevel != 1) {
            mutableComponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level));
        }
        return mutableComponent;
    }

    @SuppressWarnings("unused")
    public static class ItemData{
        protected transient boolean isEmpty = false;

        public transient Item item = Items.AIR;

        public transient List<Item> applicableItems = new ArrayList<>();

        public transient List<ItemDataSimple> itemList;

        public transient int pos = 0;

        @Nullable
        public transient TagKey<Item> tagKey;

        public transient CompoundTag compoundTag;

        public transient Item remainderItem = Items.AIR;

        public transient boolean remainderEmpty = false;

        public transient CompoundTag remainderCompoundTag;

        public transient boolean arrayOverride = false;

        public String id;

        public ItemDataSimple[] itemArray;

        public int amount;

        public String tag;

        public String remainderId;

        public int remainderAmount = 1;

        public String remainderTag;

        public ItemData() {
        }

        public ItemData(Void v) {
            this.isEmpty = true;
        }

        public ItemData(@Nullable String id, int amount) {
            this.id = id;
            this.amount = amount;
        }

        public void makeId(String eid) throws ItemDoesNotExistException {
            if(itemArray != null){
                this.itemList = new ArrayList<>();
                for (ItemDataSimple data : itemArray){
                    Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(data.id));
                    if(item == Items.AIR) throw new ItemDoesNotExistException("Item id " + data + " in id array not found in game registry for enchantment recipe " + eid);
                    this.applicableItems.add(item);
                    data.item = item;
                    if(data.tag != null) {
                        try {
                            data.compoundTag = new TagParser(new StringReader(data.tag)).readStruct();
                        } catch (CommandSyntaxException e) {
                            EnchantmentOverhaul.LOGGER.error("Unable to parse tag array for enchantment recipe " + eid);
                            throw new RuntimeException(e);
                        }
                    }
                    if(data.remainderId != null){
                        data.rItem = BuiltInRegistries.ITEM.get(new ResourceLocation(data.remainderId));
                        if(data.rItem == Items.AIR) throw new ItemDoesNotExistException("Remainder item id " + data.remainderId + " not found in game registry for enchantment recipe " + eid);
                        if(data.remainderTag != null){
                            try {
                                data.rTag = new TagParser(new StringReader(data.remainderTag)).readStruct();
                            } catch (CommandSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    this.itemList.add(data);
                    this.arrayOverride = true;
                }
            } else {
                if(id == null){
                    this.isEmpty = true;
                    return;
                }
                if(id.startsWith("tags#")){
                    this.tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(id.substring(5)));
                } else {
                    this.item = BuiltInRegistries.ITEM.get(new ResourceLocation(this.id));
                    if(this.item == Items.AIR) throw new ItemDoesNotExistException("Item id " + this.id + " not found in game registry for enchantment recipe " + eid);
                }

                if(this.tag != null){
                    try {
                        this.compoundTag = new TagParser(new StringReader(tag)).readStruct();
                    } catch (CommandSyntaxException e){
                        e.printStackTrace();
                    }
                }
                if(this.remainderId != null){
                    if(!this.remainderId.isEmpty() && !this.remainderId.isBlank() && !this.remainderId.equalsIgnoreCase("empty")){
                        this.remainderItem = BuiltInRegistries.ITEM.get(new ResourceLocation(this.remainderId));
                        if(remainderItem == Items.AIR) throw new ItemDoesNotExistException("Remainder item id " + this.remainderId + " not found in game registry for enchantment recipe " + eid);
                    } else remainderEmpty = true;
                    if(this.remainderTag != null){
                        try {
                            this.remainderCompoundTag = new TagParser(new StringReader(remainderTag)).readStruct();
                        } catch (CommandSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }

        public @Nullable ItemStack getRemainderForStack(ItemStack stack){
            if(this.remainderEmpty){
                return stack.getRecipeRemainder();
            } else if(this.remainderItem != Items.AIR){
                ItemStack s = new ItemStack(this.remainderItem, this.remainderAmount);
                s.setTag(this.remainderCompoundTag);
                return s;
            } else return stack.getRecipeRemainder();
        }

        public boolean testTag(ItemStack stack){
            if(this.compoundTag == null){
                return true;
            } else if(!stack.hasTag()){
                return false;
            }
            else return TagsUtils.havePartialMatch(this.compoundTag, stack.getOrCreateTag());
        }

        public boolean testTag(ItemStack stack, CompoundTag ref){
            if(this.compoundTag == null){
                return true;
            } else if(!stack.hasTag()){
                return false;
            }
            else return TagsUtils.havePartialMatch(ref, stack.getOrCreateTag());
        }

        public boolean testItemStackMatch(ItemStack stack){
            if(arrayOverride){
                for (ItemDataSimple ids : itemList){
                    if(ids.item == stack.getItem()){
                        if(stack.getCount() >= ids.amount && testTag(stack, ids.compoundTag)){
                            this.amount = ids.amount;
                            this.remainderItem = ids.rItem;
                            this.remainderCompoundTag = ids.rTag;
                            this.remainderAmount = ids.remainderAmount;
                            return true;
                        }
                    }
                }
            } else {
                if((tagKey != null && stack.is(tagKey) || stack.is(item) && stack.getCount() >= this.amount)){
                    return testTag(stack);
                }
            }
            return false;
        }

        public boolean isEmpty() {
            return this.isEmpty;
        }

        public void processTags(){
            if(tagKey != null){
                for (ResourceLocation location : BuiltInRegistries.ITEM.keySet()){
                    Item i = BuiltInRegistries.ITEM.get(location);
                    if(i.builtInRegistryHolder().is(tagKey)){
                        this.applicableItems.add(i);
                    }
                }
            }
        }

        public void resetPos(){
            this.pos = 0;
        }

        public void next(){
            if(++pos >= this.applicableItems.size()){
                pos = 0;
            }
        }

        public ItemDataSimple getSimpleData(){
            return itemList.get(pos);
        }

        public Item getApplicableItem(){
            return this.applicableItems.get(pos);
        }
    }
    public static class ItemDataSimple {
        public transient Item item;
        public transient Item rItem;
        public transient CompoundTag compoundTag;
        public transient CompoundTag rTag;
        public String id;
        public String tag;
        public int amount;
        public String remainderId;

        public int remainderAmount;

        public String remainderTag;


        public ItemDataSimple() {
        }
    }
}
