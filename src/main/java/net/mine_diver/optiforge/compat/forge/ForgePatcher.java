package net.mine_diver.optiforge.compat.forge;

import net.mine_diver.optiforge.compat.Patcher;
import net.mine_diver.optiforge.mod.OptifineVersion;
import net.mine_diver.optiforge.mod.OptiforgeSetup;
import net.mine_diver.optiforge.patcher.PatchClass;
import net.mine_diver.optiforge.util.Util;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipFile;

public class ForgePatcher implements Patcher {

    private interface ClassPatcher { void patch(ClassNode minecraft, ClassNode optifine, PatchClass patchClass); }
    private static final Map<String, ClassPatcher> CLASS_PATCHERS = new HashMap<>();

    private static String map(String obfuscated) {
        return Util.mapClassName(obfuscated);
    }

    private static String internal(String deobfuscated) {
        return deobfuscated.replace(".", "/");
    }

    private static String desc(String internal) {
        return "L" + internal + ";";
    }

    private static final String TESSELLATOR_OBFUSCATED = "nw";
    private static final String TESSELLATOR = map(TESSELLATOR_OBFUSCATED);
    private static final String TESSELLATOR_INTERNAL = internal(TESSELLATOR);

    private static final boolean FORGE_INSTALLED;

    static {
        try (ZipFile mcFile = new ZipFile(OptiforgeSetup.getLaunchMinecraftJar().toFile())) {
            FORGE_INSTALLED = mcFile.getEntry("forge/MinecraftForge.class") != null;
            if (FORGE_INSTALLED) {
                OptiforgeSetup.LOGGER.info("Detected Forge. Enabling compatibility patches");
                switch (OptifineVersion.EDITION) {
                    case STANDARD:
                        Mixins.addConfiguration("optiforge.compat.forge.standard.mixins.json");
                        CLASS_PATCHERS.put(TESSELLATOR_INTERNAL, ForgePatcher::patchTessellator);
                        break;
                    case SMOOTH:
                        Mixins.addConfiguration("optiforge.compat.forge.smooth.mixins.json");
                        CLASS_PATCHERS.put(TESSELLATOR_INTERNAL, ForgePatcher::patchTessellator);
                        break;
                    default:
                        throw new IllegalStateException("There are no Forge compatibility patches for OptiFine \"" + OptifineVersion.EDITION.name() + "\" edition!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preApply(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {
        CLASS_PATCHERS.getOrDefault(patchClass.name, (minecraft1, optifine1, patchClass1) -> {}).patch(minecraft, optifine, patchClass);
    }

    private static void patchTessellator(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {
        // clearing all method overwrite patches
        // handled by a mixin instead
        new HashSet<>(patchClass.methods).stream().filter(patchMethod -> patchMethod.overwrite).forEach(patchClass.methods::remove);
    }
}
