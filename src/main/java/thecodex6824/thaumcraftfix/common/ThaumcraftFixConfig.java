/**
 *  Thaumcraft Fix
 *  Copyright (c) 2024 TheCodex6824.
 *
 *  This file is part of Thaumcraft Fix.
 *
 *  Thaumcraft Fix is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Thaumcraft Fix is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Thaumcraft Fix.  If not, see <https://www.gnu.org/licenses/>.
 */

package thecodex6824.thaumcraftfix.common;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thecodex6824.thaumcraftfix.api.ThaumcraftFixApi;
import thecodex6824.thaumcraftfix.api.internal.ThaumcraftFixApiBridge;

@Config(modid = ThaumcraftFixApi.MODID)
@EventBusSubscriber(modid = ThaumcraftFixApi.MODID)
public final class ThaumcraftFixConfig {

    private static final String ALLOW_LIST_1 = "Determines if the list of objects specifies things that are allowed to have this feature,";
    private static final String ALLOW_LIST_2 = "instead of listing those that cannot.";

    private static final String BIOME_LIST_NAME = "biomeList";
    private static final String BIOME_LIST_LANG = ThaumcraftFixApi.MODID + ".text.config.biomeList";
    private static final String BIOME_LIST_ALLOW_NAME = "biomeListIsAllowList";
    private static final String BIOME_LIST_ALLOW_LANG = ThaumcraftFixApi.MODID + ".text.config.biomeListIsAllowList";
    private static final String BIOME_LIST_1 = "The list of biomes that will or will not have this feature enabled.";
    private static final String BIOME_LIST_2 = "Defaults to specifying the biomes to block, set BiomeListIsAllowList to true to invert behavior.";
    private static final String BIOME_LIST_3 = "Enter the biomes as namespaced IDs, i.e. thaumcraft:magical_forest";

    private static final String DIM_LIST_NAME = "dimList";
    private static final String DIM_LIST_LANG = ThaumcraftFixApi.MODID + ".text.config.dimList";
    private static final String DIM_LIST_ALLOW_NAME = "dimListIsAllowList";
    private static final String DIM_LIST_ALLOW_LANG = ThaumcraftFixApi.MODID + ".text.config.dimListIsAllowList";
    private static final String DIM_LIST_1 = "The list of dimensions that will or will not have this feature enabled.";
    private static final String DIM_LIST_2 = "Defaults to specifying the biomes to block, set DimListIsAllowList to true to invert behavior.";
    private static final String DIM_LIST_3 = "Use the registered dimension type name - if unsure, use \"/forge dimensions list\" ingame to see them all.";

    private ThaumcraftFixConfig() {}

    public static class WorldConfig {

	public static class AuraConfig {
	    @Name("controlAura")
	    @LangKey(ThaumcraftFixApi.MODID + ".text.config.controlAura")
	    @Comment({
		"Whether controlling Aura generation is enabled."
	    })
	    public boolean controlAura = false;

	    // the forge config system can't handle inheritance so these all have to be copied :(
	    @Name(BIOME_LIST_ALLOW_NAME)
	    @LangKey(BIOME_LIST_ALLOW_LANG)
	    @Comment({ ALLOW_LIST_1, ALLOW_LIST_2 })
	    public boolean biomeAllowList = false;

	    @Name(BIOME_LIST_NAME)
	    @LangKey(BIOME_LIST_LANG)
	    @Comment({ BIOME_LIST_1, BIOME_LIST_2, BIOME_LIST_3 })
	    public String[] biomeList = {};

	    @Name(DIM_LIST_ALLOW_NAME)
	    @LangKey(DIM_LIST_ALLOW_LANG)
	    @Comment({ ALLOW_LIST_1, ALLOW_LIST_2 })
	    public boolean dimAllowList = false;

	    @Name(DIM_LIST_NAME)
	    @LangKey(DIM_LIST_LANG)
	    @Comment({ DIM_LIST_1, DIM_LIST_2, DIM_LIST_3 })
	    public String[] dimList = {};
	}

