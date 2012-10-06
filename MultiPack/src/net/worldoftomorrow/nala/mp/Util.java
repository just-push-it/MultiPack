package net.worldoftomorrow.nala.mp;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Packet250CustomPayload;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Util {
	private final MultiPack plugin;
	
	protected Util(MultiPack plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Set the players texture pack to the given texture pack
	 * @param Player
	 * @param TexturePack
	 */
	protected void setTexturePack(Player p, TexturePack pack) {
		if (plugin.playerCurrent.get(p.getName()) == pack)
			return;
		Packet250CustomPayload packet = new Packet250CustomPayload("MC|TPack",
				(pack.getUrl() + "\0" + 16).getBytes());
		this.getEntityPlayer(p).netServerHandler.sendPacket(packet);
		plugin.playerCurrent.put(p.getName(), pack);
	}
	
	/**
	 * Set the players texture pack to the default one.
	 * @param Player
	 */
	protected void setDefaultPack(Player p, boolean force) {
		TexturePack current = plugin.playerCurrent.get(p.getName());
		String defurl = MinecraftServer.getServer().getTexturePack();
		if (current == null && !force)
			return;

		Packet250CustomPayload packet;
		if (plugin.defaultPacks.containsKey(p.getWorld().getName())) {
			String url = plugin.defaultPacks.get(p.getWorld().getName());
			packet = new Packet250CustomPayload("MC|TPack", (url + "\0" + 16).getBytes());
		} else if (defurl.equals("") || defurl == null) {
			packet = new Packet250CustomPayload("MC|TPack", ("https://dl.dropbox.com/u/52707344/default.zip" + "\0" + 16).getBytes());
		} else {
			packet = new Packet250CustomPayload("MC|TPack", (defurl + "\0" + 16).getBytes());
		}
		this.getEntityPlayer(p).netServerHandler.sendPacket(packet);
		plugin.playerCurrent.put(p.getName(), null);
	}
	
	/**
	 * Get the texture pack for the highest priority region the player is in.
	 * @param RegionManager
	 * @param Player
	 * @return Highest Priority Pack
	 */
	
	protected TexturePack getHighestPriorityPack(RegionManager rm, Player p) {
		TexturePack tp = null;
		if (!plugin.texturePacks.containsKey(p.getWorld().getName()))
			return null;
		ArrayList<TexturePack> worldPacks = plugin.texturePacks.get(p.getWorld()
				.getName());
		if (worldPacks == null || worldPacks.isEmpty()) {
			return null;
		}

		ApplicableRegionSet set = rm.getApplicableRegions(p.getLocation());
		List<TexturePack> regionPacks = this.getAplicablePacks(set, worldPacks);

		for (ProtectedRegion region : set) {
			for (TexturePack pack : regionPacks) {
				ProtectedRegion packRegion = rm.getRegion(pack.getRegion());
				if (packRegion == null)
					continue;
				if (tp == null)
					tp = pack;
				if (region.getPriority() < packRegion.getPriority())
					tp = pack;
			}
		}
		return tp;
	}

	private List<TexturePack> getAplicablePacks(ApplicableRegionSet set,
			ArrayList<TexturePack> worldPacks) {
		List<TexturePack> packs = new ArrayList<TexturePack>();
		for (ProtectedRegion region : set) {
			for (TexturePack pack : worldPacks) {
				if (region.getId().equals(pack.getRegion())) {
					packs.add(pack);
				}
			}
		}
		return packs;
	}

	private EntityPlayer getEntityPlayer(Player p) {
		return ((CraftPlayer) p).getHandle();
	}
}
