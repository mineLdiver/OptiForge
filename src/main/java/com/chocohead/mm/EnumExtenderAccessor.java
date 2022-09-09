package com.chocohead.mm;

import com.chocohead.mm.api.EnumAdder;
import org.objectweb.asm.tree.ClassNode;

import java.util.function.Consumer;

public class EnumExtenderAccessor {

    public static Consumer<ClassNode> makeEnumExtender(EnumAdder builder) {
        return EnumExtender.makeEnumExtender(builder);
    }
}