	public static class CrystalsConfig {
	    @Name("controlCrystals")
	    @LangKey(ThaumcraftFixApi.MODID + ".text.config.controlCrystals")
	    @Comment({
		"Whether controlling Vis Crystal generation is enabled."
	    })
	    public boolean controlCrystals = false;

	    @Name(BIOME_LIST_ALLOW_NAME)
	    @LangKey(BIOME_LIST_ALLOW_LANG)
	    @Comment({ ALLOW_LIST_1, ALLOW_LIST_2 })
	    public boolean biomeAllowList = false;

	    @Name(BIOME_LIST_NAME)
	    @LangKey(BIOME_LIST_LANG)
	    @Comment({ BIOME_LIST_1, BIOME_LIST_2, BIOME_LIST_3 })
	    public String[] biomeList = {};

	    @Name(DIM_LIST_ALLOW_NAME)
	    @LangKey(DIM_LIST_ALLOW_LANG)
	    @Comment({ ALLOW_LIST_1, ALLOW_LIST_2 })
	    public boolean dimAllowList = false;

	    @Name(DIM_LIST_NAME)
	    @LangKey(DIM_LIST_LANG)
	    @Comment({ DIM_LIST_1, DIM_LIST_2, DIM_LIST_3 })
	    public String[] dimList = {};
	}

	public static class VegetationConfig {
	    @Name("controlVegetation")
	    @LangKey(ThaumcraftFixApi.MODID + ".text.config.controlVegetation")
	    @Comment({
		"Whether controlling Thaumcraft vegetation generation is enabled."
	    })
	    public boolean controlVegetation = false;

	    @Name(BIOME_LIST_ALLOW_NAME)
	    @LangKey(BIOME_LIST_ALLOW_LANG)
	    @Comment({ ALLOW_LIST_1, ALLOW_LIST_2 })
	    public boolean biomeAllowList = false;

	    @Name(BIOME_LIST_NAME)
	    @LangKey(BIOME_LIST_LANG)
	    @Comment({ BIOME_LIST_1, BIOME_LIST_2, BIOME_LIST_3 })
	    public String[] biomeList = {};

	    @Name(DIM_LIST_ALLOW_NAME)
	    @LangKey(DIM_LIST_ALLOW_LANG)
	    @Comment({ ALLOW_LIST_1, ALLOW_LIST_2 })
	    public boolean dimAllowList = false;

	    @Name(DIM_LIST_NAME)
	    @LangKey(DIM_LIST_LANG)
	    @Comment({ DIM_LIST_1, DIM_LIST_2, DIM_LIST_3 })
	    public String[] dimList = {};
	}

	@Name("aura")
	@LangKey(ThaumcraftFixApi.MODID + ".text.config.aura")
	@Comment("Aura generation options")
	public AuraConfig aura = new AuraConfig();
	@Name("crystals")
	@LangKey(ThaumcraftFixApi.MODID + ".text.config.crystals")
	@Comment("Thaumcraft crystal generation options")
	public CrystalsConfig crystals = new CrystalsConfig();
	@Name("vegetation")
	@LangKey(ThaumcraftFixApi.MODID + ".text.config.vegetation")
	@Comment("Thaumcraft vegetation generation options")
	public VegetationConfig vegetation = new VegetationConfig();

    }

    @Name("world")
    @LangKey(ThaumcraftFixApi.MODID + ".text.config.world")
    public static WorldConfig world = new WorldConfig();

    @SubscribeEvent
    public static void onConfigSync(OnConfigChangedEvent event) {
	if (event.getModID().equals(ThaumcraftFixApi.MODID)) {
	    ConfigManager.sync(ThaumcraftFixApi.MODID, Type.INSTANCE);
	    ThaumcraftFixApiBridge.implementation().reloadConfig();
	}
    }

}
