package net.mine_diver.optiforge.mod;

import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OptifineVersion {

    public static Edition EDITION;

    @RequiredArgsConstructor
    public enum Edition {
        STANDARD("HD"),
        SMOOTH("HD_S"),
        MULTITHREADED("HD_MT"),
        ANTIALIASING("HD_AA");

        public final String id;

        public static Edition fromVersion(String version) {
            String editionAndVersion = version.substring("OptiFine_1.7.3_".length());
            Set<Edition> matches = Arrays.stream(values()).filter(edition -> editionAndVersion.startsWith(edition.id)).collect(Collectors.toCollection(() -> EnumSet.noneOf(Edition.class)));
            if (matches.size() == 0) throw new IllegalStateException("Unknown OptiFine edition: " + version);
            if (matches.size() > 1) matches.remove(STANDARD);
            return Iterables.getOnlyElement(matches);
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static File getOptifineFile() {
        for (File file : Objects.requireNonNull(new File(FabricLoader.getInstance().getGameDirectory(), "mods").listFiles())) {
            if (!file.isFile()) continue;
            String name = file.getName();
            if (!name.endsWith(".zip") && !name.endsWith(".jar")) continue;
            try (ZipFile zipFile = new ZipFile(file)) {
                ZipEntry entry = zipFile.getEntry("Config.class");
                if (entry != null) {
                    OptiforgeSetup.LOGGER.info("Found OptiFine in: " + name);
                    InputStream configStream = zipFile.getInputStream(entry);
                    ClassNode configNode = new ClassNode();
                    ClassReader configReader = new ClassReader(IOUtils.toByteArray(configStream));
                    configReader.accept(configNode, 0);
                    EDITION = Edition.fromVersion((String) StreamSupport.stream(configNode.methods.stream().filter(methodNode -> "getVersion".equals(methodNode.name)).findFirst().orElseThrow(NullPointerException::new).instructions.spliterator(), false).filter(abstractInsnNode -> AbstractInsnNode.LDC_INSN == abstractInsnNode.getType()).map(abstractInsnNode -> (LdcInsnNode) abstractInsnNode).findFirst().orElseThrow(NullPointerException::new).cst);
                    OptiforgeSetup.LOGGER.info("Edition detected: " + EDITION.name());
                    return file;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Couldn't find OptiFine in mods folder!");
    }

    public static boolean hasChanged(File optifine) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try (InputStream stream = Files.newInputStream(optifine.toPath()); DigestInputStream digest = new DigestInputStream(stream, md)) {
            while (true) if (digest.read() == -1) break;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] md5 = md.digest();
        File md5File = new File(OptiforgeSetup.WORK_DIR, "optifine.md5");
        if (md5File.exists()) {
            try (InputStream md5Stream = Files.newInputStream(md5File.toPath())) {
                byte[] md5Existing = IOUtils.toByteArray(md5Stream);
                return !Arrays.equals(md5, md5Existing);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                if (!md5File.createNewFile()) throw new RuntimeException("Couldn't create " + md5File + "!");
                try (OutputStream stream = Files.newOutputStream(md5File.toPath())) {
                    stream.write(md5);
                    stream.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
    }
}
