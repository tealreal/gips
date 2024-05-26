package teal.gips.mixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatMessageC2SPacket.class)
public abstract class ChatMessageC2SPacketMixin {
    @Redirect(
            method = "<init>(Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;length()I"
            )
    )
    private int lengthRedirect(String self) {
        return 256;
    }
}
