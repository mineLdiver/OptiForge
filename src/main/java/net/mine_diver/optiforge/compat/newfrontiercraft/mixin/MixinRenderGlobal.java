package net.mine_diver.optiforge.compat.newfrontiercraft.mixin;

import net.minecraft.src.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @ModifyConstant(
            method = "<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/src/RenderEngine;)V",
            constant = @Constant(
                    intValue = 3,
                    ordinal = 0
            )
    )
    private int fixNfcRenderPass1(int constant) {
        return 5;
    }

    @ModifyVariable(
            method = "loadRenderers()V",
            index = 2,
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    shift = At.Shift.AFTER,
                    remap = false
            )
    )
    private int fixNfcRenderPass2(int value) {
        return value + 2;
    }
}
