package net.mine_diver.optiforge.compat;

import net.mine_diver.optiforge.patcher.PatchClass;
import org.objectweb.asm.tree.ClassNode;

public interface Patcher {

    default void postProcess(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {}

    default void preApply(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {}
}
