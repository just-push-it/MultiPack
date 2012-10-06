/*
Copyright (c) 2012, Alan Litz
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met: 

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer. 
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution. 

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies, 
either expressed or implied, of the FreeBSD Project.
 */

package net.worldoftomorrow.nala.mp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class MultiPack extends JavaPlugin implements Listener {

	protected Map<String, ArrayList<TexturePack>> texturePacks = new HashMap<String, ArrayList<TexturePack>>();
	protected Map<String, TexturePack> playerCurrent = new HashMap<String, TexturePack>();
	protected Map<String, String> defaultPacks = new HashMap<String, String>();
	protected WorldGuardPlugin wg;
	private Logger log;
	private Metrics metrics;
	protected Util util;

	private String no_mod_perm = "You do not have permission to set the texture pack for that region!";
	private String already_set = "There is already a pack defined for that region!";

	@Override
	public void onEnable() {
		this.log = this.getLogger();
		this.util = new Util(this);
		try {
			metrics = new Metrics(this);
			metrics.start();
		} catch (IOException ex) {
			log.severe("Failed to start metrics!");
		}
		if (!this.getDataFolder().exists()) {
			if (!this.getDataFolder().mkdirs()) {
				log.severe("Could not create plugin folder!");
			}
		}
		File config = new File(this.getDataFolder(), "config.yml");
		if (!config.exists()) {
			try {
				if (!config.createNewFile()) {
					log.severe("Could not create configuration file!");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Plugin wg = Bukkit.getServer().getPluginManager()
				.getPlugin("WorldGuard");
		if (wg == null || !(wg instanceof WorldGuardPlugin) || !wg.isEnabled()) {
			log.severe("Worldguard not found! Disabling.");
			this.getPluginLoader().disablePlugin(this);
		}
		this.wg = (WorldGuardPlugin) wg;
		this.loadTexturePacks(false);
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (args.length > 0) {
			FileConfiguration config = this.getConfig();
			if (args[0].equalsIgnoreCase("add") && args.length == 4) {
				World world = this.getServer().getWorld(args[1]);
				ProtectedRegion region = wg.getRegionManager(world).getRegion(args[2]);
				if(region == null) {
					sender.sendMessage(ChatColor.RED + "That region does not exist!");
					return true;
				} else if (!this.hasPermission(sender, region)) {
					sender.sendMessage(ChatColor.RED + this.no_mod_perm);
					return true;
				}

				String path = "Packs." + args[1] + "." + args[2];
				if (!config.isSet(path)) {
					this.getConfig().set(path + ".url", args[3]);
					this.saveConfig();
					this.loadTexturePacks(true);
					sender.sendMessage(ChatColor.BLUE
							+ "Texture pack for the region \"" + args[2] + "\" has been set.");
					return true;
				} else {
					sender.sendMessage(ChatColor.RED + this.already_set);
					return true;
				}
			} else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("rm") && args.length == 3) {
				World world = this.getServer().getWorld(args[1]);
				ProtectedRegion region = wg.getRegionManager(world).getRegion(args[2]);
				if(region == null) {
					sender.sendMessage(ChatColor.RED + "That region does not exist!");
					return true;
				} else if (!this.hasPermission(sender, region)) {
					sender.sendMessage(ChatColor.RED + this.no_mod_perm);
					return true;
				}

				String path = "Packs." + args[1] + "." + args[2];
				if (!config.isSet(path)) {
					sender.sendMessage(ChatColor.RED
							+ "There is no pack set for that region.");
					return true;
				}

				this.getConfig().set(path, null);
				this.saveConfig();
				this.loadTexturePacks(true);
				return true;

			} else if (args[0].equalsIgnoreCase("set") && args.length == 4) {
				World world = this.getServer().getWorld(args[1]);
				ProtectedRegion region = wg.getRegionManager(world).getRegion(args[2]);
				if(region == null) {
					sender.sendMessage(ChatColor.RED + "That region does not exist!");
					return true;
				} else if (!this.hasPermission(sender, region)) {
					sender.sendMessage(ChatColor.RED + this.no_mod_perm);
					return true;
				}
				String path = "Packs." + args[1] + "." + args[2] + ".url";
				this.getConfig().set(path, args[3]);
				this.saveConfig();
				this.loadTexturePacks(true);
				sender.sendMessage(ChatColor.BLUE
						+ "Texture pack for the region \"" + args[2]
						+ "\" has been set.");
				return true;
			} else if (args[0].equalsIgnoreCase("reload") && args.length == 1) {
				if (!(sender instanceof Player) || sender.isOp()
						|| sender.hasPermission("multipack.reload")) {
					this.loadTexturePacks(true);
					sender.sendMessage(ChatColor.BLUE
							+ "Texture packs reloaded.");
				} else {
					sender.sendMessage(ChatColor.RED + "You do not have permission to perform this command.");
				}
			} else if (args[0].equalsIgnoreCase("default") && args.length > 2) {
				if (!(sender instanceof Player) || sender.isOp()
						|| sender.hasPermission("multipack.default")) {
					if ((args[1].equalsIgnoreCase("add") || args[1]
							.equalsIgnoreCase("set")) && args.length == 4) {
						String path = "Defaults." + args[2];
						this.getConfig().set(path, args[3]);
						this.saveConfig();
						this.loadTexturePacks(true);
						return true;
					} else if (args[1].equalsIgnoreCase("remove")
							&& args.length == 3) {
						String path = "Defaults." + args[2];
						this.getConfig().set(path, null);
						this.saveConfig();
						this.loadTexturePacks(true);
						return true;
					} else {
						sender.sendMessage(ChatColor.RED
								+ "Invalid command syntax!");
						return true;
					}
				}
			} else {
				sender.sendMessage(ChatColor.RED + "Invalid command syntax.");
				return true;
			}
		} else {
			sender.sendMessage(ChatColor.BLUE
					+ "This server is running MultiPack version: "
					+ this.getDescription().getVersion());
		}
		return true;
	}

	private boolean hasPermission(CommandSender sender, ProtectedRegion region) {
		if(!(sender instanceof Player)) {
			return true;
		} else {
			BukkitPlayer lp = new BukkitPlayer(wg, (Player) sender);
			return (region.isOwner(lp)
					|| sender.isOp()
					|| sender.hasPermission("worldguard.region.texture.regions." + region.getId()));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.isCancelled() || (this.texturePacks.isEmpty() && this.defaultPacks.isEmpty()))
			return; // Don't do anything if it is cancelled or no packs loaded
		Player p = event.getPlayer();
		RegionManager rm = this.wg.getRegionManager(p.getWorld());
		TexturePack pack = util.getHighestPriorityPack(rm, p);

		if (pack == null) {
			util.setDefaultPack(p, false);
			return;
		} else {
			util.setTexturePack(p, pack);
			return;
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		this.playerCurrent.put(p.getName(), null);
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new JoinTask(this, p), 20);
	}
	
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player p = event.getPlayer();
		RegionManager rm = this.wg.getRegionManager(p.getWorld());
		TexturePack pack = util.getHighestPriorityPack(rm, p);
		if(pack == null)
			util.setDefaultPack(p, true);
		else
			util.setTexturePack(p, pack);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		this.playerCurrent.remove(event.getPlayer().getName());
	}

	private void loadTexturePacks(boolean reload) {
		if (reload)
			this.reloadConfig();
		if (!this.getConfig().isSet("Packs"))
			return;
		Map<String, Object> worlds = this.getConfig()
				.getConfigurationSection("Packs").getValues(false);
		this.texturePacks.clear();
		for (String world : worlds.keySet()) {
			Map<String, Object> regions = this.getConfig()
					.getConfigurationSection("Packs")
					.getConfigurationSection(world).getValues(false);
			ArrayList<TexturePack> packs = new ArrayList<TexturePack>();
			for (String region : regions.keySet()) {
				String path = "Packs." + world + "." + region + ".";
				String url = this.getConfig().getString(path + "url");
				packs.add(new TexturePack(region, url));
			}
			this.texturePacks.put(world, packs);
		}

		if (!this.getConfig().isSet("Defaults"))
			return;
		ConfigurationSection defaults = this.getConfig()
				.getConfigurationSection("Defaults");
		for (String key : defaults.getKeys(false)) {
			String url = defaults.getString(key);
			this.defaultPacks.put(key, url);
		}
	}
}
