package me.plugin.scrates;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramManager {

    private sCrates plugin;
    private Map<String, List<ArmorStand>> holograms;
    private static final double LINE_SPACING = 0.25;

    public HologramManager(sCrates plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
    }

    private String locKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private void setMarkerSafe(ArmorStand stand) {
        try {
            Method m = ArmorStand.class.getMethod("setMarker", boolean.class);
            m.invoke(stand, true);
        } catch (NoSuchMethodException e) {
            try {
                Method getHandle = stand.getClass().getMethod("getHandle");
                Object nmsEntity = getHandle.invoke(stand);
                Class<?> nmsClass = nmsEntity.getClass();
                try {
                    Method nmsSetMarker = nmsClass.getMethod("setMarker", boolean.class);
                    nmsSetMarker.invoke(nmsEntity, true);
                } catch (NoSuchMethodException e2) {
                    // fallback: rely on setSmall(true) + setBasePlate(false)
                }
            } catch (Exception e3) {
                // silent fallback
            }
        } catch (Exception e) {
            // silent fallback
        }
    }

    public void createHologram(Crate crate) {
        Location blockLoc = crate.getBlockLocation();
        if (blockLoc == null) return;

        removeHologram(blockLoc);

        List<String> lines = crate.getHologramLines();
        if (lines == null || lines.isEmpty()) return;

        double baseY = blockLoc.getBlockY() + 1.2 + crate.getHologramHeightOffset();

        List<ArmorStand> stands = new ArrayList<>();

        // Line 0 is at the top, each subsequent line is LINE_SPACING below
        for (int i = 0; i < lines.size(); i++) {
            double yOffset = baseY - (i * LINE_SPACING);
            Location lineLoc = new Location(
                    blockLoc.getWorld(),
                    blockLoc.getBlockX() + 0.5,
                    yOffset,
                    blockLoc.getBlockZ() + 0.5
            );

            ArmorStand stand = (ArmorStand) blockLoc.getWorld().spawnEntity(lineLoc, EntityType.ARMOR_STAND);
            stand.setCanPickupItems(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(crate.resolveHologramLine(lines.get(i)));
            stand.setVisible(false);
            stand.setSmall(true);
            setMarkerSafe(stand);
            stand.setBasePlate(false);
            stand.setGravity(false);

            stands.add(stand);
        }

        holograms.put(locKey(blockLoc), stands);
    }

    public void removeHologram(Location blockLoc) {
        String key = locKey(blockLoc);
        List<ArmorStand> stands = holograms.remove(key);
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand != null) {
                    stand.remove();
                }
            }
        }
    }

    public void updateHologram(Crate crate) {
        Location blockLoc = crate.getBlockLocation();
        if (blockLoc == null) return;

        // Recreate the hologram to reflect changes
        createHologram(crate);
    }

    public void updateHologramName(Crate crate) {
        updateHologram(crate);
    }

    public void removeAllHolograms() {
        for (List<ArmorStand> stands : holograms.values()) {
            if (stands != null) {
                for (ArmorStand stand : stands) {
                    if (stand != null) {
                        stand.remove();
                    }
                }
            }
        }
        holograms.clear();
    }

    public void restoreAllHolograms() {
        for (Crate crate : plugin.getCrateManager().getCrates().values()) {
            if (crate.getBlockLocation() != null) {
                createHologram(crate);
            }
        }
    }

    public List<ArmorStand> getStands(String locKey) {
        return holograms.get(locKey);
    }

    public String getLocKey(Location loc) {
        return locKey(loc);
    }
}
