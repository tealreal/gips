package teal.gips;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;

public class GipsToast implements Toast {

    public final String message;
    public final boolean fail;

    public GipsToast(String message, boolean fail) {
        this.message = message;
        this.fail = fail;
    }

    @Nullable
    public String message2;

    public GipsToast(String line1, String line2, boolean fail) {
        this.message = line1;
        this.message2 = line2;
        this.fail = fail;
    }

    @Override
    public Visibility draw(ToastManager manager, long startTime) {
        manager.getGame().getTextureManager().bindTexture(TOASTS_TEX);
        RenderSystem.color3f(1.0F, 1.0F, 1.0F);
        manager.blit(0, 0, 0, 0, 160, 32);
        if(message2 == null) {
            manager.getGame().textRenderer.draw(message, 30.0f, 12.0f, 0xFFFFFF);
        } else {
            // It's easier to hard code these values than to calculate the fucking mass of the sun.
            manager.getGame().textRenderer.draw(message, 30.0f, 7.5f, 0xFFFFFF);
            manager.getGame().textRenderer.draw(message2, 30.0f, 16.5f, 0xFFFFFF);
        }
        ItemStack itemStack = (this.fail ? Items.ENCHANTED_BOOK : Items.KNOWLEDGE_BOOK).getStackForRender();
        RenderSystem.pushMatrix();
        RenderSystem.scalef(0.6F, 0.6F, 1.0F);
        RenderSystem.popMatrix();
        manager.getGame().getItemRenderer().renderGuiItem(itemStack, 8, 8);

        return (double)(startTime) >= 1000.0 ? Visibility.HIDE : Visibility.SHOW;
    }
}
