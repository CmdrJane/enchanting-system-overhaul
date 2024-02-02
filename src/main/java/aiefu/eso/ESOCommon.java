package aiefu.eso;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class ESOCommon implements ModInitializer {

	public static final String MOD_ID = "enchanting-system-overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceLocation c2s_enchant_item = new ResourceLocation(MOD_ID, "c2s_enchant_item");

	public static final ResourceLocation s2c_data_sync = new ResourceLocation(MOD_ID, "s2c_data_sync");
	public static final ResourceLocation s2c_string_to_clipboard = new ResourceLocation(MOD_ID, "s2c_string_to_clipboard");

	public static final ResourceLocation enchantment_recipe_loader = new ResourceLocation(MOD_ID,"enchantment_recipe_loader");

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static ConfigurationFile config;

	public static MaterialOverrides mat_config;

	public static final ResourceLocation defaultRecipe = new ResourceLocation("default");
	public static ConcurrentHashMap<ResourceLocation, List<RecipeHolder>> recipeMap = new ConcurrentHashMap<>();

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
					ESOCommon.recipeMap.clear();
					data.forEach((key, value) -> {
						try {
							gson.fromJson(value.openAsReader(), RecipeHolder.class).processData();
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
					});
					try {
						RecipeHolder defaultHolder = new RecipeHolder();
						defaultHolder.enchantment_id = "default";
						defaultHolder.maxLevel = 0;
						defaultHolder.levels = gson.fromJson(new FileReader("./config/eso/default-recipe.json"), new TypeToken<Int2ObjectOpenHashMap<RecipeHolder.ItemData[]>>(){}.getType());
						defaultHolder.processData();
					} catch (FileNotFoundException e) {
						throw new RuntimeException(e);
					}
				}, executor);
			}

			@Override
			public ResourceLocation getFabricId() {
				return enchantment_recipe_loader;
			}

		});
		ServerPlayNetworking.registerGlobalReceiver(c2s_enchant_item, (server, player, handler, buf, responseSender) -> {
			String s = buf.readUtf();
			int ordinal = buf.readVarInt();
			ResourceLocation location = new ResourceLocation(s);
			server.execute(() -> {
				if(player.containerMenu instanceof OverhauledEnchantmentMenu m){
					m.checkRequirementsAndConsume(location, player, ordinal);
				}
			});
		});
		try {
			this.genConfig();
			this.readConfig();
			this.genDefaultRecipe();
			MaterialOverrides.generateDefault();
			mat_config = MaterialOverrides.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			server.execute(() -> {
				recipeMap.values().forEach(l -> l.forEach(RecipeHolder::processTags));
				if(server.isSingleplayer()){
					for (ServerPlayer player : server.getPlayerList().getPlayers()){
						if(!server.isSingleplayerOwner(player.getGameProfile())){
							ESOCommon.syncData(player);
						}
					}
				} else {
					server.getPlayerList().getPlayers().forEach(ESOCommon::syncData);
				}
			});
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server -> server.execute(() -> recipeMap.values().forEach(l -> l.forEach(RecipeHolder::processTags))));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ESOCommands.register(dispatcher));
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			IServerPlayerAcc old = ((IServerPlayerAcc) oldPlayer);
			IServerPlayerAcc np = ((IServerPlayerAcc) newPlayer);
			np.enchantment_overhaul$setUnlockedEnchantments(old.enchantment_overhaul$getUnlockedEnchantments());
		});
		LOGGER.info("ESO Initialized");
	}

	public void genConfig() throws IOException {
		Path p = Paths.get("./config/eso");
		if(!Files.exists(p)){
			Files.createDirectories(p);
		}
		String p2 = "./config/eso/config.json";
		if(!Files.exists(Paths.get(p2))){
			try(FileWriter writer = new FileWriter(p2)){
				gson.toJson(ConfigurationFile.getDefault(), writer);
			}
		}
	}

	public void readConfig() throws IOException {
		JsonObject jsonObject = JsonParser.parseReader(new FileReader("./config/eso/config.json")).getAsJsonObject();
		boolean shouldSave = false;
		if(!jsonObject.has("enableEnchantability")){
			jsonObject.addProperty("enableEnchantability", true);
			shouldSave = true;
		}
		if(!jsonObject.has("enableDefaultRecipe")){
			jsonObject.addProperty("enableDefaultRecipe", true);
			shouldSave = true;
		}
		ESOCommon.config = gson.fromJson(jsonObject, ConfigurationFile.class);
		if(shouldSave){
			try(FileWriter writer = new FileWriter("./config/eso/config.json")){
				gson.toJson(config, writer);
			}
		}
	}

	public void genDefaultRecipe() throws IOException{
		String p = "./config/eso/default-recipe.json";
		if(!Files.exists(Paths.get(p))){
			LinkedHashMap<Integer, RecipeHolder.ItemData[]> levels = new LinkedHashMap<>();
			int level = 1;
			for (int i = 0; i < 3; i++) {
				RecipeHolder.ItemData[] dataArr = new RecipeHolder.ItemData[4];
				dataArr[0] = new RecipeHolder.ItemData("minecraft:lapis_lazuli", 12);
				dataArr[1] = new RecipeHolder.ItemData("minecraft:amethyst_shard", 3);
				dataArr[2] = new RecipeHolder.ItemData("minecraft:gold_ingot", 6);
				dataArr[3] = new RecipeHolder.ItemData("minecraft:diamond", 3);
				levels.put(level, dataArr);
				level++;
			}
			for (int i = 0; i < 4; i++) {
				RecipeHolder.ItemData[] dataArr = new RecipeHolder.ItemData[4];
				dataArr[0] = new RecipeHolder.ItemData("minecraft:lapis_lazuli", 32);
				dataArr[1] = new RecipeHolder.ItemData("minecraft:amethyst_shard", 7);
				dataArr[2] = new RecipeHolder.ItemData("minecraft:gold_ingot", 24);
				dataArr[3] = new RecipeHolder.ItemData("minecraft:diamond", 7);
				levels.put(level, dataArr);
				level++;
			}
			for (int i = 0; i < 3; i++) {
				RecipeHolder.ItemData[] dataArr = new RecipeHolder.ItemData[4];
				dataArr[0] = new RecipeHolder.ItemData("minecraft:lapis_lazuli", 48);
				dataArr[1] = new RecipeHolder.ItemData("minecraft:amethyst_shard", 16);
				dataArr[2] = new RecipeHolder.ItemData("minecraft:gold_ingot", 32);
				dataArr[3] = new RecipeHolder.ItemData("minecraft:diamond", 16);
				levels.put(level, dataArr);
				level++;
			}
			try(FileWriter writer = new FileWriter("./config/eso/default-recipe.json")){
				gson.toJson(levels, writer);
			}
		}
	}

	@Nullable
	public static List<RecipeHolder> getRecipeHolders(ResourceLocation location){
		List<RecipeHolder> holders = ESOCommon.recipeMap.get(location);
		return holders != null ? holders : config.enableDefaultRecipe ? ESOCommon.recipeMap.get(defaultRecipe) : null;
	}

	public static void syncData(ServerPlayer player){
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		String n = "null";

		buf.writeVarInt(recipeMap.size());
		for (Map.Entry<ResourceLocation, List<RecipeHolder>> e : recipeMap.entrySet()){
			buf.writeUtf(e.getKey().toString());
			List<RecipeHolder> holders = e.getValue();
			buf.writeVarInt(holders.size());
			for (RecipeHolder holder : holders){
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
						if(data.itemArray != null){
							buf.writeBoolean(true);
							buf.writeVarInt(data.itemArray.length);
							for (RecipeHolder.ItemDataSimple ds : data.itemArray){
								buf.writeUtf(ds.id);
								buf.writeVarInt(ds.amount);
								String tag = ds.tag == null ? n : ds.tag;
								buf.writeUtf(tag);
								String rid = ds.remainderId == null ? n : ds.remainderId;
								buf.writeUtf(rid);
								buf.writeVarInt(ds.remainderAmount);
								String rtag = ds.remainderTag == null ? n : ds.remainderTag;
								buf.writeUtf(rtag);
							}
						} else buf.writeBoolean(false);

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
		}
		ServerPlayNetworking.send(player, s2c_data_sync, buf);
	}

	public static Gson getGson(){
		return gson;
	}
}