package aiefu.eso;

import aiefu.eso.network.PacketIdentifiers;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

public class ESOCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("eso").requires(stack -> stack.hasPermission(4)).then(Commands.literal("learn")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("enchantment-id", StringArgumentType.greedyString())
                                .executes(ctx -> learnEnchantmentById(ctx, EntityArgument.getPlayer(ctx,"player"), StringArgumentType.getString(ctx,"enchantment-id")))))));
        dispatcher.register(Commands.literal("eso").requires(stack -> stack.hasPermission(4)).then(Commands.literal("forget")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("enchantment-id", StringArgumentType.greedyString())
                                .executes(ctx -> forgetEnchantment(ctx, EntityArgument.getPlayer(ctx,"player"), StringArgumentType.getString(ctx,"enchantment-id")))))));
        dispatcher.register(Commands.literal("eso").requires(stack -> stack.hasPermission(4)).then(Commands.literal("get-mat-id-in-hand")
                .executes(ESOCommands::getMaterialId)));
        dispatcher.register(Commands.literal("eso").requires(stack -> stack.hasPermission(4)).then(Commands.literal("get-item-id-in-hand")
                .executes(ESOCommands::getItemId)));
        dispatcher.register(Commands.literal("eso").requires(stack -> stack.hasPermission(4)).then(Commands.literal("get-enchantment-id-in-hand")
                .executes(ESOCommands::getEnchantmentId)));
        dispatcher.register(Commands.literal("eso").requires(stack -> stack.hasPermission(4)).then(Commands.literal("learn-leveled")
                .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("enchantment-id", StringArgumentType.greedyString())
                        .then(Commands.argument("operation", StringArgumentType.string()).then(Commands.argument("level", IntegerArgumentType.integer(1)).executes(context ->
                                setLeveledEnchantment(context, EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "enchantment-id"),
                                        StringArgumentType.getString(context, "operation"), IntegerArgumentType.getInteger(context, "level")))))))));
    }

    public static int setLeveledEnchantment(CommandContext<CommandSourceStack> ctx, ServerPlayer targetPlayer, String enchantmentId, String operation, int level){
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(enchantmentId));
        if(enchantment != null){
            MutableComponent c  = Component.translatable(enchantment.getDescriptionId());
            Object2IntOpenHashMap<Enchantment> learnedEnchantments = ((IServerPlayerAcc)targetPlayer).enchantment_overhaul$getUnlockedEnchantments();
            int maxLevel = ESOCommon.getMaximumPossibleEnchantmentLevel(enchantment);
            int i = learnedEnchantments.getInt(enchantment);
            switch (operation){
                case "add" -> {
                    int r = Math.min(maxLevel, i + level);
                    learnedEnchantments.put(enchantment, r);
                    ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.successfullyadded", c, r, targetPlayer.getDisplayName()), true);
                    targetPlayer.sendSystemMessage(Component.translatable("eso.youlearned", getFormattedNameLeveled(enchantment, r)).withStyle(ChatFormatting.GOLD), true);
                }
                case "set" -> {
                    int r = Math.min(level, maxLevel);
                    if(r == i){
                        ctx.getSource().sendFailure(Component.translatable("eso.command.feedback.playerknows", targetPlayer.getDisplayName(), c));
                    } else {
                        learnedEnchantments.put(enchantment, Math.min(level, maxLevel));
                        ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.successfullyadded", c, r, targetPlayer.getDisplayName()), true);
                        targetPlayer.sendSystemMessage(Component.translatable("eso.youlearned", getFormattedNameLeveled(enchantment, r)).withStyle(ChatFormatting.GOLD), true);
                    }
                }
                case "sub", "subtract" -> {
                    int r = i - level;
                    if(r < 1){
                        learnedEnchantments.removeInt(enchantment);
                        targetPlayer.sendSystemMessage(Component.translatable("eso.youforgot", c), true);
                        ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.removedEnchantment", c, targetPlayer.getDisplayName()), true);
                    } else {
                        learnedEnchantments.put(enchantment, r);
                        ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.successfullyadded", c, r, targetPlayer.getDisplayName()), true);
                        targetPlayer.sendSystemMessage(Component.translatable("eso.youlearned", getFormattedNameLeveled(enchantment, r)).withStyle(ChatFormatting.GOLD), true);
                    }
                }
                default -> ctx.getSource().sendFailure(Component.translatable("eso.command.feedback.invalidoperation"));
            }
        } else ctx.getSource().sendFailure(Component.translatable("eso.command.encantmentnotfound", enchantmentId));
        return 0;
    }

    public static MutableComponent getFormattedNameLeveled(Enchantment e, int l){
        MutableComponent msg = Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE);
        msg.append(e.getFullname(l));
        msg.append(Component.literal("]"));
        return msg;
    }

    public static int getMaterialId(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Item item = player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
        String mat;
        if(item instanceof TieredItem ti){
            Tier t = ti.getTier();
            if(t instanceof Enum<?> e){
                mat = e.name();
            } else mat = t.getClass().getSimpleName();
        } else if(item instanceof ArmorItem ai){
            mat = ai.getMaterial().getName();
        } else {
            mat = "null";
        }
        ctx.getSource().sendSuccess(() -> Component.literal(mat), true);
        copyToClipboard(player, mat);
        return 0;
    }

    public static int getItemId(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Item item = player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
        ctx.getSource().sendSuccess(() -> Component.literal(loc.toString()), true);
        copyToClipboard(player, loc.toString());
        return 0;
    }

    public static int getEnchantmentId(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if(stack.isEnchanted() || containsStoredEnchantments(stack)){
            Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
            StringBuilder enchantments = new StringBuilder();
            for (Map.Entry<Enchantment, Integer> e : enchs.entrySet()) {
                ResourceLocation loc = BuiltInRegistries.ENCHANTMENT.getKey(e.getKey());
                if(loc != null){
                    String s = loc.toString();
                    ctx.getSource().sendSuccess(() -> Component.literal(s), true);
                    enchantments.append(s).append(" ");
                }
            }
            String s = enchantments.toString();
            copyToClipboard(player, s.substring(0, s.length() - 1));
        }
        return 0;
    }

    public static boolean containsStoredEnchantments(ItemStack stack){
        CompoundTag tag = stack.getOrCreateTag();
        return tag.contains("StoredEnchantments", Tag.TAG_LIST);
    }
    
    public static void copyToClipboard(ServerPlayer player, String s){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(s);
        ServerPlayNetworking.send(player, PacketIdentifiers.s2c_string_to_clipboard, buf);
    }

    public static int forgetEnchantment(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String id){
        if(id.equalsIgnoreCase("all")){
            revokeAll(ctx, player);
        } else {
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(id));
            if(enchantment != null && player instanceof IServerPlayerAcc acc){
                MutableComponent discId = Component.translatable(enchantment.getDescriptionId());
                MutableComponent c = Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE);
                c.append(discId);
                c.append(Component.literal("]"));
                if(acc.enchantment_overhaul$getUnlockedEnchantments().removeInt(enchantment) != 0){
                    player.sendSystemMessage(Component.translatable("eso.youforgot", c), true);
                    ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.removedEnchantment", discId, player.getDisplayName()), true);
                } else ctx.getSource().sendFailure(Component.translatable("eso.command.feedback.doesnotknow", player.getDisplayName(), discId));
            } else ctx.getSource().sendFailure(Component.translatable("eso.command.encantmentnotfound", id));
        }
        return 0;
    }

    public static int learnEnchantmentById(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String id){
        if(id.equalsIgnoreCase("all")){
            grantAll(ctx, player);
        } else {
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(id));
            if(enchantment != null && player instanceof IServerPlayerAcc acc){
                MutableComponent discId = Component.translatable(enchantment.getDescriptionId());
                MutableComponent c = Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE);
                c.append(discId);
                c.append(Component.literal("]"));
                if(acc.enchantment_overhaul$getUnlockedEnchantments().put(enchantment, ESOCommon.getMaximumPossibleEnchantmentLevel(enchantment)) < 1) {
                    player.sendSystemMessage(Component.translatable("eso.youlearned", c).withStyle(ChatFormatting.GOLD), true);
                    ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.addedEnchantment", discId, player.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
                } else {
                    ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.playerknows", player.getDisplayName(), discId).withStyle(ChatFormatting.DARK_GREEN), true);
                }
            } else ctx.getSource().sendFailure(Component.translatable("eso.command.encantmentnotfound", id));
        }
        return 0;
    }

    public static void grantAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player){
        if(player instanceof IServerPlayerAcc acc){
            Object2IntOpenHashMap<Enchantment> enchantments = acc.enchantment_overhaul$getUnlockedEnchantments();
            for (Enchantment e: BuiltInRegistries.ENCHANTMENT){
                enchantments.put(e, ESOCommon.getMaximumPossibleEnchantmentLevel(e));
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.grantall", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("eso.command.allknowledge").withStyle(ChatFormatting.GOLD));
        }
    }

    public static void revokeAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player){
        if(player instanceof IServerPlayerAcc acc){
            acc.enchantment_overhaul$getUnlockedEnchantments().clear();
            ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.revokeall", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("eso.command.lostallknowledge").withStyle(ChatFormatting.GOLD));
        }
    }
}
