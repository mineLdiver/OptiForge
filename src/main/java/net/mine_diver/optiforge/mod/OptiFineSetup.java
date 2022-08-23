package net.mine_diver.optiforge.mod;

import com.chocohead.mm.api.ClassTinkerers;
import com.nothome.delta.Delta;
import com.nothome.delta.GDiffPatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.TinyTree;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.IMappingProvider.Member;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.OutputConsumerPath.Builder;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.mine_diver.optiforge.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OptiFineSetup implements Runnable {

    private static final boolean OUTPUT = Boolean.parseBoolean(System.getProperty("optiforge.debug.export", "false"));
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void run() {
        try {
            File ofFile = new File(FabricLoader.getInstance().getGameDirectory(), "mods/OptiFine_1_7_3_HD_G.zip");
            String namespace = FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
            System.out.println("Remapping OptiFine from official to " + namespace);
            File workDir = new File(FabricLoader.getInstance().getGameDirectory(), ".optiforge");
            workDir.mkdir();
            File vanillaJar = new File(workDir, "minecraft-b1.7.3-client.jar");
            Util.downloadIfChanged(new URL("https://launcher.mojang.com/v1/objects/43db9b498cb67058d2e12d394e6507722e71bb45/client.jar"), vanillaJar, LOGGER);
            File vanillaJarRemapped = new File(workDir, "minecraft-b1.7.3-client-remapped.jar");
            IMappingProvider mappings = createMappings("official", namespace);
            Path[] libs = getLibs(getMinecraftJar());
            remap(vanillaJar, libs, vanillaJarRemapped, mappings);
            File completeJar = new File(workDir, "OptiFine-remapped.jar");
            remap(ofFile, libs, completeJar, mappings);

            Delta delta = new Delta();
            try (ZipFile of = new ZipFile(completeJar); ZipFile mc = new ZipFile(vanillaJarRemapped)) {
                Enumeration<? extends ZipEntry> ofEntries = of.entries();
                while (ofEntries.hasMoreElements()) {
                    ZipEntry ofEntry = ofEntries.nextElement();
                    ZipEntry mcEntry = mc.getEntry(ofEntry.getName());
                    if (!ofEntry.isDirectory() && mcEntry != null) {
                        byte[] vanillaBytes = Util.readAll(mc.getInputStream(mcEntry));
                        byte[] rawOFBytes = Util.readAll(of.getInputStream(ofEntry));
                        ClassReader ofReader = new ClassReader(rawOFBytes);
                        ClassNode ofNode = new ClassNode();
                        ofReader.accept(ofNode, ClassReader.EXPAND_FRAMES);
                        ofNode.methods.stream().filter(methodNode -> methodNode.localVariables != null).forEach(methodNode -> methodNode.localVariables.clear());
                        ClassWriter ofWriter = new ClassWriter(0);
                        ofNode.accept(ofWriter);
                        byte[] ofBytes = ofWriter.toByteArray();
                        ClassTinkerers.addTransformation(mcEntry.getName().replace(".class", ""), classNode -> {
                            ClassWriter writer = new ClassWriter(0);
                            classNode.accept(writer);
                            byte[] mcBytes = writer.toByteArray();
                            byte[] patchBytes;
                            byte[] patchedBytes;
                            try {
                                patchBytes = delta.compute(vanillaBytes.clone(), ofBytes);
                                debugExport(workDir, mcEntry, mcBytes, patchBytes, new byte[0]);
                                patchedBytes = new GDiffPatcher().patch(mcBytes.clone(), patchBytes);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            ClassReader reader = new ClassReader(patchedBytes);
                            ClassNode patchedNode = new ClassNode();
                            reader.accept(patchedNode, ClassReader.EXPAND_FRAMES);
                            classNode.methods = patchedNode.methods;
                            classNode.fields = patchedNode.fields;
                        });
                    }
                }
            }
            ClassTinkerers.addURL(completeJar.toURI().toURL());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void remap(File input, Path[] libraries, File output, IMappingProvider mappings) throws IOException {
        remap(input.toPath(), libraries, output.toPath(), mappings);
    }

    private static void remap(Path input, Path[] libraries, Path output, IMappingProvider mappings) throws IOException {
        Files.deleteIfExists(output);

        TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(mappings).renameInvalidLocals(FabricLoader.getInstance().isDevelopmentEnvironment()).rebuildSourceFilenames(true).fixPackageAccess(true).build();

        try (OutputConsumerPath outputConsumer = new Builder(output).assumeArchive(true).build()) {
            outputConsumer.addNonClassFiles(input);
            remapper.readInputs(input);
            remapper.readClassPath(libraries);

            remapper.apply(outputConsumer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remap jar", e);
        } finally {
            remapper.finish();
        }
    }

    private static IMappingProvider createMappings(String from, String to) {
        TinyTree normalMappings = FabricLauncherBase.getLauncher().getMappingConfiguration().getMappings();

        //In prod
        return (out) -> {
            for (ClassDef classDef : normalMappings.getClasses()) {
                String className = classDef.getName(from);
                out.acceptClass(className, classDef.getName(to));

                for (FieldDef field : classDef.getFields()) {
                    out.acceptField(new Member(className, field.getName(from), field.getDescriptor(from)), field.getName(to));
                }

                for (MethodDef method : classDef.getMethods()) {
                    out.acceptMethod(new Member(className, method.getName(from), method.getDescriptor(from)), method.getName(to));
                }
            }
        };
    }

    private static Path[] getLibs(Path minecraftJar) {
        Path[] libs = FabricLauncherBase.getLauncher().getLoadTimeDependencies().stream().map(url -> {
            try {
                return Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException("Failed to convert " + url + " to path", e);
            }
        }).filter(Files::exists).toArray(Path[]::new);

        out: if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            Path launchJar = getLaunchMinecraftJar();

            for (int i = 0, end = libs.length; i < end; i++) {
                Path lib = libs[i];

                if (launchJar.equals(lib)) {
                    libs[i] = minecraftJar;
                    break out;
                }
            }

            //Can't find the launch jar apparently, remapping will go wrong if it is left in
            throw new IllegalStateException("Unable to find Minecraft jar (at " + launchJar + ") in classpath: " + Arrays.toString(libs));
        }

        return libs;
    }

    private static Path getMinecraftJar() {
        String givenJar = System.getProperty("optiforge.mc-jar");
        if (givenJar != null) {
            File givenJarFile = new File(givenJar);

            if (givenJarFile.exists()) {
                return givenJarFile.toPath();
            } else {
                System.err.println("Supplied Minecraft jar at " + givenJar + " doesn't exist, falling back");
            }
        }

        Path minecraftJar = getLaunchMinecraftJar();

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            Path officialNames = minecraftJar.resolveSibling(String.format("minecraft-%s-client.jar", "b1.7.3"));

            if (Files.notExists(officialNames)) {
                Path parent = minecraftJar.getParent().resolveSibling(String.format("minecraft-%s-client.jar", "b1.7.3"));

                if (Files.notExists(parent)) {
                    Path alternativeParent = parent.resolveSibling("minecraft-client.jar");

                    if (Files.notExists(alternativeParent)) {
                        throw new AssertionError("Unable to find Minecraft dev jar! Tried " + officialNames + ", " + parent + " and " + alternativeParent
                                + "\nPlease supply it explicitly with -Doptiforge.mc-jar");
                    }

                    parent = alternativeParent;
                }

                officialNames = parent;
            }

            minecraftJar = officialNames;
        }

        return minecraftJar;
    }

    private static Path getLaunchMinecraftJar() {
        ModContainer mod = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow(() -> new IllegalStateException("No Minecraft?"));
        URI uri = mod.getRootPath().toUri();
        assert "jar".equals(uri.getScheme());

        String path = uri.getSchemeSpecificPart();
        int split = path.lastIndexOf("!/");

        if (path.substring(0, split).indexOf(' ') > 0 && path.startsWith("file:///")) {//This is meant to be a URI...
            Path out = Paths.get(path.substring(8, split));
            if (Files.exists(out)) return out;
        }

        try {
            return Paths.get(new URI(path.substring(0, split)));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to find Minecraft jar from " + uri + " (calculated " + path.substring(0, split) + ')', e);
        }
    }

    private static void debugExport(File workDir, ZipEntry mcEntry, byte[] mcBytes, byte[] patchBytes, byte[] patchedBytes) {
        if (OUTPUT) {
            System.out.println("OUTPUT!");
            File outVanilla = new File(workDir, "vanilla/" + mcEntry.getName());
            try {
                if (!outVanilla.getParentFile().exists() && !outVanilla.getParentFile().mkdirs() || !outVanilla.exists() && !outVanilla.createNewFile())
                    throw new RuntimeException("Couldn't create " + outVanilla + "!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (OutputStream outStream = Files.newOutputStream(outVanilla.toPath())) {
                outStream.write(mcBytes.clone());
                outStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            File outPatch = new File(workDir, "patch/" + mcEntry.getName().replace(".class", ".xdelta"));
            try {
                if (!outPatch.getParentFile().exists() && !outPatch.getParentFile().mkdirs() || !outPatch.exists() && !outPatch.createNewFile())
                    throw new RuntimeException("Couldn't create " + outPatch + "!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (OutputStream outStream = Files.newOutputStream(outPatch.toPath())) {
                outStream.write(patchBytes.clone());
                outStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            File outPatched = new File(workDir, "patched/" + mcEntry.getName());
            try {
                if (!outPatched.getParentFile().exists() && !outPatched.getParentFile().mkdirs() || !outPatched.exists() && !outPatched.createNewFile())
                    throw new RuntimeException("Couldn't create " + outPatched + "!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (OutputStream outStream = Files.newOutputStream(outPatched.toPath())) {
                outStream.write(patchedBytes.clone());
                outStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
