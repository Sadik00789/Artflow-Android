import torch
import torch.nn as nn
from typing import Optional

def export_to_dynamic_onnx(
    model: nn.Module,
    output_path: str,
    dummy_input: Optional[torch.Tensor] = None,
    opset_version: int = 17
):
    """
    Exports a PyTorch model to ONNX with dynamic spatial axes:
    Input: [1, height, width, 3]
    Output: [1, height, width, 3]
    """
    model.eval()
    if dummy_input is None:
        dummy_input = torch.randn(1, 256, 256, 3, dtype=torch.float32)

    dynamic_axes = {
        "input": {1: "height", 2: "width"},
        "output": {1: "height", 2: "width"}
    }

    torch.onnx.export(
        model,
        dummy_input,
        output_path,
        export_params=True,
        opset_version=opset_version,
        do_constant_folding=True,
        input_names=["input"],
        output_names=["output"],
        dynamic_axes=dynamic_axes
    )
