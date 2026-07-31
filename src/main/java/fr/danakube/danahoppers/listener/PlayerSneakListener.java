package fr.danakube.danahoppers.listener;

import fr.danakube.danahoppers.DanaHoppersPlugin;
import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.config.HopperTierConfig;
import fr.danakube.danahoppers.config.HopperTypeConfig;
import fr.danakube.danahoppers.manager.HopperManager;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.util.PDCUtil;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

/**
 * Écouteur gérant la téléportation du joueur lorsqu'il s'accroupit (sneak) sur un SauteLien.
 */
public class PlayerSneakListener implements Listener {

    private final HopperManager hopperManager;
    private final ConfigManager configManager;

    public PlayerSneakListener(HopperManager hopperManager, ConfigManager configManager) {
        this.hopperManager = Objects.requireNonNull(hopperManager, "hopperManager cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();

        Block exactBlock = playerLoc.getBlock();
        Block underBlock = playerLoc.clone().subtract(0, 0.5, 0).getBlock();

        CustomHopper hopper = hopperManager.getHopper(exactBlock.getLocation());
        if (hopper == null) {
            hopper = hopperManager.getHopper(underBlock.getLocation());
        }

        if (hopper == null) {
            return;
        }

        HopperTypeConfig typeConfig = configManager.getHopperType(hopper.getTypeId());
        if (typeConfig == null) {
            return;
        }

        HopperTierConfig tierConfig = typeConfig.getTier(hopper.getTier());
        if (tierConfig == null) {
            return;
        }

        if (!"TELEPORTATION".equalsIgnoreCase(tierConfig.suctionType())) {
            return;
        }

        Location targetLoc = hopper.getLinkedLocation();
        if (targetLoc == null) {
            return;
        }

        if (targetLoc.isWorldLoaded()) {
            Block targetBlock = targetLoc.getBlock();
            if (!(targetBlock.getState() instanceof InventoryHolder)) {
                hopper.setLinkedLocation(null);
                
                Location hopperLoc = hopper.getLocation();
                if (hopperLoc != null && hopperLoc.isWorldLoaded()) {
                    Block hopperBlock = hopperLoc.getBlock();
                    if (hopperBlock.getState() instanceof TileState tileState) {
                        PDCUtil.saveToPDC(tileState, hopper, DanaHoppersPlugin.getPlugin(DanaHoppersPlugin.class));
                    }
                }
                
                hopperManager.registerHopper(hopper);
                return;
            }
        }

        Location destination = targetLoc.clone().add(0.5, 1.0, 0.5);
        destination.setYaw(playerLoc.getYaw());
        destination.setPitch(playerLoc.getPitch());

        player.teleportAsync(destination);
        configManager.sendMessage(player, "teleport_success");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
    }
}
