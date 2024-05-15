package aiefu.eso.data.itemdata;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RecipeViewerItemData {
    protected ItemDataPrepared itemData;
    protected ItemStack stack = ItemStack.EMPTY;
    protected List<ItemStack> animatedStacks;

    protected boolean isAnimated = false;
    protected transient int pos = 0;

    public RecipeViewerItemData(ItemDataPrepared itemData) {
        this.itemData = itemData;
        this.animatedStacks = new ArrayList<>();
        if(!itemData.applicableItems.isEmpty()){
            if(itemData.itemList != null){
                for (ItemDataPrepared prepared : itemData.itemList){
                    ItemStack s = new ItemStack(prepared.item, prepared.amount);
                    if(prepared.compoundTag != null){
                        s.setTag(prepared.compoundTag);
                    }
                    this.animatedStacks.add(s);
                }
            } else if(itemData.tagKey != null){
                for (Item item : itemData.applicableItems){
                    this.animatedStacks.add(new ItemStack(item, itemData.amount));
                }
            }
            this.isAnimated = animatedStacks.size() > 0;
        } else {
            this.stack = new ItemStack(itemData.item, itemData.amount);
            if(itemData.compoundTag != null){
                stack.setTag(itemData.compoundTag);
            }
        }
    }

    public void next(){
        if(pos + 1 >= animatedStacks.size()){
            pos = 0;
        } else pos += 1;
    }

    public ItemStack getNextStack(){
        return animatedStacks.get(pos);
    }

    public ItemStack getStack() {
        return stack;
    }

    public boolean isAnimated(){
        return this.isAnimated;
    }

}
