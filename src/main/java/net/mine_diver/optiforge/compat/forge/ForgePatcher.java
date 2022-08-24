package net.mine_diver.optiforge.compat.forge;

import net.mine_diver.optiforge.compat.Patcher;
import net.mine_diver.optiforge.mod.OptiforgeSetup;
import net.mine_diver.optiforge.patcher.PatchClass;
import net.mine_diver.optiforge.util.Util;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipFile;

public class ForgePatcher implements Patcher {

    private interface ClassPatcher { void patch(ClassNode minecraft, ClassNode optifine, PatchClass patchClass); }
    private static final Map<String, ClassPatcher> CLASS_PATCHERS = new HashMap<>();

    private static String map(String obfuscated) {
        return Util.mapClassName(obfuscated);
    }

    private static String internal(String deobfuscated) {
        return deobfuscated.replace(".", "/");
    }

    private static String desc(String internal) {
        return "L" + internal + ";";
    }

    private static final String TESSELLATOR_OBFUSCATED = "nw";
    private static final String TESSELLATOR = map(TESSELLATOR_OBFUSCATED);
    private static final String TESSELLATOR_INTERNAL = internal(TESSELLATOR);
//    private static final String TESSELLATOR_DESCRIPTOR = desc(TESSELLATOR_INTERNAL);
//    private static final String TESSELLATOR_NATIVE_BUFFER_SIZE = TESSELLATOR_DESCRIPTOR + "nativeBufferSize:" + Type.getDescriptor(int.class);

    private static final String WORLDRENDERER_OBFUSCATED = "dk";
    private static final String WORLDRENDERER = map(WORLDRENDERER_OBFUSCATED);
    private static final String WORLDRENDERER_INTERNAL = internal(WORLDRENDERER);
    private static final String WORLDRENDERER_DESRIPTOR = desc(WORLDRENDERER_INTERNAL);
//    private static final String WORLDRENDERER_TESSELLATOR = WORLDRENDERER_DESRIPTOR + Util.mapFieldName(WORLDRENDERER_OBFUSCATED, "D", desc(TESSELLATOR_OBFUSCATED)) + ":" + TESSELLATOR_DESCRIPTOR;

    private static final boolean FORGE_INSTALLED;

