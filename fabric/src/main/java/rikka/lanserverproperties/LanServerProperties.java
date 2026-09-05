package rikka.lanserverproperties;

import net.minecraft.client.server.IntegratedServer;

public class LanServerProperties {
	/**
	 * The vanilla 26.x integrated server hard-codes its player limit to 8
	 * (IntegratedServer#getMaxPlayers). This mod therefore stores the value
	 * chosen by the player here, and MixinIntegratedServer makes the server
	 * report it.
	 */
	private static int configuredMaxPlayers = -1;

	/**
	 * Called by the configuration UI when the settings are applied.
	 */
	public static void setMaxPlayers(IntegratedServer server, int num) {
		configuredMaxPlayers = num;
	}

	/**
	 * @return the player limit requested by this mod, or -1 to keep vanilla behaviour
	 */
	public static int getConfiguredMaxPlayers() {
		return configuredMaxPlayers;
	}
}
