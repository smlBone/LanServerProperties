package rikka.lanserverproperties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;

/**
 * The extra widgets that this mod adds to the vanilla Multiplayer Options
 * (LAN) screen:
 * preference save/load, PvP toggle, online-mode/UUID options, player limit and
 * the always-offline player list.
 */
public class CommonWidgets {
	private final static Component preferenceEnabledLabel = Component.translatable("lanserverproperties.options.preference_enabled");
	private final static Component preferenceEnabledTooltip = Component.translatable("lanserverproperties.options.preference_enabled.message");
	private final static Component preferenceLoadLabel = Component.translatable("lanserverproperties.button.preference_load");
	private final static Component preferenceSaveLabel = Component.translatable("lanserverproperties.button.preference_save");
	private final static Component pvpAllowedLabel = Component.translatable("lanserverproperties.gui.pvp_allowed");
	private final static Component maxPlayerDescLabel = Component.translatable("lanserverproperties.gui.max_player");
	private final static Component alwaysOfflineLabel = Component.translatable("lanserverproperties.gui.always_offline");
	private final static Component alwaysOfflineDescLabel = Component.translatable("lanserverproperties.gui.always_offline.message");

	private final static Function<String, Boolean> maxPlayerValidator = IntegerEditBox.makeValidator(0, 16);

	private final List<AbstractWidget> widgets = new ArrayList<>();

	private final ConfigContainer configContainer;

	private Button savePreferenceButton;
	private Button loadPreferenceButton;
	private CycleButton<Boolean> enablePreferenceOption;
	private CycleButton<OnlineMode> onlineModeOption;
	private CycleButton<Boolean> pvpAllowedOption;
	private IntegerEditBox maxPlayerEditBox;
	private EditBox alwaysOfflinesEditBox;
	private SpriteIconButton alwaysOfflineToggle;

	private final LinearLayout section = LinearLayout.vertical().spacing(4);

	/**
	 * @param screen     the vanilla screen the widgets are added to
	 * @param config     the configuration container
	 * @param textRenderer the font
	 * @param onChanged  called whenever a mod option is changed by the player
	 */
	public CommonWidgets(Screen screen, ConfigContainer config, Font textRenderer, Runnable onChanged) {
		this.configContainer = config;

		// Row 1: save / enable preferences
		LinearLayout row1 = LinearLayout.horizontal().spacing(8);
		this.savePreferenceButton = Button.builder(preferenceSaveLabel, (btn) -> {
			configContainer.copyToPreferences();
			configContainer.preferences.save();
		}).bounds(0, 0, 150, 20).build();
		this.widgets.add(this.savePreferenceButton);
		row1.addChild(this.savePreferenceButton);

		this.enablePreferenceOption = CycleButton
			.onOffBuilder(configContainer.preferences.enablePreference)
			.withTooltip((curState) -> Tooltip.create(preferenceEnabledTooltip))
			.create(0, 0, 150, 20, preferenceEnabledLabel,
				(cycleButton, newVal) -> {
					configContainer.preferences.enablePreference = newVal;
					onChanged.run();
				}
			);
		this.widgets.add(this.enablePreferenceOption);
		row1.addChild(this.enablePreferenceOption);
		this.section.addChild(row1);

		// Row 2: load preferences / PvP allowed
		LinearLayout row2 = LinearLayout.horizontal().spacing(8);
		this.loadPreferenceButton = Button.builder(preferenceLoadLabel, (btn) -> {
			configContainer.loadFromPreferences(true);
			this.syncWidgetValues();
			onChanged.run();
		}).bounds(0, 0, 150, 20).build();
		this.widgets.add(this.loadPreferenceButton);
		row2.addChild(this.loadPreferenceButton);

		this.pvpAllowedOption = CycleButton
			.onOffBuilder(configContainer.pvpAllowed)
			.create(0, 0, 150, 20, pvpAllowedLabel,
				(cycleButton, newVal) -> {
					configContainer.pvpAllowed = newVal;
					onChanged.run();
				});
		this.widgets.add(this.pvpAllowedOption);
		row2.addChild(this.pvpAllowedOption);
		this.section.addChild(row2);

		// Row 3: online mode / max players
		LinearLayout row3 = LinearLayout.horizontal().spacing(8);
		this.onlineModeOption = new CycleButton.Builder<OnlineMode>((state) -> state.stateName, () -> configContainer.onlineMode)
			.withValues(OnlineMode.values())
			.withTooltip((curState) -> Tooltip.create(curState.tooltip))
			.displayOnlyValue()
			.create(0, 0, 150, 20, OnlineMode.translation,
				(cycleButton, newVal) -> {
					configContainer.onlineMode = newVal;
					onChanged.run();
				});
		this.widgets.add(this.onlineModeOption);
		row3.addChild(this.onlineModeOption);

		this.maxPlayerEditBox = new IntegerEditBox(textRenderer, 0, 0, 150, 20,
			maxPlayerDescLabel, configContainer.maxPlayer, (ieb) -> {
				if (ieb.isContentValid()) {
					configContainer.maxPlayer = ieb.getValueAsInt();
				}
				onChanged.run();
			}, maxPlayerValidator, null);
		this.maxPlayerEditBox.setHint(maxPlayerDescLabel);
		this.widgets.add(this.maxPlayerEditBox);
		row3.addChild(this.maxPlayerEditBox);
		this.section.addChild(row3);

		// Row 4: always-offline player list
		LinearLayout row4 = LinearLayout.horizontal().spacing(8);
		this.alwaysOfflineToggle = SpriteIconButton
			.builder(alwaysOfflineLabel, (button) -> alwaysOfflinesEditBox.visible ^= true, true)
			.width(20)
			.sprite(Identifier.tryParse("icon/accessibility"), 15, 15)
			.build();
		this.alwaysOfflineToggle.setTooltip(Tooltip.create(alwaysOfflineLabel));
		this.widgets.add(this.alwaysOfflineToggle);
		row4.addChild(this.alwaysOfflineToggle);

		this.alwaysOfflinesEditBox = new EditBox(textRenderer, 0, 0, 292, 20, alwaysOfflineLabel);
		this.alwaysOfflinesEditBox.setHint(alwaysOfflineLabel);
		this.alwaysOfflinesEditBox.setTooltip(Tooltip.create(alwaysOfflineDescLabel));
		this.alwaysOfflinesEditBox.setMaxLength(1024);
		this.alwaysOfflinesEditBox.setValue(configContainer.playersAlwaysOffline);
		this.alwaysOfflinesEditBox.setResponder((newValue) -> {
			configContainer.playersAlwaysOffline = newValue;
		});
		this.widgets.add(this.alwaysOfflinesEditBox);
		row4.addChild(this.alwaysOfflinesEditBox);
		this.section.addChild(row4);
	}

