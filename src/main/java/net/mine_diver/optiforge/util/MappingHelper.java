package net.mine_diver.optiforge.util;

import net.fabricmc.loader.api.FabricLoader;

public class MappingHelper {

    public static String mapClass(String obfuscated) {
        return FabricLoader.getInstance().isDevelopmentEnvironment() ? FabricLoader.getInstance().getMappingResolver().mapClassName("official", obfuscated) : obfuscated;
    }

    public static String mapField(String className, String obfuscated, String descriptor) {
        return FabricLoader.getInstance().isDevelopmentEnvironment() ? FabricLoader.getInstance().getMappingResolver().mapFieldName("official", className, obfuscated, descriptor) : obfuscated;
    }

    public static String mapMethod(String className, String obfuscated, String descriptor) {
        return FabricLoader.getInstance().isDevelopmentEnvironment() ? FabricLoader.getInstance().getMappingResolver().mapMethodName("official", className, obfuscated, descriptor) : obfuscated;
    }
}
