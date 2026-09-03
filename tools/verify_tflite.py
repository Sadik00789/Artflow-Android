import os
import glob
import numpy as np
import tensorflow as tf

# Suppress excessive logs
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"

def verify_model(model_path: str):
    basename = os.path.basename(model_path)
    interpreter = tf.lite.Interpreter(model_path=model_path)
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    in_shape = list(input_details[0]["shape"])
    if in_shape[1] is None or in_shape[1] <= 0:
        h, w = 256, 256
        interpreter.resize_tensor_input(0, [1, h, w, 3])
        interpreter.allocate_tensors()
    else:
        h, w = in_shape[1], in_shape[2]
        interpreter.allocate_tensors()

    sample_input = np.random.uniform(0.0, 255.0, size=(1, h, w, 3)).astype(np.float32)
    interpreter.set_tensor(input_details[0]["index"], sample_input)
    interpreter.invoke()

    out = interpreter.get_tensor(output_details[0]["index"])

    # Validation assertions
    assert not np.isnan(out).any(), f"NaN detected in {basename} output!"
    assert not np.isinf(out).any(), f"Inf detected in {basename} output!"

    if "fsrcnn" in basename:
        expected_shape = (1, h * 2, w * 2, 3)
        assert out.shape == expected_shape, f"FSRCNN shape mismatch: expected {expected_shape}, got {out.shape}"
        assert 0.0 <= np.min(out) and np.max(out) <= 255.0, f"Out-of-bounds in FSRCNN: min={np.min(out)}, max={np.max(out)}"
    elif "segmenter" in basename:
        expected_shape = (1, h, w, 1)
        assert out.shape == expected_shape, f"Segmenter shape mismatch: expected {expected_shape}, got {out.shape}"
        assert 0.0 <= np.min(out) and np.max(out) <= 1.0, f"Out-of-bounds in segmenter: min={np.min(out)}, max={np.max(out)}"
    else:
        assert h == 1024 and w == 1024, f"Expected static 1024x1024 input for {basename}, got {h}x{w}"
        expected_shape = (1, 1024, 1024, 3)
        assert out.shape == expected_shape, f"Style model shape mismatch: expected {expected_shape}, got {out.shape}"
        assert 0.0 <= np.min(out) and np.max(out) <= 255.0, f"Out-of-bounds in {basename}: min={np.min(out)}, max={np.max(out)}"

    return out.shape, float(np.min(out)), float(np.max(out))

def main():
    base_assets_dir = "app/src/main/assets/models"
    models = sorted(glob.glob(os.path.join(base_assets_dir, "**", "*.tflite"), recursive=True))
    
    print(f"Verifying {len(models)} models in {base_assets_dir}...")
    assert len(models) == 52, f"Expected 52 models, but found {len(models)}!"
    
    for i, model_path in enumerate(models, start=1):
        rel_path = os.path.relpath(model_path, base_assets_dir)
        shape, vmin, vmax = verify_model(model_path)
        print(f"[{i:02d}/52] PASSED {rel_path:40s} shape={str(shape):20s} range=[{vmin:.1f}, {vmax:.1f}]")
        
    print("\nALL 52 MODELS VERIFIED SUCCESSFULLY! Zero NaNs, strictly bounded outputs.")

if __name__ == "__main__":
    main()
