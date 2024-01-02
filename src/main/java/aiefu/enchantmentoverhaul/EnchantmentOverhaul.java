package aiefu.enchantmentoverhaul;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class EnchantmentOverhaul implements ModInitializer {

	public static final String MOD_ID = "enchantment-overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceLocation c2s_enchant_item = new ResourceLocation(MOD_ID, "c2s_enchant_item");

	public static final ResourceLocation s2c_data_sync = new ResourceLocation(MOD_ID, "s2c_data_sync");

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static ConfigurationFile config;

	public static ConcurrentHashMap<ResourceLocation, RecipeHolder> recipeMap = new ConcurrentHashMap<>();


	public static final ExtendedScreenHandlerType<OverhauledEnchantmentMenu> enchantment_menu_ovr =
			Registry.register(BuiltInRegistries.MENU, new ResourceLocation(MOD_ID, "enchs_menu_ovr"), new ExtendedScreenHandlerType<>(OverhauledEnchantmentMenu::new));


	@Override
	public void onInitialize() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleResourceReloadListener<Map<ResourceLocation, Resource>>() {

			@Override
			public CompletableFuture<Map<ResourceLocation, Resource>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
				return CompletableFuture.supplyAsync(() -> manager.listResources("ench-recipes", resourceLocation -> resourceLocation.getPath().endsWith(".json")), executor);
			}

			@Override
			public CompletableFuture<Void> apply(Map<ResourceLocation, Resource> data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
				return CompletableFuture.runAsync(() -> {
					EnchantmentOverhaul.recipeMap.clear();
					Gson gson = new Gson();
					data.forEach((key, value) -> {
						try {
							gson.fromJson(value.openAsReader(), RecipeHolder.class).processData();
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
					});
				}, executor);
			}

			@Override
			public ResourceLocation getFabricId() {
				return new ResourceLocation(MOD_ID, "enchantment_recipe_loader");
			}

		});
		ServerPlayNetworking.registerGlobalReceiver(c2s_enchant_item, (server, player, handler, buf, responseSender) -> {
			String s = buf.readUtf();
			ResourceLocation location = new ResourceLocation(s);
			server.execute(() -> {
				if(player.containerMenu instanceof OverhauledEnchantmentMenu m){
					m.checkRequirementsAndConsume(location, player);
				}
			});
		});
		try {
			this.genConfig();
			this.readConfig();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if(!server.isDedicatedServer()){
				server.execute(() -> recipeMap.values().forEach(RecipeHolder::processTags));
				server.getPlayerList().getPlayers().forEach(EnchantmentOverhaul::syncData);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if(!server.isDedicatedServer()){
				server.execute(() -> recipeMap.values().forEach(RecipeHolder::processTags));
			}
		});
		LOGGER.info("Hello Fabric world!");
	}

	public void genConfig() throws IOException {
		Path p = Paths.get("./config/enchantment-overhaul");
		if(!Files.exists(p)){
			Files.createDirectory(p);
		}
		String p2 = "./config/enchantment-overhaul/config.json";
		if(!Files.exists(Paths.get(p2))){
			try(FileWriter writer = new FileWriter(p2)){
				gson.toJson(ConfigurationFile.getDefault(), writer);
			}
		}
	}

	public static void syncData(ServerPlayer player){
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		String n = "null";

		buf.writeVarInt(recipeMap.size());
		for (Map.Entry<ResourceLocation, RecipeHolder> e : recipeMap.entrySet()){
			buf.writeUtf(e.getKey().toString());
			RecipeHolder holder = e.getValue();
			buf.writeUtf(holder.enchantment_id);
			buf.writeVarInt(holder.maxLevel);

			buf.writeVarInt(holder.levels.size());
			for (Int2ObjectMap.Entry<RecipeHolder.ItemData[]> entry : holder.levels.int2ObjectEntrySet()){
				buf.writeVarInt(entry.getIntKey());
				RecipeHolder.ItemData[] arr = entry.getValue();
				buf.writeVarInt(arr.length);
				for (RecipeHolder.ItemData data : arr){
					String id = data.id == null ? n : data.id;
					buf.writeUtf(id);
					buf.writeVarInt(data.amount);
					String tag = data.tag == null ? n : data.tag;
					buf.writeUtf(tag);
					String remainder = data.remainderId == null ? n : data.remainderId;
					buf.writeUtf(remainder);
					buf.writeVarInt(data.remainderAmount);
					String remainderTag = data.remainderTag == null ? n : data.remainderTag;
					buf.writeUtf(remainderTag);
				}
			}
		}
		ServerPlayNetworking.send(player, s2c_data_sync, buf);
	}

	public void readConfig() throws FileNotFoundException {
		EnchantmentOverhaul.config = gson.fromJson(new FileReader("./config/enchantment-overhaul/config.json"), ConfigurationFile.class);
	}
}