import os
import torch
import torch.nn as nn

# Model configurations
FINE_ART_STYLES = [
    "starry_night", "the_scream", "great_wave", "guernica",
    "monet_water_lilies", "kandinsky_composition", "klimt_the_kiss", "van_gogh_sunflowers",
    "cezanne_mont_sainte_victoire", "turner_rain_steam_speed", "hokusai_red_fuji", "degas_ballet",
    "renoir_boating_party", "munch_madonna", "picasso_weeping_woman", "matisse_dance",
    "gauguin_tahitian_women", "seurat_la_grande_jatte"
]

ANIME_STYLES = [
    "shinkai_sky", "ghibli_pastoral", "cyberpunk_neo_tokyo", "retro_80s_anime",
    "kyoto_bloom", "manga_ink_wash", "ufotable_digital", "chibi_pastel",
    "mecha_cel_shade", "lofi_chill", "fantasy_isekai", "shoujo_sparkle",
    "dark_fantasy_berserk", "vaporwave_sunset", "city_pop_1984", "trigger_action"
]

GRAPHIC_STYLES = [
    "bauhaus_geometry", "pop_art_warhol", "risograph_print", "synthwave_neon",
    "comic_halftone", "swiss_typographic", "art_deco_gold", "cyber_glitch",
    "blueprint_cyanotype", "vector_flat", "stencil_street_art", "linocut_print",
    "psychedelic_60s", "holographic_iridescent", "charcoal_sketch", "woodblock_ukiyoe"
]

class MockStyleNet(nn.Module):
    """
    Lightweight feed-forward style transfer network.
    Accepts channels-first [1, 3, H, W] and outputs [1, 3, H, W].
    """
    def __init__(self, seed: int = 42):
        super(MockStyleNet, self).__init__()
        torch.manual_seed(seed)
        self.conv1 = nn.Conv2d(3, 16, kernel_size=3, padding=1)
        self.relu = nn.ReLU(inplace=True)
        self.conv2 = nn.Conv2d(16, 16, kernel_size=3, padding=1)
        self.conv3 = nn.Conv2d(16, 3, kernel_size=3, padding=1)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        h = self.relu(self.conv1(x))
        h = self.relu(self.conv2(h))
        out = self.conv3(h)
        return out

class MockFSRCNN(nn.Module):
    """
    Lightweight FSRCNN x2 super-resolution network.
    Accepts [1, 3, H, W] and outputs [1, 3, 2H, 2W].
    """
    def __init__(self):
        super(MockFSRCNN, self).__init__()
        self.feature_extraction = nn.Sequential(
            nn.Conv2d(3, 16, kernel_size=3, padding=1),
            nn.PReLU()
        )
        self.mapping = nn.Sequential(
            nn.Conv2d(16, 16, kernel_size=3, padding=1),
            nn.PReLU()
        )
        self.deconv = nn.ConvTranspose2d(16, 3, kernel_size=4, stride=2, padding=1)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        h = self.feature_extraction(x)
        h = self.mapping(h)
        out = self.deconv(h)
        return out

class MockSelfieSegmenter(nn.Module):
    """
    Lightweight selfie segmenter producing 1-channel alpha mask [1, 1, H, W] in [0, 1].
    """
    def __init__(self):
        super(MockSelfieSegmenter, self).__init__()
        self.net = nn.Sequential(
            nn.Conv2d(3, 8, kernel_size=3, padding=1),
            nn.ReLU(inplace=True),
            nn.Conv2d(8, 1, kernel_size=3, padding=1),
            nn.Sigmoid()
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.net(x)

def save_all_checkpoints(output_dir: str = "tools/checkpoints"):
    os.makedirs(output_dir, exist_ok=True)
    
    # Save fine art checkpoints
    for idx, name in enumerate(FINE_ART_STYLES):
        net = MockStyleNet(seed=100 + idx)
        torch.save(net.state_dict(), os.path.join(output_dir, f"{name}.pth"))
        
    # Save anime checkpoints
    for idx, name in enumerate(ANIME_STYLES):
        net = MockStyleNet(seed=200 + idx)
        torch.save(net.state_dict(), os.path.join(output_dir, f"{name}.pth"))
        
    # Save graphic checkpoints
    for idx, name in enumerate(GRAPHIC_STYLES):
        net = MockStyleNet(seed=300 + idx)
        torch.save(net.state_dict(), os.path.join(output_dir, f"{name}.pth"))

    # Save vision models
    fsrcnn = MockFSRCNN()
    torch.save(fsrcnn.state_dict(), os.path.join(output_dir, "fsrcnn_x2.pth"))

    segmenter = MockSelfieSegmenter()
    torch.save(segmenter.state_dict(), os.path.join(output_dir, "selfie_segmenter.pth"))
    
    print(f"Successfully generated checkpoints in {output_dir}")

if __name__ == "__main__":
    save_all_checkpoints()
