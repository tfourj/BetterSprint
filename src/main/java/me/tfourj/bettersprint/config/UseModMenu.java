package me.tfourj.bettersprint.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.tfourj.bettersprint.config.BetterSprintConfig;

public class UseModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BetterSprintConfig::createScreen;
    }
}

