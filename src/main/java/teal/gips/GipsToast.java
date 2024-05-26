package teal.gips;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class GipsToast implements Toast {

    public final Text message;
    public final boolean fail;

    public GipsToast(String message, boolean fail) {
        this.message = Text.literal(message);
        this.fail = fail;
    }

    @Nullable
    public Text message2;

    public GipsToast(String line1, String line2, boolean fail) {
        this.message = Text.literal(line1);
        this.message2 = Text.literal(line2);
        this.fail = fail;
    }

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        DrawableHelper.drawTexture(matrices, 0, 0, 0, 0, this.getWidth(), this.getHeight());
        if(message2 == null) {
            manager.getClient().textRenderer.draw(matrices, message, 30.0f, 12.0f, 0xFFFFFF);
        } else {
            // It's easier to hard code these values than to calculate the fucking mass of the sun.
            manager.getClient().textRenderer.draw(matrices, message, 30.0f, 7.5f, 0xFFFFFF);
            manager.getClient().textRenderer.draw(matrices, message2, 30.0f, 16.5f, 0xFFFFFF);
        }
        ItemStack itemStack = (this.fail ? Items.ENCHANTED_BOOK : Items.KNOWLEDGE_BOOK).getDefaultStack();
        matrices.push();
        matrices.scale(0.6f, 0.6f, 1.0f);
        matrices.pop();
        manager.getClient().getItemRenderer().renderInGui(matrices, itemStack, 8, 8);

        return (double)(startTime) >= 1000.0 * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
}
