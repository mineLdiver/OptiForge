package net.mine_diver.optiforge.mod;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipFile;

public class OptifineVersion {

    public static File getOptiFineFile() {
        for (File file : Objects.requireNonNull(new File(FabricLoader.getInstance().getGameDirectory(), "mods").listFiles())) {
            if (!file.isFile()) continue;
            String name = file.getName();
            if (!name.endsWith(".zip") && !name.endsWith(".jar")) continue;
            try (ZipFile zipFile = new ZipFile(file)) {
                if (zipFile.getEntry("Config.class") != null) {
                    OptiforgeSetup.LOGGER.info("Found OptiFine in: " + name);
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
