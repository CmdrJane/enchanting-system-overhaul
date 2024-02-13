package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import aiefu.eso.data.materialoverrides.MaterialData;
import aiefu.eso.data.materialoverrides.MaterialOverrides;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MaterialDataLoader {
    public static final ResourceLocation mat_data_loader = new ResourceLocation(ESOCommon.MOD_ID,"mat_data_loader");
    public static void registerReloadListener(Gson gson){
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleResourceReloadListener<Map<ResourceLocation, Resource>>() {
            @Override
            public ResourceLocation getFabricId() {
                return mat_data_loader;
            }

            @Override
            public CompletableFuture<Map<ResourceLocation, Resource>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
                return CompletableFuture.supplyAsync(() -> manager.listResources("mat-overrides", resourceLocation -> resourceLocation.getPath().endsWith(".json")), executor);
            }

            @Override
            public CompletableFuture<Void> apply(Map<ResourceLocation, Resource> data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
                return CompletableFuture.runAsync(() -> {
                    HashMap<String, MaterialData> armor = new HashMap<>();
                    HashMap<String, MaterialData> tools = new HashMap<>();
                    HashMap<String, MaterialData> items = new HashMap<>();
                    data.forEach((l, r) -> {
                        try {
                            JsonElement jsonTree = JsonParser.parseReader(r.openAsReader());
                            JsonObject obj = jsonTree.getAsJsonObject();
                            String id = obj.get("material_id").getAsString();
                            String type = obj.get("type").getAsString();
                            MaterialData md = gson.fromJson(jsonTree, MaterialData.class);
                            if(type.equalsIgnoreCase("armor")){
                                armor.put(id, md);
                            } else if(type.equalsIgnoreCase("tool")){
                                tools.put(id, md);
                            } else if(type.equalsIgnoreCase("item")){
                                items.put(id, md);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    try {
                        ESOCommon.mat_config = MaterialOverrides.readWithAttachments(tools, armor, items);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }, executor);
            }
        });
    }
}
