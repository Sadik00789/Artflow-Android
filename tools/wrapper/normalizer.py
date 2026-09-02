import torch
import torch.nn as nn

class UnifiedStyleWrapper(nn.Module):
    """
    Unified style wrapper enforcing the ArtFlow Graph Contract:
    - Accepts raw [0.0, 255.0] RGB floats in shape [1, H, W, 3].
    - Internally handles color-space transformations, normalization, and tensor transposition.
    - Outputs raw [0.0, 255.0] RGB floats in shape [1, H, W, 3].
    """
    def __init__(self, core_model: nn.Module, model_type: str = "fast_style"):
        super(UnifiedStyleWrapper, self).__init__()
        self.core_model = core_model
        self.model_type = model_type.lower()
        
        # ImageNet mean: [123.68, 116.78, 103.94] in RGB
        self.register_buffer(
            "imagenet_mean",
            torch.tensor([123.68, 116.78, 103.94], dtype=torch.float32).view(1, 3, 1, 1)
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # Input shape: [1, H, W, 3] in range [0.0, 255.0]
        # Permute to channels-first: [1, 3, H, W]
        x_ch_first = x.permute(0, 3, 1, 2)
        
        if self.model_type == "animegan":
            # Normalize to [-1.0, 1.0]
            x_norm = (x_ch_first / 127.5) - 1.0
            out = self.core_model(x_norm)
            # Post-process from [-1.0, 1.0] to [0.0, 255.0]
            out = (out + 1.0) * 127.5
        elif self.model_type == "whitebox":
            # Normalize to [-1.0, 1.0]
            x_norm = (x_ch_first / 127.5) - 1.0
            out = self.core_model(x_norm)
            # Post-process from [-1.0, 1.0] to [0.0, 255.0]
            out = (out + 1.0) * 127.5
        elif self.model_type == "fast_style":
            # Direct [0.0, 255.0] RGB input for Johnson et al. TransformerNet
            out = self.core_model(x_ch_first)
        else:
            # Default fallback: direct passthrough through core model
            out = self.core_model(x_ch_first)

        # Clamp output to valid RGB range [0.0, 255.0]
        out_clamped = torch.clamp(out, 0.0, 255.0)

        # Permute back to channels-last: [1, H, W, 3]
        return out_clamped.permute(0, 2, 3, 1)
