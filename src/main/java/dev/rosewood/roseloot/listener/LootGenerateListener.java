package dev.rosewood.roseloot.listener;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.roseloot.loot.LootContents;
import dev.rosewood.roseloot.loot.LootResult;
import dev.rosewood.roseloot.loot.LootTableType;
import dev.rosewood.roseloot.loot.context.LootContext;
import dev.rosewood.roseloot.loot.context.LootContextParams;
import dev.rosewood.roseloot.manager.ConfigurationManager.Setting;
import dev.rosewood.roseloot.manager.LootTableManager;
import dev.rosewood.roseloot.util.LootUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

public class LootGenerateListener implements Listener {

    private final LootTableManager lootTableManager;

    public LootGenerateListener(RosePlugin rosePlugin) {
        this.lootTableManager = rosePlugin.getManager(LootTableManager.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (!(event.getInventoryHolder() instanceof Container))
            return;

        Block block = ((Container) event.getInventoryHolder()).getBlock();
        if (Setting.DISABLED_WORLDS.getStringList().stream().anyMatch(x -> x.equalsIgnoreCase(block.getWorld().getName())))
            return;

        LivingEntity looter = null;
        if (event.getEntity() instanceof LivingEntity)
            looter = (LivingEntity) event.getEntity();

        LootContext lootContext = LootContext.builder(LootUtils.getEntityLuck(looter))
                .put(LootContextParams.ORIGIN, block.getLocation())
                .put(LootContextParams.LOOTER, looter)
                .put(LootContextParams.LOOTED_BLOCK, block)
                .put(LootContextParams.VANILLA_LOOT_TABLE_KEY, event.getLootTable().getKey())
                .build();
        LootResult lootResult = this.lootTableManager.getLoot(LootTableType.CONTAINER, lootContext);
        LootContents lootContents = lootResult.getLootContents();

        // Overwrite existing loot if applicable
        if (lootResult.shouldOverwriteItems())
            event.getLoot().clear();

        // Set items and drop experience
        event.getLoot().addAll(lootResult.getLootContents().getItems());

        int experience = lootContents.getExperience();
        if (experience > 0) {
            Location location = looter == null ? block.getLocation() : looter.getLocation();
            block.getWorld().spawn(location, ExperienceOrb.class, x -> x.setExperience(experience));
        }

        lootContents.triggerExtras(block.getLocation());
    }

}
