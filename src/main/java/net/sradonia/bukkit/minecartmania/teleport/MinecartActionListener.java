package net.sradonia.bukkit.minecartmania.teleport;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

import com.afforess.minecartmaniacore.MinecartManiaMinecart;
import com.afforess.minecartmaniacore.event.MinecartActionEvent;
import com.afforess.minecartmaniacore.event.MinecartManiaListener;

public class MinecartActionListener extends MinecartManiaListener {
	private final MinecartManiaTeleport plugin;
	private final TeleporterList teleporters;

	public MinecartActionListener(MinecartManiaTeleport plugin, TeleporterList teleporters) {
		this.plugin = plugin;
		this.teleporters = teleporters;
	}

	@Override
	public void onMinecartActionEvent(MinecartActionEvent event) {
		if (event.isActionTaken())
			return;
		Block blockAhead = event.getMinecart().getBlockTypeAhead();
		if (blockAhead != null) {
			Material type = blockAhead.getType();
			if (type.equals(Material.SIGN_POST) || type.equals(Material.WALL_SIGN)) {
				// Minecart is going to crash into a sign...

				Location signLocation = blockAhead.getLocation();
				Teleporter teleporter = teleporters.search(signLocation);
				if (teleporter != null) {
					// ... which is a teleporter!

					event.setActionTaken(true);

					MinecartManiaMinecart minecart = event.getMinecart();
					Location targetLocation = teleporter.getOther(signLocation);
					if (targetLocation == null) {
						// a) but we're missing the second waypoint!
						if (minecart.hasPlayerPassenger())
							minecart.getPlayerPassenger().sendMessage("You just crashed into an unconnected teleporter sign ;-)");
					} else {
						// b) and we have a lift-off!
						teleportMinecart(minecart, targetLocation);
					}
				}
			}
		}
	}

	private void teleportMinecart(MinecartManiaMinecart minecart, Location targetLocation) {
		// search for minecart tracks around the target waypoint
		Location trackLocation = findTrackAround(targetLocation);
		if (trackLocation == null) {
			if (minecart.hasPlayerPassenger())
				minecart.getPlayerPassenger().sendMessage("Couldn't find tracks at target sign.");
			return;
		}

		final Minecart cart = minecart.minecart;

		// check it's speed and calculate new velocity
		double speed = cart.getVelocity().length();
		final Vector newVelocity;
		if (targetLocation.getX() > trackLocation.getX())
			newVelocity = new Vector(-speed, 0, 0);
		else if (targetLocation.getX() < trackLocation.getX())
			newVelocity = new Vector(speed, 0, 0);
		else if (targetLocation.getZ() > trackLocation.getZ())
			newVelocity = new Vector(0, 0, -speed);
		else if (targetLocation.getZ() < trackLocation.getZ())
			newVelocity = new Vector(0, 0, speed);
		else // something went wrong?
			newVelocity = cart.getVelocity();

		// teleport minecart...
		final Entity passenger = cart.getPassenger();
		if (passenger == null) {
			// empty minecart, just teleport it the simple way
			if (cart.teleport(trackLocation))
				cart.setVelocity(newVelocity);
		} else {
			// we have a passenger, do some hacky stuff - idea thanks to 'Wormhole X-Treme'
			cart.eject();
			cart.remove();

			final Minecart newCart = trackLocation.getWorld().spawnMinecart(trackLocation);
			passenger.teleport(targetLocation);
			newCart.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					newCart.setPassenger(passenger);
					newCart.setVelocity(newVelocity);
				}
			}, 5);
		}
	}

	public static Location findTrackAround(Location center) {
		Block centerBlock = center.getBlock();

		Block block;
		block = centerBlock.getRelative(BlockFace.NORTH);
		if (block.getType().equals(Material.RAILS))
			return block.getLocation();
		block = centerBlock.getRelative(BlockFace.SOUTH);
		if (block.getType().equals(Material.RAILS))
			return block.getLocation();
		block = centerBlock.getRelative(BlockFace.EAST);
		if (block.getType().equals(Material.RAILS))
			return block.getLocation();
		block = centerBlock.getRelative(BlockFace.WEST);
		if (block.getType().equals(Material.RAILS))
			return block.getLocation();

		return null;
	}

}
