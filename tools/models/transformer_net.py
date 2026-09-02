import torch
import torch.nn as nn
from typing import Dict, Any

class ConvBlock(nn.Module):
    def __init__(self, in_channels: int, out_channels: int, kernel_size: int, stride: int):
        super().__init__()
        reflection_padding = kernel_size // 2
        self.reflection_pad = nn.ReflectionPad2d(reflection_padding)
        self.conv2d = nn.Conv2d(in_channels, out_channels, kernel_size, stride)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        out = self.reflection_pad(x)
        out = self.conv2d(out)
        return out

class ResidualBlock(nn.Module):
    """
    Residual block with reflection padding and instance normalization.
    """
    def __init__(self, channels: int):
        super().__init__()
        self.conv1 = ConvBlock(channels, channels, kernel_size=3, stride=1)
        self.in1 = nn.InstanceNorm2d(channels, affine=True)
        self.conv2 = ConvBlock(channels, channels, kernel_size=3, stride=1)
        self.in2 = nn.InstanceNorm2d(channels, affine=True)
        self.relu = nn.ReLU(inplace=True)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        residual = x
        out = self.relu(self.in1(self.conv1(x)))
        out = self.in2(self.conv2(out))
        out = out + residual
        return out

class UpsampleConvBlock(nn.Module):
    """
    Upsample layer using nearest interpolation followed by reflection padded convolution.
    """
    def __init__(self, in_channels: int, out_channels: int, kernel_size: int, stride: int, upsample: int = 2):
        super().__init__()
        self.upsample = upsample
        if upsample:
            self.upsample_layer = nn.Upsample(mode='nearest', scale_factor=upsample)
        reflection_padding = kernel_size // 2
        self.reflection_pad = nn.ReflectionPad2d(reflection_padding)
        self.conv2d = nn.Conv2d(in_channels, out_channels, kernel_size, stride)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x_in = x
        if self.upsample:
            x_in = self.upsample_layer(x_in)
        out = self.reflection_pad(x_in)
        out = self.conv2d(out)
        return out

class TransformerNet(nn.Module):
    """
    Johnson et al. Fast-Neural-Style Transformer network.
    Accepts [1, 3, H, W] in ImageNet-normalized space.
    """
    def __init__(self):
        super().__init__()
        # Initial 3 convolution layers
        self.conv1 = ConvBlock(3, 32, kernel_size=9, stride=1)
        self.in1 = nn.InstanceNorm2d(32, affine=True)
        self.conv2 = ConvBlock(32, 64, kernel_size=3, stride=2)
        self.in2 = nn.InstanceNorm2d(64, affine=True)
        self.conv3 = ConvBlock(64, 128, kernel_size=3, stride=2)
        self.in3 = nn.InstanceNorm2d(128, affine=True)

        # 5 Residual blocks
        self.res1 = ResidualBlock(128)
        self.res2 = ResidualBlock(128)
        self.res3 = ResidualBlock(128)
        self.res4 = ResidualBlock(128)
        self.res5 = ResidualBlock(128)

        # 2 Upsampling layers
        self.deconv1 = UpsampleConvBlock(128, 64, kernel_size=3, stride=1, upsample=2)
        self.in4 = nn.InstanceNorm2d(64, affine=True)
        self.deconv2 = UpsampleConvBlock(64, 32, kernel_size=3, stride=1, upsample=2)
        self.in5 = nn.InstanceNorm2d(32, affine=True)

        # Final projection
        self.deconv3 = ConvBlock(32, 3, kernel_size=9, stride=1)
        self.relu = nn.ReLU(inplace=True)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        y = self.relu(self.in1(self.conv1(x)))
        y = self.relu(self.in2(self.conv2(y)))
        y = self.relu(self.in3(self.conv3(y)))
        y = self.res1(y)
        y = self.res2(y)
        y = self.res3(y)
        y = self.res4(y)
        y = self.res5(y)
        y = self.relu(self.in4(self.deconv1(y)))
        y = self.relu(self.in5(self.deconv2(y)))
        y = self.deconv3(y)
        return y

def load_transformer_net(checkpoint_path: str) -> TransformerNet:
    """
    Instantiates TransformerNet and loads weights, handling module prefix and version variations.
    """
    net = TransformerNet()
    state_dict: Dict[str, Any] = torch.load(checkpoint_path, map_location="cpu")

    cleaned_dict = {}
    for k, v in state_dict.items():
        key = k.replace("module.", "")
        cleaned_dict[key] = v

    model_dict = net.state_dict()
    matching_dict = {
        k: v for k, v in cleaned_dict.items()
        if k in model_dict and v.shape == model_dict[k].shape
    }
    model_dict.update(matching_dict)
    net.load_state_dict(model_dict)
    net.eval()
    return net
