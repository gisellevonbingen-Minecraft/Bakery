package satisfy.bakery.util;

import net.minecraft.world.level.block.state.properties.EnumProperty;

public class BakeryProperties {
    public static final EnumProperty<LineConnectingType> LINE_CONNECTING_TYPE;

    static {
        LINE_CONNECTING_TYPE = EnumProperty.create("type", LineConnectingType.class);
    }
}
