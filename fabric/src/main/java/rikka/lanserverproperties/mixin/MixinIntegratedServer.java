package rikka.lanserverproperties.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.server.IntegratedServer;

import rikka.lanserverproperties.LanServerProperties;

/**
 * The vanilla 26.x integrated server always reports a player limit of 8
 * (IntegratedServer#getMaxPlayers). Let it report the limit configured by the
 * player instead.
 */
@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {
	@Inject(method = "getMaxPlayers", at = @At("HEAD"), cancellable = true)
	private void overrideGetMaxPlayers(CallbackInfoReturnable<Integer> cir) {
		int configured = LanServerProperties.getConfiguredMaxPlayers();
		if (configured >= 0) {
			cir.setReturnValue(configured);
		}
	}
}
