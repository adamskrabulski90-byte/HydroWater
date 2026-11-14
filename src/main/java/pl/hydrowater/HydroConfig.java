package pl.hydrowater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HydroConfig {
    public static final int MAX_FRIENDS = 50;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hydrowater.json");
    private static HydroConfig instance = new HydroConfig();

    public boolean enabled = false;
    public int range = 4;          // 1..5
    public int delay = 2;          // 0..10 tickow
    public int rotation = 360;     // 90..360
    public int toggleKey = GLFW.GLFW_KEY_0;
    public List<String> friends = new ArrayList<>();

    public static HydroConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                HydroConfig c = GSON.fromJson(Files.readString(FILE), HydroConfig.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        instance.sanitize();
    }

    public static void save() {
        try {
            instance.sanitize();
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sanitize() {
        range = Math.max(1, Math.min(5, range));
        delay = Math.max(0, Math.min(10, delay));
        rotation = Math.max(90, Math.min(360, rotation));
        if (friends == null) friends = new ArrayList<>();
        while (friends.size() > MAX_FRIENDS) friends.remove(friends.size() - 1);
    }

    public boolean isFriend(String name) {
        for (String f : friends) if (f.equalsIgnoreCase(name)) return true;
        return false;
    }

    public boolean addFriend(String name) {
        if (name.isEmpty() || friends.size() >= MAX_FRIENDS || isFriend(name)) return false;
        friends.add(name);
        return true;
    }
}
