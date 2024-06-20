package aiefu.eso.data.materialoverrides;

import aiefu.eso.ESOCommon;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class MaterialOverrides {
    public static final MaterialData defaultMatData = new MaterialData(ESOCommon.config.maxEnchantments, ESOCommon.config.maxCurses, ESOCommon.config.enchantmentLimitIncreasePerCurse);
    public HashMap<Item, MaterialData> toolsMatOverridesCompiled = new HashMap<>();

    public HashMap<Item, MaterialData> armorMatOverridesCompiled = new HashMap<>();

    public HashMap<Item, MaterialData> hardOverridesCompiled = new HashMap<>();

    public MaterialOverrides(HashMap<String, MaterialData>  toolsMatOverrides, HashMap<String, MaterialData>  armorMatOverrides, HashMap<String, MaterialData>  hardOverrides){
        hardOverrides.forEach((k, v) -> {
            Item item = Registry.ITEM.get(new ResourceLocation(k));
            if(item != Items.AIR){
                this.hardOverridesCompiled.put(item, v);
            }
        });
        for (Item item : Registry.ITEM){
            if(item instanceof TieredItem ti){
                Tier mat = ti.getTier();
                String name = mat instanceof Enum<?> e ? e.name() : mat.getClass().getSimpleName();
                MaterialData data = toolsMatOverrides.get(name);
                if(data != null){
                    this.toolsMatOverridesCompiled.put(item, data);
                }
            } else if (item instanceof ArmorItem am){
                MaterialData data = armorMatOverrides.get(am.getMaterial().getName());
                if(data != null){
                    this.armorMatOverridesCompiled.put(item, data);
                }
            }
        }
    }

    public MaterialOverrides() {
    }

    public MaterialData getMaterialData(Item item){
        MaterialData data = hardOverridesCompiled.get(item);
        if(data != null){
            return data;
        } else {
            if(item instanceof TieredItem){
                data = toolsMatOverridesCompiled.get(item);
            } else if (item instanceof ArmorItem) {
                data = armorMatOverridesCompiled.get(item);
            }
            if(data != null){
                return data;
            }
        }
        return defaultMatData;
    }

    public static void generateDefault() throws IOException {
        Path folder = Paths.get("./config/eso/material-overrides");
        if(!Files.exists(folder)){
            Files.createDirectories(folder);
        }
        Gson gson = ESOCommon.getGson();
        String tools = "./config/eso/material-overrides/tools.json";
        if(!Files.exists(Paths.get(tools))){

            HashMap<String, MaterialData> toolsMatOverrides = new HashMap<>();
            toolsMatOverrides.put("WOOD", new MaterialData(4, 1, 1));
            toolsMatOverrides.put("STONE", new MaterialData(2, 1, 1));
            toolsMatOverrides.put("IRON", new MaterialData(3, 1, 1));
            toolsMatOverrides.put("DIAMOND", new MaterialData(4, 1, 1));
            toolsMatOverrides.put("GOLD", new MaterialData(6, 1, 1));
            toolsMatOverrides.put("NETHERITE", new MaterialData(5, 1, 1));

            try(FileWriter writer = new FileWriter(tools)){
                gson.toJson(toolsMatOverrides, writer);
            }
        }
        String armor = "./config/eso/material-overrides/armor.json";
        if(!Files.exists(Paths.get(armor))){

            HashMap<String, MaterialData> armorMatOverrides = new HashMap<>();
            armorMatOverrides.put("leather", new MaterialData(5, 1, 1));
            armorMatOverrides.put("chainmail", new MaterialData(4, 1, 1));
            armorMatOverrides.put("iron", new MaterialData(3, 1, 1));
            armorMatOverrides.put("gold", new MaterialData(6, 1, 1));
            armorMatOverrides.put("diamond", new MaterialData(4, 1, 1));
            armorMatOverrides.put("turtle", new MaterialData(5, 1, 1));
            armorMatOverrides.put("netherite", new MaterialData(5, 1, 1));

            try(FileWriter writer = new FileWriter(armor)){
                gson.toJson(armorMatOverrides, writer);
            }
        }
        String items = "./config/eso/material-overrides/items.json";
        if(!Files.exists(Paths.get(items))){

            HashMap<String, MaterialData>hardOverrides = new HashMap<>();
            hardOverrides.put("minecraft:fishing_rod",new MaterialData(5, 1, 1));
            hardOverrides.put("minecraft:bow", new MaterialData(4, 1, 1));
            hardOverrides.put("minecraft:crossbow", new MaterialData(4, 1, 1));
            hardOverrides.put("minecraft:trident", new MaterialData(4, 1, 1));

            try(FileWriter writer = new FileWriter(items)){
                gson.toJson(hardOverrides, writer);
            }
        }
    }

    public static MaterialOverrides read() throws FileNotFoundException {
        Gson gson = ESOCommon.getGson();
        Type type = new TypeToken<HashMap<String, MaterialData>>(){}.getType();
        HashMap<String, MaterialData> toolsMat = gson.fromJson(new FileReader("./config/eso/material-overrides/tools.json"), type);
        HashMap<String, MaterialData>  armorMat = gson.fromJson(new FileReader("./config/eso/material-overrides/armor.json"), type);
        HashMap<String, MaterialData>  hardOverrides = gson.fromJson(new FileReader("./config/eso/material-overrides/items.json"), type);

        return new MaterialOverrides(toolsMat, armorMat, hardOverrides);
    }

    public static MaterialOverrides readWithAttachments(HashMap<String, MaterialData> tools, HashMap<String, MaterialData> armor, HashMap<String, MaterialData> items) throws FileNotFoundException {
        Gson gson = ESOCommon.getGson();
        Type type = new TypeToken<HashMap<String, MaterialData>>(){}.getType();
        HashMap<String, MaterialData> toolsMat = gson.fromJson(new FileReader("./config/eso/material-overrides/tools.json"), type);
        HashMap<String, MaterialData>  armorMat = gson.fromJson(new FileReader("./config/eso/material-overrides/armor.json"), type);
        HashMap<String, MaterialData>  hardOverrides = gson.fromJson(new FileReader("./config/eso/material-overrides/items.json"), type);

        tools.putAll(toolsMat);
        armor.putAll(armorMat);
        items.putAll(hardOverrides);

        return new MaterialOverrides(tools, armor, items);
    }

    public static MaterialOverrides reconstructFromPacket(HashMap<String, MaterialData> tools, HashMap<String, MaterialData> armor, HashMap<String, MaterialData> items){
        HashMap<Item, MaterialData> toolsOverrides = new HashMap<>();
        tools.forEach((k, v) -> {
            Item i = Registry.ITEM.get(new ResourceLocation(k));
            toolsOverrides.put(i, v);
        });

        HashMap<Item, MaterialData> armorOverrides = new HashMap<>();
        armor.forEach((k, v) -> {
            Item i = Registry.ITEM.get(new ResourceLocation(k));
            armorOverrides.put(i, v);
        });

        HashMap<Item, MaterialData> hardOverrides = new HashMap<>();
        items.forEach((k, v) -> {
            Item i = Registry.ITEM.get(new ResourceLocation(k));
            hardOverrides.put(i, v);
        });
        MaterialOverrides m = new MaterialOverrides();
        m.hardOverridesCompiled = hardOverrides;
        m.toolsMatOverridesCompiled = toolsOverrides;
        m.armorMatOverridesCompiled = armorOverrides;
        return m;
    }

}
