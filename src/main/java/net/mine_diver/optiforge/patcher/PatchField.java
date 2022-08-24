package net.mine_diver.optiforge.patcher;

import lombok.RequiredArgsConstructor;
import org.objectweb.asm.tree.FieldNode;

@RequiredArgsConstructor
public class PatchField {

    public final FieldNode node;
    public final boolean overwrite;
}
