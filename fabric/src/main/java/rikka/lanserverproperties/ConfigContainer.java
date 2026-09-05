package rikka.lanserverproperties;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;

public abstract class ConfigContainer {
	public Preferences preferences = Preferences.read();

	public boolean pvpAllowed;
	public OnlineMode onlineMode;
	public int maxPlayer;
	public String playersAlwaysOffline;

	protected abstract GameType getGuiGameType();
	protected abstract boolean getGuiCommandEnabled();
	protected abstract int getGuiPort();

	protected abstract void setGameType(GameType gameType);
	protected abstract void setCommandEnabled(boolean commandEnabled);
	protected abstract void setGuiPort(int port);

	/**
	 * Minecraft 26.x keeps the "allow PvP" setting as a per-level game rule.
	 */
	public static boolean isPvpAllowed(IntegratedServer server) {
		return server.overworld().getGameRules().get(GameRules.PVP);
	}

	public static void setPvpAllowed(IntegratedServer server, boolean allowed) {
		for (ServerLevel level : server.getAllLevels()) {
			level.getGameRules().set(GameRules.PVP, allowed, server);
		}
	}

	/**
	 * Reads the preferences. When {@code forceLoad} is false and the
	 * "enable preference" option is off, fresh defaults are used instead
	 * (except for the enablePreference flag itself).
	 */
	public void loadFromPreferences(boolean forceLoad) {
		preferences = Preferences.read();
		if (!forceLoad && !this.preferences.enablePreference) {
			boolean enablePreference = this.preferences.enablePreference;
			// Preference disabled, load default values except for enablePreference
			this.preferences = new Preferences();
			this.preferences.enablePreference = enablePreference;
		}

		this.setGameType(this.preferences.gameMode);
		this.setCommandEnabled(this.preferences.allowCheat);
		this.setGuiPort(this.preferences.defaultPort);

		this.onlineMode = OnlineMode.of(this.preferences.onlineMode, this.preferences.fixUUID);
		this.pvpAllowed = this.preferences.allowPVP;
		this.maxPlayer = this.preferences.maxPlayer;
		this.playersAlwaysOffline = Preferences.getAlwaysOfflineString(this.preferences.playersAlwaysOffline);
	}

	/**
	 * Reads the values of the currently running server. Used when the screen is
	 * opened for an already-published server, so that the UI reflects reality.
	 */
	public void loadFromCurrentServer(IntegratedServer server) {
		this.setGameType(server.getGameTypeForOtherPlayers());
		this.setCommandEnabled(server.commandsAllowedForOtherPlayers());
		this.setGuiPort(server.isPublished() ? server.getPort() : this.preferences.defaultPort);

		this.onlineMode = OnlineMode.of(server.usesAuthentication(), UUIDFixer.tryOnlineFirst);
		this.pvpAllowed = isPvpAllowed(server);

		int configured = LanServerProperties.getConfiguredMaxPlayers();
		this.maxPlayer = configured >= 0 ? configured : this.preferences.maxPlayer;
		this.playersAlwaysOffline = Preferences.getAlwaysOfflineString(UUIDFixer.alwaysOfflinePlayers);
	}

	public void copyToPreferences() {
		this.preferences.gameMode = this.getGuiGameType();
		this.preferences.allowCheat = this.getGuiCommandEnabled();
		this.preferences.defaultPort = this.getGuiPort();

		this.preferences.onlineMode = this.onlineMode.onlineModeEnabled;
		this.preferences.fixUUID = this.onlineMode.tryOnlineUUIDFirst;
		this.preferences.allowPVP = this.pvpAllowed;
		this.preferences.maxPlayer = this.maxPlayer;
		this.preferences.playersAlwaysOffline = Preferences.listOfAlwaysOffline(this.playersAlwaysOffline);
	}

	/**
	 * Applies the mod-only settings (PvP rule, player limit, online mode/UUID
	 * behaviour and the always-offline player list) to the running server.
	 * The vanilla "Multiplayer Options" screen applies the game mode, the
	 * "allow commands" flag and the port by itself.
	 */
	public void applyToCurrentServer(IntegratedServer server) {
		server.setUsesAuthentication(this.onlineMode.onlineModeEnabled);
		setPvpAllowed(server, this.pvpAllowed);
		UUIDFixer.tryOnlineFirst = this.onlineMode.tryOnlineUUIDFirst;
		UUIDFixer.alwaysOfflinePlayers = Preferences.listOfAlwaysOffline(this.playersAlwaysOffline);
		LanServerProperties.setMaxPlayers(server, this.maxPlayer);
	}

	/**
	 * A ConfigContainer that reads/writes the fields of the vanilla
	 * {@code MultiplayerOptionsScreen} (game mode, commands and port).
	 */
	public static class Vanilla extends ConfigContainer {
		private final VanillaScreenData screenData;

		public Vanilla(VanillaScreenData screenData) {
			this.screenData = screenData;
		}

		@Override
		protected GameType getGuiGameType() {
			return this.screenData.getGameMode();
		}

		@Override
		protected boolean getGuiCommandEnabled() {
			return this.screenData.isCommandsEnabled();
		}

		@Override
		protected int getGuiPort() {
			return this.screenData.getPort();
		}

		@Override
		protected void setGameType(GameType gameType) {
			this.screenData.setGameMode(gameType);
		}

		@Override
		protected void setCommandEnabled(boolean commandEnabled) {
			this.screenData.setCommandsEnabled(commandEnabled);
		}

		@Override
		protected void setGuiPort(int port) {
			this.screenData.setPort(port);
		}
	}

	/**
	 * Abstraction over the fields of the vanilla Multiplayer Options screen.
	 * Implemented by the mixin applied to that screen.
	 */
	public interface VanillaScreenData {
		GameType getGameMode();
		void setGameMode(GameType gameMode);
		boolean isCommandsEnabled();
		void setCommandsEnabled(boolean commandsEnabled);
		int getPort();
		void setPort(int port);
	}
}
