package net.mine_diver.optiforge.compat.newfrontiercraft;

import com.chocohead.mm.EnumExtenderAccessor;
import com.chocohead.mm.api.ClassTinkerers;
import com.google.common.collect.Iterators;
import com.google.common.collect.ObjectArrays;
import net.mine_diver.optiforge.compat.PatchPostProcessor;
import net.mine_diver.optiforge.compat.PatchPreApplier;
import net.mine_diver.optiforge.compat.Patcher;
import net.mine_diver.optiforge.mod.OptifineVersion;
import net.mine_diver.optiforge.mod.OptiforgeSetup;
import net.mine_diver.optiforge.patcher.PatchClass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.Mixins;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import java.util.zip.ZipFile;

import static net.mine_diver.optiforge.util.ASMClass.*;
import static net.mine_diver.optiforge.util.ASMField.BLOCK$BLOCK_ID;
import static net.mine_diver.optiforge.util.ASMMethod.*;

public class NewFrontierCraftPatcher implements Patcher {

    private static final Map<String, PatchPostProcessor> POST_PROCESSORS = new HashMap<>();
    private static final Map<String, PatchPreApplier> PRE_APPLIERS = new HashMap<>();
    private static final Map<String, PatchPreApplier> POST_APPLIERS = new HashMap<>();

    public static final boolean NEWFRONTIERCRAFT_INSTALLED;

