package net.mine_diver.optiforge.compat.newfrontiercraft.mixin;

import net.minecraft.src.GameSettings;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.TexturePackList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.awt.image.BufferedImage;
import java.util.HashMap;

@Mixin(RenderEngine.class)
public class MixinRenderEngine {

    @SuppressWarnings("MixinAnnotationTarget")
    @Shadow(remap = false)
    private HashMap<Integer, Integer> textureSizeIdMap;

    @Inject(
            method = "<init>(Lnet/minecraft/src/TexturePackList;Lnet/minecraft/src/GameSettings;)V",
            at = @At("RETURN")
    )
    private void initNFC(TexturePackList texturepacklist, GameSettings gamesettings, CallbackInfo ci) {
        textureSizeIdMap = new HashMap<>();
    }

    @Inject(
            method = "setupTexture(Ljava/awt/image/BufferedImage;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/awt/image/BufferedImage;getWidth()I",
                    ordinal = 1,
                    shift = At.Shift.BY,
                    by = 2
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void setSize(BufferedImage bufferedimage, int i, CallbackInfo ci, int width) {
        textureSizeIdMap.put(i, width);
    }
}
