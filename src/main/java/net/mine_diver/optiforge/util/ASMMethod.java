package net.mine_diver.optiforge.util;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;

public class ASMMethod {

    public static final ASMMethod
            RENDER_ENGINE$READ_TEXTURE_IMAGE = new ASMMethod(ASMClass.RENDER_ENGINE, "a", BufferedImage.class, InputStream.class),
            RENDER_GLOBAL$RENDER_CLOUDS_FANCY = new ASMMethod(ASMClass.RENDER_GLOBAL, "c", void.class, float.class),
            RENDER_BLOCKS$RENDER_BLOCK_FLUIDS = new ASMMethod(ASMClass.RENDER_BLOCKS, "k", boolean.class, ASMClass.BLOCK, int.class, int.class, int.class),
            BLOCK_FLUID$FUNC_293_A = new ASMMethod(ASMClass.BLOCK_FLUID, "a", double.class, ASMClass.I_BLOCK_ACCESS, int.class, int.class, int.class, ASMClass.MATERIAL);

    public final ASMClass owner;
    public final String obf, mapped;
    public final Type typeObf, typeMapped;
    public final String ref;

    public ASMMethod(ASMClass owner, String obf, Object ret, Object... params) {
        this.owner = owner;
        this.obf = obf;
        typeObf = Type.getMethodType(ASMHelper.obf(ret), Arrays.stream(params).map(ASMHelper::obf).toArray(Type[]::new));
        typeMapped = Type.getMethodType(ASMHelper.mapped(ret), Arrays.stream(params).map(ASMHelper::mapped).toArray(Type[]::new));
        mapped = MappingHelper.mapMethod(owner.obf, obf, typeObf.getDescriptor());
        ref = owner.typeMapped.getDescriptor() + mapped + typeMapped.getDescriptor();
    }

    public boolean matches(ClassNode owner, MethodNode node) {
        return ref.equals(ASMHelper.desc(owner.name) + node.name + node.desc);
    }

    public boolean matches(MethodInsnNode node) {
        return ref.equals(ASMHelper.desc(node.owner) + node.name + node.desc);
    }
}
