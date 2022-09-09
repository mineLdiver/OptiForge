package forge;

import net.minecraft.src.Block;
import net.minecraft.src.RenderBlocks;

public class ForgeHooksClient {

    public static void beforeRenderPass(int pass) {}
    public static void beforeBlockRender(Block block, RenderBlocks renderBlocks) {}
    public static void afterBlockRender(Block block, RenderBlocks renderBlocks) {}
    public static void afterRenderPass(int pass) {}

    // NFC START
    public static void beforeBlockRender(Block block, RenderBlocks renderBlocks, int meta) {}
    public static void afterBlockRender(Block block, RenderBlocks renderBlocks, int meta) {}
    // NFC END
}
