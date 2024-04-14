package aiefu.eso;

import aiefu.eso.data.EnchantmentRecipesLoader;
import aiefu.eso.data.MaterialDataLoader;
import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.itemdata.ItemData;
import aiefu.eso.data.materialoverrides.MaterialOverrides;
import aiefu.eso.menu.OverhauledEnchantmentMenu;
import aiefu.eso.network.ServersideNetworkManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ESOCommon implements ModInitializer {

	public static final String MOD_ID = "enchanting-system-overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static ConfigurationFile config;

	public static MaterialOverrides mat_config;

	public static final ResourceLocation defaultRecipe = new ResourceLocation("default");
	public static ConcurrentHashMap<ResourceLocation, List<RecipeHolder>> recipeMap = new ConcurrentHashMap<>();

	public static final ExtendedScreenHandlerType<OverhauledEnchantmentMenu> enchantment_menu_ovr =
			Registry.register(BuiltInRegistries.MENU, new ResourceLocation(MOD_ID, "enchs_menu_ovr"), new ExtendedScreenHandlerType<>(OverhauledEnchantmentMenu::new));


	@Override
	public void onInitialize() {
		EnchantmentRecipesLoader.registerReloadListener();
		MaterialDataLoader.registerReloadListener(getGson());
		ServersideNetworkManager.registerReceivers();
		try {
			this.genConfig();
			this.readConfig();
			this.genDefaultRecipe();
			MaterialOverrides.generateDefault();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			server.execute(() -> {
				recipeMap.values().forEach(l -> l.forEach(RecipeHolder::processTags));
				server.getPlayerList().getPlayers().forEach(player -> {
					if(!server.isSingleplayerOwner(player.getGameProfile())){
						ServersideNetworkManager.syncData(player);
						ServersideNetworkManager.syncMatConfig(player);
					}
				});
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
		if(!jsonObject.has("disableDiscoverySystem")){
			jsonObject.addProperty("disableDiscoverySystem", false);
			shouldSave = true;
		}
		if(!jsonObject.has("enableEnchantmentsLeveling")){
			jsonObject.addProperty("enableEnchantmentsLeveling", false);
			shouldSave = true;
		}
		if(!jsonObject.has("hideEnchantmentsWithoutRecipe")){
			jsonObject.addProperty("hideEnchantmentsWithoutRecipe", false);
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
			LinkedHashMap<Integer, ItemData[]> levels = new LinkedHashMap<>();
			int level = 1;
			for (int i = 0; i < 3; i++) {
				ItemData[] dataArr = new ItemData[4];
				dataArr[0] = new ItemData("minecraft:lapis_lazuli", 12);
				dataArr[1] = new ItemData("minecraft:amethyst_shard", 3);
				dataArr[2] = new ItemData("minecraft:gold_ingot", 6);
				dataArr[3] = new ItemData("minecraft:diamond", 3);
				levels.put(level, dataArr);
				level++;
			}
			for (int i = 0; i < 4; i++) {
				ItemData[] dataArr = new ItemData[4];
				dataArr[0] = new ItemData("minecraft:lapis_lazuli", 32);
				dataArr[1] = new ItemData("minecraft:amethyst_shard", 7);
				dataArr[2] = new ItemData("minecraft:gold_ingot", 24);
				dataArr[3] = new ItemData("minecraft:diamond", 7);
				levels.put(level, dataArr);
				level++;
			}
			for (int i = 0; i < 3; i++) {
				ItemData[] dataArr = new ItemData[4];
				dataArr[0] = new ItemData("minecraft:lapis_lazuli", 48);
				dataArr[1] = new ItemData("minecraft:amethyst_shard", 16);
				dataArr[2] = new ItemData("minecraft:gold_ingot", 32);
				dataArr[3] = new ItemData("minecraft:diamond", 16);
				levels.put(level, dataArr);
				level++;
			}
			try(FileWriter writer = new FileWriter(p)){
				gson.toJson(levels, writer);
			}
		}
		String p2 = "./config/eso/default-xp-map.json";
		if(!Files.exists(Paths.get(p2))){
			JsonObject tree = new JsonObject();
			tree.addProperty("useExpPoints", false);
			tree.add("xp", gson.toJsonTree(new Int2IntOpenHashMap()));
			try(FileWriter writer = new FileWriter(p2)){
				gson.toJson(tree, writer);
			}
		}
	}

	@Nullable
	public static List<RecipeHolder> getRecipeHolders(ResourceLocation location){
		List<RecipeHolder> holders = ESOCommon.recipeMap.get(location);
		return holders != null ? holders : config.enableDefaultRecipe ? ESOCommon.recipeMap.get(defaultRecipe) : null;
	}

	public static int getMaximumPossibleEnchantmentLevel(Enchantment enchantment){
		ResourceLocation location = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
		List<RecipeHolder> holders = ESOCommon.recipeMap.get(location);
		int maxLevel = 0;
		if(holders != null && !holders.isEmpty()){
			for (RecipeHolder holder : holders){
				int l = holder.getMaxLevel(enchantment);
				if(l > maxLevel){
					maxLevel = l;
				}
			}
		} else maxLevel = enchantment.getMaxLevel();
		return maxLevel;
	}

	public static Gson getGson(){
		return gson;
	}
}