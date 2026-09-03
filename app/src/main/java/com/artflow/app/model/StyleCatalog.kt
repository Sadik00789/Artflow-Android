package com.artflow.app.model

import com.artflow.app.R

/**
 * Official catalog of 50 neural art styles bundled with ArtFlow.
 */
object StyleCatalog {

    val fineArtStyles = listOf(
        StylePreset("starry_night", "Starry Night", StyleCategory.FINE_ART, "models/fine_art/starry_night.tflite", R.drawable.ic_style_fine_art, 0xFF1C2A4A),
        StylePreset("the_scream", "The Scream", StyleCategory.FINE_ART, "models/fine_art/the_scream.tflite", R.drawable.ic_style_fine_art, 0xFFD8572A),
        StylePreset("great_wave", "The Great Wave", StyleCategory.FINE_ART, "models/fine_art/great_wave.tflite", R.drawable.ic_style_fine_art, 0xFF1A4568),
        StylePreset("guernica", "Guernica", StyleCategory.FINE_ART, "models/fine_art/guernica.tflite", R.drawable.ic_style_fine_art, 0xFF4A4A4A),
        StylePreset("monet_water_lilies", "Water Lilies", StyleCategory.FINE_ART, "models/fine_art/monet_water_lilies.tflite", R.drawable.ic_style_fine_art, 0xFF2E6B60),
        StylePreset("kandinsky_composition", "Composition VII", StyleCategory.FINE_ART, "models/fine_art/kandinsky_composition.tflite", R.drawable.ic_style_fine_art, 0xFFC93D2C),
        StylePreset("klimt_the_kiss", "The Kiss", StyleCategory.FINE_ART, "models/fine_art/klimt_the_kiss.tflite", R.drawable.ic_style_fine_art, 0xFFD4AF37),
        StylePreset("van_gogh_sunflowers", "Sunflowers", StyleCategory.FINE_ART, "models/fine_art/van_gogh_sunflowers.tflite", R.drawable.ic_style_fine_art, 0xFFE5A823),
        StylePreset("cezanne_mont_sainte_victoire", "Mont Sainte-Victoire", StyleCategory.FINE_ART, "models/fine_art/cezanne_mont_sainte_victoire.tflite", R.drawable.ic_style_fine_art, 0xFF5C6B47),
        StylePreset("turner_rain_steam_speed", "Rain Steam Speed", StyleCategory.FINE_ART, "models/fine_art/turner_rain_steam_speed.tflite", R.drawable.ic_style_fine_art, 0xFFBFA054),
        StylePreset("hokusai_red_fuji", "Red Fuji", StyleCategory.FINE_ART, "models/fine_art/hokusai_red_fuji.tflite", R.drawable.ic_style_fine_art, 0xFFB33927),
        StylePreset("degas_ballet", "Ballet Rehearsal", StyleCategory.FINE_ART, "models/fine_art/degas_ballet.tflite", R.drawable.ic_style_fine_art, 0xFF8A9A86),
        StylePreset("renoir_boating_party", "Boating Party", StyleCategory.FINE_ART, "models/fine_art/renoir_boating_party.tflite", R.drawable.ic_style_fine_art, 0xFFE07A5F),
        StylePreset("munch_madonna", "Madonna", StyleCategory.FINE_ART, "models/fine_art/munch_madonna.tflite", R.drawable.ic_style_fine_art, 0xFF6B2D2D),
        StylePreset("picasso_weeping_woman", "Weeping Woman", StyleCategory.FINE_ART, "models/fine_art/picasso_weeping_woman.tflite", R.drawable.ic_style_fine_art, 0xFF3D5A80),
        StylePreset("matisse_dance", "The Dance", StyleCategory.FINE_ART, "models/fine_art/matisse_dance.tflite", R.drawable.ic_style_fine_art, 0xFFE63946),
        StylePreset("gauguin_tahitian_women", "Tahitian Women", StyleCategory.FINE_ART, "models/fine_art/gauguin_tahitian_women.tflite", R.drawable.ic_style_fine_art, 0xFFD4723C),
        StylePreset("seurat_la_grande_jatte", "La Grande Jatte", StyleCategory.FINE_ART, "models/fine_art/seurat_la_grande_jatte.tflite", R.drawable.ic_style_fine_art, 0xFF437A58)
    )

