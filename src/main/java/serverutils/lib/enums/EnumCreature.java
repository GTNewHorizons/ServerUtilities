package serverutils.lib.enums;

import net.minecraft.entity.EnumCreatureType;

import serverutils.lib.util.misc.NameMap;

public enum EnumCreature {

    AMBIENT(EnumCreatureType.ambient),
    ANIMAL(EnumCreatureType.creature),
    MOB(EnumCreatureType.monster),
    WATER_CREATURE(EnumCreatureType.waterCreature);

    public final EnumCreatureType creatureType;

    public static final EnumCreature[] VALUES = values();
    public static final NameMap<EnumCreature> NAME_MAP = NameMap.create(MOB, VALUES);

    EnumCreature(EnumCreatureType creatureType) {
        this.creatureType = creatureType;
    }

    public static EnumCreatureType getFromString(String s) {
        return NAME_MAP.get(s).creatureType;
    }
}
