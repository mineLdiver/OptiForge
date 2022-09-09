package net.mine_diver.optiforge.compat.forge;

import net.mine_diver.optiforge.compat.PatchPostProcessor;
import net.mine_diver.optiforge.compat.Patcher;
import net.mine_diver.optiforge.compat.newfrontiercraft.NewFrontierCraftPatcher;
import net.mine_diver.optiforge.mod.OptifineVersion;
import net.mine_diver.optiforge.mod.OptiforgeSetup;
import net.mine_diver.optiforge.patcher.PatchClass;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipFile;

import static net.mine_diver.optiforge.util.ASMClass.TESSELLATOR;

public class ForgePatcher implements Patcher {

    private static final Map<String, PatchPostProcessor> POST_PROCESSORS = new HashMap<>();

    public static final boolean FORGE_INSTALLED;

    static {
        if (NewFrontierCraftPatcher.NEWFRONTIERCRAFT_INSTALLED) {
            OptiforgeSetup.LOGGER.info("Disabling original Forge compatibility patches since NewFrontierCraft is present");
            FORGE_INSTALLED = false;
        } else
            try (ZipFile mcFile = new ZipFile(OptiforgeSetup.getLaunchMinecraftJar().toFile())) {
                FORGE_INSTALLED = mcFile.getEntry("forge/MinecraftForge.class") != null;
                if (FORGE_INSTALLED) {
                    OptiforgeSetup.LOGGER.info("Detected Forge. Enabling compatibility patches");
                    switch (OptifineVersion.EDITION) {
                        case STANDARD:
                            Mixins.addConfiguration("optiforge.compat.forge.standard.mixins.json");
                            POST_PROCESSORS.put(TESSELLATOR.typeMapped.getInternalName(), ForgePatcher::postProcessTessellator);
                            break;
                        case SMOOTH:
                            Mixins.addConfiguration("optiforge.compat.forge.smooth.mixins.json");
                            POST_PROCESSORS.put(TESSELLATOR.typeMapped.getInternalName(), ForgePatcher::postProcessTessellator);
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
    public void postProcess(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {
        POST_PROCESSORS.getOrDefault(patchClass.name, (minecraft1, optifine1, patchClass1) -> {}).postProcess(vanilla, optifine, patchClass);
    }

    private static void postProcessTessellator(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {
        // clearing all method overwrite patches
        // TODO: somehow merge forge and optifine tessellators into one
        new HashSet<>(patchClass.methods).stream().filter(patchMethod -> patchMethod.overwrite).forEach(patchClass.methods::remove);
    }
}
