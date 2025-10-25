package me.tfourj.bettersprint.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.tfourj.bettersprint.config.BetterSprintConfig;

public class BettersprintModMenuPlugin implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BetterSprintConfig::createScreen;
    }
}
