package teal.gips;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class GipsToast implements Toast {

    public final Text message;
    public final boolean fail;

    public GipsToast(String message, boolean fail) {
        this.message = new LiteralText(message);
        this.fail = fail;
    }

    @Nullable
    public Text message2;

    public GipsToast(String line1, String line2, boolean fail) {
        this.message = new LiteralText(line1);
        this.message2 = new LiteralText(line2);
        this.fail = fail;
    }

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        manager.getGame().getTextureManager().bindTexture(TEXTURE);
        RenderSystem.color3f(1.0F, 1.0F, 1.0F);
        manager.drawTexture(matrices, 0, 0, 0, 0, this.getWidth(), this.getHeight());
        if(message2 == null) {
            manager.getGame().textRenderer.draw(matrices, message, 30.0f, 12.0f, 0xFFFFFF);
        } else {
            // It's easier to hard code these values than to calculate the fucking mass of the sun.
            manager.getGame().textRenderer.draw(matrices, message, 30.0f, 7.5f, 0xFFFFFF);
            manager.getGame().textRenderer.draw(matrices, message2, 30.0f, 16.5f, 0xFFFFFF);
        }
        ItemStack itemStack = (this.fail ? Items.ENCHANTED_BOOK : Items.KNOWLEDGE_BOOK).getDefaultStack();
        matrices.push();
        matrices.scale(0.6f, 0.6f, 1.0f);
        matrices.pop();
        manager.getGame().getItemRenderer().renderInGui(itemStack, 8, 8);

        return (double)(startTime) >= 1000.0 ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
}
