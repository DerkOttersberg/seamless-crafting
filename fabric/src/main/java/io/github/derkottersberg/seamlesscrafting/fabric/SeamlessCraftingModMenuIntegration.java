package io.github.derkottersberg.seamlesscrafting.fabric;

import com.derk.easyinventorycrafter.client.EasyInventoryCrafterConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class SeamlessCraftingModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return EasyInventoryCrafterConfigScreen::new;
    }
}
