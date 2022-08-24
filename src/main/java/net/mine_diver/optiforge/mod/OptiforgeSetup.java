package net.mine_diver.optiforge.mod;

import com.chocohead.mm.api.ClassTinkerers;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class OptiforgeSetup implements Runnable {

    public static final Logger LOGGER = LogManager.getLogger("OptiForge");
    static {
        Configurator.setLevel("OptiForge", Level.ALL);
    }
    public static final File WORK_DIR = new File(FabricLoader.getInstance().getGameDirectory(), ".optiforge");
    static {
        if (!WORK_DIR.exists() && ! WORK_DIR.mkdirs()) throw new RuntimeException("Couldn't create " + WORK_DIR + "!");
    }

    @Override
    public void run() {
        try {
            String namespace = FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
            Path mcJar = getMinecraftJar();

            File vanillaFile = new File(WORK_DIR, "minecraft-b1.7.3-client.jar");
            boolean mcChanged = Util.downloadIfChanged(new URL("https://launcher.mojang.com/v1/objects/43db9b498cb67058d2e12d394e6507722e71bb45/client.jar"), vanillaFile, LOGGER);
            File vanillaJar = vanillaFile;
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                vanillaJar = new File(WORK_DIR, "minecraft-remapped.jar");
                if (mcChanged) {
                    LOGGER.info("Remapping Minecraft from official to " + namespace);
                    remap(vanillaFile, getLibs(mcJar), vanillaJar, createMappings("official", namespace));
                }
            }

            File ofFile = OptifineVersion.getOptiFineFile();
            File ofJar = ofFile;
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                ofJar = new File(WORK_DIR, "optifine-remapped.jar");
                if (OptifineVersion.hasChanged(ofFile)) {
                    LOGGER.info("Remapping OptiFine from official to " + namespace);
                    remap(ofFile, getLibs(mcJar), ofJar, createMappings("official", namespace));
                }
            }

            OptifineInjector.generatePatches(vanillaJar, ofJar);
            ClassTinkerers.addURL(ofJar.toURI().toURL());
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

    public static Path getMinecraftJar() {
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

    public static Path getLaunchMinecraftJar() {
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
}
