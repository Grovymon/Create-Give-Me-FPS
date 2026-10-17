package dev.creategmf.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface LegacyMinecraftAccessor {
    @Accessor("fps")
    static int createGmf$getFps() { throw new AssertionError("Mixin accessor was not applied"); }
}
