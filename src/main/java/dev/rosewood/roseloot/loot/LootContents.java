package dev.rosewood.roseloot.loot;

import dev.rosewood.roseloot.loot.context.LootContext;
import dev.rosewood.roseloot.loot.item.ExperienceLootItem;
import dev.rosewood.roseloot.loot.item.ItemLootItem;
import dev.rosewood.roseloot.loot.item.LootItem;
import dev.rosewood.roseloot.loot.item.LootTableLootItem;
import dev.rosewood.roseloot.loot.item.TriggerableLootItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Holds miscellaneous contents that can be obtained from looting
 */
public class LootContents {

    private final LootContext context;
    private final List<LootItem<?>> contents;

    public LootContents(LootContext context) {
        this.context = context;
        this.contents = new ArrayList<>();
    }

    /**
     * Processes and adds a List of LootItems to the stored contents
     *
     * @param lootItems The LootItems to add
     */
    public void add(List<LootItem<?>> lootItems) {
        // Turn LootTableLootItems into a List<LootItem<?>> and add them to the stored contents
        lootItems.stream()
                .flatMap(x -> x instanceof LootTableLootItem ? ((LootTableLootItem) x).create(this.context).stream() : Stream.of(x))
                .forEach(this.contents::add);

        // Attempt to merge LootItems
        for (int i = 0; i < this.contents.size(); i++) {
            LootItem<?> item = this.contents.get(i);
            for (int j = i + 1; j < this.contents.size(); j++) {
                LootItem<?> other = this.contents.get(j);
                if (item.combineWith(other))
                    this.contents.remove(j--);
            }
        }
    }

    /**
     * Gets a List of ItemStacks created by this LootContents.
     * Handled separately from {@link LootContents#triggerExtras(Location)}.
     *
     * @return the items of this loot contents
     */
    public List<ItemStack> getItems() {
        return this.contents.stream()
                .filter(x -> x instanceof ItemLootItem)
                .map(x -> (ItemLootItem) x)
                .flatMap(x -> x.create(this.context).stream())
                .collect(Collectors.toList());
    }

    /**
     * Gets the experience amount created by this LootContents.
     * Handled separately from {@link LootContents#triggerExtras(Location)}.
     *
     * @return the experience amount of this loot contents
     */
    public int getExperience() {
        return this.contents.stream()
                .filter(x -> x instanceof ExperienceLootItem)
                .map(x -> (ExperienceLootItem) x)
                .mapToInt(x -> x.create(this.context))
                .sum();
    }

    /**
     * Triggers the execution for anything that isn't an item or experience drop
     *
     * @param location The Location to execute the rest of the drops at
     */
    public void triggerExtras(Location location) {
        this.contents.stream()
                .filter(x -> x instanceof TriggerableLootItem)
                .forEach(x -> ((TriggerableLootItem<?>) x).trigger(this.context, location));
    }

    /**
     * @return true if there are other actions that will happen due to the loot generation, false otherwise
     */
    public boolean hasExtraTriggers() {
        return this.contents.stream().anyMatch(x -> x instanceof TriggerableLootItem);
    }

    /**
     * Removes all ItemLootItems from the contents
     */
    public void removeItems() {
        this.contents.removeIf(x -> x instanceof ItemLootItem);
    }

    /**
     * Removes all ExperienceLootItems from the contents
     */
    public void removeExperience() {
        this.contents.removeIf(x -> x instanceof ExperienceLootItem);
    }

    /**
     * Removes all TriggerableLootItems from the contents
     */
    public void removeExtraTriggers() {
        this.contents.removeIf(x -> x instanceof TriggerableLootItem);
    }

    /**
     * Gives the loot contained within this LootContents to a Player and executes all triggers
     *
     * @param player The Player to
     */
    public void dropForPlayer(Player player) {
        player.getInventory().addItem(this.getItems().toArray(new ItemStack[0])).forEach((x, y) -> player.getWorld().dropItemNaturally(player.getLocation(), y));

        int experience = this.getExperience();
        if (experience > 0) {
            Location location = player.getLocation();
            player.getWorld().spawn(location, ExperienceOrb.class, x -> x.setExperience(experience));
        }

        this.triggerExtras(player.getLocation());
    }

}
