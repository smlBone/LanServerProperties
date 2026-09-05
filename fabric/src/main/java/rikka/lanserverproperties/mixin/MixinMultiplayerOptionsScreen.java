package rikka.lanserverproperties.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.MultiplayerOptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

import rikka.lanserverproperties.CommonWidgets;
import rikka.lanserverproperties.ConfigContainer;

/**
 * Since Minecraft 26.x the "Open to LAN" UI lives inside the vanilla
 * {@code MultiplayerOptionsScreen}. This mixin adds the LanServerProperties
 * controls to that screen and applies the mod settings when the vanilla
 * "Apply changes" button is pressed.
 */
@Mixin(MultiplayerOptionsScreen.class)
public abstract class MixinMultiplayerOptionsScreen extends Screen implements ConfigContainer.VanillaScreenData {
	protected MixinMultiplayerOptionsScreen(Component component) {
		super(component);
	}

	@Shadow
	@Final
	private HeaderAndFooterLayout layout;
	@Shadow
	@Final
	private Screen lastScreen;
	@Shadow
	private GameType gameMode;
	@Shadow
	private boolean commands;
	@Shadow
	private int port;
	@Shadow
	private EditBox portEdit;
	@Shadow
	private boolean portValid;
	@Shadow
	private Button applyChanges;

	@Unique
	private ConfigContainer configContainer;
	@Unique
	private CommonWidgets commonWidgets;
	@Unique
	private boolean lspDirty;

	@Unique
	private Minecraft getMC() {
		return Minecraft.getInstance();
	}

	///////////////////////////////////////////////////////////////////
	/// Vanilla screen hooks
	///////////////////////////////////////////////////////////////////

	@Inject(method = "init", at = @At("TAIL"))
	private void onInitTail(CallbackInfo ci) {
		IntegratedServer server = getMC().getSingleplayerServer();
		if (server == null)
			return;

		this.configContainer = new ConfigContainer.Vanilla(this);
		if (server.isPublished()) {
			// Editing the server that is currently open to LAN: reflect reality.
			this.configContainer.loadFromCurrentServer(server);
		} else {
			// About to publish: start from the preferences.
			this.configContainer.loadFromPreferences(false);
		}

		this.commonWidgets = new CommonWidgets((Screen)(Object)this, this.configContainer, this.font, this::lsp_markChanged);
		this.commonWidgets.syncWidgetValues();

		// The vanilla screen stacks everything into one vertical content column
		// (the only LinearLayout child of the layout's contents frame, visited
		// before the footer). Appending our section to that same column makes
		// the mod rows flow below the vanilla "other players" rows instead of
		// overlaying them (adding to layout.addToContents() would put the
		// section into a FrameLayout, where it is centered on top of the
		// vanilla content).
		LinearLayout contentColumn = this.findVanillaContentColumn();
		if (contentColumn != null) {
			contentColumn.addChild(this.commonWidgets.getSection());
		}

		for (AbstractWidget widget : this.commonWidgets.getWidgets()) {
			this.addRenderableWidget(widget);
		}

		this.syncVanillaControls();
		this.repositionElements();
	}

	/**
	 * Returns the vanilla vertical content column of the Multiplayer Options
	 * screen (the container holding the LAN toggle, the port row and the
	 * "other players" settings). The layout visits its header, contents and
	 * footer children in that order, so the first LinearLayout found is the
	 * content column (the header only holds a title string and the footer
	 * comes afterwards).
	 */
	@Unique
	private LinearLayout findVanillaContentColumn() {
		if (this.layout == null)
			return null;

		List<LayoutElement> children = new ArrayList<>();
		this.layout.visitChildren(children::add);
		for (LayoutElement element : children) {
			if (element instanceof LinearLayout) {
				return (LinearLayout) element;
			}
		}
		return null;
	}

	/**
	 * Called when the player presses the vanilla "Apply changes" button
	 * (i.e. the button that starts/stops/adjusts the published server).
	 */
	@Inject(method = "lambda$init$2", at = @At("TAIL"))
	private void onApplyChangesPressed(IntegratedServer server, Button button, CallbackInfo ci) {
		if (this.configContainer != null) {
			this.configContainer.applyToCurrentServer(server);
		}
		this.lspDirty = false;
	}

	/**
	 * Vanilla enables the apply button only when it detects its own changes.
	 * This mod also has to enable it when only the mod options were changed,
	 * and to keep it disabled while the player limit field is invalid.
	 */
	@Inject(method = "updateApplyChangesActiveState", at = @At("TAIL"))
	private void onUpdateApplyChangesActiveState(CallbackInfo ci) {
		if (this.applyChanges == null)
			return;

		boolean valid = this.commonWidgets == null || this.commonWidgets.isMaxPlayerEditBoxValid();
		boolean active = this.applyChanges.active;
		if (valid) {
			active = active || this.lspDirty;
		} else {
			active = false;
		}
		this.applyChanges.active = active;
	}

	/**
	 * When the port field is left blank vanilla asks the OS for a free port.
	 * Let the player's preferred port (see preferences) be used instead.
	 */
	@Redirect(method = "tryParsePort",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/HttpUtil;getAvailablePort()I"))
	private int redirect_tryParsePort() {
		if (this.configContainer != null) {
			int preferred = this.configContainer.preferences.defaultPort;
			if (preferred >= 1024 && preferred <= 65535) {
				return preferred;
			}
		}
		return HttpUtil.getAvailablePort();
	}

	@Unique
	private void lsp_markChanged() {
		this.lspDirty = true;
	}

	/**
	 * Push the currently configured game mode / commands / port values onto the
	 * vanilla widgets (used after loading the preferences).
	 */
	@Unique
	@SuppressWarnings("unchecked")
	private void syncVanillaControls() {
		if (this.configContainer == null || this.layout == null)
			return;

		CycleButton<GameType> gameModeSelector = CommonWidgets.findWidget(this.children(), CycleButton.class, "selectWorld.gameMode");
		if (gameModeSelector != null) {
			gameModeSelector.setValue(this.getGameMode());
		}

		CycleButton<Boolean> allowCommandsSelector = CommonWidgets.findAllowCommandToggleButton(this.children());
		if (allowCommandsSelector != null) {
			allowCommandsSelector.setValue(this.isCommandsEnabled());
		}
	}

	///////////////////////////////////////////////////////////////////
	/// ConfigContainer.VanillaScreenData
	///////////////////////////////////////////////////////////////////

	@Override
	public GameType getGameMode() {
		return this.gameMode;
	}

	@Override
	public void setGameMode(GameType gameMode) {
		this.gameMode = gameMode;
	}

	@Override
	public boolean isCommandsEnabled() {
		return this.commands;
	}

	@Override
	public void setCommandsEnabled(boolean commandsEnabled) {
		this.commands = commandsEnabled;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void setPort(int port) {
		this.port = port;
	}
}
