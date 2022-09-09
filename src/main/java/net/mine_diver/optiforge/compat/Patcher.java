package net.mine_diver.optiforge.compat;

import net.mine_diver.optiforge.patcher.PatchClass;
import org.objectweb.asm.tree.ClassNode;

public interface Patcher extends PatchPostProcessor, PatchPreApplier, PatchPostApplier {

    @Override
    default void postProcess(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {}

    @Override
    default void preApply(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {}

    @Override
    default void postApply(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {}
}
