package dev.creategmf;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Build-time contracts. Not a substitute for actually applying mixins in Minecraft. */
class ResourceContractTest {
    private static final Path JAVA = Path.of("src/main/java");
    private static JsonObject language(String name) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/create_gmf/lang/" + name + ".json"))).getAsJsonObject();
    }
    @Test void russianAndEnglishKeysMatch() throws Exception {
        assertEquals(language("en_us").keySet(), language("ru_ru").keySet());
    }
    @Test void constantTranslationKeysExist() throws Exception {
        Set<String> keys = language("en_us").keySet();
        Pattern pattern = Pattern.compile("\"((?:gui|config|key|unit|enum|diagnostic|recommendation|benchmark|tooltip)\\.create_gmf\\.[a-zA-Z0-9_.]+)\"");
        try (var files = Files.walk(JAVA)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var match = pattern.matcher(Files.readString(path));
                while (match.find()) if (!match.group(1).endsWith("."))
                    assertTrue(keys.contains(match.group(1)), () -> path + ": missing " + match.group(1));
            }
        }
    }
    @Test void everyListedMixinHasTheDeclaredPackageAndClientOnlyPlacement() throws Exception {
        var mixins = JsonParser.parseString(Files.readString(Path.of("src/main/resources/create_gmf.mixins.json"))).getAsJsonObject();
        String prefix = mixins.get("package").getAsString() + ".";
        Set<String> classes = new HashSet<>();
        var packagePattern = Pattern.compile("package ([a-zA-Z0-9_.]+);");
        try (var files = Files.walk(JAVA)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var match = packagePattern.matcher(Files.readString(file));
                if (match.find()) classes.add(match.group(1) + "." + file.getFileName().toString().replace(".java", ""));
            }
        }
        for (var mixin : mixins.getAsJsonArray("client"))
            assertTrue(classes.contains(prefix + mixin.getAsString()), () -> "Missing " + prefix + mixin.getAsString());
        assertFalse(mixins.has("mixins"), "Rendering hooks must not be installed on a dedicated server");
    }
    @Test void arbitraryItemAndServerFluidHooksAreGone() throws Exception {
        String json = Files.readString(Path.of("src/main/resources/create_gmf.mixins.json"));
        assertFalse(json.contains("ItemEntityRenderer"));
        assertFalse(json.contains("FluidManipulationBehaviour"));
        assertFalse(Files.exists(JAVA.resolve("dev/creategmf/optimization/crushing/CrushingOutputOptimizer.java")));
        assertFalse(Files.readString(JAVA.resolve("dev/creategmf/CreateGmf.java")).contains("net.minecraft.client"));
    }
    @Test void clientDependencyAndMitNoticesAreRetained() throws Exception {
        String metadata = Files.readString(Path.of("src/main/resources/META-INF/neoforge.mods.toml"));
        assertTrue(metadata.contains("entityculling"));
        assertTrue(metadata.contains("CLIENT"));
        String notices = Files.readString(Path.of("THIRD_PARTY_NOTICES.md"));
        assertTrue(notices.contains("CreateBetterFPS"));
        assertTrue(notices.contains("Nowheel"));
    }
}
