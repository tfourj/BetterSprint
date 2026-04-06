package me.tfourj.bettersprint.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import me.tfourj.bettersprint.Bettersprint;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.Locale;

public class BetterSprintConfig {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Bettersprint.MOD_ID, "config");
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("bettersprint.json5");

    public static final ConfigClassHandler<BetterSprintConfig> HANDLER =
            ConfigClassHandler.createBuilder(BetterSprintConfig.class)
                    .id(ID)
                    .serializer(handler -> GsonConfigSerializerBuilder.create(handler)
                            .setPath(CONFIG_PATH)
                            .setJson5(true)
                            .build())
                    .build();

    @SerialEntry(comment = "Whether sprint toggle should persist as enabled. Updated automatically when you use the toggle key.")
    public boolean sprintToggled = false;

    @SerialEntry(comment = "Show the on-screen indicator reflecting sprint toggle state.")
    public boolean showIndicator = true;

    @SerialEntry(comment = "Horizontal (0.0 - 1.0) position of the sprint indicator relative to the screen width.")
    public double indicatorX = 0.5D;

    @SerialEntry(comment = "Vertical (0.0 - 1.0) position of the sprint indicator relative to the screen height.")
    public double indicatorY = 0.85D;

    @SerialEntry(comment = "Scale multiplier applied to the indicator text.")
    public double indicatorScale = 1.0D;

    @SerialEntry(comment = "Render the indicator text with a shadow for improved readability.")
    public boolean indicatorShadow = true;

    @SerialEntry(comment = "Show a background behind the indicator text for better visibility.")
    public boolean indicatorBackground = false;

    @SerialEntry(comment = "Show a border around the indicator background.")
    public boolean indicatorBorder = false;

    @SerialEntry(comment = "Reduce FOV jitter when transitioning between sprinting and sneaking states.")
    public boolean reduceFovJitter = true;

    public static BetterSprintConfig get() {
        return HANDLER.instance();
    }

    public static void load() {
        if (!HANDLER.load()) {
            Bettersprint.LOGGER.warn("Failed to load BetterSprint config, falling back to defaults.");
            HANDLER.save();
        }
        clampHudValues();
    }

    public static void save() {
        clampHudValues();
        HANDLER.save();
    }

    private static void clampHudValues() {
        BetterSprintConfig config = get();
        config.indicatorX = clamp01(config.indicatorX);
        config.indicatorY = clamp01(config.indicatorY);
        config.indicatorScale = clampScale(config.indicatorScale);
    }

    private static double clamp01(double value) {
        if (value < 0.0D) return 0.0D;
        if (value > 1.0D) return 1.0D;
        return value;
    }

    private static double clampScale(double value) {
        if (value < 0.5D) return 0.5D;
        if (value > 3.0D) return 3.0D;
        return value;
    }

    public static Screen createScreen(Screen parent) {
        YetAnotherConfigLib configScreen = YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> builder
                .title(Component.translatable("config.bettersprint.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.bettersprint.category.general"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.bettersprint.option.show_indicator"))
                                .description(OptionDescription.of(Component.translatable("config.bettersprint.option.show_indicator.desc")))
                                .binding(defaults.showIndicator, () -> config.showIndicator, value -> config.showIndicator = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.bettersprint.option.indicator_x"))
                                .description(OptionDescription.of(Component.translatable("config.bettersprint.option.indicator_x.desc")))
                                .binding(defaults.indicatorX, () -> config.indicatorX, value -> config.indicatorX = clamp01(value))
                                .controller(option -> DoubleSliderControllerBuilder.create(option)
                                        .range(0.0D, 1.0D)
                                        .step(0.01D)
                                        .formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.0f%%", value * 100))))
                                .build())

                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.bettersprint.option.indicator_y"))
                                .description(OptionDescription.of(Component.translatable("config.bettersprint.option.indicator_y.desc")))
                                .binding(defaults.indicatorY, () -> config.indicatorY, value -> config.indicatorY = clamp01(value))
                                .controller(option -> DoubleSliderControllerBuilder.create(option)
                                        .range(0.0D, 1.0D)
                                        .step(0.01D)
                                        .formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.0f%%", value * 100))))
                                .build())

                        .option(Option.<Double>createBuilder()
                                .name(Component.translatable("config.bettersprint.option.indicator_scale"))
                                .description(OptionDescription.of(Component.translatable("config.bettersprint.option.indicator_scale.desc")))
                                .binding(defaults.indicatorScale, () -> config.indicatorScale, value -> config.indicatorScale = clampScale(value))
                                .controller(option -> DoubleSliderControllerBuilder.create(option)
                                        .range(0.5D, 3.0D)
                                        .step(0.05D)
                                        .formatValue(value -> Component.literal(String.format(Locale.ROOT, "x%.2f", value))))
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.bettersprint.option.indicator_shadow"))
                                .description(OptionDescription.of(Component.translatable("config.bettersprint.option.indicator_shadow.desc")))
                                .binding(defaults.indicatorShadow, () -> config.indicatorShadow, value -> config.indicatorShadow = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.bettersprint.option.indicator_background"))
                                .description(OptionDescription.of(Component.translatable("config.bettersprint.option.indicator_background.desc")))
                                .binding(defaults.indicatorBackground, () -> config.indicatorBackground, value -> config.indicatorBackground = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.bettersprint.option.indicator_border"))
                                .description(OptionDescription.of(Component.translatable("config.bettersprint.option.indicator_border.desc")))
                                .binding(defaults.indicatorBorder, () -> config.indicatorBorder, value -> config.indicatorBorder = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.bettersprint.option.reduce_fov_jitter"))
                                .description(OptionDescription.of(Component.translatable("config.bettersprint.option.reduce_fov_jitter.desc")))
                                .binding(defaults.reduceFovJitter, () -> config.reduceFovJitter, value -> config.reduceFovJitter = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .build()));

        return configScreen.generateScreen(parent);
    }
}