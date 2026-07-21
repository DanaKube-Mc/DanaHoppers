package fr.danakube.danahoppers.model;

import org.bukkit.Location;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Représente un entonnoir personnalisé (CustomHopper) dans le monde.
 */
public class CustomHopper {

    private final UUID hopperUuid;
    private UUID ownerUuid;
    private Location location;
    private String typeId;
    private int tier;
    private Location linkedLocation;
    private HopperFilter filter;
    private final AtomicLong itemsTransferred;
    private boolean hologramEnabled;
    private boolean teleportEnabled;

    public CustomHopper(UUID hopperUuid, UUID ownerUuid, Location location, String typeId, int tier) {
        this(hopperUuid, ownerUuid, location, typeId, tier, null, new HopperFilter(), 0L, true, false);
    }

    public CustomHopper(UUID hopperUuid, UUID ownerUuid, Location location, String typeId, int tier,
                        Location linkedLocation, HopperFilter filter, long itemsTransferred,
                        boolean hologramEnabled, boolean teleportEnabled) {
        this.hopperUuid = Objects.requireNonNull(hopperUuid, "hopperUuid cannot be null");
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.typeId = Objects.requireNonNull(typeId, "typeId cannot be null");
        this.tier = Math.max(1, tier);
        this.linkedLocation = linkedLocation;
        this.filter = filter != null ? filter : new HopperFilter();
        this.itemsTransferred = new AtomicLong(Math.max(0L, itemsTransferred));
        this.hologramEnabled = hologramEnabled;
        this.teleportEnabled = teleportEnabled;
    }

    public UUID getHopperUuid() {
        return hopperUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid cannot be null");
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = Objects.requireNonNull(location, "location cannot be null");
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = Objects.requireNonNull(typeId, "typeId cannot be null");
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = Math.max(1, tier);
    }

    public Location getLinkedLocation() {
        return linkedLocation;
    }

    public void setLinkedLocation(Location linkedLocation) {
        this.linkedLocation = linkedLocation;
    }

    public HopperFilter getFilter() {
        return filter;
    }

    public void setFilter(HopperFilter filter) {
        this.filter = filter != null ? filter : new HopperFilter();
    }

    public long getItemsTransferred() {
        return itemsTransferred.get();
    }

    public void setItemsTransferred(long count) {
        this.itemsTransferred.set(Math.max(0L, count));
    }

    public long addItemsTransferred(long count) {
        return this.itemsTransferred.addAndGet(Math.max(0L, count));
    }

    public boolean isHologramEnabled() {
        return hologramEnabled;
    }

    public void setHologramEnabled(boolean hologramEnabled) {
        this.hologramEnabled = hologramEnabled;
    }

    public boolean isTeleportEnabled() {
        return teleportEnabled;
    }

    public void setTeleportEnabled(boolean teleportEnabled) {
        this.teleportEnabled = teleportEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomHopper hopper = (CustomHopper) o;
        return Objects.equals(hopperUuid, hopper.hopperUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hopperUuid);
    }
}
