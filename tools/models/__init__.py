"""Real neural network architecture definitions for ArtFlow hero models."""
from .transformer_net import TransformerNet, load_transformer_net
from .animegan_generator import AnimeGANGenerator, load_animegan_generator

__all__ = [
    "TransformerNet",
    "load_transformer_net",
    "AnimeGANGenerator",
    "load_animegan_generator"
]