    static {
        try (ZipFile mcFile = new ZipFile(OptiforgeSetup.getLaunchMinecraftJar().toFile())) {
            NEWFRONTIERCRAFT_INSTALLED = mcFile.getEntry("NFC.class") != null;
            if (NEWFRONTIERCRAFT_INSTALLED) {
                OptiforgeSetup.LOGGER.info("Detected NewFrontierCraft. Enabling compatibility patches");
                switch (OptifineVersion.EDITION) {
                    case STANDARD:
                        Mixins.addConfiguration("optiforge.compat.newfrontiercraft.standard.mixins.json");
                        break;
                    case SMOOTH:
                        Mixins.addConfiguration("optiforge.compat.newfrontiercraft.smooth.mixins.json");
                        break;
                    default:
                        throw new IllegalStateException("There are no NewFrontierCraft compatibility patches for OptiFine \"" + OptifineVersion.EDITION.name() + "\" edition!");
                }
                POST_PROCESSORS.put(RENDER_BLOCKS.typeMapped.getInternalName(), NewFrontierCraftPatcher::postProcessRenderBlocks);
                POST_PROCESSORS.put(RENDER_GLOBAL.typeMapped.getInternalName(), NewFrontierCraftPatcher::postProcessRenderGlobal);
                POST_PROCESSORS.put(RENDER_ENGINE.typeMapped.getInternalName(), NewFrontierCraftPatcher::postProcessRenderEngine);
                POST_PROCESSORS.put(FONT_RENDERER.typeMapped.getInternalName(), NewFrontierCraftPatcher::postProcessFontRenderer);
                POST_PROCESSORS.put(TESSELLATOR.typeMapped.getInternalName(), NewFrontierCraftPatcher::postProcessTessellator);
                POST_PROCESSORS.put(ENUM_OPTIONS.typeMapped.getInternalName(), NewFrontierCraftPatcher::postProcessEnumOptions);
                PRE_APPLIERS.put(ENUM_OPTIONS.typeMapped.getInternalName(), NewFrontierCraftPatcher::preApplyEnumOptions);
                POST_APPLIERS.put(ENUM_OPTIONS.typeMapped.getInternalName(), NewFrontierCraftPatcher::postApplyEnumOptions);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postProcess(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {
        POST_PROCESSORS.getOrDefault(patchClass.name, (vanilla1, optifine1, patchClass1) -> {}).postProcess(vanilla, optifine, patchClass);
    }

    @Override
    public void preApply(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {
        PRE_APPLIERS.getOrDefault(patchClass.name, (minecraft1, optifine1, patchClass1) -> {}).preApply(minecraft, optifine, patchClass);
    }

    @Override
    public void postApply(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {
        POST_APPLIERS.getOrDefault(patchClass.name, (minecraft1, optifine1, patchClass1) -> {}).preApply(minecraft, optifine, patchClass);
    }

    private static void postProcessRenderBlocks(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {
        InsnList insnList = patchClass.methods.stream()
                .filter(patchMethod -> RENDER_BLOCKS$RENDER_BLOCK_FLUIDS.matches(optifine, patchMethod.node))
                .findFirst()
                .orElseThrow(NullPointerException::new).node.instructions;
        StreamSupport.stream(insnList.spliterator(), false)
                .filter(abstractInsnNode -> abstractInsnNode instanceof MethodInsnNode)
                .map(abstractInsnNode -> (MethodInsnNode) abstractInsnNode)
                .filter(BLOCK_FLUID$FUNC_293_A::matches)
                .forEach(methodInsnNode -> {
                    methodInsnNode.name = "func_293_a";
                    Type type = Type.getMethodType(methodInsnNode.desc);
                    methodInsnNode.desc = Type.getMethodDescriptor(type.getReturnType(), ObjectArrays.concat(type.getArgumentTypes(), Type.getType(int.class)));
                    insnList.insertBefore(methodInsnNode, new VarInsnNode(Opcodes.ALOAD, 1));
                    insnList.insertBefore(methodInsnNode, new FieldInsnNode(Opcodes.GETFIELD, BLOCK$BLOCK_ID.owner.typeMapped.getInternalName(), BLOCK$BLOCK_ID.mapped, BLOCK$BLOCK_ID.typeMapped.getDescriptor()));
                });
    }

    private static void postProcessRenderGlobal(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {
        Iterators.removeIf(patchClass.methods.iterator(), input -> RENDER_GLOBAL$RENDER_CLOUDS_FANCY.matches(optifine, input.node));
    }

    private static void postProcessRenderEngine(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {
        // fixing readTextureImage method access to be public
        // required for nfc to work properly
        MethodNode readTextureImage = patchClass.methods.stream().filter(patchMethod -> RENDER_ENGINE$READ_TEXTURE_IMAGE.matches(optifine, patchMethod.node)).findFirst().orElseThrow(NullPointerException::new).node;
        readTextureImage.access &= ~Modifier.PRIVATE;
        readTextureImage.access |= Modifier.PUBLIC;
    }

    private static void postProcessFontRenderer(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {
        // letting nfc handle the fontrenderer
        patchClass.methods.clear();
        patchClass.fields.clear();
    }

    private static void postProcessTessellator(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {
        // clearing all method overwrite patches
        // TODO: somehow merge forge and optifine tessellators into one
        Iterators.removeIf(patchClass.methods.iterator(), input -> input.overwrite);
    }

    private static void postProcessEnumOptions(ClassNode vanilla, ClassNode optifine, PatchClass patchClass) {
        // clearing constructor patches
        // fixes duplicate enum constructor
        Iterators.removeIf(patchClass.methods.iterator(), input -> input.node.name.equals("<init>"));
    }

    private static void preApplyEnumOptions(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {
        // removing original enum fields added by nfc
        Set<String> nfcOptions = new HashSet<>();
        nfcOptions.add("FOV");
        nfcOptions.add("FANCY_ITEMS");
        nfcOptions.add("CLOUD_HEIGHT");
        nfcOptions.add("ADVENTURE_TREES");
        nfcOptions.add("VALID_OUTLINE");
        nfcOptions.add("INVALID_OUTLINE");
        nfcOptions.add("DISTORTION_EFFECTS");
        nfcOptions.add("FOV_EFFECTS");
        nfcOptions.add("ITEM_TOOLTIPS");
        nfcOptions.add("TOOLTIP_BACKGROUND");
        nfcOptions.add("CHAT_BACKGROUND");
        nfcOptions.add("OUTLINE_OPACITY");
        nfcOptions.add("OUTLINE_THICCNESS");
        Iterators.removeIf(minecraft.fields.iterator(), input -> nfcOptions.remove(input.name));
    }

    private static void postApplyEnumOptions(ClassNode minecraft, ClassNode optifine, PatchClass patchClass) {
        // adding nfc options in a compatible way
        EnumExtenderAccessor.makeEnumExtender(ClassTinkerers.enumBuilder(ENUM_OPTIONS.typeMapped.getInternalName(), String.class, int.class, String.class, boolean.class, boolean.class)
                .addEnum("FOV", "FOV", 13, "FOV", true, false)
                .addEnum("FANCY_ITEMS", "FANCY_ITEMS", 14, "options.fancyItems", false, true)
//                .addEnum("CLOUD_HEIGHT", "CLOUD_HEIGHT", 15, "options.cloudHeight", true, false)
                .addEnum("ADVENTURE_TREES", "ADVENTURE_TREES", 16, "options.adventureTrees", false, true)
                .addEnum("VALID_OUTLINE", "VALD_OUTLINE", 17, "accessibility.validOutline", false, false)
                .addEnum("INVALID_OUTLINE", "INVALID_OUTLINE", 18, "accessibility.invalidOutline", false, false)
                .addEnum("DISTORTION_EFFECTS", "DISTORTION_EFFECTS", 19, "accessibility.distortionEffects", true, false)
                .addEnum("FOV_EFFECTS", "FOV_EFFECTS", 20, "accessibility.fovEffects", true, false)
                .addEnum("ITEM_TOOLTIPS", "ITEM_TOOLTIPS", 21, "options.tooltips", false, true)
                .addEnum("TOOLTIP_BACKGROUND", "ITEM_TOOLTIPS", 22, "accessibility.tooltipBG", true, false)
                .addEnum("CHAT_BACKGROUND", "ITEM_TOOLTIPS", 23, "accessibility.chatBG", true, false)
                .addEnum("OUTLINE_OPACITY", "OUTLINE_OPACITY", 24, "accessibility.outlineOpacity", true, false)
                .addEnum("OUTLINE_THICCNESS", "OUTLINE_OPACITY", 25, "accessibility.outlineThiccness", true, false)
        ).accept(minecraft);
    }
}
