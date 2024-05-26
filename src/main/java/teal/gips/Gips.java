package teal.gips;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandException;
import net.minecraft.command.arguments.ItemStackArgumentType;
import net.minecraft.command.arguments.NbtCompoundTagArgumentType;
import net.minecraft.command.arguments.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
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
    private static Tag nbtCache;

    private static void setClipboard(String contents) {
        minecraft.keyboard.setClipboard(contents);
    }

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(GetNBTKeybind);
        KeyBindingHelper.registerKeyBinding(GetNameKeybind);

        ClientTickEvents.END_CLIENT_TICK.register(Gips::tickEvent);

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(
                CommandManager.literal("gips").then(
                        CommandManager.literal("viewnbt")
                                .executes(Gips::getNbt)
                                .then(CommandManager.argument("copy", BoolArgumentType.bool())
                                        .executes(Gips::getNbt)
                                        .then(CommandManager.argument("path", NbtPathArgumentType.nbtPath())
                                                .executes(Gips::getNbt)
                                        )
                                )
                ).then(
                        CommandManager.literal("modifynbt")
                                .then(CommandManager.argument("data", NbtCompoundTagArgumentType.nbtCompound())
                                        .executes(Gips::setNbt)
                                )
                ).then(
                        CommandManager.literal("dump")
                                .executes(Gips::dumpNbt)
                ).then(
                        CommandManager.literal("give")
                                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                                        .executes(Gips::giveItem)
                                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(Gips::giveItem)
                                        )
                                )
                )
        ));
    }

    private static int giveItem(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (minecraft.player == null) throw new CommandException(new LiteralText("Could not get player."));
        if (!minecraft.player.isCreative()) throw new CommandException(new LiteralText("You need to be in creative mode."));
        int amount = 1;
        try {
            amount = IntegerArgumentType.getInteger(context, "count");
        } catch (IllegalArgumentException IAE) {

        }
        for(int slot = 0; slot < 9; slot++) {
            if(!minecraft.player.inventory.getInvStack(slot).isEmpty()) continue;
            minecraft.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot + 36, ItemStackArgumentType.getItemStackArgument(context, "item").createStack(amount, true)));
            minecraft.player.addChatMessage(new LiteralText("Created item."), true);
            return 0;
        }
        throw new CommandException(new LiteralText("Your hotbar is full."));
    }

    private static int dumpNbt(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        dumpNbt = !dumpNbt;
        context.getSource().getPlayer().addChatMessage(new LiteralText("Turned NBT dumping ").append(new LiteralText(dumpNbt ? "ON" : "OFF").formatted(dumpNbt ? Formatting.GREEN : Formatting.RED, Formatting.BOLD)), true);
        return 0;
    }

    private static int setNbt(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (minecraft.player == null) throw new CommandException(new LiteralText("Could not get player."));
        if (!minecraft.player.isCreative()) throw new CommandException(new LiteralText("You need to be in creative mode."));
        ItemStack heldItem = minecraft.player.getMainHandStack();
        if (heldItem.isEmpty()) throw new CommandException(new LiteralText("You need to hold an item."));
        heldItem.setTag(NbtCompoundTagArgumentType.getCompoundTag(context, "data"));
        context.getSource().getPlayer().addChatMessage(new LiteralText("Modified item."), true);
        return 0;
    }

    private static int getNbt(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (minecraft.player == null) throw new CommandException(new LiteralText("Could not get player."));
        ItemStack heldItem = minecraft.player.getMainHandStack();
        if (heldItem.isEmpty()) throw new CommandException(new LiteralText("You need to hold an item."));
        boolean copy = false;
        try {
            copy = BoolArgumentType.getBool(context, "copy");
        } catch (IllegalArgumentException IAE) {

        }

        NbtPathArgumentType.NbtPath path;
        Tag nbt = heldItem.getOrCreateTag();

        try {
            path = context.getArgument("path", NbtPathArgumentType.NbtPath.class);
            // Pray and hope that the first element exists.
            // Apparently returns a singleton list, so it will always exist.
            nbt = path.get(heldItem.getOrCreateTag()).get(0);
        } catch (IllegalArgumentException IAE) {

        } catch (CommandSyntaxException e) {
            throw new CommandException(new LiteralText("Invalid NBT Path."));
        }

        if(copy) {
            setClipboard(nbt.asString());

            context.getSource().getPlayer().addChatMessage(new LiteralText("Copied NBT to clipboard."), true);
        } else {
            Text msg = new LiteralText("Properties of ").append(heldItem.getName()).append("\n");
            msg.append(nbt.asString());

            context.getSource().getPlayer().addChatMessage(msg, false);
        }

        return 0;
    }

    private static void tickEvent(MinecraftClient client) {
        boolean gNBT = GetNBTKeybind.wasPressed();
        boolean ALT = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT);
        if(gNBT) {
            Entity entity = minecraft.getCameraEntity();
            if(entity != null) {
                HitResult blockHit = entity.rayTrace(50.0D, 0.0F, true);
                Entity entityHit = null;
                Vec3d vec3d = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
                Vec3d vec3d2 = entity.getRotationVec(1.0F).multiply(50.0F);
                Vec3d vec3d3 = vec3d.add(vec3d2);
                Box box = entity.getBoundingBox().stretch(vec3d2).expand(1.0D);
                Predicate<Entity> predicate = (entityx) -> !entityx.isSpectator() && entityx.collides();
                EntityHitResult entityHitResult = ProjectileUtil.rayTrace(entity, vec3d, vec3d3, box, predicate, 50*50);
                if (entityHitResult != null && !(vec3d.squaredDistanceTo(entityHitResult.getPos()) > (double)50*50)) entityHit = entityHitResult.getEntity();
                if(entityHit != null) {
                    CompoundTag nbt = new CompoundTag();
                    CompoundTag ET = NbtPredicate.entityToTag(entityHitResult.getEntity());
                    ET.putString("id", EntityType.getId(entityHitResult.getEntity().getType()).toString());
                    if(!ALT) {
                        ET.remove("UUIDLeast");
                        ET.remove("UUIDMost");

                        ET.remove("UUID");
                        ET.remove("Pos");
                        ET.remove("Dimension");
                    }
                    nbt.put("EntityTag", ET);
                    if (client.player.allowsPermissionLevel(2)) {
                        final CompoundTag Fnbt = nbt;
                        client.getNetworkHandler().getDataQueryHandler().queryEntityNbt(entityHitResult.getEntity().getEntityId(), (nbtCompound) -> {
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
                    CompoundTag nbt = new CompoundTag();
                    if (blockEntity != null) {
                        nbt = client.addBlockEntityNbt(blockEntity.getCachedState().getBlock().asItem().getStackForRender(), blockEntity).getOrCreateTag();
                        // Get rid of the lore
                        if(nbt.contains("display")) nbt.remove("display");
                    }
                    if (client.player.allowsPermissionLevel(2) && !(blockEntity instanceof SkullBlockEntity)) {
                        // Copy a detailed version if the server / integrated server allows
                        // Doesn't do anything if we don't have permission.
                        final CompoundTag fNbt = nbt;
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

    public static void copyNBT(CompoundTag nbt) {
        if(nbt.equals(nbtCache) || nbt.isEmpty()) return;
        nbtCache = nbt;
        Gips.setClipboard(nbt.asString());
        minecraft.getToastManager().add(new GipsToast("Copied NBT to clipboard!", false));
    }
}