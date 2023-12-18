package home.sethome;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class SetHome extends JavaPlugin implements Listener {

    private File dataFile;
    private HashMap<String, Map<String, HomeData>> playerHomes;
    private HashMap<Player, Map<String, HomeData>> playerSelectedHomes;
    private Gson gson;

    @Override
    public void onEnable() {
        gson = new Gson();
        dataFile = new File(getDataFolder(), "homes.json");
        boolean fileExists = dataFile.exists();
        playerSelectedHomes = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);

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
        playerSelectedHomes.clear();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase("sethome") && sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /sethome <name>");
                return true;
            }

            String homeName = args[0];
            saveHomeLocation(player.getName(), homeName, player.getLocation());
            player.sendMessage(ChatColor.AQUA + "Home '" + ChatColor.BLUE + homeName + ChatColor.AQUA + "' location set!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0f, 1.0f);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("home") && sender instanceof Player) {
            Player player = (Player) sender;
            String homeName = args.length > 0 ? args[0] : "blank";
            Location homeLocation = getHomeLocation(player.getName(), homeName);
            if (homeLocation != null) {
                teleportWithCountdown(player, homeLocation);
            } else {
                player.sendMessage(ChatColor.RED + "Home '" + homeName + "' not found!");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("delhome") && sender instanceof Player) {
            Player player = (Player) sender;
            String homeName = args.length > 0 ? args[0] : "blank";
            if (deleteHomeLocation(player.getName(), homeName)) {
                player.sendMessage(ChatColor.RED + "Home '" + homeName + "' deleted!");
            } else {
                player.sendMessage(ChatColor.RED + "Home '" + homeName + "' not found!");
            }
        } else if (cmd.getName().equalsIgnoreCase("homes") && sender instanceof Player) {
            Player player = (Player) sender;
            openHomesGUI(player);
            return true;
        }
        return false;
    }

    private Map<String, HomeData> getPlayerHomes(String playerName) {
        if (playerHomes != null && playerHomes.containsKey(playerName)) {
            return playerHomes.get(playerName);
        }
        return null;
    }

    private void openHomesGUI(Player player) {
        Map<String, HomeData> playerHomeData = getPlayerHomes(player.getName());
        if (playerHomeData != null && !playerHomeData.isEmpty()) {
            int numRows = (int) Math.ceil((double) playerHomeData.size() / 9);
            Inventory homesGUI = Bukkit.createInventory(null, numRows * 9, ChatColor.BOLD + "Your Homes");

            int slot = 0;
            for (Map.Entry<String, HomeData> entry : playerHomeData.entrySet()) {
                String homeName = entry.getKey();
                ItemStack bedItem = createBedItem(homeName);
                homesGUI.setItem(slot++, bedItem);
            }

            player.openInventory(homesGUI);
            playerSelectedHomes.put(player, playerHomeData);
        } else {
            player.sendMessage(ChatColor.RED + "You don't have any homes set!");
        }
    }

    private ItemStack createBedItem(String homeName) {
        ItemStack bedItem = new ItemStack(Material.RED_BED);
        ItemMeta meta = bedItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + homeName);
            bedItem.setItemMeta(meta);
        }
        return bedItem;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null && event.getView().getTitle().equals(ChatColor.BOLD + "Your Homes")) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.RED_BED && clickedItem.hasItemMeta()) {
                event.setCancelled(true);
                ItemMeta meta = clickedItem.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String homeName = ChatColor.stripColor(meta.getDisplayName().replace(ChatColor.AQUA.toString(), ""));

                    // Execute /home <name> command for teleportation
                    player.performCommand("home " + homeName);
                }
            }
        }
    }



    private void teleportWithCountdown(Player player, Location targetLocation) {
        player.sendMessage(ChatColor.AQUA + "Teleporting to your home in 3 seconds...");

        new BukkitRunnable() {
            int count = 3;

            @Override
            public void run() {
                if (count > 0) {
                    player.sendMessage(String.valueOf(ChatColor.AQUA) + ChatColor.ITALIC + count + " seconds...");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.0f);
                    count--;
                } else {
                    player.teleport(targetLocation);
                    player.sendMessage(ChatColor.AQUA + "Welcome home, " + ChatColor.BLUE + player.getName() + "!");
                    playTeleportSound(player.getLocation());
                    player.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, new Particle.DustOptions(Color.AQUA, 1));
                    player.spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
                    cancel(); // Cancel the task after teleportation
                }
            }
        }.runTaskTimer(this, 0L, 20L); // Delay: 0 ticks, Repeat: 20 ticks (1 second)
    }

    private boolean deleteHomeLocation(String playerName, String homeName) {
        if (playerHomes != null && playerHomes.containsKey(playerName)) {
            Map<String, HomeData> homes = playerHomes.get(playerName);
            if (homes != null && homes.containsKey(homeName)) {
                homes.remove(homeName);
                saveHomesData();
                return true;
            }
        }
        return false;
    }

    private void saveHomeLocation(String playerName, String homeName, Location location) {
        if (playerHomes == null) {
            playerHomes = new HashMap<>();
        }

        Map<String, HomeData> homes = playerHomes.computeIfAbsent(playerName, k -> new HashMap<>());
        homes.put(homeName, new HomeData(location.getX(), location.getY(), location.getZ(), Objects.requireNonNull(location.getWorld()).getName()));
        saveHomesData();
    }

    private Location getHomeLocation(String playerName, String homeName) {
        if (playerHomes != null && playerHomes.containsKey(playerName)) {
            Map<String, HomeData> homes = playerHomes.get(playerName);
            if (homes != null && homes.containsKey(homeName)) {
                HomeData homeData = homes.get(homeName);
                World world = Bukkit.getWorld(homeData.getWorld());
                if (world != null) {
                    return new Location(world, homeData.getX(), homeData.getY(), homeData.getZ());
                }
            }
        }
        return null;
    }

    private void loadHomesData() {
        try (FileReader reader = new FileReader(dataFile)) {
            TypeToken<HashMap<String, Map<String, HomeData>>> token = new TypeToken<>() {};
            playerHomes = gson.fromJson(reader, token.getType());

            if (playerHomes == null) {
                playerHomes = new HashMap<>();
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error loading homes data: " + e.getMessage());
        }
    }

    private void saveHomesData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(playerHomes, writer);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error saving homes data: " + e.getMessage());
        }
    }

    private void playTeleportSound(Location location) {
        Objects.requireNonNull(location.getWorld()).playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
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
