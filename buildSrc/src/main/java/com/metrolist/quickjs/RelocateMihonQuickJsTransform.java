package com.metrolist.quickjs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.gradle.api.artifacts.transform.CacheableTransform;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

/**
 * Relocates MihonKit's embedded QuickJS JNI library so it can coexist with the
 * distinct QuickJS runtime used by Innertubex. Only MihonKit's loader constant
 * and its matching native-library filename are changed.
 */
@CacheableTransform
public abstract class RelocateMihonQuickJsTransform implements TransformAction<TransformParameters.None> {
    private static final String LOADER_CLASS = "app/cash/quickjs/QuickJsNativeLoader.class";
    private static final String ORIGINAL_LIBRARY = "libquickjs.so";
    private static final String RELOCATED_LIBRARY = "libmihonqs.so";
    private static final String ORIGINAL_LIBRARY_NAME = "quickjs";
    private static final String RELOCATED_LIBRARY_NAME = "mihonqs";

    @InputArtifact
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        var input = getInputArtifact().get().getAsFile();
        var output = outputs.file(input.getName().replaceFirst("\\.aar$", "-mihon-relocated.aar"));
        boolean sawLoader = false;
        boolean sawNativeLibrary = false;
        try (ZipFile archive = new ZipFile(input); OutputStream file = Files.newOutputStream(output.toPath()); ZipOutputStream out = new ZipOutputStream(file)) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                String outputName = name;
                byte[] contents;
                try (InputStream stream = archive.getInputStream(entry)) {
                    contents = stream.readAllBytes();
                }
                if (name.equals("classes.jar")) {
                    JarRewriteResult result = rewriteLoaderClass(contents);
                    contents = result.bytes();
                    sawLoader = result.found();
                }
                if (name.startsWith("jni/") && name.endsWith("/" + ORIGINAL_LIBRARY)) {
                    outputName = name.substring(0, name.length() - ORIGINAL_LIBRARY.length()) + RELOCATED_LIBRARY;
                    sawNativeLibrary = true;
                }
                ZipEntry relocated = new ZipEntry(outputName);
                if (entry.getTime() >= 0) relocated.setTime(entry.getTime());
                out.putNextEntry(relocated);
                out.write(contents);
                out.closeEntry();
            }
            if (!sawLoader || !sawNativeLibrary) {
                throw new IOException("MihonKit QuickJS AAR did not contain the expected loader and JNI library");
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not relocate MihonKit QuickJS JNI library", e);
        }
    }

    private static JarRewriteResult rewriteLoaderClass(byte[] classesJar) throws IOException {
        ByteArrayOutputStream rewrittenJar = new ByteArrayOutputStream(classesJar.length);
        boolean found = false;
        try (ZipFile jar = new ZipFile(writeTemporaryJar(classesJar))) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            try (ZipOutputStream out = new ZipOutputStream(rewrittenJar)) {
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    byte[] bytes;
                    try (InputStream stream = jar.getInputStream(entry)) {
                        bytes = stream.readAllBytes();
                    }
                    if (entry.getName().equals(LOADER_CLASS)) {
                        bytes = rewriteClassConstant(bytes);
                        found = true;
                    }
                    ZipEntry copy = new ZipEntry(entry.getName());
                    if (entry.getTime() >= 0) copy.setTime(entry.getTime());
                    out.putNextEntry(copy);
                    out.write(bytes);
                    out.closeEntry();
                }
            }
        }
        return new JarRewriteResult(rewrittenJar.toByteArray(), found);
    }

    private static java.io.File writeTemporaryJar(byte[] bytes) throws IOException {
        java.io.File file = Files.createTempFile("mihon-quickjs-classes", ".jar").toFile();
        file.deleteOnExit();
        Files.write(file.toPath(), bytes);
        return file;
    }

    private static byte[] rewriteClassConstant(byte[] classFile) throws IOException {
        ByteArrayInputStream inputBytes = new ByteArrayInputStream(classFile);
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream(classFile.length);
        DataInputStream input = new DataInputStream(inputBytes);
        DataOutputStream output = new DataOutputStream(outputBytes);
        int magic = input.readInt();
        if (magic != 0xCAFEBABE) throw new IOException("Invalid MihonKit QuickJS loader class");
        output.writeInt(magic);
        output.writeShort(input.readUnsignedShort());
        output.writeShort(input.readUnsignedShort());
        int constantPoolCount = input.readUnsignedShort();
        output.writeShort(constantPoolCount);
        boolean replaced = false;
        for (int index = 1; index < constantPoolCount; index++) {
            int tag = input.readUnsignedByte();
            output.writeByte(tag);
            switch (tag) {
                case 1 -> {
                    int length = input.readUnsignedShort();
                    byte[] value = input.readNBytes(length);
                    if (value.length != length) throw new IOException("Truncated class constant");
                    String text = new String(value, java.nio.charset.StandardCharsets.UTF_8);
                    if (text.equals(ORIGINAL_LIBRARY_NAME)) {
                        value = RELOCATED_LIBRARY_NAME.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        replaced = true;
                    }
                    output.writeShort(value.length);
                    output.write(value);
                }
                case 3, 4 -> copy(input, output, 4);
                case 5, 6 -> {
                    copy(input, output, 8);
                    index++;
                }
                case 7, 8, 16, 19, 20 -> copy(input, output, 2);
                case 9, 10, 11, 12, 17, 18 -> copy(input, output, 4);
                case 15 -> copy(input, output, 3);
                default -> throw new IOException("Unsupported class constant tag: " + tag);
            }
        }
        if (!replaced) throw new IOException("QuickJSNativeLoader did not contain the expected library name");
        input.transferTo(output);
        return outputBytes.toByteArray();
    }

    private static void copy(DataInputStream input, DataOutputStream output, int count) throws IOException {
        byte[] data = input.readNBytes(count);
        if (data.length != count) throw new IOException("Truncated class constant");
        output.write(data);
    }

    private record JarRewriteResult(byte[] bytes, boolean found) {}
}
