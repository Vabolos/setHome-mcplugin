package home.sethome;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public class SetHome extends JavaPlugin {

    private File dataFile;

    @Override
    public void onEnable() {
        dataFile = new File(getDataFolder(), "..\\homes.json");
        if (!dataFile.exists()) {
            if (!dataFile.getParentFile().mkdirs()) {
                getLogger().warning("Failed to create parent directories for homes.json.");
            }

            try {
                boolean fileCreated = dataFile.createNewFile();
                if (fileCreated) {
                    getLogger().info("homes.json created successfully!");
                } else {
                    getLogger().warning("homes.json already exists!");
                }
            } catch (IOException e) {
                getLogger().severe("Error creating homes.json file: " + e.getMessage());
            }
        }

        getLogger().info("SetHomePlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SetHomePlugin has been disabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase("sethome") && sender instanceof Player) {
            Player player = (Player) sender;
            saveHomeLocation(player.getName(), player.getLocation());
            player.sendMessage("Home location set!");
            return true;
        } else if (cmd.getName().equalsIgnoreCase("home") && sender instanceof Player) {
            Player player = (Player) sender;
            Location homeLocation = getHomeLocation(player.getName());
            if (homeLocation != null) {
                player.teleport(homeLocation);
                player.sendMessage("Teleported to your home!");

                // Play the teleportation sound effect at the player's location
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            } else {
                player.sendMessage("You don't have a home set. Use /sethome to set your home.");
            }
            return true;
        }
        return false;
    }


    private void saveHomeLocation(String playerName, Location location) {
        FileConfiguration homesConfig = YamlConfiguration.loadConfiguration(dataFile);

        homesConfig.set("homes." + playerName + ".x", location.getX());
        homesConfig.set("homes." + playerName + ".y", location.getY());
        homesConfig.set("homes." + playerName + ".z", location.getZ());
        homesConfig.set("homes." + playerName + ".world", Objects.requireNonNull(location.getWorld()).getName());

        try {
            FileWriter writer = new FileWriter(dataFile);
            homesConfig.save(String.valueOf(writer));
            writer.close();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error saving home location for " + playerName, e);
        }
    }

    private Location getHomeLocation(String playerName) {
        FileConfiguration homesConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (homesConfig.contains("homes." + playerName)) {
            World world = getServer().getWorld(Objects.requireNonNull(homesConfig.getString("homes." + playerName + ".world")));
            double x = homesConfig.getDouble("homes." + playerName + ".x");
            double y = homesConfig.getDouble("homes." + playerName + ".y");
            double z = homesConfig.getDouble("homes." + playerName + ".z");

            return new Location(world, x, y, z);
        }
        return null;
    }
}
