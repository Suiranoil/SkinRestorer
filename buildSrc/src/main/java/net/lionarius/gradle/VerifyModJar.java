package net.lionarius.gradle;

import groovy.json.JsonSlurper;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public abstract class VerifyModJar extends DefaultTask {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String OVERWRITE = "Lorg/spongepowered/asm/mixin/Overwrite;";
    private static final String SHADOW_PREFIX = "shadow$";

    private static final Pattern MIXIN_CONFIG = Pattern.compile("[^/]+\\.mixins\\.json");
    private static final Pattern TOML_MIXIN_CONFIG = Pattern.compile("(?m)^\\s*config\\s*=\\s*\"([^\"]+)\"");
    private static final List<String> METADATA =
            List.of("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "pack.mcmeta");
    private static final int MAX_LISTED = 10;

    @Internal
    public abstract RegularFileProperty getJarFile();

    @Internal
    public abstract ConfigurableFileCollection getExpectedFiles();

    @Internal
    public abstract RegularFileProperty getSrgMappings();

    @TaskAction
    public void verify() throws IOException {
        var jar = this.getJarFile().getAsFile().get();
        if (!jar.isFile()) throw new GradleException("Mod jar does not exist: " + jar);

        var problems = new ArrayList<String>();
        int mixinCount;
        int classCount;

        try (var zip = new ZipFile(jar)) {
            var entries = zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(ZipEntry::getName)
                    .collect(Collectors.toSet());
            classCount = (int)
                    entries.stream().filter(name -> name.endsWith(".class")).count();

            this.checkExpectedFiles(entries, problems);
            this.checkMetadata(zip, entries, problems);
            var mixins = this.checkMixinConfigs(zip, entries, problems);
            mixinCount = mixins.size();
            this.checkNestedJars(zip, entries, problems);
            this.checkShadowsReobfuscated(zip, mixins, problems);
        }

        if (!problems.isEmpty())
            throw new GradleException(jar.getName() + " must not be published:\n - " + String.join("\n - ", problems));

        this.getLogger().lifecycle("{} is fine ({} classes, {} mixins)", jar.getName(), classCount, mixinCount);
    }

    private void checkExpectedFiles(Set<String> entries, List<String> problems) throws IOException {
        var missing = new ArrayList<String>();
        for (File root : this.getExpectedFiles().getFiles()) {
            if (!root.isDirectory()) continue;

            var rootPath = root.toPath();
            try (var files = Files.walk(rootPath)) {
                files.filter(Files::isRegularFile)
                        .map(file -> rootPath.relativize(file).toString().replace(File.separatorChar, '/'))
                        .filter(name -> !entries.contains(name))
                        .forEach(missing::add);
            }
        }
        if (missing.isEmpty()) return;

        var listed = missing.stream().sorted().limit(MAX_LISTED).collect(Collectors.joining(", "));
        var more = missing.size() > MAX_LISTED ? " and " + (missing.size() - MAX_LISTED) + " more" : "";
        problems.add(missing.size() + " compiled classes or resources are missing from the jar: " + listed + more);
    }

    private void checkMetadata(ZipFile zip, Set<String> entries, List<String> problems) throws IOException {
        for (var name : METADATA) {
            if (!entries.contains(name)) continue;

            var text = readText(zip, name).replace("${file.jarVersion}", "");
            if (text.contains("${")) problems.add(name + " still contains an unexpanded ${...} placeholder");
        }

        for (var config : referencedMixinConfigs(zip, entries))
            if (!entries.contains(config)) problems.add("mixin config " + config + " is referenced but not in the jar");
    }

    private static Set<String> referencedMixinConfigs(ZipFile zip, Set<String> entries) throws IOException {
        var configs = new HashSet<String>();

        if (entries.contains("fabric.mod.json")
                && parseJson(zip, "fabric.mod.json").get("mixins") instanceof List<?> mixins) {
            for (var mixin : mixins) {
                if (mixin instanceof String config) configs.add(config);
                else if (mixin instanceof Map<?, ?> map && map.get("config") instanceof String config)
                    configs.add(config);
            }
        }

        if (entries.contains("META-INF/MANIFEST.MF")) {
            try (var in = zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF"))) {
                var value = new Manifest(in).getMainAttributes().getValue("MixinConfigs");
                if (value != null)
                    for (var config : value.split(",")) if (!config.isBlank()) configs.add(config.trim());
            }
        }

        if (entries.contains("META-INF/neoforge.mods.toml")) {
            var matcher = TOML_MIXIN_CONFIG.matcher(readText(zip, "META-INF/neoforge.mods.toml"));
            while (matcher.find()) configs.add(matcher.group(1));
        }

        return configs;
    }

    private List<String> checkMixinConfigs(ZipFile zip, Set<String> entries, List<String> problems) throws IOException {
        var mixinClasses = new ArrayList<String>();

        for (var name : entries) {
            if (!MIXIN_CONFIG.matcher(name).matches()) continue;

            var config = parseJson(zip, name);
            if (config.get("refmap") instanceof String refmap
                    && this.getSrgMappings().isPresent()
                    && !entries.contains(refmap))
                problems.add(name + " needs refmap " + refmap + ", which is not in the jar");

            if (!(config.get("package") instanceof String pkg)) continue;
            for (var side : List.of("mixins", "client", "server")) {
                if (!(config.get(side) instanceof List<?> mixins)) continue;

                for (var mixin : mixins) {
                    var path = (pkg + "." + mixin).replace('.', '/') + ".class";
                    if (entries.contains(path)) mixinClasses.add(path);
                    else problems.add(name + " lists mixin " + mixin + ", which is not in the jar");
                }
            }
        }

        return mixinClasses;
    }

    private void checkNestedJars(ZipFile zip, Set<String> entries, List<String> problems) throws IOException {
        var nested = new ArrayList<String>();

        if (entries.contains("META-INF/jarjar/metadata.json")
                && parseJson(zip, "META-INF/jarjar/metadata.json").get("jars") instanceof List<?> jars) {
            for (var jar : jars)
                if (jar instanceof Map<?, ?> map && map.get("path") instanceof String path) nested.add(path);
        }

        if (entries.contains("fabric.mod.json")
                && parseJson(zip, "fabric.mod.json").get("jars") instanceof List<?> jars) {
            for (var jar : jars)
                if (jar instanceof Map<?, ?> map && map.get("file") instanceof String path) nested.add(path);
        }

        for (var path : nested) {
            if (!entries.contains(path)) {
                problems.add("bundled jar " + path + " is declared but not in the jar");
                continue;
            }

            try (var in = new ZipInputStream(zip.getInputStream(zip.getEntry(path)))) {
                if (in.getNextEntry() == null) problems.add("bundled jar " + path + " is empty or corrupt");
            }
        }
    }

    private void checkShadowsReobfuscated(ZipFile zip, List<String> mixinClasses, List<String> problems)
            throws IOException {
        if (!this.getSrgMappings().isPresent()) return;

        var mappingsFile = this.getSrgMappings().getAsFile().get();
        if (!mappingsFile.isFile()) {
            problems.add("cannot check mixin reobfuscation, mappings are missing: " + mappingsFile);
            return;
        }

        var mixins = new ArrayList<MixinClass>();
        var targets = new HashSet<String>();
        for (var path : mixinClasses) {
            var mixin =
                    MixinClass.read(path, zip.getInputStream(zip.getEntry(path)).readAllBytes());
            mixins.add(mixin);
            targets.addAll(mixin.targets);
        }

        var mappings = readSrgMappings(mappingsFile, targets);
        for (var mixin : mixins) {
            for (var member : mixin.shadows) {
                for (var target : mixin.targets) {
                    var srgName = mappings.getOrDefault(target, Map.of()).get(member.key());
                    if (srgName != null && !srgName.equals(member.name))
                        problems.add(mixin.name + "." + member.name + " was not reobfuscated to " + srgName
                                + " and will not be found in " + target + " at runtime");
                }
            }
        }
    }

    private static Map<String, Map<String, String>> readSrgMappings(File file, Set<String> classes) throws IOException {
        var mappings = new HashMap<String, Map<String, String>>();
        Map<String, String> current = null;

        try (var lines = Files.lines(file.toPath())) {
            for (var line : (Iterable<String>) lines::iterator) {
                if (line.isEmpty() || line.startsWith("tsrg2")) continue;

                if (line.charAt(0) != '\t') {
                    var name = line.substring(0, line.indexOf(' '));
                    current = classes.contains(name) ? mappings.computeIfAbsent(name, key -> new HashMap<>()) : null;
                    continue;
                }
                if (current == null || line.startsWith("\t\t")) continue;

                var parts = line.substring(1).split(" ");
                if (parts.length == 2) current.put(parts[0], parts[1]);
                else if (parts.length == 3) current.put(parts[0] + parts[1], parts[2]);
            }
        }

        return mappings;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(ZipFile zip, String name) throws IOException {
        return new JsonSlurper().parseText(readText(zip, name)) instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
    }

    private static String readText(ZipFile zip, String name) throws IOException {
        try (var in = zip.getInputStream(zip.getEntry(name))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Member(String name, String descriptor, boolean method) {
        String key() {
            return this.method ? this.name + this.descriptor : this.name;
        }
    }

    private static final class MixinClass {
        final String name;
        final List<String> targets = new ArrayList<>();
        final List<Member> shadows = new ArrayList<>();

        private final ByteBuffer data;
        private Object[] constants;

        private MixinClass(String path, byte[] bytes) {
            this.name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".class".length());
            this.data = ByteBuffer.wrap(bytes);
        }

        static MixinClass read(String path, byte[] bytes) throws IOException {
            var mixin = new MixinClass(path, bytes);
            try {
                mixin.parse();
            } catch (RuntimeException e) {
                throw new IOException("Cannot parse " + path, e);
            }
            return mixin;
        }

        private void parse() {
            this.data.position(8);
            this.readConstants();
            this.data.position(this.data.position() + 6);
            int interfaces = this.data.getShort() & 0xFFFF;
            this.data.position(this.data.position() + 2 * interfaces);

            var members = new ArrayList<Member>();
            var memberAnnotations = new ArrayList<Map<String, Map<String, Object>>>();
            for (var method : new boolean[] {false, true}) {
                int count = this.data.getShort() & 0xFFFF;
                for (int i = 0; i < count; i++) {
                    this.data.getShort();
                    var memberName = (String) this.constants[this.data.getShort() & 0xFFFF];
                    var descriptor = (String) this.constants[this.data.getShort() & 0xFFFF];
                    members.add(new Member(memberName, descriptor, method));
                    memberAnnotations.add(this.readAnnotations());
                }
            }

            var mixin = this.readAnnotations().get(MIXIN);
            if (mixin == null || Boolean.FALSE.equals(mixin.get("remap"))) return;

            if (mixin.get("value") instanceof List<?> values)
                for (var value : values) {
                    var descriptor = (String) value;
                    this.targets.add(descriptor.substring(1, descriptor.length() - 1));
                }
            if (mixin.get("targets") instanceof List<?> values)
                for (var value : values) this.targets.add(((String) value).replace('.', '/'));

            for (int i = 0; i < members.size(); i++) {
                var annotations = memberAnnotations.get(i);
                var shadow = annotations.get(SHADOW);
                if (shadow == null && !annotations.containsKey(OVERWRITE)) continue;
                if (shadow != null && Boolean.FALSE.equals(shadow.get("remap"))) continue;

                var member = members.get(i);
                var memberName = member.name.startsWith(SHADOW_PREFIX)
                        ? member.name.substring(SHADOW_PREFIX.length())
                        : member.name;
                this.shadows.add(new Member(memberName, member.descriptor, member.method));
            }
        }

        private void readConstants() {
            int count = this.data.getShort() & 0xFFFF;
            this.constants = new Object[count];
            for (int i = 1; i < count; i++) {
                int tag = this.data.get();
                switch (tag) {
                    case 1 -> {
                        var bytes = new byte[this.data.getShort() & 0xFFFF];
                        this.data.get(bytes);
                        this.constants[i] = new String(bytes, StandardCharsets.UTF_8);
                    }
                    case 3 -> this.constants[i] = this.data.getInt();
                    case 4 -> this.data.getInt();
                    case 5, 6 -> {
                        this.data.getLong();
                        i++;
                    }
                    case 7, 8, 16, 19, 20 -> this.data.getShort();
                    case 9, 10, 11, 12, 17, 18 -> this.data.getInt();
                    case 15 -> this.data.position(this.data.position() + 3);
                    default -> throw new IllegalStateException("Unknown constant pool tag " + tag);
                }
            }
        }

        private Map<String, Map<String, Object>> readAnnotations() {
            var annotations = new HashMap<String, Map<String, Object>>();
            int attributes = this.data.getShort() & 0xFFFF;
            for (int i = 0; i < attributes; i++) {
                var attribute = (String) this.constants[this.data.getShort() & 0xFFFF];
                int end = this.data.getInt() + this.data.position();
                if (attribute.equals("RuntimeVisibleAnnotations") || attribute.equals("RuntimeInvisibleAnnotations")) {
                    int count = this.data.getShort() & 0xFFFF;
                    for (int j = 0; j < count; j++) {
                        var type = (String) this.constants[this.data.getShort() & 0xFFFF];
                        annotations.put(type, this.readAnnotationValues());
                    }
                }
                this.data.position(end);
            }
            return annotations;
        }

        private Map<String, Object> readAnnotationValues() {
            var values = new HashMap<String, Object>();
            int count = this.data.getShort() & 0xFFFF;
            for (int i = 0; i < count; i++) {
                var key = (String) this.constants[this.data.getShort() & 0xFFFF];
                values.put(key, this.readAnnotationValue());
            }
            return values;
        }

        private Object readAnnotationValue() {
            char tag = (char) this.data.get();
            switch (tag) {
                case 'Z':
                    return (Integer) this.constants[this.data.getShort() & 0xFFFF] != 0;
                case 's':
                case 'c':
                    return this.constants[this.data.getShort() & 0xFFFF];
                case 'e':
                    this.data.getInt();
                    return null;
                case '@':
                    this.data.getShort();
                    return this.readAnnotationValues();
                case '[':
                    int count = this.data.getShort() & 0xFFFF;
                    var list = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) list.add(this.readAnnotationValue());
                    return list;
                default:
                    this.data.getShort();
                    return null;
            }
        }
    }
}
