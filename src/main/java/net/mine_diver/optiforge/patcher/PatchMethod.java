package net.mine_diver.optiforge.patcher;

import lombok.RequiredArgsConstructor;
import org.objectweb.asm.tree.MethodNode;

@RequiredArgsConstructor
public class PatchMethod {

    public final MethodNode node;
    public final boolean overwrite;
}