	/** The row container that should be appended to the vanilla layout. */
	public LinearLayout getSection() {
		return this.section;
	}

	/** Every leaf widget, to be registered with the screen. */
	public List<AbstractWidget> getWidgets() {
		return this.widgets;
	}

	/** Refresh the widgets from the configuration container. */
	public void syncWidgetValues() {
		enablePreferenceOption.setValue(this.configContainer.preferences.enablePreference);
		onlineModeOption.setValue(this.configContainer.onlineMode);
		pvpAllowedOption.setValue(this.configContainer.pvpAllowed);
		maxPlayerEditBox.setValue("" + this.configContainer.maxPlayer);
		alwaysOfflinesEditBox.setValue(this.configContainer.playersAlwaysOffline);
	}

	public boolean isMaxPlayerEditBoxValid() {
		return this.maxPlayerEditBox == null || this.maxPlayerEditBox.isContentValid();
	}

	///////////////////////////////////////////////////////////////////
	/// Widget finding (copied from the 1.21-era implementation)
	///////////////////////////////////////////////////////////////////
	@SuppressWarnings("unchecked")
	public static <T extends AbstractWidget> T findWidget(List<? extends GuiEventListener> list, Class<T> cls, String vanillaLangKey) {
		for (GuiEventListener child: list) {
			if (!(child instanceof AbstractWidget))
				continue;

			AbstractWidget widget = (AbstractWidget) child;
			// We only look for AbstractWidget
			if (cls.isAssignableFrom(widget.getClass())) {
				Component component = widget.getMessage();
				if (component.getContents() instanceof TranslatableContents) {
					TranslatableContents content = (TranslatableContents) component.getContents();
					if (content.getKey().equals(vanillaLangKey)) {
						return (T) widget;
					} else {
						Object[] args = content.getArgs();
						if (args.length == 0)
							continue;

						if (!(args[0] instanceof MutableComponent))
							continue;

						MutableComponent mutableComponent = (MutableComponent) args[0];
						if (!(mutableComponent.getContents() instanceof TranslatableContents))
							continue;

						content = (TranslatableContents) mutableComponent.getContents();
						if (content.getKey().equals(vanillaLangKey)) {
							return (T) widget;
						}
					}
				}
			}
		}

		return null;
	}

	/**
	 * Find the "Allow Commands" toggle button. Depending on the version it is
	 * labelled "selectWorld.allowCommands.new" or "selectWorld.allowCommands".
	 */
	@SuppressWarnings("unchecked")
	public static CycleButton<Boolean> findAllowCommandToggleButton(List<? extends GuiEventListener> list) {
		CycleButton<Boolean> allowCommandsSelector = findWidget(list, CycleButton.class, "selectWorld.allowCommands.new");
		return allowCommandsSelector == null ? findWidget(list, CycleButton.class, "selectWorld.allowCommands") : allowCommandsSelector;
	}
}
