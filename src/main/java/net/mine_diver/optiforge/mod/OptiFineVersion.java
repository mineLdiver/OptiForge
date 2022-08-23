package net.mine_diver.optiforge.mod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public class OptiFineVersion {

    public static File getOptiFineFile() {
        new File(FabricLoader.getInstance().getGameDirectory(), "mods");
        return null;
    }
}
