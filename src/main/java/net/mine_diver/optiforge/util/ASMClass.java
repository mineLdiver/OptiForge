package net.mine_diver.optiforge.util;

import org.objectweb.asm.Type;

public class ASMClass {

    public static final ASMClass
            RENDER_ENGINE = new ASMClass("ji"),
            FONT_RENDERER = new ASMClass("sj"),
            TESSELLATOR = new ASMClass("nw"),
            ENUM_OPTIONS = new ASMClass("ht"),
            RENDER_GLOBAL = new ASMClass("n"),
            RENDER_BLOCKS = new ASMClass("cv"),
            BLOCK = new ASMClass("uu"),
            BLOCK_FLUID = new ASMClass("rp"),
            I_BLOCK_ACCESS = new ASMClass("xp"),
            MATERIAL = new ASMClass("ln");

    public final String obf, mapped;
    public final Type typeObf, typeMapped;

    public ASMClass(String obf) {
        this.obf = obf;
        mapped = MappingHelper.mapClass(obf);
        typeObf = Type.getType(ASMHelper.desc(ASMHelper.internal(obf)));
        typeMapped = Type.getType(ASMHelper.desc(ASMHelper.internal(mapped)));
    }
}
