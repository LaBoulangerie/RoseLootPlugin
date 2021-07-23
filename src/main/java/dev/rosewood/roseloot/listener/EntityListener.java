package dev.rosewood.roseloot.listener;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.rosewood.roseloot.loot.LootContents;
import dev.rosewood.roseloot.loot.LootContext;
import dev.rosewood.roseloot.loot.LootResult;
import dev.rosewood.roseloot.loot.LootTableType;
import dev.rosewood.roseloot.manager.ConfigurationManager.Setting;
import dev.rosewood.roseloot.manager.LootTableManager;
import dev.rosewood.roseloot.util.LootUtils;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockShearEntityEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

public class EntityListener implements Listener {

    private final LootTableManager lootTableManager;
    private Reference<Player> lastShearer;

    public EntityListener(RosePlugin rosePlugin) {
        this.lootTableManager = rosePlugin.getManager(LootTableManager.class);
        this.lastShearer = new WeakReference<>(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (Setting.DISABLED_WORLDS.getStringList().stream().anyMatch(x -> x.equalsIgnoreCase(entity.getWorld().getName())))
            return;

        Entity looter = null;
        if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent)
            looter = ((EntityDamageByEntityEvent) entity.getLastDamageCause()).getDamager();

        LootContext lootContext = new LootContext(looter, entity);
        LootResult lootResult = this.lootTableManager.getLoot(LootTableType.ENTITY, lootContext);
        LootContents lootContents = lootResult.getLootContents();

        // Overwrite existing drops if applicable
        if (lootResult.shouldOverwriteExisting()) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        // Trigger explosion if applicable
        if (lootContents.getExplosionState() != null)
            lootContents.getExplosionState().trigger(entity.getLocation());

        // Add items to drops and adjust experience
        event.getDrops().addAll(lootContents.getItems());
        event.setDroppedExp(event.getDroppedExp() + lootContents.getExperience());

        // Run commands
        if (!lootContents.getCommands().isEmpty()) {
            Location location = entity.getLocation();
            StringPlaceholders.Builder stringPlaceholdersBuilder = StringPlaceholders.builder("world", entity.getWorld().getName())
                    .addPlaceholder("x", location.getX())
                    .addPlaceholder("y", location.getY())
                    .addPlaceholder("z", location.getZ());

            boolean isPlayer = looter instanceof Player;
            if (isPlayer)
                stringPlaceholdersBuilder.addPlaceholder("player", looter.getName());

            StringPlaceholders stringPlaceholders = stringPlaceholdersBuilder.build();
            for (String command : lootContents.getCommands())
                if (!command.contains("%player%") || isPlayer)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stringPlaceholders.apply(command));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        // Tag all spawned entities with the spawn reason
        LootUtils.setEntitySpawnReason(event.getEntity(), event.getSpawnReason());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        this.lastShearer = new WeakReference<>(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockShearEntity(BlockShearEntityEvent event) {
        this.lastShearer = new WeakReference<>(null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof LivingEntity))
            return;

        LivingEntity entity = (LivingEntity) event.getEntity();

        Player shearer;
        switch (entity.getType()) {
            case SHEEP:
            case SNOWMAN:
            case MUSHROOM_COW:
                shearer = this.lastShearer.get();
                break;
            default:
                shearer = null;
                break;
        }

        LootContext lootContext = new LootContext(shearer, entity);
        LootResult lootResult = this.lootTableManager.getLoot(LootTableType.ENTITY_DROP_ITEM, lootContext);
        LootContents lootContents = lootResult.getLootContents();

        Location dropLocation = event.getItemDrop().getLocation();

        // Overwrite existing drops if applicable
        if (lootResult.shouldOverwriteExisting())
            event.setCancelled(true);

        // Trigger explosion if applicable
        if (lootContents.getExplosionState() != null)
            lootContents.getExplosionState().trigger(entity.getLocation());

        // Add items to drops and adjust experience
        lootContents.getItems().forEach(x -> entity.getWorld().dropItemNaturally(dropLocation, x));

        int experience = lootContents.getExperience();
        if (experience > 0)
            entity.getWorld().spawn(entity.getLocation(), ExperienceOrb.class, x -> x.setExperience(experience));

        // Run commands
        if (!lootContents.getCommands().isEmpty()) {
            Location location = entity.getLocation();
            StringPlaceholders.Builder stringPlaceholdersBuilder = StringPlaceholders.builder("world", entity.getWorld().getName())
                    .addPlaceholder("x", location.getX())
                    .addPlaceholder("y", location.getY())
                    .addPlaceholder("z", location.getZ());

            boolean isPlayer = shearer != null;
            if (isPlayer)
                stringPlaceholdersBuilder.addPlaceholder("player", shearer.getName());

            StringPlaceholders stringPlaceholders = stringPlaceholdersBuilder.build();
            for (String command : lootContents.getCommands())
                if (!command.contains("%player%") || isPlayer)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stringPlaceholders.apply(command));
        }
    }

}
