package net.mine_diver.optiforge.compat;

import net.mine_diver.optiforge.patcher.PatchClass;
import org.objectweb.asm.tree.ClassNode;

public interface PatchPostProcessor {
    void postProcess(ClassNode vanilla, ClassNode optifine, PatchClass patchClass);
}
