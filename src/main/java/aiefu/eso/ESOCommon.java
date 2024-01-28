package aiefu.eso;

import aiefu.eso.network.NetworkManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Mod(ESOCommon.MOD_ID)
public class ESOCommon{

	public static final String MOD_ID = "enchanting-system-overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static ConfigurationFile config;

	public static ConcurrentHashMap<ResourceLocation, List<RecipeHolder>> recipeMap = new ConcurrentHashMap<>();

	public static final DeferredRegister<MenuType<?>> MENU_REGISTER = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ESOCommon.MOD_ID);
	public static final RegistryObject<MenuType<OverhauledEnchantmentMenu>> enchantment_menu_ovr =
			MENU_REGISTER.register("enchs_menu_ovr", () -> IForgeMenuType.create(OverhauledEnchantmentMenu::new));

	public ESOCommon(){
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::onInitialize);
		modEventBus.addListener(this::reloadListeners);
		modEventBus.addListener(this::registerCommands);
		modEventBus.addListener(this::serverStarted);
		modEventBus.addListener(this::copyPlayerData);
	}

	public void reloadListeners(final AddReloadListenerEvent event){
		event.addListener(EnchantmentRecipeDataListener::reload);
	}

	public void registerCommands(final RegisterCommandsEvent event){
		ESOCommands.register(event.getDispatcher());
	}

	public void serverStarted(final ServerStartedEvent event){
		recipeMap.values().forEach(l -> l.forEach(RecipeHolder::processTags));
	}

	public void copyPlayerData(final PlayerEvent.Clone event){
		IServerPlayerAcc old = ((IServerPlayerAcc) event.getOriginal());
		IServerPlayerAcc np = ((IServerPlayerAcc) event.getEntity());
		np.enchantment_overhaul$setUnlockedEnchantments(old.enchantment_overhaul$getUnlockedEnchantments());
	}

	public void onInitialize(final FMLCommonSetupEvent event) {
		try {
			this.genConfig();
			this.readConfig();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		NetworkManager.setup();
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

	public void readConfig() throws FileNotFoundException {
		ESOCommon.config = gson.fromJson(new FileReader("./config/eso/config.json"), ConfigurationFile.class);
	}

	public static Gson getGson(){
		return gson;
	}
}