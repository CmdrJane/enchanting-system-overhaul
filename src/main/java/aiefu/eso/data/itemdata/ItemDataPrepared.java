package aiefu.eso.data.itemdata;

import aiefu.eso.TagsUtils;
import aiefu.eso.exception.ItemDoesNotExistException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ItemDataPrepared {
    protected boolean isEmpty = false;

    public Item item = Items.AIR;

    public int amount;

    public List<Item> applicableItems = new ArrayList<>();

    public List<ItemDataPrepared> itemList;

    public int pos = 0;

    @Nullable
    public TagKey<Item> tagKey;

    public CompoundTag compoundTag;

    public Item remainderItem = Items.AIR;

    public boolean remainderEmpty = false;

    public CompoundTag remainderCompoundTag;

    public int remainderAmount;

    public boolean arrayOverride = false;

    //For sync
    public ItemData data;

    public ItemDataPrepared() {
    }

    public ItemDataPrepared(Void v) {
        this.isEmpty = true;
    }

    public ItemDataPrepared(ItemData data, ResourceLocation location, String eid, boolean shouldProcessArrays) throws ItemDoesNotExistException {
        this.data = data;
        if (data.itemArray != null && shouldProcessArrays) {
            this.itemList = new ArrayList<>();
            for(ItemData d : data.itemArray) {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(d.id));
                if (item == Items.AIR)
                    throw new ItemDoesNotExistException("Item id " + d + " in id array not found in game registry for enchantment recipe " + eid);

                this.applicableItems.add(item);

                this.itemList.add(new ItemDataPrepared(d, location, eid, false));
                this.arrayOverride = true;
            }
        } else {
            if (data.id == null) {
                this.isEmpty = true;
                return;
            }
            if (data.id.startsWith("tags#")) {
                this.tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(data.id.substring(5)));
            } else {
                this.item = BuiltInRegistries.ITEM.get(new ResourceLocation(data.id));
                if (this.item == Items.AIR)
                    throw new ItemDoesNotExistException("Item id " + data.id + " not found in game registry for enchantment recipe " + eid);
            }

            this.amount = data.amount;

            if (data.tag != null) {
                try {
                    this.compoundTag = new TagParser(new StringReader(data.tag)).readStruct();
                } catch (CommandSyntaxException e) {
                    e.printStackTrace();
                }
            }
            if (data.remainderId != null) {
                if (!data.remainderId.isEmpty() && !data.remainderId.isBlank() && !data.remainderId.equalsIgnoreCase("empty")) {
                    this.remainderItem = BuiltInRegistries.ITEM.get(new ResourceLocation(data.remainderId));
                    if (remainderItem == Items.AIR)
                        throw new ItemDoesNotExistException("Remainder item id " + data.remainderId + " not found in game registry for enchantment recipe " + eid);
                } else remainderEmpty = true;
                remainderAmount = data.remainderAmount;
                if (data.remainderTag != null) {
                    try {
                        this.remainderCompoundTag = new TagParser(new StringReader(data.remainderTag)).readStruct();
                    } catch (CommandSyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public @Nullable ItemStack getRemainderForStack(ItemStack stack) {
        if (this.remainderEmpty) {
            return stack.getRecipeRemainder();
        } else if (this.remainderItem != Items.AIR) {
            ItemStack s = new ItemStack(this.remainderItem, this.remainderAmount);
            s.setTag(this.remainderCompoundTag);
            return s;
        } else return stack.getRecipeRemainder();
    }

    public boolean testTag(ItemStack stack) {
        if (this.compoundTag == null) {
            return true;
        } else if (!stack.hasTag()) {
            return false;
        } else return TagsUtils.havePartialMatch(this.compoundTag, stack.getOrCreateTag());
    }

    public boolean testTag(ItemStack stack, CompoundTag ref) {
        if (this.compoundTag == null) {
            return true;
        } else if (!stack.hasTag()) {
            return false;
        } else return TagsUtils.havePartialMatch(ref, stack.getOrCreateTag());
    }

    public boolean testItemStackMatch(ItemStack stack) {
        if (arrayOverride) {
            for (ItemDataPrepared ids : itemList) {
                if (ids.item == stack.getItem()) {
                    if (stack.getCount() >= ids.amount && testTag(stack, ids.compoundTag)) {
                        this.amount = ids.amount;
                        this.remainderItem = ids.remainderItem;
                        this.remainderCompoundTag = ids.remainderCompoundTag;
                        this.remainderAmount = ids.remainderAmount;
                        return true;
                    }
                }
            }
        } else {
            if ((tagKey != null && stack.is(tagKey) || stack.is(item)) && stack.getCount() >= this.amount) {
                return testTag(stack);
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public void processTags() {
        if (tagKey != null) {
            for(Item i : BuiltInRegistries.ITEM){
                if (i.builtInRegistryHolder().is(tagKey)) {
                    this.applicableItems.add(i);
                }
            }
        }
    }

    public void resetPos() {
        this.pos = 0;
    }

    public void next() {
        if (++pos >= this.applicableItems.size()) {
            pos = 0;
        }
    }

    public ItemDataPrepared getNotNestedData() {
        return itemList.get(pos);
    }

    public Item getApplicableItem() {
        return this.applicableItems.get(pos);
    }
}
