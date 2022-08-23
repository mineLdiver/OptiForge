package net.mine_diver.optiforge.compat.forge.mixin;

import net.minecraft.src.Tessellator;
import net.minecraft.src.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.objectweb.asm.Opcodes.GETSTATIC;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Redirect(
            method = "updateRenderer()V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/src/WorldRenderer;tessellator:Lnet/minecraft/src/Tessellator;",
                    opcode = GETSTATIC
            )
    )
    private Tessellator redirectTessellator() {
        return Tessellator.instance;
    }


}
