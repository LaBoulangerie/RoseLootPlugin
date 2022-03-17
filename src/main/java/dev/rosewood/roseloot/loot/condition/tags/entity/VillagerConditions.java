package dev.rosewood.roseloot.loot.condition.tags.entity;

import dev.rosewood.rosegarden.utils.NMSUtil;
import dev.rosewood.roseloot.event.LootConditionRegistrationEvent;
import dev.rosewood.roseloot.loot.condition.EntityConditions;
import dev.rosewood.roseloot.loot.condition.LootCondition;
import dev.rosewood.roseloot.loot.context.LootContext;
import dev.rosewood.roseloot.loot.context.LootContextParams;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Villager;

public class VillagerConditions extends EntityConditions {

    public VillagerConditions(LootConditionRegistrationEvent event) {
        event.registerLootCondition("villager-profession", VillagerProfessionCondition.class);
        event.registerLootCondition("villager-type", VillagerTypeCondition.class);
        if (NMSUtil.getVersionNumber() >= 14)
            event.registerLootCondition("villager-level", VillagerLevelCondition.class);
    }

    public static class VillagerProfessionCondition extends LootCondition {

        private List<Villager.Profession> professions;

        public VillagerProfessionCondition(String tag) {
            super(tag);
        }

        @Override
        public boolean checkInternal(LootContext context) {
            return context.getAs(LootContextParams.LOOTED_ENTITY, Villager.class)
                    .map(Villager::getProfession)
                    .filter(this.professions::contains)
                    .isPresent();
        }

        @Override
        public boolean parseValues(String[] values) {
            this.professions = new ArrayList<>();

            for (String value : values) {
                try {
                    Villager.Profession profession = Villager.Profession.valueOf(value.toUpperCase());
                    this.professions.add(profession);
                } catch (Exception ignored) { }
            }

            return !this.professions.isEmpty();
        }

    }

    public static class VillagerTypeCondition extends LootCondition {

        private List<Villager.Type> types;

        public VillagerTypeCondition(String tag) {
            super(tag);
        }

        @Override
        public boolean checkInternal(LootContext context) {
            return context.getAs(LootContextParams.LOOTED_ENTITY, Villager.class)
                    .map(Villager::getVillagerType)
                    .filter(this.types::contains)
                    .isPresent();
        }

        @Override
        public boolean parseValues(String[] values) {
            this.types = new ArrayList<>();

            for (String value : values) {
                try {
                    Villager.Type type = Villager.Type.valueOf(value.toUpperCase());
                    this.types.add(type);
                } catch (Exception ignored) { }
            }

            return !this.types.isEmpty();
        }

    }

    public static class VillagerLevelCondition extends LootCondition {

        private int level;

        public VillagerLevelCondition(String tag) {
            super(tag);
        }

        @Override
        protected boolean checkInternal(LootContext context) {
            return context.getAs(LootContextParams.LOOTED_ENTITY, Villager.class)
                    .map(Villager::getVillagerLevel)
                    .filter(x -> x >= this.level)
                    .isPresent();
        }

        @Override
        public boolean parseValues(String[] values) {
            this.level = -1;

            try {
                this.level = Integer.parseInt(values[0]);
            } catch (Exception ignored) { }

            return this.level >= 0;
        }

    }

}
