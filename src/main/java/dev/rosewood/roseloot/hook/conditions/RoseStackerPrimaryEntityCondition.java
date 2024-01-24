package dev.rosewood.roseloot.hook.conditions;

import dev.rosewood.roseloot.listener.hook.RoseStackerEntityDeathListener;
import dev.rosewood.roseloot.loot.condition.BaseLootCondition;
import dev.rosewood.roseloot.loot.context.LootContext;

public class RoseStackerPrimaryEntityCondition extends BaseLootCondition {

    public RoseStackerPrimaryEntityCondition(String tag) {
        super(tag);
    }

    @Override
    public boolean check(LootContext context) {
        return context.get(RoseStackerEntityDeathListener.STACKED_ENTITY_PRIMARY).orElse(false);
    }

    @Override
    public boolean parseValues(String[] values) {
        return values.length == 0;
    }

}
