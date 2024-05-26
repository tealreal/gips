package teal.gips;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
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
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        context.drawTexture(TEXTURE, 0, 0, 0, 0, this.getWidth(), this.getHeight());
        if(message2 == null) {
            context.drawText(manager.getClient().textRenderer, message, 30, 12, 0xFFFFFF, false);
        } else {
            // It's easier to hard code these values than to calculate the fucking mass of the sun.
            context.drawText(manager.getClient().textRenderer, message, 30, 7, 0xFFFFFF, false);
            context.drawText(manager.getClient().textRenderer, message2, 30, 17, 0xFFFFFF, false);
        }
        ItemStack itemStack = (this.fail ? Items.ENCHANTED_BOOK : Items.KNOWLEDGE_BOOK).getDefaultStack();
        context.getMatrices().push();
        context.getMatrices().scale(0.6f, 0.6f, 1.0f);
        context.getMatrices().pop();
        context.drawItemWithoutEntity(itemStack, 8, 8);

        return (double)(startTime) >= 1000.0 * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
}
