package dev.creategmf.diagnostics;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.creategmf.CreateGmf;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.integration.ModCompatibilityDetector;
import dev.creategmf.integration.ShaderStatusDetector;
import dev.creategmf.optimization.belts.BeltShadowCounters;
import dev.creategmf.optimization.belts.BeltShadowOptimizer;
import dev.creategmf.optimization.particles.CreateParticleOptimizer;
import dev.creategmf.optimization.particles.CreateParticleOptimizer.ParticleCounters;
import dev.creategmf.profiler.MemoryMetricsCollector;
import dev.creategmf.profiler.MemorySnapshot;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Low-overhead client-side performance black box.  The render callback writes
 * primitives into a fixed ring buffer only; JSON and filesystem work happen on
 * one daemon writer after a captured event has completed.
 */
public final class DeveloperDiagnostics {
    public static final DeveloperDiagnostics INSTANCE = new DeveloperDiagnostics();

    private static final int SAMPLE_CAPACITY = 8_192;
    private static final long PRE_EVENT_NANOS = 10_000_000_000L;
    private static final long POST_EVENT_NANOS = 5_000_000_000L;
    private static final long COOLDOWN_NANOS = 8_000_000_000L;
    private static final long SPIKE_MIN_NANOS = 33_000_000L;
    private static final long LONG_FRAME_NANOS = 100_000_000L;
    private static final long FREEZE_NANOS = 500_000_000L;
    private static final double SPIKE_FACTOR = 2.5D;
    private static final DateTimeFormatter SESSION_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "GMF diagnostics writer");
        thread.setDaemon(true);
        return thread;
    });

    private final long[] sampleTime = new long[SAMPLE_CAPACITY];
    private final long[] frameTime = new long[SAMPLE_CAPACITY];
    private final long[] baselineTime = new long[SAMPLE_CAPACITY];
    private final long[] particleRequests = new long[SAMPLE_CAPACITY];
    private final long[] particleAllowed = new long[SAMPLE_CAPACITY];
    private final long[] particleSuppressed = new long[SAMPLE_CAPACITY];
    private final long[] beltAttempts = new long[SAMPLE_CAPACITY];
    private final long[] beltRendered = new long[SAMPLE_CAPACITY];
    private final long[] beltSkipped = new long[SAMPLE_CAPACITY];
    private final long[] heapUsed = new long[SAMPLE_CAPACITY];
    private final long[] gcCount = new long[SAMPLE_CAPACITY];
    private final int[] createBlockEntities = new int[SAMPLE_CAPACITY];
    private final int[] kineticBlockEntities = new int[SAMPLE_CAPACITY];
    private final int[] beltControllers = new int[SAMPLE_CAPACITY];
    private final int[] transportedItems = new int[SAMPLE_CAPACITY];
    private final int[] contraptions = new int[SAMPLE_CAPACITY];
    private final int[] looseItems = new int[SAMPLE_CAPACITY];

    private volatile SceneCensus latestScene = SceneCensus.EMPTY;
    private volatile String lastEventText = "—";
    private volatile String lastActionKey = "";
    private volatile Path sessionDirectory;
    private int nextSample;
    private int sampleCount;
    private int ticks;
    private long rollingBaselineNanos;
    private long lastAutoEventNanos;
    private int eventNumber;
    private PendingCapture pendingCapture;

    private DeveloperDiagnostics() {
    }

    public void onFrame(long durationNanos) {
        if (!enabled() || !GmfConfig.CLIENT.developerLogging.get() || durationNanos <= 0L) return;

        long now = System.nanoTime();
        long baseline = rollingBaselineNanos <= 0L ? durationNanos : rollingBaselineNanos;
        writeSample(nextSample, now, durationNanos, baseline);
        nextSample = (nextSample + 1) % SAMPLE_CAPACITY;
        sampleCount = Math.min(SAMPLE_CAPACITY, sampleCount + 1);

        if (hasLiveScene() && GmfConfig.CLIENT.automaticSpikeDetection.get() && pendingCapture == null
                && now - lastAutoEventNanos >= COOLDOWN_NANOS) {
            String type = automaticEventType(durationNanos, baseline);
            if (type != null) {
                scheduleCapture(type, now, durationNanos, baseline, true);
                lastAutoEventNanos = now;
            }
        }

        // A spike is deliberately excluded from the EMA.  Otherwise one pause
        // raises the baseline and hides the next real spike.
        if (durationNanos < Math.max(SPIKE_MIN_NANOS, (long) (baseline * SPIKE_FACTOR))) {
            rollingBaselineNanos = rollingBaselineNanos <= 0L
                    ? durationNanos
                    : (long) (rollingBaselineNanos * 0.94D + durationNanos * 0.06D);
        }
    }

    public void onClientTick() {
        if (!enabled()) return;
        if (!GmfConfig.CLIENT.developerLogging.get()) return;
        if (!ensureSession()) return;
        if (++ticks >= 20) {
            ticks = 0;
            latestScene = CreateSceneScanner.captureNearby();
        }
        PendingCapture capture = pendingCapture;
        if (capture != null && System.nanoTime() >= capture.completeAtNanos()) {
            pendingCapture = null;
            queueEvent(capture);
        }
    }

    public void requestMarker() {
        requestManual("USER_MARKER");
    }

    public void requestManualCapture() {
        if (!enabled()) {
            lastActionKey = "gui.create_gmf.developer.status.disabled";
            return;
        }
        if (!GmfConfig.CLIENT.developerLogging.get()) {
            lastActionKey = "gui.create_gmf.developer.status.recording_disabled";
            return;
        }
        if (!ensureSession()) return;

        long now = System.nanoTime();
        PendingCapture capture = pendingCapture;
        if (capture != null) {
            // Do not throw away a marker that is still collecting its post-
            // event window.  Save the samples gathered so far instead.
            pendingCapture = null;
            capture = new PendingCapture(capture.type(), capture.triggerNanos(), now, capture.triggerFrameNanos(),
                    capture.baselineNanos(), capture.automatic(), capture.sceneAtTrigger());
        } else {
            long last = sampleCount == 0 ? 0L : frameTime[(nextSample + SAMPLE_CAPACITY - 1) % SAMPLE_CAPACITY];
            capture = new PendingCapture("MANUAL_CAPTURE", now, now, last, rollingBaselineNanos, false, latestScene);
            eventNumber++;
            lastEventText = "#" + eventNumber + " — " + LocalDateTime.now().format(DISPLAY_TIME);
        }
        queueEvent(capture);
        lastActionKey = "gui.create_gmf.developer.status.report_saving";
    }

    /**
     * Starts a new on-disk diagnostic session immediately.  This deliberately
     * creates the session files before a frame hitch happens, so the Start
     * button itself is a visible confirmation that the selected log path is
     * writable.
     */
    public void startLogging() {
        if (!enabled()) {
            lastActionKey = "gui.create_gmf.developer.status.disabled";
            return;
        }
        GmfConfig.CLIENT.developerLogging.set(true);
        GmfConfig.save();
        if (ensureSession()) {
            lastActionKey = "gui.create_gmf.developer.status.logging_started";
        }
    }

    /**
     * Stops recording and always queues one final snapshot.  A pending manual
     * capture is completed early rather than discarded, so pressing Stop is a
     * safe way to finish a report before leaving the game.
     */
    public void stopLoggingAndSave() {
        if (!enabled()) {
            lastActionKey = "gui.create_gmf.developer.status.disabled";
            return;
        }
        if (!GmfConfig.CLIENT.developerLogging.get()) {
            lastActionKey = "gui.create_gmf.developer.status.recording_disabled";
            return;
        }
        if (!ensureSession()) return;

        long now = System.nanoTime();
        PendingCapture capture = pendingCapture;
        if (capture != null) {
            pendingCapture = null;
            capture = new PendingCapture(capture.type(), capture.triggerNanos(), now, capture.triggerFrameNanos(),
                    capture.baselineNanos(), capture.automatic(), capture.sceneAtTrigger());
        } else {
            long last = sampleCount == 0 ? 0L : frameTime[(nextSample + SAMPLE_CAPACITY - 1) % SAMPLE_CAPACITY];
            capture = new PendingCapture("LOGGING_STOPPED", now, now, last, rollingBaselineNanos, false, latestScene);
            eventNumber++;
            lastEventText = "#" + eventNumber + " — " + LocalDateTime.now().format(DISPLAY_TIME);
        }
        queueEvent(capture);
        GmfConfig.CLIENT.developerLogging.set(false);
        GmfConfig.save();
        // A subsequent Start action creates a separate, clearly dated session.
        sessionDirectory = null;
        lastActionKey = "gui.create_gmf.developer.status.stopping_and_saving";
    }

    private void requestManual(String type) {
        if (!enabled()) {
            lastActionKey = "gui.create_gmf.developer.status.disabled";
            return;
        }
        if (!GmfConfig.CLIENT.developerLogging.get()) {
            lastActionKey = "gui.create_gmf.developer.status.recording_disabled";
            return;
        }
        if (!ensureSession()) return;
        if (pendingCapture != null) {
            lastActionKey = "gui.create_gmf.developer.status.pending";
            return;
        }
        long now = System.nanoTime();
        long last = sampleCount == 0 ? 0L : frameTime[(nextSample + SAMPLE_CAPACITY - 1) % SAMPLE_CAPACITY];
        scheduleCapture(type, now, last, rollingBaselineNanos, false);
        lastActionKey = "gui.create_gmf.developer.status.capturing";
    }

    private String automaticEventType(long frame, long baseline) {
        if (frame >= FREEZE_NANOS) return "FREEZE";
        if (frame >= LONG_FRAME_NANOS) return "LONG_FRAME";
        if (frame >= SPIKE_MIN_NANOS && frame >= baseline * SPIKE_FACTOR) return "FRAME_SPIKE";
        return null;
    }

    /** Do not write automatic "spike" reports from menus or world loading. */
    private boolean hasLiveScene() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.player != null && latestScene.chunksScanned() > 0;
    }

    private void scheduleCapture(String type, long now, long frame, long baseline, boolean automatic) {
        pendingCapture = new PendingCapture(type, now, now + POST_EVENT_NANOS, frame, baseline, automatic, latestScene);
        eventNumber++;
        lastEventText = "#" + eventNumber + " — " + LocalDateTime.now().format(DISPLAY_TIME);
    }

    private void writeSample(int index, long now, long duration, long baseline) {
        SceneCensus scene = latestScene;
        ParticleCounters particles = CreateParticleOptimizer.counters();
        BeltShadowCounters belts = BeltShadowOptimizer.counters();
        MemorySnapshot memory = MemoryMetricsCollector.INSTANCE.snapshot();
        sampleTime[index] = now;
        frameTime[index] = duration;
        baselineTime[index] = baseline;
        particleRequests[index] = particles.requests();
        particleAllowed[index] = particles.allowed();
        particleSuppressed[index] = particles.suppressed();
        beltAttempts[index] = belts.attempted();
        beltRendered[index] = belts.rendered();
        beltSkipped[index] = belts.skipped();
        heapUsed[index] = memory.heapUsedBytes();
        gcCount[index] = memory.gcCount();
        createBlockEntities[index] = scene.createBlockEntities();
        kineticBlockEntities[index] = scene.kineticBlockEntities();
        beltControllers[index] = scene.beltControllers();
        transportedItems[index] = scene.transportedItems();
        contraptions[index] = scene.contraptions();
        looseItems[index] = scene.looseItemEntities();
    }

    private boolean ensureSession() {
        Path existing = sessionDirectory;
        if (existing != null) {
            try {
                // The launcher or an antivirus can remove an empty directory
                // between captures.  Recreate it immediately before every
                // recording action instead of silently losing the report.
                Files.createDirectories(existing.resolve("events"));
                return true;
            } catch (IOException error) {
                lastActionKey = "gui.create_gmf.developer.status.folder_failed";
                CreateGmf.LOGGER.warn("[GMF] Cannot restore developer diagnostics folder", error);
                return false;
            }
        }
        try {
            Path root = logRoot();
            Files.createDirectories(root);
            Path candidate = root.resolve(SESSION_TIME.format(LocalDateTime.now()));
            int suffix = 2;
            while (Files.exists(candidate)) candidate = root.resolve(SESSION_TIME.format(LocalDateTime.now()) + "_" + suffix++);
            Files.createDirectories(candidate.resolve("events"));
            sessionDirectory = candidate;
            // Gather Minecraft and mod-loader state on the client thread.  The
            // writer below only performs filesystem work and string output.
            String sessionLog = sessionLog();
            String sessionJson = sessionJson();
            String mods = modList();
            Path created = candidate;
            WRITER.execute(() -> writeSessionFiles(created, sessionLog, sessionJson, mods));
            return true;
        } catch (IOException | RuntimeException error) {
            lastActionKey = "gui.create_gmf.developer.status.folder_failed";
            CreateGmf.LOGGER.warn("[GMF] Cannot create developer diagnostics folder", error);
            return false;
        }
    }

    private void queueEvent(PendingCapture capture) {
        Path session = sessionDirectory;
        if (session == null) return;
        List<Sample> samples = snapshotSamples(capture.triggerNanos() - PRE_EVENT_NANOS, capture.completeAtNanos());
        RuntimeState state = runtimeState();
        int number = eventNumber;
        WRITER.execute(() -> writeEvent(session, number, capture, samples, state));
    }

    private List<Sample> snapshotSamples(long from, long until) {
        List<Sample> copied = new ArrayList<>();
        int start = sampleCount < SAMPLE_CAPACITY ? 0 : nextSample;
        for (int i = 0; i < sampleCount; i++) {
            int index = (start + i) % SAMPLE_CAPACITY;
            if (sampleTime[index] >= from && sampleTime[index] <= until) copied.add(copySample(index));
        }
        return copied;
    }

    private Sample copySample(int index) {
        return new Sample(sampleTime[index], frameTime[index], baselineTime[index], particleRequests[index],
                particleAllowed[index], particleSuppressed[index], beltAttempts[index], beltRendered[index],
                beltSkipped[index], heapUsed[index], gcCount[index], createBlockEntities[index],
                kineticBlockEntities[index], beltControllers[index], transportedItems[index], contraptions[index],
                looseItems[index]);
    }

    private RuntimeState runtimeState() {
        ParticleCounters particles = CreateParticleOptimizer.counters();
        return new RuntimeState(GmfRuntimeStatus.animationHookObserved(), GmfRuntimeStatus.beltItemHookObserved(),
                GmfRuntimeStatus.particleHookObserved(), GmfConfig.CLIENT.enabled.get(),
                GmfConfig.CLIENT.renderTransportedBeltItems.get(), GmfConfig.CLIENT.createParticleMode.get().name(),
                GmfConfig.CLIENT.distantAnimationMode.get().name(), GmfConfig.CLIENT.flywheelBackend.get().name(),
                GmfConfig.CLIENT.acceleratedRenderer.get(), ShaderStatusDetector.isShaderPackActive(),
                String.join(", ", ModCompatibilityDetector.overlappingOptimizers()), particles.steamSmoke(),
                particles.sparks(), particles.itemBreak(), particles.fluidSplash(), particles.directFluidEffectsSuppressed());
    }

    private void writeSessionFiles(Path session, String sessionLog, String sessionJson, String mods) {
        try {
            Files.createDirectories(session.resolve("events"));
            Files.writeString(session.resolve("session.log"), sessionLog);
            Files.writeString(session.resolve("session.json"), sessionJson);
            Files.writeString(session.resolve("mods.txt"), mods);
        } catch (IOException error) {
            CreateGmf.LOGGER.warn("[GMF] Cannot write developer session files", error);
        }
    }

    private String sessionLog() {
        Minecraft minecraft = Minecraft.getInstance();
        return "GMF developer diagnostics session\n"
                + "started=" + LocalDateTime.now() + "\n"
                + "gmf=" + versionOf("create_gmf") + "\n"
                + "create=" + versionOf("create") + "\n"
                + "flywheel=" + versionOf("flywheel") + "\n"
                + "java=" + System.getProperty("java.version", "unknown") + "\n"
                + "os=" + System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "") + "\n"
                + "cpu=" + ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() + " logical processors\n"
                + "resolution=" + minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight() + "\n"
                + "gpu=not queried (safe OpenGL device query unavailable)\n";
    }

    private String sessionJson() {
        Minecraft minecraft = Minecraft.getInstance();
        return "{\n"
                + "  \"schema\": 1,\n"
                + "  \"gmfVersion\": \"" + json(versionOf("create_gmf")) + "\",\n"
                + "  \"createVersion\": \"" + json(versionOf("create")) + "\",\n"
                + "  \"flywheelVersion\": \"" + json(versionOf("flywheel")) + "\",\n"
                + "  \"buildCommit\": \"" + json(buildCommit()) + "\",\n"
                + "  \"java\": \"" + json(System.getProperty("java.version", "unknown")) + "\",\n"
                + "  \"os\": \"" + json(System.getProperty("os.name", "unknown")) + "\",\n"
                + "  \"cpuLogicalProcessors\": " + ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() + ",\n"
                + "  \"resolution\": \"" + minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight() + "\",\n"
                + "  \"gpu\": \"unavailable\"\n"
                + "}\n";
    }

    private String modList() {
        StringBuilder result = new StringBuilder("Relevant mods:\n");
        for (String id : List.of("create_gmf", "create", "flywheel", "sodium", "embeddium", "iris", "oculus", "spark",
                "createbetterfps", "flerovium")) {
            String version = versionOf(id);
            if (!"not loaded".equals(version)) result.append(id).append(" = ").append(version).append('\n');
        }
        result.append("\nAll loaded mods:\n");
        ModList.get().getMods().stream()
                .sorted(Comparator.comparing(info -> info.getModId()))
                .forEach(info -> result.append(info.getModId()).append(" = ").append(info.getVersion()).append('\n'));
        return result.toString();
    }

    private void writeEvent(Path session, int number, PendingCapture capture, List<Sample> samples, RuntimeState state) {
        try {
            Path events = session.resolve("events");
            Files.createDirectories(events);
            String base = String.format("event-%03d-%s", number, capture.type().toLowerCase());
            Files.writeString(events.resolve(base + ".log"), eventLog(capture, samples, state));
            Files.writeString(events.resolve(base + ".json"), eventJson(capture, samples, state));
            lastActionKey = "gui.create_gmf.developer.status.report_saved";
        } catch (IOException error) {
            lastActionKey = "gui.create_gmf.developer.status.report_failed";
            CreateGmf.LOGGER.warn("[GMF] Cannot write developer event", error);
        }
    }

    private String eventLog(PendingCapture capture, List<Sample> samples, RuntimeState state) {
        SceneCensus scene = capture.sceneAtTrigger();
        StringBuilder result = new StringBuilder();
        result.append("GMF event: ").append(capture.type()).append('\n');
        result.append("automatic=").append(capture.automatic()).append('\n');
        result.append("trigger_frame_ms=").append(ms(capture.triggerFrameNanos())).append('\n');
        result.append("baseline_ms=").append(ms(capture.baselineNanos())).append('\n');
        result.append("samples=").append(samples.size()).append(" (10 sec before + 5 sec after when available)\n\n");
        result.append("Nearby scene scan: chunks=").append(scene.chunksScanned())
                .append(", create_block_entities=").append(scene.createBlockEntities())
                .append(", kinetics=").append(scene.kineticBlockEntities())
                .append(", belts=").append(scene.beltControllers())
                .append(", transported_items=").append(scene.transportedItems())
                .append(", contraptions=").append(scene.contraptions())
                .append(", loose_items=").append(scene.looseItemEntities()).append('\n');
        result.append("Top mechanism groups (estimated visual cost, not a profiler measurement):\n");
        scene.topMechanisms(10).forEach(entry -> result.append(" - ").append(entry.group().name())
                .append(": ").append(entry.objects()).append(" objects, estimate ").append(entry.estimatedWeight()).append('\n'));
        result.append("Top block entity types:\n");
        scene.topBlockEntityTypes(10).forEach(entry -> result.append(" - ").append(entry.typeName()).append(": ")
                .append(entry.objects()).append('\n'));
        result.append("\nRuntime: animation_hook=").append(state.animationHookSeen())
                .append(", belt_item_hook=").append(state.beltItemHookSeen())
                .append(", particle_hook=").append(state.particleHookSeen())
                .append(", shader_active=").append(state.shaderActive())
                .append(", overlaps=").append(state.overlappingOptimizers().isBlank() ? "none" : state.overlappingOptimizers())
                .append('\n');
        result.append("Particle categories (cumulative requests): steam_smoke=").append(state.steamSmoke())
                .append(", sparks=").append(state.sparks()).append(", item_break=").append(state.itemBreak())
                .append(", fluid_splash=").append(state.fluidSplash())
                .append(", direct_fluid_effects_suppressed=").append(state.directFluidEffectsSuppressed()).append('\n');
        return result.toString();
    }

    private String eventJson(PendingCapture capture, List<Sample> samples, RuntimeState state) {
        StringBuilder result = new StringBuilder("{\n");
        result.append("  \"event\": \"").append(json(capture.type())).append("\",\n");
        result.append("  \"automatic\": ").append(capture.automatic()).append(",\n");
        result.append("  \"triggerFrameMs\": ").append(ms(capture.triggerFrameNanos())).append(",\n");
        result.append("  \"baselineMs\": ").append(ms(capture.baselineNanos())).append(",\n");
        result.append("  \"runtime\": {\"animationHook\": ").append(state.animationHookSeen())
                .append(", \"beltItemHook\": ").append(state.beltItemHookSeen())
                .append(", \"particleHook\": ").append(state.particleHookSeen())
                .append(", \"shaderActive\": ").append(state.shaderActive())
                .append(", \"flywheelBackend\": \"").append(json(state.flywheelBackend()))
                .append("\", \"gmfEnabled\": ").append(state.gmfEnabled())
                .append(", \"beltItemsEnabled\": ").append(state.beltItemsEnabled())
                .append(", \"particleMode\": \"").append(json(state.particleMode()))
                .append("\", \"directFluidEffectsSuppressed\": ").append(state.directFluidEffectsSuppressed())
                .append(", \"distantAnimationMode\": \"").append(json(state.animationMode()))
                .append("\", \"acceleratedRenderer\": ").append(state.acceleratedRenderer()).append("},\n");
        result.append("  \"nearbyScan\": ").append(sceneJson(capture.sceneAtTrigger())).append(",\n");
        result.append("  \"samples\": [\n");
        for (int i = 0; i < samples.size(); i++) {
            Sample sample = samples.get(i);
            result.append("    ").append(sampleJson(sample, i == 0 ? null : samples.get(i - 1)));
            if (i + 1 < samples.size()) result.append(',');
            result.append('\n');
        }
        result.append("  ]\n}\n");
        return result.toString();
    }

    private String sceneJson(SceneCensus scene) {
        return "{\"chunks\":" + scene.chunksScanned() + ",\"createBlockEntities\":" + scene.createBlockEntities()
                + ",\"kinetics\":" + scene.kineticBlockEntities() + ",\"belts\":" + scene.beltControllers()
                + ",\"transportedItems\":" + scene.transportedItems() + ",\"contraptions\":" + scene.contraptions()
                + ",\"looseItems\":" + scene.looseItemEntities() + "}";
    }

    private String sampleJson(Sample sample, Sample previous) {
        long particleDelta = previous == null ? 0L : sample.particleRequests() - previous.particleRequests();
        long beltDelta = previous == null ? 0L : sample.beltAttempts() - previous.beltAttempts();
        return "{\"t\":" + sample.timeNanos() + ",\"frameMs\":" + ms(sample.frameNanos())
                + ",\"fps\":" + fps(sample.frameNanos()) + ",\"baselineMs\":" + ms(sample.baselineNanos())
                + ",\"particles\":{\"requested\":"
                + sample.particleRequests() + ",\"allowed\":" + sample.particleAllowed() + ",\"suppressed\":"
                + sample.particleSuppressed() + ",\"deltaRequested\":" + particleDelta
                + "},\"belts\":{\"attempted\":" + sample.beltAttempts()
                + ",\"rendered\":" + sample.beltRendered() + ",\"skipped\":" + sample.beltSkipped()
                + ",\"deltaAttempted\":" + beltDelta + "},\"heapUsed\":" + sample.heapUsed() + ",\"gcCount\":" + sample.gcCount()
                + ",\"scene\":{\"createBlockEntities\":" + sample.createBlockEntities() + ",\"kinetics\":"
                + sample.kineticBlockEntities() + ",\"belts\":" + sample.beltControllers() + ",\"items\":"
                + sample.transportedItems() + ",\"contraptions\":" + sample.contraptions() + ",\"looseItems\":"
                + sample.looseItems() + "}}";
    }

    public boolean openLogFolder() {
        try {
            Files.createDirectories(logRoot());
            Util.getPlatform().openUri(logRoot().toUri().toString());
            lastActionKey = "gui.create_gmf.developer.status.folder_opened";
            return true;
        } catch (IOException | RuntimeException error) {
            lastActionKey = "gui.create_gmf.developer.status.folder_failed";
            CreateGmf.LOGGER.warn("[GMF] Cannot open developer diagnostics folder", error);
            return false;
        }
    }

    public boolean copyLogPath() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(logRoot().toAbsolutePath().toString()), null);
            lastActionKey = "gui.create_gmf.developer.status.path_copied";
            return true;
        } catch (RuntimeException error) {
            lastActionKey = "gui.create_gmf.developer.status.path_failed";
            CreateGmf.LOGGER.warn("[GMF] Cannot copy developer diagnostics path", error);
            return false;
        }
    }

    public String logPath() {
        return logRoot().toAbsolutePath().toString();
    }

    public String lastEventText() {
        return lastEventText;
    }

    public Component lastActionMessage() {
        return lastActionKey.isBlank() ? Component.empty() : Component.translatable(lastActionKey);
    }

    private boolean enabled() {
        return GmfConfig.SPEC.isLoaded() && GmfConfig.CLIENT.developerMode.get();
    }

    private static Path logRoot() {
        return FMLPaths.GAMEDIR.get().resolve("logs").resolve("GMF");
    }

    private static String versionOf(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("not loaded");
    }

    private static String buildCommit() {
        try (InputStream input = DeveloperDiagnostics.class.getClassLoader().getResourceAsStream("gmf_build.properties")) {
            if (input == null) return "unknown";
            java.util.Properties properties = new java.util.Properties();
            properties.load(input);
            return properties.getProperty("git_commit", "unknown");
        } catch (IOException ignored) {
            return "unknown";
        }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String ms(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000_000D);
    }

    private static String fps(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.2f", nanos <= 0L ? 0D : 1_000_000_000D / nanos);
    }

    private record PendingCapture(String type, long triggerNanos, long completeAtNanos, long triggerFrameNanos,
            long baselineNanos, boolean automatic, SceneCensus sceneAtTrigger) {
    }

    private record RuntimeState(boolean animationHookSeen, boolean beltItemHookSeen, boolean particleHookSeen,
            boolean gmfEnabled, boolean beltItemsEnabled, String particleMode, String animationMode,
            String flywheelBackend, boolean acceleratedRenderer, boolean shaderActive, String overlappingOptimizers,
            long steamSmoke, long sparks, long itemBreak, long fluidSplash, long directFluidEffectsSuppressed) {
    }

    private record Sample(long timeNanos, long frameNanos, long baselineNanos, long particleRequests,
            long particleAllowed, long particleSuppressed, long beltAttempts, long beltRendered, long beltSkipped,
            long heapUsed, long gcCount, int createBlockEntities, int kineticBlockEntities, int beltControllers,
            int transportedItems, int contraptions, int looseItems) {
    }
}
