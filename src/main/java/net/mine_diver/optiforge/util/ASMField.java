package net.mine_diver.optiforge.util;

import org.objectweb.asm.Type;

public class ASMField {

    public static final ASMField
            BLOCK$BLOCK_ID = new ASMField(ASMClass.BLOCK, "bn", int.class);

    public final ASMClass owner;
    public final String obf, mapped;
    public final Type typeObf, typeMapped;
    public final String ref;

    public ASMField(ASMClass owner, String obf, Object type) {
        this.owner = owner;
        this.obf = obf;
        typeObf = ASMHelper.obf(type);
        typeMapped = ASMHelper.mapped(type);
        mapped = MappingHelper.mapField(owner.obf, obf, typeObf.getDescriptor());
        ref = owner.typeMapped.getDescriptor() + mapped + ":" + typeMapped.getDescriptor();
    }
}
