package net.mine_diver.optiforge.compat.newfrontiercraft.mixin.smooth;

import forge.ForgeHooksClient;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashSet;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.PUTSTATIC;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Shadow public boolean[] skipRenderPass;

    public boolean[] tempSkipRenderPass;

    @ModifyConstant(
            method = "<init>(Lnet/minecraft/src/World;Ljava/util/List;IIIII)V",
            constant = {
                    @Constant(intValue = 2, ordinal = 0),
                    @Constant(intValue = 2, ordinal = 2),
                    @Constant(intValue = 2, ordinal = 3),
                    @Constant(intValue = 2, ordinal = 4),
                    @Constant(intValue = 2, ordinal = 6),
                    @Constant(intValue = 2, ordinal = 7)
            }
    )
    private int fixNfcRenderPass1(int constant) {
        return 4;
    }

    @ModifyConstant(
            method = {
                    "finishUpdate()V",
                    "updateRenderer(J)Z",
                    "setDontDraw()V"
            },
            constant = @Constant(intValue = 2),
            remap = false
    )
    private int fixNfcRenderPass2(int constant) {
        return 4;
    }

    @Inject(
            method = "updateRenderer(J)Z",
            at = @At("TAIL"),
            remap = false
    )
    private void fixNfcRenderPass3(long finishTime, CallbackInfoReturnable<Boolean> cir) {
        skipRenderPass[2] = tempSkipRenderPass[2];
        skipRenderPass[3] = tempSkipRenderPass[3];
    }

    @ModifyConstant(
            method = "callOcclusionQueryList()V",
            constant = @Constant(intValue = 2)
    )
    private int fixNfcRenderPass4(int constant) {
        return 4;
    }

    @Inject(
            method = "skipAllRenderPasses()Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void fixNfcRenderPass5(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValueZ() && skipRenderPass[2] && skipRenderPass[3]);
    }

    @Redirect(
            method = "<clinit>()V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/src/WorldRenderer;tessellator:Lnet/minecraft/src/Tessellator;",
                    opcode = PUTSTATIC
            )
    )
    private static void redirectTessellatorSet(Tessellator value) {}

    @Redirect(
            method = "updateRenderer(J)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/src/WorldRenderer;tessellator:Lnet/minecraft/src/Tessellator;",
                    opcode = GETSTATIC
            )
    )
    private Tessellator redirectTessellator() {
        return Tessellator.instance;
    }

    @Inject(
            method = "updateRenderer(J)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/src/WorldRenderer;tessellator:Lnet/minecraft/src/Tessellator;",
                    opcode = GETSTATIC,
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectBeforeRenderPass1(
            long finishTime, CallbackInfoReturnable<Boolean> cir,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, ChunkCache chunkcache, RenderBlocks renderblocks, HashSet<TileEntity> setOldEntityRenders,
            int renderPass
    ) {
        ForgeHooksClient.beforeRenderPass(renderPass);
    }

    @Inject(
            method = "updateRenderer(J)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/src/RenderBlocks;renderBlockByRenderType(Lnet/minecraft/src/Block;III)Z",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectBeforeBlockRender(
            long finishTime, CallbackInfoReturnable<Boolean> cir,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, ChunkCache chunkcache,
            RenderBlocks renderblocks,
            HashSet<TileEntity> setOldEntityRenders, int renderPass, boolean flag, boolean hasRenderedBlocks, boolean hasGlList, int y, int z, int x, int i3,
            Block block
    ) {
        ForgeHooksClient.beforeBlockRender(block, renderblocks, chunkcache.getBlockMetadata(x, y, z));
    }

    @Inject(
            method = "updateRenderer(J)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/src/RenderBlocks;renderBlockByRenderType(Lnet/minecraft/src/Block;III)Z",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectAfterBlockRender(
            long finishTime, CallbackInfoReturnable<Boolean> cir,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, ChunkCache chunkcache,
            RenderBlocks renderblocks,
            HashSet<TileEntity> setOldEntityRenders, int renderPass, boolean flag, boolean hasRenderedBlocks, boolean hasGlList, int y, int z, int x, int i3,
            Block block
    ) {
        ForgeHooksClient.afterBlockRender(block, renderblocks, chunkcache.getBlockMetadata(x, y, z));
    }

    @Inject(
            method = "updateRenderer(J)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/src/WorldRenderer;tessellator:Lnet/minecraft/src/Tessellator;",
                    opcode = GETSTATIC,
                    ordinal = 2,
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectAfterBlockRenderPass1(
            long finishTime, CallbackInfoReturnable<Boolean> cir,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, ChunkCache chunkcache, RenderBlocks renderblocks, HashSet<TileEntity> setOldEntityRenders,
            int renderPass
    ) {
        ForgeHooksClient.afterRenderPass(renderPass);
    }

    @Inject(
            method = "updateRenderer(J)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/src/WorldRenderer;tessellator:Lnet/minecraft/src/Tessellator;",
                    opcode = GETSTATIC,
                    ordinal = 4,
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectBeforeRenderPass2(
            long finishTime, CallbackInfoReturnable<Boolean> cir,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, ChunkCache chunkcache, RenderBlocks renderblocks, HashSet<TileEntity> setOldEntityRenders,
            int renderPass
    ) {
        ForgeHooksClient.beforeRenderPass(renderPass);
    }

    @Inject(
            method = "updateRenderer(J)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/src/WorldRenderer;tessellator:Lnet/minecraft/src/Tessellator;",
                    opcode = GETSTATIC,
                    ordinal = 6,
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectAfterBlockRenderPass2(
            long finishTime, CallbackInfoReturnable<Boolean> cir,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, ChunkCache chunkcache, RenderBlocks renderblocks, HashSet<TileEntity> setOldEntityRenders,
            int renderPass
    ) {
        ForgeHooksClient.afterRenderPass(renderPass);
    }
}
