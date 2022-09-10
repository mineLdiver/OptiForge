package net.mine_diver.optiforge.compat.newfrontiercraft.mixin;

import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityRenderer;
import net.minecraft.src.RenderGlobal;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(
            method = "renderWorld(FJ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V",
                    ordinal = 7,
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void fixNfcRenderPass1(float partialTicks, long n, CallbackInfo ci, EntityLiving entityliving, RenderGlobal renderglobal) {
        GL11.glShadeModel(7425);
        renderglobal.sortAndRender(entityliving, 2, partialTicks);
        renderglobal.sortAndRender(entityliving, 3, partialTicks);
        GL11.glShadeModel(7424);
    }
}
