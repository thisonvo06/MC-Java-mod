package snhyz.tutorialmod.net.mixin;

import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snhyz.tutorialmod.net.WolfEvolveMod;

@Mixin(Wolf.class)
public class WolfEvolveMixin {
	@Inject(at = @At("RETURN"), method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V")
	private void tutorialmod$saveEvolveData(ValueOutput output, CallbackInfo ci) {
		WolfEvolveMod.writeSaveData((Wolf) (Object) this, output);
	}

	@Inject(at = @At("RETURN"), method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V")
	private void tutorialmod$loadEvolveData(ValueInput input, CallbackInfo ci) {
		WolfEvolveMod.readSaveData((Wolf) (Object) this, input);
	}
}
