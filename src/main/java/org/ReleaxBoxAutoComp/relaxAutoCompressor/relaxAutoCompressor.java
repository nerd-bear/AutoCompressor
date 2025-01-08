package org.ReleaxBoxAutoComp.relaxAutoCompressor;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class relaxAutoCompressor extends JavaPlugin implements Listener, CommandExecutor {
        private Map<UUID, Boolean> autoCompMap = new HashMap<>();
        private Map<UUID, Boolean> waitMap = new HashMap<>();
        private Map<Material, CompressionRule> compressionRules = new HashMap<>();

        @Override
        public void onEnable() {
            // Load configuration
            saveDefaultConfig();
            loadCompressionRules();

            // Register events and commands
            getServer().getPluginManager().registerEvents(this, this);
            this.getCommand("autocompress").setExecutor(new AutoCompressCommand(this));
        }

        private void loadCompressionRules() {
            FileConfiguration config = getConfig();
            for (String key : config.getConfigurationSection("compression-rules").getKeys(false)) {
                Material material = Material.valueOf(key.toUpperCase());
                CompressionRule rule = new CompressionRule(
                        Material.valueOf(config.getString("compression-rules." + key + ".compressed")),
                        Material.valueOf(config.getString("compression-rules." + key + ".super-compressed")),
                        Material.valueOf(config.getString("compression-rules." + key + ".ultra-compressed")),
                        ChatColor.translateAlternateColorCodes('&', config.getString("compression-rules." + key + ".compressed-name", "")),
                        ChatColor.translateAlternateColorCodes('&', config.getString("compression-rules." + key + ".super-compressed-name", "")),
                        ChatColor.translateAlternateColorCodes('&', config.getString("compression-rules." + key + ".ultra-compressed-name", ""))
                );
                compressionRules.put(material, rule);
            }
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            if (waitMap.getOrDefault(uuid, false)) {
                return;
            }

            Material blockType = event.getBlock().getType();
            CompressionRule rule = compressionRules.get(blockType);

            if (rule != null && (player.hasPermission("autocompressor.use") || autoCompMap.getOrDefault(uuid, false))) {
                compressItems(player, rule);
                waitMap.put(uuid, true);
                Bukkit.getScheduler().runTaskLater(this, () -> waitMap.remove(uuid), 1);
            }
        }

        private void compressItems(Player player, CompressionRule rule) {
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null && item.getType() == rule.compressed()) {
                    if (item.getAmount() >= 64) {
                        item.setAmount(item.getAmount() - 64);
                        ItemStack compressedItem = new ItemStack(rule.superCompressed());
                        if (!rule.superCompressedName().isEmpty()) {
                            compressedItem.getItemMeta().setDisplayName(rule.superCompressedName());
                        }
                        player.getInventory().addItem(compressedItem);
                    }
                } else if (item != null && item.getType() == rule.superCompressed()) {
                    if (item.getAmount() >= 64) {
                        item.setAmount(item.getAmount() - 64);
                        ItemStack ultraCompressedItem = new ItemStack(rule.ultraCompressed());
                        if (!rule.ultraCompressedName().isEmpty()) {
                            ultraCompressedItem.getItemMeta().setDisplayName(rule.ultraCompressedName());
                        }
                        player.getInventory().addItem(ultraCompressedItem);
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            waitMap.remove(event.getPlayer().getUniqueId());
        }

        public Map<UUID, Boolean> getAutoCompMap() {
            return autoCompMap;
        }

    private record CompressionRule(Material compressed, Material superCompressed, Material ultraCompressed,
                                   String compressedName, String superCompressedName, String ultraCompressedName) {
    }

    private record AutoCompressCommand(relaxAutoCompressor plugin) implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                UUID uuid = player.getUniqueId();
                boolean autoComp = plugin.getAutoCompMap().getOrDefault(uuid, false);
                plugin.getAutoCompMap().put(uuid, !autoComp);
                player.sendMessage(autoComp ? ChatColor.RED + "Disabled Auto-Compressor!" : ChatColor.GREEN + "Enabled Auto-Compressor!");
                return true;
            }
            return false;
        }
    }
    }