package net.mine_diver.optiforge.mod;

import com.chocohead.mm.api.ClassTinkerers;
import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.optiforge.compat.Patcher;
import net.mine_diver.optiforge.patcher.MethodComparison;
import net.mine_diver.optiforge.patcher.PatchClass;
import net.mine_diver.optiforge.patcher.PatchField;
import net.mine_diver.optiforge.patcher.PatchMethod;
import net.mine_diver.optiforge.util.MixinHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OptifineInjector {

    private static final boolean DEBUG_EXPORT = Boolean.parseBoolean(System.getProperty("optiforge.debug.export", "false"));
    private static final File EXPORT_DIR = new File(OptiforgeSetup.WORK_DIR, "export");
    static {
        try {
            FileUtils.deleteDirectory(EXPORT_DIR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generatePatches(File mcFile, File ofFile) {
        List<Patcher> patchers = FabricLoader.getInstance().getEntrypoints("optiforge:patcher", Patcher.class);
        try (ZipFile mc = new ZipFile(mcFile); ZipFile of = new ZipFile(ofFile)) {
            Enumeration<? extends ZipEntry> ofEntries = of.entries();
            while (ofEntries.hasMoreElements()) {
                ZipEntry ofEntry = ofEntries.nextElement();
                ZipEntry mcEntry = mc.getEntry(ofEntry.getName());
                if (!ofEntry.isDirectory() && mcEntry != null) {
                    byte[] mcBytes;
                    byte[] ofBytes;
                    try (InputStream mcStream = mc.getInputStream(mcEntry); InputStream ofStream = of.getInputStream(ofEntry)) {
                        mcBytes = IOUtils.toByteArray(mcStream);
                        ofBytes = IOUtils.toByteArray(ofStream);
                    }
                    debugExport("minecraft/" + mcEntry.getName(), mcBytes);
                    debugExport("optifine/" + ofEntry.getName(), ofBytes);
                    ClassReader mcReader = new ClassReader(mcBytes);
                    ClassNode mcNode = new ClassNode();
                    mcReader.accept(mcNode, ClassReader.EXPAND_FRAMES);
                    ClassReader ofReader = new ClassReader(ofBytes);
                    ClassNode ofNode = new ClassNode();
                    ofReader.accept(ofNode, ClassReader.EXPAND_FRAMES);
                    PatchClass patch = new PatchClass(ofNode.name);
                    ofNode.fields.forEach(ofFieldNode -> {
                        Optional<FieldNode> mcFieldNode = mcNode.fields.stream().filter(fieldNode -> ofFieldNode.name.equals(fieldNode.name)).findFirst();
                        boolean fieldPresent = mcFieldNode.isPresent();
                        if (fieldPresent) {
                            FieldNode mcField = mcFieldNode.get();
                            if (mcField.desc.equals(ofFieldNode.desc) && mcField.access == ofFieldNode.access) return;
                        }
                        patch.fields.add(new PatchField(ofFieldNode, fieldPresent));
                    });
                    ofNode.methods.forEach(ofMethodNode -> {
                        Optional<MethodNode> mcMethodNode = mcNode.methods.stream().filter(methodNode -> (ofMethodNode.name + ofMethodNode.desc).equals(methodNode.name + methodNode.desc)).findFirst();
                        boolean methodPresent = mcMethodNode.isPresent();
                        if (methodPresent && new MethodComparison(mcMethodNode.get(), ofMethodNode).effectivelyEqual) return;
                        patch.methods.add(new PatchMethod(ofMethodNode, methodPresent));
                    });
                    patchers.forEach(patcher -> patcher.postProcess(mcNode, ofNode, patch));
                    ClassTinkerers.addTransformation(patch.name, classNode -> {
                        patchers.forEach(patcher -> patcher.preApply(classNode, ofNode, patch));
                        patch.fields.forEach(patchField -> {
                            if (patchField.overwrite) {
                                for (int i = 0; i < classNode.fields.size(); i++)
                                    if (patchField.node.name.equals(classNode.fields.get(i).name)) {
                                        classNode.fields.set(i, patchField.node);
                                        break;
                                    }
                            } else if (classNode.fields.stream().noneMatch(fieldNode -> patchField.node.name.equals(fieldNode.name)))
                                classNode.fields.add(patchField.node);
                        });
                        patch.methods.forEach(patchMethod -> {
                            if (patchMethod.overwrite) {
                                for (int i = 0; i < classNode.methods.size(); i++) {
                                    MethodNode mcMethodNode = classNode.methods.get(i);
                                    if ((patchMethod.node.name + patchMethod.node.desc).equals(mcMethodNode.name + mcMethodNode.desc)) {
                                        classNode.methods.set(i, patchMethod.node);
                                        break;
                                    }
                                }
                            } else {
                                classNode.methods.add(patchMethod.node);
                                MixinHelper.addMethodInfo(classNode, patchMethod.node);
                            }
                        });
                        ClassWriter writer = new ClassWriter(0);
                        classNode.accept(writer);
                        debugExport("patched/" + patch.name + ".class", writer.toByteArray());
                    });
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void debugExport(String path, byte[] bytes) {
        if (DEBUG_EXPORT) {
            File export = new File(OptiforgeSetup.WORK_DIR, "export/" + path);
            try {
                if (!export.getParentFile().exists() && !export.getParentFile().mkdirs() || !export.exists() && !export.createNewFile())
                    throw new RuntimeException("Couldn't create " + export + "!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (OutputStream outStream = Files.newOutputStream(export.toPath())) {
                outStream.write(bytes.clone());
                outStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
