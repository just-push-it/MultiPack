package net.worldoftomorrow.nala.mp;

import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.managers.RegionManager;

public class JoinTask implements Runnable {
	
	private final MultiPack plugin;
	private final Player p;
	
	protected JoinTask(MultiPack plugin, Player p) {
		this.p = p;
		this.plugin = plugin;
	}

	public void run() {
		RegionManager rm = plugin.wg.getRegionManager(p.getWorld());
		TexturePack pack = plugin.util.getHighestPriorityPack(rm, p);
		if(pack == null)
			plugin.util.setDefaultPack(p, true);
		else
			plugin.util.setTexturePack(p, pack);
	}

}
