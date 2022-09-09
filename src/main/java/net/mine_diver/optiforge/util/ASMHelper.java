package net.mine_diver.optiforge.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.objectweb.asm.Type;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ASMHelper {

    public static String internal(String mapped) {
        return mapped.replace(".", "/");
    }

    public static String desc(String internal) {
        return "L" + internal + ";";
    }

    public static Type obf(Object obj) {
        return obj instanceof ASMClass ?
                Type.getType(desc(internal(((ASMClass) obj).obf))) :
                obj instanceof Class ?
                        Type.getType((Class<?>) obj) :
                        null;
    }

    public static Type mapped(Object obj) {
        return obj instanceof ASMClass ?
                Type.getType(desc(internal(((ASMClass) obj).mapped))) :
                obj instanceof Class ?
                        Type.getType((Class<?>) obj) :
                        null;
    }
}
