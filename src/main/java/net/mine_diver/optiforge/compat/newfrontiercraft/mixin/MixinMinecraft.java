package net.mine_diver.optiforge.compat.newfrontiercraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.src.FontRenderer;
import net.minecraft.src.GameSettings;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.TexturePackBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @SuppressWarnings({"InvalidMemberReference", "UnresolvedMixinReference", "MixinAnnotationTarget", "InvalidInjectorMethodSignature"})
    @Redirect(
            method = "startGame()V",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/src/GameSettings;Lnet/minecraft/src/TexturePackBase;Lnet/minecraft/src/RenderEngine;)Lnet/minecraft/src/FontRenderer;"
            )
    )
    private FontRenderer redirectFontRendererConstructor(GameSettings var1, TexturePackBase var2, RenderEngine var3) {
        return new FontRenderer(var1, "/font/default.png", var3);
    }
}
