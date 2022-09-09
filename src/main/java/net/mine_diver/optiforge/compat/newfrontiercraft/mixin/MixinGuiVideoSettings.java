package net.mine_diver.optiforge.compat.newfrontiercraft.mixin;

import com.google.common.collect.ObjectArrays;
import net.minecraft.src.EnumOptions;
import net.minecraft.src.GuiVideoSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiVideoSettings.class)
public class MixinGuiVideoSettings {

    @Shadow private static EnumOptions[] field_22108_k;

    @Inject(
            method = "<clinit>()V",
            at = @At("RETURN")
    )
    private static void addNfcOptions(CallbackInfo ci) {
        field_22108_k = ObjectArrays.concat(
                field_22108_k,
                new EnumOptions[] {
                        EnumOptions.valueOf("FANCY_ITEMS"),
                        EnumOptions.valueOf("ADVENTURE_TREES"),
                        EnumOptions.valueOf("CLOUD_HEIGHT"),
                        EnumOptions.valueOf("ITEM_TOOLTIPS")
                },
                EnumOptions.class
        );
    }
}