    val animeStyles = listOf(
        StylePreset("shinkai_sky", "Shinkai Sky", StyleCategory.ANIME, "models/anime/shinkai_sky.tflite", R.drawable.ic_style_anime, 0xFF3A86FF),
        StylePreset("ghibli_pastoral", "Ghibli Pastoral", StyleCategory.ANIME, "models/anime/ghibli_pastoral.tflite", R.drawable.ic_style_anime, 0xFF52B788),
        StylePreset("cyberpunk_neo_tokyo", "Cyberpunk Neo-Tokyo", StyleCategory.ANIME, "models/anime/cyberpunk_neo_tokyo.tflite", R.drawable.ic_style_anime, 0xFFFF007F),
        StylePreset("retro_80s_anime", "Retro 80s Anime", StyleCategory.ANIME, "models/anime/retro_80s_anime.tflite", R.drawable.ic_style_anime, 0xFFFF70A6),
        StylePreset("kyoto_bloom", "Kyoto Bloom", StyleCategory.ANIME, "models/anime/kyoto_bloom.tflite", R.drawable.ic_style_anime, 0xFFFF99C8),
        StylePreset("manga_ink_wash", "Manga Ink Wash", StyleCategory.ANIME, "models/anime/manga_ink_wash.tflite", R.drawable.ic_style_anime, 0xFF2B2B2B),
        StylePreset("ufotable_digital", "Ufotable Sparks", StyleCategory.ANIME, "models/anime/ufotable_digital.tflite", R.drawable.ic_style_anime, 0xFFFF5400),
        StylePreset("chibi_pastel", "Chibi Pastel", StyleCategory.ANIME, "models/anime/chibi_pastel.tflite", R.drawable.ic_style_anime, 0xFFFEE440),
        StylePreset("mecha_cel_shade", "Mecha Cel-Shade", StyleCategory.ANIME, "models/anime/mecha_cel_shade.tflite", R.drawable.ic_style_anime, 0xFF00BBF9),
        StylePreset("lofi_chill", "Lo-Fi Beats", StyleCategory.ANIME, "models/anime/lofi_chill.tflite", R.drawable.ic_style_anime, 0xFF9B5DE5),
        StylePreset("fantasy_isekai", "Fantasy Isekai", StyleCategory.ANIME, "models/anime/fantasy_isekai.tflite", R.drawable.ic_style_anime, 0xFF00F5D4),
        StylePreset("shoujo_sparkle", "Shoujo Sparkle", StyleCategory.ANIME, "models/anime/shoujo_sparkle.tflite", R.drawable.ic_style_anime, 0xFFFFB3C6),
        StylePreset("dark_fantasy_berserk", "Dark Eclipse", StyleCategory.ANIME, "models/anime/dark_fantasy_berserk.tflite", R.drawable.ic_style_anime, 0xFF3F0008),
        StylePreset("vaporwave_sunset", "Vaporwave Sunset", StyleCategory.ANIME, "models/anime/vaporwave_sunset.tflite", R.drawable.ic_style_anime, 0xFFFF6B6B),
        StylePreset("city_pop_1984", "City Pop 1984", StyleCategory.ANIME, "models/anime/city_pop_1984.tflite", R.drawable.ic_style_anime, 0xFF48CAE4),
        StylePreset("trigger_action", "Trigger High-Action", StyleCategory.ANIME, "models/anime/trigger_action.tflite", R.drawable.ic_style_anime, 0xFFFF0054)
    )

    val graphicStyles = listOf(
        StylePreset("bauhaus_geometry", "Bauhaus Geometry", StyleCategory.GRAPHIC, "models/graphic/bauhaus_geometry.tflite", R.drawable.ic_style_graphic, 0xFFE63946),
        StylePreset("pop_art_warhol", "Pop Art Warhol", StyleCategory.GRAPHIC, "models/graphic/pop_art_warhol.tflite", R.drawable.ic_style_graphic, 0xFFFFBE0B),
        StylePreset("risograph_print", "Risograph Print", StyleCategory.GRAPHIC, "models/graphic/risograph_print.tflite", R.drawable.ic_style_graphic, 0xFFFB5607),
        StylePreset("synthwave_neon", "Synthwave Neon", StyleCategory.GRAPHIC, "models/graphic/synthwave_neon.tflite", R.drawable.ic_style_graphic, 0xFF8338EC),
        StylePreset("comic_halftone", "Comic Halftone", StyleCategory.GRAPHIC, "models/graphic/comic_halftone.tflite", R.drawable.ic_style_graphic, 0xFF3A86FF),
        StylePreset("swiss_typographic", "Swiss International", StyleCategory.GRAPHIC, "models/graphic/swiss_typographic.tflite", R.drawable.ic_style_graphic, 0xFFD00000),
        StylePreset("art_deco_gold", "Art Deco Gold", StyleCategory.GRAPHIC, "models/graphic/art_deco_gold.tflite", R.drawable.ic_style_graphic, 0xFFCCA43B),
        StylePreset("cyber_glitch", "Cyber Glitch", StyleCategory.GRAPHIC, "models/graphic/cyber_glitch.tflite", R.drawable.ic_style_graphic, 0xFF00F0FF),
        StylePreset("blueprint_cyanotype", "Cyanotype Blueprint", StyleCategory.GRAPHIC, "models/graphic/blueprint_cyanotype.tflite", R.drawable.ic_style_graphic, 0xFF0077B6),
        StylePreset("vector_flat", "Vector Flat", StyleCategory.GRAPHIC, "models/graphic/vector_flat.tflite", R.drawable.ic_style_graphic, 0xFF06D6A0),
        StylePreset("stencil_street_art", "Street Art Stencil", StyleCategory.GRAPHIC, "models/graphic/stencil_street_art.tflite", R.drawable.ic_style_graphic, 0xFF14213D),
        StylePreset("linocut_print", "Linocut Print", StyleCategory.GRAPHIC, "models/graphic/linocut_print.tflite", R.drawable.ic_style_graphic, 0xFF3D348B),
        StylePreset("psychedelic_60s", "Psychedelic 60s", StyleCategory.GRAPHIC, "models/graphic/psychedelic_60s.tflite", R.drawable.ic_style_graphic, 0xFFF72585),
        StylePreset("holographic_iridescent", "Holographic", StyleCategory.GRAPHIC, "models/graphic/holographic_iridescent.tflite", R.drawable.ic_style_graphic, 0xFF70D6FF),
        StylePreset("charcoal_sketch", "Charcoal Sketch", StyleCategory.GRAPHIC, "models/graphic/charcoal_sketch.tflite", R.drawable.ic_style_graphic, 0xFF333333),
        StylePreset("woodblock_ukiyoe", "Woodblock Print", StyleCategory.GRAPHIC, "models/graphic/woodblock_ukiyoe.tflite", R.drawable.ic_style_graphic, 0xFF9E2A2B)
    )

    val allStyles: List<StylePreset> = fineArtStyles + animeStyles + graphicStyles

    val defaultStyle: StylePreset = fineArtStyles.first()

    fun getStyleById(id: String): StylePreset {
        return allStyles.find { it.id == id } ?: defaultStyle
    }
}
