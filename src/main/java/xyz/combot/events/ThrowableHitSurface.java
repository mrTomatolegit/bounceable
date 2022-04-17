package xyz.combot.events;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;

import xyz.combot.App;

public class ThrowableHitSurface implements Listener {
    FileConfiguration config = Bukkit.getPluginManager().getPlugin(App.PluginName).getConfig();
    int maxBounceCount = config.getInt("maxBounceCount");

    boolean bouncesOffFloor = config.getBoolean("bouncesOffFloor");
    boolean bouncesOffWalls = config.getBoolean("bouncesOffWalls");
    boolean bouncesOffCeiling = config.getBoolean("bouncesOffCeiling");
    boolean chickenInEgg = config.getBoolean("chickenInEgg");

    List<String> bounceable = config.getStringList("bounceables");

    public int getBounceCount(Projectile projectile) {
        List<MetadataValue> metadata = projectile.getMetadata("bounceCount");
        if (metadata.size() == 0)
            return 0;

        return (int) metadata.get(0).value();
    }

    public void setBounceCount(Projectile projectile, int bounceCount) {
        projectile.setMetadata("bounceCount",
                new FixedMetadataValue(Bukkit.getPluginManager().getPlugin(App.PluginName), bounceCount));
    }

    public boolean getCanSplash(Projectile projectile) {
        List<MetadataValue> metadata = projectile.getMetadata("canSplash");
        if (metadata.size() == 0)
            return true;

        return (boolean) metadata.get(0).value();
    }

    public void setCanSplash(Projectile projectile, Boolean canSplash) {
        if (projectile instanceof ThrownPotion || projectile instanceof ThrownExpBottle || projectile instanceof Egg)
            projectile.setMetadata("canSplash",
                    new FixedMetadataValue(Bukkit.getPluginManager().getPlugin(App.PluginName), canSplash));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOW)
    public void onExpBottleEvent(ExpBottleEvent event) {
        ThrownExpBottle potion = (ThrownExpBottle) event.getEntity();

        if (!getCanSplash(potion)) {
            event.setCancelled(true);
            event.setExperience(0);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOW)
    public void onPotionSplashEvent(PotionSplashEvent event) {
        ThrownPotion potion = (ThrownPotion) event.getEntity();

        if (!getCanSplash(potion)) {
            event.setCancelled(true);
            event.getAffectedEntities().forEach(entity -> event.setIntensity(entity, 0));
        }
    }

    public Vector makeResultingVector(BlockFace hitBlockFace, Vector initVector) {
        Vector resulting = initVector.clone();
        if (hitBlockFace.getModX() != 0) // Hit Wall
            resulting.setX(initVector.getX() * -1);

        if (hitBlockFace.getModY() != 0) // Hit Floor/Ceiling
            resulting.setY(initVector.getY() * -1);

        if (hitBlockFace.getModZ() != 0) // Hit Wall
            resulting.setZ(initVector.getZ() * -1);

        return resulting.normalize();
    }

    public Projectile cloneProjectile(Projectile projectile) {
        Projectile newProjectile = (Projectile) projectile.getWorld().spawnEntity(projectile.getLocation(),
                projectile.getType());

        newProjectile.setShooter(projectile.getShooter());
        projectile.getPassengers().forEach(newProjectile::addPassenger);
        projectile.getScoreboardTags().forEach(newProjectile::addScoreboardTag);
        newProjectile.setBounce(projectile.doesBounce());
        newProjectile.setCustomName(projectile.getCustomName());
        newProjectile.setCustomNameVisible(projectile.isCustomNameVisible());
        newProjectile.setFireTicks(projectile.getFireTicks());
        newProjectile.setGlowing(projectile.isGlowing());
        newProjectile.setGravity(projectile.hasGravity());
        newProjectile.setTicksLived(projectile.getTicksLived());
        setBounceCount(newProjectile, getBounceCount(projectile));

        if (projectile instanceof ThrownPotion) {
            ThrownPotion potion = (ThrownPotion) projectile;
            ThrownPotion newPotion = (ThrownPotion) newProjectile;
            newPotion.setItem(potion.getItem());
        }
        if (projectile instanceof ThrownExpBottle) {
            ThrownExpBottle potion = (ThrownExpBottle) projectile;
            ThrownExpBottle newPotion = (ThrownExpBottle) newProjectile;
            newPotion.setItem(potion.getItem());
        }

        setCanSplash(newProjectile, getCanSplash(projectile));

        return newProjectile;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOW)
    public void onPlayerEggThrow(PlayerEggThrowEvent event) {
        boolean canSplash = getCanSplash(event.getEgg());
        event.setHatching(chickenInEgg && canSplash);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onThrowableHitSurface(ProjectileHitEvent event) {
        Projectile projectile = (Projectile) event.getEntity();
        if (event.getHitEntity() != null) {
            setCanSplash(projectile, true);
            return;
        }
        if (projectile instanceof Snowball && !bounceable.contains("snowball"))
            return;
        else if (projectile instanceof Egg && !bounceable.contains("egg")) {
            setCanSplash(projectile, true);
            return;
        } else if (projectile instanceof ThrownPotion && !bounceable.contains("potion")) {
            setCanSplash(projectile, true);
            return;
        } else if (projectile instanceof ThrownExpBottle && !bounceable.contains("xp_bottle")) {
            setCanSplash(projectile, true);
            return;
        } else if (projectile instanceof Arrow && !bounceable.contains("arrow"))
            return;
        else if (projectile instanceof EnderPearl && !bounceable.contains("endereye"))
            return;
        else if (projectile instanceof Firework && !bounceable.contains("firework"))
            return;
        else if (projectile instanceof Trident && !bounceable.contains("trident"))
            return;
        else if (projectile instanceof Fireball && !bounceable.contains("fireball"))
            return;

        BlockFace hitBlockFace = event.getHitBlockFace();

        if (hitBlockFace.getModY() > 0 && !bouncesOffFloor) { // Can't Bounce Floor
            setCanSplash(projectile, true);
            return;
        }

        if ((hitBlockFace.getModZ() != 0 || hitBlockFace.getModX() != 0) && !bouncesOffWalls) { // Can't Bounce Walls
            setCanSplash(projectile, true);
            return;
        }

        if (hitBlockFace.getModY() < 0 && !bouncesOffCeiling) { // Can't Bounce Ceiling
            setCanSplash(projectile, true);
            return;
        }

        int bounceCount = getBounceCount(projectile);

        if (bounceCount < maxBounceCount) {
            Projectile newentity = cloneProjectile(projectile);
            setCanSplash(projectile, false);
            setCanSplash(newentity, false);
            projectile.remove();


            setBounceCount(newentity, bounceCount + 1);
            newentity.setVelocity(makeResultingVector(hitBlockFace, projectile.getVelocity()));
        } else {
            setCanSplash(projectile, true);
        }
    }
}
