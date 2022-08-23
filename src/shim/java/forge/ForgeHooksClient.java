package forge;

import net.minecraft.src.Block;
import net.minecraft.src.RenderBlocks;

public class ForgeHooksClient {

    public static void beforeRenderPass(int pass) {}
    public static void beforeBlockRender(Block block, RenderBlocks renderBlocks) {}
}
