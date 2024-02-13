package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import aiefu.eso.data.materialoverrides.MaterialData;
import aiefu.eso.data.materialoverrides.MaterialOverrides;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MaterialDataLoader {
    public static final ResourceLocation mat_data_loader = new ResourceLocation(ESOCommon.MOD_ID,"mat_data_loader");

    public static CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                                 ResourceManager manager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor backgroundExecutor, Executor gameExecutor){
        return CompletableFuture.supplyAsync(() -> manager.listResources("mat-overrides", resourceLocation -> resourceLocation.getPath().endsWith(".json")), backgroundExecutor)
                .thenCompose(preparationBarrier::wait).thenAcceptAsync(map -> {
                    HashMap<String, MaterialData> armor = new HashMap<>();
                    HashMap<String, MaterialData> tools = new HashMap<>();
                    HashMap<String, MaterialData> items = new HashMap<>();
                    map.forEach((l, r) -> {
                        try {
                            JsonElement jsonTree = JsonParser.parseReader(r.openAsReader());
                            JsonObject obj = jsonTree.getAsJsonObject();
                            String id = obj.get("material_id").getAsString();
                            String type = obj.get("type").getAsString();
                            MaterialData md = ESOCommon.getGson().fromJson(jsonTree, MaterialData.class);
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
                }, gameExecutor);
    }
}
