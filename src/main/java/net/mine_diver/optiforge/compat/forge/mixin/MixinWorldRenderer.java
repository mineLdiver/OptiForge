package net.mine_diver.optiforge.compat.forge.mixin;

import forge.ForgeHooksClient;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashSet;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.PUTSTATIC;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

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

    @Inject(
            method = "updateRenderer()V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/src/WorldRenderer;tessellator:Lnet/minecraft/src/Tessellator;",
                    opcode = GETSTATIC,
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectBeforeRenderPass(
            CallbackInfo ci,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Object lightCache, HashSet<TileEntity> hashset, int one, ChunkCache chunkcache, RenderBlocks renderblocks,
            int renderPass
    ) {
        ForgeHooksClient.beforeRenderPass(renderPass);
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(
            method = "updateRenderer()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/src/RenderBlocks;renderBlockByRenderType(Lnet/minecraft/src/Block;III)Z",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectBeforeBlockRender(
            CallbackInfo ci,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Object lightCache, HashSet<TileEntity> hashset, int one, ChunkCache chunkcache,
            RenderBlocks renderblocks,
            int renderPass, int flag, int hasRenderedBlocks, int hasGlList, int y, int z, int x, int i3,
            Block block
    ) {
        ForgeHooksClient.beforeBlockRender(block, renderblocks);
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(
            method = "updateRenderer()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/src/RenderBlocks;renderBlockByRenderType(Lnet/minecraft/src/Block;III)Z",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectAfterBlockRender(
            CallbackInfo ci,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Object lightCache, HashSet<TileEntity> hashset, int one, ChunkCache chunkcache,
            RenderBlocks renderblocks,
            int renderPass, int flag, int hasRenderedBlocks, int hasGlList, int y, int z, int x, int i3,
            Block block
    ) {
        ForgeHooksClient.afterBlockRender(block, renderblocks);
    }

    @Inject(
            method = "updateRenderer()V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/src/WorldRenderer;tessellator:Lnet/minecraft/src/Tessellator;",
                    opcode = GETSTATIC,
                    ordinal = 2,
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectAfterBlockRenderPass(
            CallbackInfo ci,
            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Object lightCache, HashSet<TileEntity> hashset, int one, ChunkCache chunkcache, RenderBlocks renderblocks,
            int renderPass
    ) {
        ForgeHooksClient.afterRenderPass(renderPass);
    }
}
