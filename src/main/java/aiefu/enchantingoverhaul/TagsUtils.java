package aiefu.enchantingoverhaul;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class TagsUtils {
    public static boolean havePartialMatch(CompoundTag tag, CompoundTag ref){
        int matches = 0;
        for (String s : tag.getAllKeys()){
            if(ref.contains(s)){
                Tag other = ref.get(s);
                Tag main = tag.get(s);
                if(findPartialMatch(main, other)) matches++;
            }
        }
        return matches >= tag.getAllKeys().size();
    }

    public static boolean findPartialMatch(Tag main, Tag other){
        if(other instanceof CompoundTag ct && main instanceof CompoundTag ct2){
            return havePartialMatch(ct2, ct);
        } else if (other instanceof ListTag lt && main instanceof ListTag lt2) {
            return findPartialMatchInList(lt2, lt);
        } else return main.equals(other);
    }

    public static boolean findPartialMatchInList(ListTag main, ListTag other){
        int listMatches = 0;
        for (Tag t : main){
            for (Tag i : other){
                if(findPartialMatch(t, i)) listMatches++;
            }
        }
        return listMatches >= main.size();
    }
}
