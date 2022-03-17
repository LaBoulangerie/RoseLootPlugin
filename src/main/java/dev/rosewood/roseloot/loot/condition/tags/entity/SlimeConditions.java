package dev.rosewood.roseloot.loot.condition.tags.entity;

import dev.rosewood.roseloot.event.LootConditionRegistrationEvent;
import dev.rosewood.roseloot.loot.condition.EntityConditions;
import dev.rosewood.roseloot.loot.condition.LootCondition;
import dev.rosewood.roseloot.loot.context.LootContext;
import dev.rosewood.roseloot.loot.context.LootContextParams;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Slime;

public class SlimeConditions extends EntityConditions {

    public SlimeConditions(LootConditionRegistrationEvent event) {
        event.registerLootCondition("slime-size", SlimeSizeCondition.class);
    }

    public static class SlimeSizeCondition extends LootCondition {

        private List<Integer> sizes;

        public SlimeSizeCondition(String tag) {
            super(tag);
        }

        @Override
        protected boolean checkInternal(LootContext context) {
            return context.getAs(LootContextParams.LOOTED_ENTITY, Slime.class)
                    .map(Slime::getSize)
                    .filter(this.sizes::contains)
                    .isPresent();
        }

        @Override
        public boolean parseValues(String[] values) {
            this.sizes = new ArrayList<>();

            for (String value : values) {
                try {
                    this.sizes.add(Integer.parseInt(value));
                } catch (Exception ignored) { }
            }

            return !this.sizes.isEmpty();
        }

    }

}
