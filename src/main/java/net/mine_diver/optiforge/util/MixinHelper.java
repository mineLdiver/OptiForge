package net.mine_diver.optiforge.util;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MixinHelper {

    private static final Method CLASSINFO_ADDMETHOD;
    static {
        try {
            CLASSINFO_ADDMETHOD = ClassInfo.class.getDeclaredMethod("addMethod", MethodNode.class, boolean.class);
            CLASSINFO_ADDMETHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addMethodInfo(ClassNode classNode, MethodNode methodNode) {
        try {
            CLASSINFO_ADDMETHOD.invoke(ClassInfo.forName(classNode.name), methodNode, false);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
