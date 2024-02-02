package aiefu.eso;

import aiefu.eso.client.ESOClient;
import aiefu.eso.datalisteners.EnchantmentRecipeDataListener;
import aiefu.eso.datalisteners.MaterialOverridesDataListener;
import aiefu.eso.network.NetworkManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
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

@Mod(ESOCommon.MOD_ID)
public class ESOCommon{

	public static final String MOD_ID = "enchanting_system_overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static ConfigurationFile config;

	public static MaterialOverrides mat_config;

	public static final ResourceLocation defaultRecipe = new ResourceLocation("default");
	public static ConcurrentHashMap<ResourceLocation, List<RecipeHolder>> recipeMap = new ConcurrentHashMap<>();

	public static final DeferredRegister<MenuType<?>> MENU_REGISTER = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ESOCommon.MOD_ID);
	public static final RegistryObject<MenuType<OverhauledEnchantmentMenu>> enchantment_menu_ovr =
			MENU_REGISTER.register("enchs_menu_ovr", () -> IForgeMenuType.create(OverhauledEnchantmentMenu::new));

	public ESOCommon(){
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::onInitialize);
		MENU_REGISTER.register(modEventBus);
		if(FMLEnvironment.dist.isClient()){
			modEventBus.addListener(ESOClient::onInitializeClient);
			modEventBus.addListener(ESOClient::onLoadComplete);
		}
	}

	public void onInitialize(final FMLCommonSetupEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
		try {
			this.genConfig();
			this.readConfig();
			this.genDefaultRecipe();
			MaterialOverrides.generateDefault();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		NetworkManager.setup();
		LOGGER.info("ESO Initialized");
	}

	@SubscribeEvent
	public void reloadListeners(final AddReloadListenerEvent event){
		event.addListener(EnchantmentRecipeDataListener::reload);
		event.addListener(MaterialOverridesDataListener::reload);
	}

	@SubscribeEvent
	public void registerCommands(final RegisterCommandsEvent event){
		ESOCommands.register(event.getDispatcher());
	}

	@SubscribeEvent
	public void serverStarted(final ServerStartedEvent event){
		recipeMap.values().forEach(l -> l.forEach(RecipeHolder::processTags));
	}

	@SubscribeEvent
	public void copyPlayerData(final PlayerEvent.Clone event){
		IServerPlayerAcc old = ((IServerPlayerAcc) event.getOriginal());
		IServerPlayerAcc np = ((IServerPlayerAcc) event.getEntity());
		np.enchantment_overhaul$setUnlockedEnchantments(old.enchantment_overhaul$getUnlockedEnchantments());
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

	public static Gson getGson(){
		return gson;
	}
}