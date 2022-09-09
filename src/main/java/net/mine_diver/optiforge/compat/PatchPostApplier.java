package net.mine_diver.optiforge.compat;

import net.mine_diver.optiforge.patcher.PatchClass;
import org.objectweb.asm.tree.ClassNode;

public interface PatchPostApplier {
    void postApply(ClassNode minecraft, ClassNode optifine, PatchClass patchClass);
}
