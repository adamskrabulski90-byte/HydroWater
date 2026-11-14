package pl.hydrowater;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.IntConsumer;

public class HydroScreen extends Screen {
    private static final int W = 430, H = 262;
    private static final int ROW = 14, ROWS = 11;
    private static final int ACCENT = 0xFF2E9BFF;

    private int left, top, rx, rw, listTop;
    private int scroll = 0;
    private boolean listening = false;
    private TextFieldWidget nameField;
    private FlatButton keyButton;

    public HydroScreen() {
        super(Text.literal("HydroWater"));
    }

    @Override
    protected void init() {
        HydroConfig c = HydroConfig.get();
        left = (width - W) / 2;
        top = (height - H) / 2;
        int lx = left + 16, lw = 190, y = top + 46;

        addDrawableChild(new FlatButton(lx, y, lw, 20, enabledText(), b -> {
            c.enabled = !c.enabled;
            b.setMessage(enabledText());
        }));
        addDrawableChild(new Slider(lx, y + 26, lw, 20, "Zasieg", "", 1, 5, c.range, v -> c.range = v));
        addDrawableChild(new Slider(lx, y + 52, lw, 20, "Delay", " tick", 0, 10, c.delay, v -> c.delay = v));
        addDrawableChild(new Slider(lx, y + 78, lw, 20, "Rotation", "\u00b0", 90, 360, c.rotation, v -> c.rotation = v));
        keyButton = addDrawableChild(new FlatButton(lx, y + 104, lw, 20, keyText(), b -> {
            listening = true;
            b.setMessage(keyText());
        }));

        addDrawableChild(new FlatButton(left + W / 2 - 50, top + H - 30, 100, 20, Text.literal("Gotowe"), b -> close()));

        rx = left + 224;
        rw = 190;
        listTop = top + 74;
        nameField = new TextFieldWidget(textRenderer, rx, top + 46, rw - 54, 18, Text.literal("Nick"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Nick gracza..."));
        addDrawableChild(nameField);
        addDrawableChild(new FlatButton(rx + rw - 50, top + 46, 50, 18, Text.literal("Dodaj"), b -> addFriend()));
    }

    private void addFriend() {
        String n = nameField.getText().trim();
        if (HydroConfig.get().addFriend(n)) nameField.setText("");
    }

    private Text enabledText() {
        boolean on = HydroConfig.get().enabled;
        return Text.literal("Mod: ").append(Text.literal(on ? "WLACZONY" : "WYLACZONY")
                .formatted(on ? Formatting.GREEN : Formatting.RED));
    }

    private Text keyText() {
        if (listening) return Text.literal("Nacisnij klawisz...").formatted(Formatting.YELLOW);
        int k = HydroConfig.get().toggleKey;
        Text name = k < 0 ? Text.literal("Brak") : InputUtil.fromKeyCode(k, -1).getLocalizedText();
        return Text.literal("Klawisz wlaczania: [").append(name).append("]");
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF0B0F16);
        ctx.fill(left, top, left + W, top + H, 0xF0141A24);
        ctx.fill(left, top, left + W, top + 3, ACCENT);
        ctx.fill(left + W / 2 - 1, top + 34, left + W / 2, top + H - 40, 0xFF263042);
        ctx.fill(rx - 2, listTop - 2, rx + rw + 2, listTop + ROWS * ROW + 2, 0xFF0F141C);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        HydroConfig c = HydroConfig.get();

        ctx.drawText(textRenderer, Text.literal("HydroWater").formatted(Formatting.BOLD), left + 16, top + 12, 0xFF4FC3F7, true);
        ctx.drawText(textRenderer, Text.literal("Auto Water Pickup  |  1.21.4"), left + 100, top + 13, 0xFF7A8699, false);
        ctx.drawText(textRenderer, Text.literal("USTAWIENIA"), left + 16, top + 32, 0xFF8FA3BF, false);
        ctx.drawText(textRenderer, Text.literal("ZNAJOMI  " + c.friends.size() + "/" + HydroConfig.MAX_FRIENDS), rx, top + 32, 0xFF8FA3BF, false);

        List<String> f = c.friends;
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= f.size()) break;
            int ry = listTop + i * ROW;
            boolean hover = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY < ry + ROW;
            if (hover) ctx.fill(rx, ry, rx + rw, ry + ROW, 0xFF1A2230);
            ctx.drawText(textRenderer, Text.literal(f.get(idx)), rx + 4, ry + 3, 0xFFE6EDF7, false);
            boolean overX = hover && mouseX >= rx + rw - 16;
            ctx.drawText(textRenderer, Text.literal("x"), rx + rw - 11, ry + 3, overX ? 0xFFFF5555 : 0xFF7A8699, false);
        }
        if (f.isEmpty()) {
            ctx.drawText(textRenderer, Text.literal("Brak znajomych"), rx + 4, listTop + 4, 0xFF5B6678, false);
        }
        ctx.drawText(textRenderer, Text.literal("Rotation: max kat obrotu do wody (360 = wszedzie)"),
                left + 16, top + H - 60, 0xFF5B6678, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            List<String> f = HydroConfig.get().friends;
            for (int i = 0; i < ROWS; i++) {
                int idx = scroll + i;
                if (idx >= f.size()) break;
                int ry = listTop + i * ROW;
                if (mx >= rx + rw - 16 && mx <= rx + rw && my >= ry && my < ry + ROW) {
                    f.remove(idx);
                    scroll = Math.max(0, Math.min(scroll, f.size() - ROWS));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        int max = Math.max(0, HydroConfig.get().friends.size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vAmount)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                HydroConfig.get().toggleKey = -1;
            } else if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                HydroConfig.get().toggleKey = keyCode;
            }
            listening = false;
            keyButton.setMessage(keyText());
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && nameField.isFocused()) {
            addFriend();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        HydroConfig.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---------- wlasne, plaskie widgety ----------

    private static class FlatButton extends ButtonWidget {
        FlatButton(int x, int y, int w, int h, Text msg, PressAction action) {
            super(x, y, w, h, msg, action, DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            ctx.fill(x, y, x + w, y + h, isHovered() ? 0xFF26364D : 0xFF1B2638);
            ctx.fill(x, y + h - 1, x + w, y + h, ACCENT);
            ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                    x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        }
    }

    private static class Slider extends SliderWidget {
        private final String label, suffix;
        private final int min, max;
        private final IntConsumer setter;

        Slider(int x, int y, int w, int h, String label, String suffix, int min, int max, int cur, IntConsumer setter) {
            super(x, y, w, h, Text.empty(), (cur - min) / (double) (max - min));
            this.label = label;
            this.suffix = suffix;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + current() + suffix));
        }

        @Override
        protected void applyValue() {
            int v = current();
            value = (v - min) / (double) (max - min);
            setter.accept(v);
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            ctx.fill(x, y, x + w, y + h, 0xFF1B2638);
            ctx.fill(x, y, x + (int) (value * w), y + h, isHovered() ? 0xFF2E7BD6 : 0xFF2565B0);
            ctx.fill(x, y + h - 1, x + w, y + h, ACCENT);
            ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                    x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        }
    }
}
