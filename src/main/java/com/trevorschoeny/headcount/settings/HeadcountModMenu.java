package com.trevorschoeny.headcount.settings;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Headcount's settings screen in Mod Menu's list (and Mod Hub's, which reads the same entrypoint). */
public final class HeadcountModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HeadcountScreen::new;
    }
}
