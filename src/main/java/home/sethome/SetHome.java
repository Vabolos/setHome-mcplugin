package home.sethome;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;

public class SetHome extends JavaPlugin {

    private File dataFile;
    private HashMap<String, HomeData> homesData;
    private Gson gson;

    @Override
    public void onEnable() {
        gson = new Gson();
        dataFile = new File(getDataFolder(), "homes.json");
        boolean fileExists = dataFile.exists();

        if (!fileExists) {
            if (!dataFile.getParentFile().mkdirs()) {
                getLogger().warning("Failed to create parent directories for homes.json.");
            }

            try {
                fileExists = dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Error creating homes.json file: " + e.getMessage());
            }
        }

        if (!fileExists) {
            getLogger().warning("Failed to create homes.json.");
        } else {
            loadHomesData();
        }

        getLogger().info("SetHomePlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        saveHomesData();
        getLogger().info("SetHomePlugin has been disabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase("sethome") && sender instanceof Player) {
            Player player = (Player) sender;
            Location existingHome = getHomeLocation(player.getName());
            if (existingHome != null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + ChatColor.BOLD + "You already have a home set!" + ChatColor.ITALIC + " (use /delhome)");
            } else {
                saveHomeLocation(player.getName(), player.getLocation());
                player.sendMessage(String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + "Home location set!");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("home") && sender instanceof Player) {
            Player player = (Player) sender;
            Location homeLocation = getHomeLocation(player.getName());
            if (homeLocation != null) {
                player.sendMessage(String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + "Teleporting to your home in 3 seconds...");
                BukkitTask task = new BukkitRunnable() {
                    int count = 3;

                    @Override
                    public void run() {
                        if (count > 0) {
                            player.sendMessage(String.valueOf(ChatColor.AQUA) + ChatColor.BOLD + ChatColor.ITALIC + count + " seconds...");
                            count--;
                        } else {
                            player.teleport(homeLocation);
                            player.sendMessage(String.valueOf(ChatColor.AQUA) + ChatColor.BOLD  + "Welcome home, " + ChatColor.BLUE + ChatColor.BOLD + player.getName());
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

                            for (int i = 0; i < 20; i++) {
                                player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation(), 20);
                                player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1, new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.0F));
                            }

                            cancel(); // Cancel the task after teleportation
                        }
                    }
                }.runTaskTimer(this, 0L, 20L); // Delay: 0 ticks, Repeat: 20 ticks (1 second)
            } else {
                player.sendMessage(String.valueOf(ChatColor.RED) + ChatColor.BOLD + "You don't have a home set!" + ChatColor.ITALIC + " (use /sethome)");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("delhome") && sender instanceof Player) {
            Player player = (Player) sender;
            if (deleteHomeLocation(player.getName())) {
                player.sendMessage(String.valueOf(ChatColor.RED) + ChatColor.BOLD + "Home location deleted!");
            } else {
                player.sendMessage(String.valueOf(ChatColor.RED) + ChatColor.BOLD + "You don't have a home set!");
            }
            return true;
        }
        return false;
    }

    private boolean deleteHomeLocation(String playerName) {
        if (homesData != null && homesData.containsKey(playerName)) {
            homesData.remove(playerName);
            saveHomesData(); // Save updated homes data after deletion
            return true;
        }
        return false;
    }


    private void saveHomeLocation(String playerName, Location location) {
        if (homesData == null) {
            homesData = new HashMap<>();
        }

        homesData.put(playerName, new HomeData(location.getX(), location.getY(), location.getZ(), Objects.requireNonNull(location.getWorld()).getName()));
        saveHomesData();
    }

    private Location getHomeLocation(String playerName) {
        if (homesData != null && homesData.containsKey(playerName)) {
            HomeData homeData = homesData.get(playerName);
            World world = getServer().getWorld(homeData.getWorld());
            return new Location(world, homeData.getX(), homeData.getY(), homeData.getZ());
        }
        return null;
    }

    private void loadHomesData() {
        try (FileReader reader = new FileReader(dataFile)) {
            TypeToken<HashMap<String, HomeData>> token = new TypeToken<>() {};
            homesData = gson.fromJson(reader, token.getType());

            if (homesData == null) {
                homesData = new HashMap<>();
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error loading homes data: " + e.getMessage());
        }
    }

    private void saveHomesData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(homesData, writer);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error saving homes data: " + e.getMessage());
        }
    }

    private static class HomeData {
        private final double x, y, z;
        private final String world;

        public HomeData(double x, double y, double z, String world) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public String getWorld() {
            return world;
        }
    }
}
