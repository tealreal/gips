package teal.gips.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import teal.gips.GipsToast;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static teal.gips.Gips.*;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin <T extends ScreenHandler> extends Screen {

    @Shadow @Nullable protected Slot focusedSlot;
    @Shadow @Final protected Text playerInventoryTitle;
    @Shadow @Final protected T handler;
    @Shadow protected int y;

    @Unique private static final int FUCKINGVALUE = 36;
    @Unique private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(
            method = "init",
            at = @At("TAIL")
    )
    protected void init(CallbackInfo ci) {
        final int offsetY = handler instanceof CreativeInventoryScreen.CreativeScreenHandler ? -30 : 0;
        final int centerX = (this.width/2) - (65/2);
        addDrawableChild(new ButtonWidget(centerX - 65, y - 18 + offsetY, 60, 14, new TranslatableText("teal.gips.key.copynbt"), b -> copyNBT(getContainerNBT(false).get(0))));
        addDrawableChild(new ButtonWidget(centerX, y - 18 + offsetY, 65, 14, new TranslatableText("teal.gips.key.copyname"), b -> copyName(title)));
        addDrawableChild(new ButtonWidget(centerX + 70, y - 18 + offsetY, 60, 14, new TranslatableText("teal.gips.dumpnbt"), this::writeToFile));
    }

    @Inject(
            method = "close",
            at = @At("TAIL")
    )
    public void close(CallbackInfo ci) {
        writeToFile(null);
    }

    @Unique
    private void writeToFile(@Nullable ButtonWidget b) {
        try {
            if (dumpNbt) {
                if (!gipsFolder.exists()) gipsFolder.mkdirs();

                List<NbtCompound> splitBETs = getContainerNBT(true);
                NbtList playerInventory = client.player.getInventory().writeNbt(new NbtList());

                String title = this.getTitle().getString();
                String inventory = this.playerInventoryTitle.getString();
                String date = dateFormat.format(new Date());
                for(int i=0; i < splitBETs.size(); i++) {
                    // Order of "best" name: Name of container -> "Inventory" -> Name of obfuscated class (though when modding it shows the deobfuscated class)
                    String bestName = (date + String.format("-%s-", i) + (title.isEmpty() ? inventory.isEmpty() ? this.getClass().getSimpleName() : inventory : title)).replaceAll("[^A-Za-z0-9\\._-]+","_");
                    FileWriter fileWriter = new FileWriter(String.format("./gips/%s.nbt", bestName.substring(0, Math.min(bestName.length(), 252))));
                    fileWriter.write(splitBETs.get(i).asString());
                    fileWriter.close();
                }

                FileWriter fileWriter2 = new FileWriter("./gips/" + date + "+InventoryDump.nbt");
                fileWriter2.write(playerInventory.asString());
                fileWriter2.close();

                if (b != null && client != null) {
                    client.getToastManager().add(new GipsToast("Dumped NBT", false));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if(b == null) {
                minecraft.player.sendMessage(new LiteralText("Couldn't dump NBT to file, see logs for details.").formatted(Formatting.RED), false);
            } else {
                b.setMessage(new LiteralText("Dump NBT").formatted(Formatting.RED));
            }
        }
    }

    @Unique
    private List<NbtCompound> getContainerNBT(boolean split) {
        boolean isCreativeScreen = handler instanceof CreativeInventoryScreen.CreativeScreenHandler;
        boolean isSurvivalScreen = handler instanceof PlayerScreenHandler;
        final int offsetSlots = isCreativeScreen ? 9*3 : 0;
        List<ItemStack> itemStacks = handler.slots.stream().map(Slot::getStack).toList();
        if (isCreativeScreen) {
            // 47 = the 9*4 slots of inventory space, 5 of armor + shield, 5 for the invisible crafting, and 1 for destroy item(?)
            // 47 = 36                               +5                   +5                                +1
            if (itemStacks.size() != 47) {
                itemStacks = ((CreativeInventoryScreen.CreativeScreenHandler) this.handler).itemList;
            }
        } else if (!isSurvivalScreen) {
            int sliceIndex = itemStacks.size() - FUCKINGVALUE + offsetSlots;
            itemStacks = itemStacks.subList(0, sliceIndex);
        }
        List<NbtCompound> splitBETs = new ArrayList<>();
        List<NbtList> splitItems = new ArrayList<>(List.of(new NbtList()));
        for(int j=0; j < itemStacks.size(); j++) {
            int index = split ? (int) Math.floor((double)j/27) : 0;
            ItemStack itemStack = itemStacks.get(j);
            if(itemStack.isEmpty()) continue;
            NbtCompound nbtCompound = itemStack.writeNbt(new NbtCompound());
            nbtCompound.putInt("Slot", split ? j%27 : j);
            if(splitItems.size()-1 < index) {
                splitItems.add(new NbtList());
            }
            NbtList Items = splitItems.get(index);
            Items.add(nbtCompound);
            splitItems.set(index, Items);
        }
        for(NbtList Items : splitItems) {
            NbtCompound nbt = new NbtCompound();
            NbtCompound BET = new NbtCompound();
            BET.put("Items", Items);
            BET.putString("CustomName", Text.Serializer.toJson(title));
            nbt.put("BlockEntityTag", BET);
            splitBETs.add(nbt);
        }
        return splitBETs;
    }

    @Inject(
            method = "keyPressed",
            at = @At("TAIL")
    )
    public void keyPressedInject(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        super.keyPressed(keyCode, scanCode, modifiers);
        if (focusedSlot != null) {
            if (GetNBTKeybind.matchesKey(keyCode, scanCode)) copyNBT(focusedSlot.getStack().getOrCreateNbt());
            else if (GetNameKeybind.matchesKey(keyCode, scanCode)) copyName(focusedSlot.getStack().getName());
        }
    }
}