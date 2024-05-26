package teal.gips;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandException;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public class Gips implements ClientModInitializer {

    public static final MinecraftClient minecraft = MinecraftClient.getInstance();
    public static final KeyBinding GetNBTKeybind = new KeyBinding("teal.gips.key.copynbt", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "teal.gips");
    public static final KeyBinding GetNameKeybind = new KeyBinding("teal.gips.key.copyname", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "teal.gips");
    public static final File gipsFolder = new File("./gips/");
    public static boolean dumpNbt = true;

    // Prevents spam by making sure the element isn't saved multiple times and creating 10 billion toasts.
    private static NbtElement nbtCache;

    private static void setClipboard(String contents) {
        minecraft.keyboard.setClipboard(contents);
    }

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(GetNBTKeybind);
        KeyBindingHelper.registerKeyBinding(GetNameKeybind);

        ClientTickEvents.END_CLIENT_TICK.register(Gips::tickEvent);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher,commandRegistryAccess) -> dispatcher.register(
                ClientCommandManager.literal("gips").then(
                        ClientCommandManager.literal("viewnbt")
                                .executes(Gips::getNbt)
                                .then(ClientCommandManager.argument("copy", BoolArgumentType.bool())
                                        .executes(Gips::getNbt)
                                        .then(ClientCommandManager.argument("path", NbtPathArgumentType.nbtPath())
                                                .executes(Gips::getNbt)
                                        )
                                )
                ).then(
                        ClientCommandManager.literal("modifynbt")
                                .then(ClientCommandManager.argument("data", NbtCompoundArgumentType.nbtCompound())
                                        .executes(Gips::setNbt)
                                )
                ).then(
                        ClientCommandManager.literal("dump")
                                .executes(Gips::dumpNbt)
                ).then(
                        ClientCommandManager.literal("give")
                                .then(ClientCommandManager.argument("item", ItemStackArgumentType.itemStack(commandRegistryAccess))
                                        .executes(Gips::giveItem)
                                        .then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(Gips::giveItem)
                                        )
                                )
                )
        ));
    }

    private static int giveItem(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        if (minecraft.player == null) throw new CommandException(Text.literal("Could not get player."));
        if (!minecraft.player.isCreative()) throw new CommandException(Text.literal("You need to be in creative mode."));
        int amount = 1;
        try {
            amount = IntegerArgumentType.getInteger(context, "count");
        } catch (IllegalArgumentException IAE) {

        }
        for(int slot = 0; slot < 9; slot++) {
            if(!minecraft.player.getInventory().getStack(slot).isEmpty()) continue;
            minecraft.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot + 36, ItemStackArgumentType.getItemStackArgument(context, "item").createStack(amount, true)));
            minecraft.player.sendMessage(Text.literal("Created item."), true);
            return 0;
        }
        throw new CommandException(Text.literal("Your hotbar is full."));
    }

    private static int dumpNbt(CommandContext<FabricClientCommandSource> context) {
        dumpNbt = !dumpNbt;
        context.getSource().getPlayer().sendMessage(Text.literal("Turned NBT dumping ").append(Text.literal(dumpNbt ? "ON" : "OFF").formatted(dumpNbt ? Formatting.GREEN : Formatting.RED, Formatting.BOLD)), true);
        return 0;
    }

    private static int setNbt(CommandContext<FabricClientCommandSource> context) {
        if (minecraft.player == null) throw new CommandException(Text.literal("Could not get player."));
        if (!minecraft.player.isCreative()) throw new CommandException(Text.literal("You need to be in creative mode."));
        ItemStack heldItem = minecraft.player.getMainHandStack();
        if (heldItem.isEmpty()) throw new CommandException(Text.literal("You need to hold an item."));
        heldItem.setNbt(NbtCompoundArgumentType.getNbtCompound(context, "data"));
        context.getSource().getPlayer().sendMessage(Text.literal("Modified item."), true);
        return 0;
    }

    private static int getNbt(CommandContext<FabricClientCommandSource> context) {
        if (minecraft.player == null) throw new CommandException(Text.literal("Could not get player."));
        ItemStack heldItem = minecraft.player.getMainHandStack();
        if (heldItem.isEmpty()) throw new CommandException(Text.literal("You need to hold an item."));
        boolean copy = false;
        try {
            copy = BoolArgumentType.getBool(context, "copy");
        } catch (IllegalArgumentException IAE) {

        }

        NbtPathArgumentType.NbtPath path;
        NbtElement nbt = heldItem.getOrCreateNbt();

        try {
            path = context.getArgument("path", NbtPathArgumentType.NbtPath.class);
            // Pray and hope that the first element exists.
            // Apparently returns a singleton list, so it will always exist.
            nbt = path.get(heldItem.getOrCreateNbt()).get(0);
        } catch (IllegalArgumentException IAE) {

        } catch (CommandSyntaxException e) {
            throw new CommandException(Text.literal("Invalid NBT Path."));
        }

        if(copy) {
            setClipboard(nbt.asString());

            context.getSource().getPlayer().sendMessage(Text.literal("Copied NBT to clipboard."), true);
        } else {
            MutableText msg = Text.literal("Properties of ").append(heldItem.getName()).append("\n");
            msg.append(NbtHelper.toPrettyPrintedText(nbt));

            context.getSource().getPlayer().sendMessage(msg, false);
        }

        return 0;
    }

    private static void tickEvent(MinecraftClient client) {
        boolean gNBT = GetNBTKeybind.wasPressed();
        boolean ALT = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT);
        if(gNBT) {
            Entity entity = minecraft.getCameraEntity();
            if(entity != null) {
                HitResult blockHit = entity.raycast(50.0D, 0.0F, true);
                Entity entityHit = null;
                Vec3d vec3d = entity.getEyePos();
                Vec3d vec3d2 = entity.getRotationVec(1.0F).multiply(50.0F);
                Vec3d vec3d3 = vec3d.add(vec3d2);
                Box box = entity.getBoundingBox().stretch(vec3d2).expand(1.0D);
                Predicate<Entity> predicate = (entityx) -> !entityx.isSpectator() && entityx.canHit();
                EntityHitResult entityHitResult = ProjectileUtil.raycast(entity, vec3d, vec3d3, box, predicate, 50*50);
                if (entityHitResult != null && !(vec3d.squaredDistanceTo(entityHitResult.getPos()) > (double)50*50)) entityHit = entityHitResult.getEntity();
                if(entityHit != null) {
                    NbtCompound nbt = new NbtCompound();
                    NbtCompound ET = NbtPredicate.entityToNbt(entityHitResult.getEntity());
                    ET.putString("id", EntityType.getId(entityHitResult.getEntity().getType()).toString());
                    if(!ALT) {
                        ET.remove("UUIDLeast");
                        ET.remove("UUIDMost");

                        ET.remove("UUID");
                        ET.remove("Pos");
                        ET.remove("Dimension");
                    }
                    nbt.put("EntityTag", ET);
                    if (client.player.hasPermissionLevel(2)) {
                        final NbtCompound Fnbt = nbt;
                        client.getNetworkHandler().getDataQueryHandler().queryEntityNbt(entityHitResult.getEntity().getId(), (nbtCompound) -> {
                            if(nbtCompound != null) {
                                if(!ALT) {
                                    nbtCompound.remove("UUIDLeast");
                                    nbtCompound.remove("UUIDMost");

                                    nbtCompound.remove("UUID");
                                    nbtCompound.remove("Pos");
                                    nbtCompound.remove("Dimension");
                                }
                                nbtCompound.putString("id", EntityType.getId(entityHitResult.getEntity().getType()).toString());
                                Fnbt.put("EntityTag", nbtCompound);
                            }
                            nbtCompound = Fnbt;
                            copyNBT(nbtCompound);
                        });
                    } else copyNBT(nbt);
                } else if(blockHit.getType() == HitResult.Type.BLOCK) {
                    BlockPos blockPos = ((BlockHitResult) blockHit).getBlockPos();
                    BlockEntity blockEntity = client.world.getBlockEntity(blockPos);
                    NbtCompound nbt = new NbtCompound();
                    if (blockEntity != null) {
                        ItemStack is = blockEntity.getCachedState().getBlock().asItem().getDefaultStack();
                        client.addBlockEntityNbt(is, blockEntity);
                        nbt = is.getOrCreateNbt();
                        // Get rid of the lore
                        if(nbt.contains("display")) nbt.remove("display");
                    }
                    if (client.player.hasPermissionLevel(2) && !(blockEntity instanceof SkullBlockEntity)) {
                        // Copy a detailed version if the server / integrated server allows
                        // Doesn't do anything if we don't have permission.
                        final NbtCompound fNbt = nbt;
                        client.getNetworkHandler().getDataQueryHandler().queryBlockNbt(blockPos, (nbtCompound) -> {
                            // The compound doesn't come back wrapped in a BET so just overwrite it manually
                            if(nbtCompound != null) fNbt.put("BlockEntityTag", nbtCompound);
                            nbtCompound = fNbt;
                            copyNBT(nbtCompound);
                        });
                    } else {
                        copyNBT(nbt);
                    }
                }
            }
        }
    }

    public static void copyName(Text title) {
        Gips.setClipboard(Text.Serializer.toJson(title));
        minecraft.getToastManager().add(new GipsToast("Copied stack name", "to clipboard!", false));
    }

    public static void copyNBT(NbtElement nbt) {
        if(nbt.equals(nbtCache) || (nbt instanceof NbtCompound && ((NbtCompound) nbt).isEmpty())) return;
        nbtCache = nbt;
        Gips.setClipboard(nbt.asString());
        minecraft.getToastManager().add(new GipsToast("Copied NBT to clipboard!", false));
    }
}