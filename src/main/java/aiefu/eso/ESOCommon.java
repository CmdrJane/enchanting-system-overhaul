package aiefu.eso;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(ESOCommon.MOD_ID)
public class ESOCommon{

	public static final String MOD_ID = "enchanting-system-overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceLocation c2s_enchant_item = new ResourceLocation(MOD_ID, "c2s_enchant_item");

	public static final ResourceLocation s2c_data_sync = new ResourceLocation(MOD_ID, "s2c_data_sync");

	public static final ResourceLocation enchantment_recipe_loader = new ResourceLocation(MOD_ID,"enchantment_recipe_loader");

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
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		LOGGER.info("Hello Fabric world!");
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

	public void readConfig() throws FileNotFoundException {
		ESOCommon.config = gson.fromJson(new FileReader("./config/eso/config.json"), ConfigurationFile.class);
	}

	public static Gson getGson(){
		return gson;
	}
}