    static {
        try (ZipFile mcFile = new ZipFile(OptiforgeSetup.getLaunchMinecraftJar().toFile())) {
            FORGE_INSTALLED = mcFile.getEntry("forge/MinecraftForge.class") != null;
            if (FORGE_INSTALLED) {
                OptiforgeSetup.LOGGER.info("Detected Forge. Enabling compatibility patches");
                Mixins.addConfiguration("optiforge.compat.forge.mixins.json");
                CLASS_PATCHERS.put(TESSELLATOR_INTERNAL, ForgePatcher::patchTessellator);
                CLASS_PATCHERS.put(WORLDRENDERER_INTERNAL, ForgePatcher::patchWorldRenderer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preApply(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {
        CLASS_PATCHERS.getOrDefault(patchClass.name, (minecraft1, optifine1, patchClass1) -> {}).patch(minecraft, optifine, patchClass);
    }

    private static void patchTessellator(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {
        // clearing all method overwrite patches
        // handled by a mixin instead
        new HashSet<>(patchClass.methods).stream().filter(patchMethod -> patchMethod.overwrite).forEach(patchClass.methods::remove);
//        patchClass.methods.stream().filter(patchMethod -> "<clinit>".equals(patchMethod.node.name)).findFirst().ifPresent(patchMethod -> {
//            // replacing static initialization with Forge's one
//            // this is done because it's easier to patch optifine in forge's <clinit> that the other way around
//            patchMethod.node.instructions = minecraft.methods.stream().filter(methodNode -> "<clinit>".equals(methodNode.name)).findFirst().orElseThrow(NullPointerException::new).instructions;
//            // changing buffer size to match optifine's
//            // might lead to visual glitches, so probably will be removed in future versions
//            StreamSupport.stream(patchMethod.node.instructions.spliterator(), false)
//                    .filter(abstractInsnNode -> abstractInsnNode.getType() == AbstractInsnNode.FIELD_INSN)
//                    .map(abstractInsnNode -> (FieldInsnNode) abstractInsnNode)
//                    .filter(fieldInsnNode -> fieldInsnNode.getOpcode() == Opcodes.PUTSTATIC && TESSELLATOR_NATIVE_BUFFER_SIZE.equals(desc(fieldInsnNode.owner) + fieldInsnNode.name + ":" + fieldInsnNode.desc))
//                    .findFirst()
//                    .ifPresent(fieldInsnNode -> {
//                        AbstractInsnNode abstractInsnNode = patchMethod.node.instructions.get(patchMethod.node.instructions.indexOf(fieldInsnNode) - 1);
//                        if (abstractInsnNode.getType() == AbstractInsnNode.LDC_INSN && ((LdcInsnNode) abstractInsnNode).cst.equals(2097152))
//                            patchMethod.node.instructions.set(abstractInsnNode, new FieldInsnNode(Opcodes.GETSTATIC, TESSELLATOR_INTERNAL, "BUFFER_SIZE", Type.getDescriptor(int.class)));
//                        else throw new RuntimeException("Unexpected instruction \"" + abstractInsnNode + "\" encountered while changing tessellator buffer size for compatibility with Forge");
//                    });
//        });
//        // fixing access to static fields
//        Set<String> staticFields = new HashSet<>();
//        staticFields.add(Util.mapFieldName(TESSELLATOR_OBFUSCATED, "A", Type.getDescriptor(int.class)));
//        staticFields.add(Util.mapFieldName(TESSELLATOR_OBFUSCATED, "d", Type.getDescriptor(ByteBuffer.class)));
//        staticFields.add(Util.mapFieldName(TESSELLATOR_OBFUSCATED, "e", Type.getDescriptor(IntBuffer.class)));
//        staticFields.add(Util.mapFieldName(TESSELLATOR_OBFUSCATED, "f", Type.getDescriptor(FloatBuffer.class)));
//        staticFields.add(Util.mapFieldName(TESSELLATOR_OBFUSCATED, "x", Type.getDescriptor(boolean.class)));
//        staticFields.add(Util.mapFieldName(TESSELLATOR_OBFUSCATED, "y", Type.getDescriptor(IntBuffer.class)));
//        patchClass.methods.forEach(method -> StreamSupport.stream(method.node.instructions.spliterator(), false)
//                .filter(abstractInsnNode -> abstractInsnNode.getType() == AbstractInsnNode.FIELD_INSN)
//                .map(abstractInsnNode -> (FieldInsnNode) abstractInsnNode)
//                .filter(fieldInsnNode -> (fieldInsnNode.getOpcode() == Opcodes.PUTFIELD || fieldInsnNode.getOpcode() == Opcodes.GETFIELD) && TESSELLATOR_INTERNAL.equals(fieldInsnNode.owner) && staticFields.contains(fieldInsnNode.name))
//                .forEach(fieldInsnNode -> {
//                    fieldInsnNode.setOpcode(fieldInsnNode.getOpcode() == Opcodes.GETFIELD ? Opcodes.GETSTATIC : Opcodes.PUTSTATIC);
//                    // removing aload0 before non-static opcode
//                    method.node.instructions.remove(method.node.instructions.get(method.node.instructions.indexOf(fieldInsnNode) - 1));
//                })
//        );
    }

    private static void patchWorldRenderer(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {
//        // redirecting all worldrenderer tessellator references to tessellator's main instance
//        // this is done because forge sometimes changes tessellator's main instance, so we need to get the current one
//        patchClass.methods.forEach(patchMethod -> StreamSupport.stream(patchMethod.node.instructions.spliterator(), false)
//                .filter(abstractInsnNode -> abstractInsnNode.getType() == AbstractInsnNode.FIELD_INSN)
//                .map(abstractInsnNode -> (FieldInsnNode) abstractInsnNode)
//                .filter(fieldInsnNode -> WORLDRENDERER_TESSELLATOR.equals(desc(fieldInsnNode.owner) + fieldInsnNode.name + ":" + fieldInsnNode.desc))
//                .forEach(fieldInsnNode -> {
//                    switch (fieldInsnNode.getOpcode()) {
//                        case Opcodes.GETSTATIC:
//                            fieldInsnNode.owner = TESSELLATOR_INTERNAL;
//                            fieldInsnNode.name = Util.mapFieldName(TESSELLATOR_OBFUSCATED, "a", desc(TESSELLATOR_OBFUSCATED));
//                            break;
//                        case Opcodes.PUTSTATIC:
//                            patchMethod.node.instructions.remove(patchMethod.node.instructions.get(patchMethod.node.instructions.indexOf(fieldInsnNode) - 1));
//                            patchMethod.node.instructions.remove(fieldInsnNode);
//                            break;
//                    }
//                })
//        );
    }
}
