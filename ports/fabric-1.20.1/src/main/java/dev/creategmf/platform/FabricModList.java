package dev.creategmf.platform;

import java.util.Collection;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;

/** Loader-neutral shape for GMF's mod inventory; all data comes from Fabric Loader. */
public final class FabricModList {
    private static final FabricModList INSTANCE = new FabricModList();
    public static FabricModList get() { return INSTANCE; }
    public boolean isLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }
    public Collection<Info> getMods() {
        return FabricLoader.getInstance().getAllMods().stream().map(FabricModList::info).toList();
    }
    public Optional<Container> getModContainerById(String id) {
        return FabricLoader.getInstance().getModContainer(id).map(mod -> new Container(info(mod)));
    }
    private static Info info(net.fabricmc.loader.api.ModContainer mod) {
        return new Info(mod.getMetadata().getId(), mod.getMetadata().getVersion().getFriendlyString());
    }
    public record Info(String getModId, String getVersion) {}
    public record Container(Info getModInfo) {}
}
