import os
import torch
import numpy as np
import tensorflow as tf
from typing import Optional, Tuple, Union

from tools.models.transformer_net import load_transformer_net
from tools.models.animegan_generator import load_animegan_generator

class TransformerNetTF(tf.Module):
    """
    TensorFlow implementation of Johnson et al. Fast-Neural-Style TransformerNet.
    Conforms to ArtFlow Graph Contract:
    Input: [1, H, W, 3] float32 in [0.0, 255.0]
    Output: [1, H, W, 3] float32 in [0.0, 255.0]
    """
    def __init__(self, pt_checkpoint_path: str):
        super().__init__()
        net = load_transformer_net(pt_checkpoint_path)
        sd = net.state_dict()

        def get_conv(name, k_size, in_c, out_c):
            w = sd[f"{name}.conv2d.weight"].numpy() # [out, in, kH, kW]
            b = sd[f"{name}.conv2d.bias"].numpy()   # [out]
            w_tf = np.transpose(w, (2, 3, 1, 0))    # [kH, kW, in, out]
            return tf.Variable(w_tf, trainable=False, dtype=tf.float32), tf.Variable(b, trainable=False, dtype=tf.float32)

        def get_in(name, c):
            gamma = sd[f"{name}.weight"].numpy()
            beta = sd[f"{name}.bias"].numpy()
            return tf.Variable(gamma, trainable=False, dtype=tf.float32), tf.Variable(beta, trainable=False, dtype=tf.float32)

        # conv1, in1 (k=9, s=1, in=3, out=32)
        self.w_c1, self.b_c1 = get_conv("conv1", 9, 3, 32)
        self.g_in1, self.b_in1 = get_in("in1", 32)

        # conv2, in2 (k=3, s=2, in=32, out=64)
        self.w_c2, self.b_c2 = get_conv("conv2", 3, 32, 64)
        self.g_in2, self.b_in2 = get_in("in2", 64)

        # conv3, in3 (k=3, s=2, in=64, out=128)
        self.w_c3, self.b_c3 = get_conv("conv3", 3, 64, 128)
        self.g_in3, self.b_in3 = get_in("in3", 128)

        # 5 Residual Blocks (res1 to res5, c=128)
        self.res_weights = []
        for i in range(1, 6):
            w1, b1 = get_conv(f"res{i}.conv1", 3, 128, 128)
            g1, bet1 = get_in(f"res{i}.in1", 128)
            w2, b2 = get_conv(f"res{i}.conv2", 3, 128, 128)
            g2, bet2 = get_in(f"res{i}.in2", 128)
            self.res_weights.append((w1, b1, g1, bet1, w2, b2, g2, bet2))

        # deconv1, in4 (upsample 2x, k=3, s=1, in=128, out=64)
        self.w_dc1, self.b_dc1 = get_conv("deconv1", 3, 128, 64)
        self.g_in4, self.b_in4 = get_in("in4", 64)

        # deconv2, in5 (upsample 2x, k=3, s=1, in=64, out=32)
        self.w_dc2, self.b_dc2 = get_conv("deconv2", 3, 64, 32)
        self.g_in5, self.b_in5 = get_in("in5", 32)

        # deconv3 (k=9, s=1, in=32, out=3)
        self.w_dc3, self.b_dc3 = get_conv("deconv3", 9, 32, 3)

    def _instance_norm(self, x, gamma, beta):
        mean, var = tf.nn.moments(x, axes=[1, 2], keepdims=True)
        return ((x - mean) / tf.sqrt(var + 1e-5)) * gamma + beta

    def _conv_block(self, x, w, b, pad, stride=1):
        x_pad = tf.pad(x, [[0, 0], [pad, pad], [pad, pad], [0, 0]], mode="REFLECT")
        return tf.nn.conv2d(x_pad, w, strides=[1, stride, stride, 1], padding="VALID") + b

    def _upsample(self, x, scale=2):
        in_shape = tf.shape(x)
        out_h = in_shape[1] * scale
        out_w = in_shape[2] * scale
        return tf.image.resize(x, [out_h, out_w], method="nearest")

    @tf.function(input_signature=[tf.TensorSpec(shape=[1, 768, 768, 3], dtype=tf.float32, name="input")])
    def __call__(self, x):
        # Initial 3 conv blocks
        y = self._conv_block(x, self.w_c1, self.b_c1, pad=4, stride=1)
        y = tf.nn.relu(self._instance_norm(y, self.g_in1, self.b_in1))

        y = self._conv_block(y, self.w_c2, self.b_c2, pad=1, stride=2)
        y = tf.nn.relu(self._instance_norm(y, self.g_in2, self.b_in2))

        y = self._conv_block(y, self.w_c3, self.b_c3, pad=1, stride=2)
        y = tf.nn.relu(self._instance_norm(y, self.g_in3, self.b_in3))

        # 5 Residual Blocks
        for (w1, b1, g1, bet1, w2, b2, g2, bet2) in self.res_weights:
            res = y
            out = self._conv_block(y, w1, b1, pad=1, stride=1)
            out = tf.nn.relu(self._instance_norm(out, g1, bet1))
            out = self._conv_block(out, w2, b2, pad=1, stride=1)
            out = self._instance_norm(out, g2, bet2)
            y = res + out

        # 2 Upsample blocks
        y = self._upsample(y, scale=2)
        y = self._conv_block(y, self.w_dc1, self.b_dc1, pad=1, stride=1)
        y = tf.nn.relu(self._instance_norm(y, self.g_in4, self.b_in4))

        y = self._upsample(y, scale=2)
        y = self._conv_block(y, self.w_dc2, self.b_dc2, pad=1, stride=1)
        y = tf.nn.relu(self._instance_norm(y, self.g_in5, self.b_in5))

        # Final projection
        y = self._conv_block(y, self.w_dc3, self.b_dc3, pad=4, stride=1)

        # Output clamping to [0.0, 255.0]
        return tf.clip_by_value(y, 0.0, 255.0, name="output")


class AnimeGANGeneratorTF(tf.Module):
    """
    TensorFlow implementation of AnimeGANv2 Generator (Bryan D. Lee / TachibanaYoshino).
    Conforms to ArtFlow Graph Contract:
    Input: [1, 768, 768, 3] float32 in [0.0, 255.0]
    Output: [1, 768, 768, 3] float32 in [0.0, 255.0]
    """
    def __init__(self, pt_checkpoint_path: str):
        super().__init__()
        gen = load_animegan_generator(pt_checkpoint_path)
        sd = gen.state_dict()

        def conv_norm_lrelu_vars(prefix, is_dw=False):
            w = sd[f"{prefix}.1.weight"].numpy()
            b = sd[f"{prefix}.1.bias"].numpy() if f"{prefix}.1.bias" in sd else None
            g = sd[f"{prefix}.2.weight"].numpy()
            bet = sd[f"{prefix}.2.bias"].numpy()
            if is_dw:
                w_tf = np.transpose(w, (2, 3, 0, 1)) # [kH, kW, in, 1]
            else:
                w_tf = np.transpose(w, (2, 3, 1, 0)) # [kH, kW, in, out]
            w_var = tf.Variable(w_tf, trainable=False, dtype=tf.float32)
            b_var = tf.Variable(b, trainable=False, dtype=tf.float32) if b is not None else None
            g_var = tf.Variable(g, trainable=False, dtype=tf.float32)
            bet_var = tf.Variable(bet, trainable=False, dtype=tf.float32)
            return w_var, b_var, g_var, bet_var

        # block_a
        # 0: ConvNormLReLU(3, 32, k=7, p=3)
        self.ba0 = conv_norm_lrelu_vars("block_a.0")
        # 1: ConvNormLReLU(32, 64, k=3, s=2, p=(0,1,0,1))
        self.ba1 = conv_norm_lrelu_vars("block_a.1")
        # 2: ConvNormLReLU(64, 64, k=3, p=1)
        self.ba2 = conv_norm_lrelu_vars("block_a.2")

        # block_b
        # 0: ConvNormLReLU(64, 128, k=3, s=2, p=(0,1,0,1))
        self.bb0 = conv_norm_lrelu_vars("block_b.0")
        # 1: ConvNormLReLU(128, 128, k=3, p=1)
        self.bb1 = conv_norm_lrelu_vars("block_b.1")

        # block_c
        # 0: ConvNormLReLU(128, 128)
        self.bc0 = conv_norm_lrelu_vars("block_c.0")

        # 1 to 4: InvertedResBlocks
        self.inv_blocks = []
        for i in range(1, 5):
            prefix = f"block_c.{i}.layers"
            # 0: 1x1 conv
            l0 = conv_norm_lrelu_vars(f"{prefix}.0")
            # 1: dw conv
            l1 = conv_norm_lrelu_vars(f"{prefix}.1", is_dw=True)
            # 2 & 3: pw conv + groupnorm (no act)
            w_pw = np.transpose(sd[f"{prefix}.2.weight"].numpy(), (2, 3, 1, 0))
            g_pw = sd[f"{prefix}.3.weight"].numpy()
            b_pw = sd[f"{prefix}.3.bias"].numpy()
            l2_3 = (
                tf.Variable(w_pw, trainable=False, dtype=tf.float32),
                tf.Variable(g_pw, trainable=False, dtype=tf.float32),
                tf.Variable(b_pw, trainable=False, dtype=tf.float32)
            )
            self.inv_blocks.append((l0, l1, l2_3))

        # 5: ConvNormLReLU(256, 128)
        self.bc5 = conv_norm_lrelu_vars("block_c.5")

        # block_d
        self.bd0 = conv_norm_lrelu_vars("block_d.0")
        self.bd1 = conv_norm_lrelu_vars("block_d.1")

        # block_e
        self.be0 = conv_norm_lrelu_vars("block_e.0")
        self.be1 = conv_norm_lrelu_vars("block_e.1")
        self.be2 = conv_norm_lrelu_vars("block_e.2")

        # out_layer: Conv2d(32, 3, 1) + Tanh
        w_out = np.transpose(sd["out_layer.0.weight"].numpy(), (2, 3, 1, 0))
        self.w_out = tf.Variable(w_out, trainable=False, dtype=tf.float32)

    def _conv_norm_lrelu(self, x, params, pad_val, stride=1, is_dw=False):
        w, b, g, bet = params
        if isinstance(pad_val, tuple):
            pad_top, pad_bottom, pad_left, pad_right = pad_val
            paddings = [[0, 0], [pad_top, pad_bottom], [pad_left, pad_right], [0, 0]]
        else:
            paddings = [[0, 0], [pad_val, pad_val], [pad_val, pad_val], [0, 0]]
        if pad_val != 0 and pad_val != (0, 0, 0, 0):
            x = tf.pad(x, paddings, mode="REFLECT")

        if is_dw:
            y = tf.nn.depthwise_conv2d(x, w, strides=[1, stride, stride, 1], padding="VALID")
        else:
            y = tf.nn.conv2d(x, w, strides=[1, stride, stride, 1], padding="VALID")
        if b is not None:
            y = y + b

        mean, var = tf.nn.moments(y, axes=[1, 2, 3], keepdims=True)
        y_norm = ((y - mean) / tf.sqrt(var + 1e-5)) * g + bet
        return tf.nn.leaky_relu(y_norm, alpha=0.2)

    def _run_inverted_res(self, x, inv_tuple, in_ch, out_ch):
        l0, l1, l2_3 = inv_tuple
        res = x
        # 1x1 conv
        out = self._conv_norm_lrelu(x, l0, pad_val=0, stride=1, is_dw=False)
        # dw conv
        out = self._conv_norm_lrelu(out, l1, pad_val=1, stride=1, is_dw=True)
        # pw conv + gn
        w_pw, g_pw, b_pw = l2_3
        out = tf.nn.conv2d(out, w_pw, strides=[1, 1, 1, 1], padding="VALID")
        mean, var = tf.nn.moments(out, axes=[1, 2, 3], keepdims=True)
        out = ((out - mean) / tf.sqrt(var + 1e-5)) * g_pw + b_pw
        if in_ch == out_ch:
            out = res + out
        return out

    def _upsample_bilinear(self, x, scale=2):
        in_shape = tf.shape(x)
        out_h = in_shape[1] * scale
        out_w = in_shape[2] * scale
        return tf.image.resize(x, [out_h, out_w], method="bilinear")

    @tf.function(input_signature=[tf.TensorSpec(shape=[1, 768, 768, 3], dtype=tf.float32, name="input")])
    def __call__(self, x):
        # Normalize [0.0, 255.0] to [-1.0, 1.0]
        x_norm = (x / 127.5) - 1.0

        # RGB to BGR channel reversal as AnimeGANv2 expects BGR
        x_bgr = tf.reverse(x_norm, axis=[-1])

        # block_a
        y = self._conv_norm_lrelu(x_bgr, self.ba0, pad_val=3, stride=1)
        # padding=(0, 1, 0, 1) in PyTorch is: left=0, right=1, top=0, bottom=1
        y = self._conv_norm_lrelu(y, self.ba1, pad_val=(0, 1, 0, 1), stride=2)
        y = self._conv_norm_lrelu(y, self.ba2, pad_val=1, stride=1)

        # block_b
        y = self._conv_norm_lrelu(y, self.bb0, pad_val=(0, 1, 0, 1), stride=2)
        y = self._conv_norm_lrelu(y, self.bb1, pad_val=1, stride=1)

        # block_c
        y = self._conv_norm_lrelu(y, self.bc0, pad_val=1, stride=1)
        y = self._run_inverted_res(y, self.inv_blocks[0], 128, 256)
        y = self._run_inverted_res(y, self.inv_blocks[1], 256, 256)
        y = self._run_inverted_res(y, self.inv_blocks[2], 256, 256)
        y = self._run_inverted_res(y, self.inv_blocks[3], 256, 256)
        y = self._conv_norm_lrelu(y, self.bc5, pad_val=1, stride=1)

        # block_d with upsample 2x
        y = self._upsample_bilinear(y, scale=2)
        y = self._conv_norm_lrelu(y, self.bd0, pad_val=1, stride=1)
        y = self._conv_norm_lrelu(y, self.bd1, pad_val=1, stride=1)

        # block_e with upsample 2x
        y = self._upsample_bilinear(y, scale=2)
        y = self._conv_norm_lrelu(y, self.be0, pad_val=1, stride=1)
        y = self._conv_norm_lrelu(y, self.be1, pad_val=1, stride=1)
        y = self._conv_norm_lrelu(y, self.be2, pad_val=3, stride=1)

        # out_layer
        y = tf.nn.conv2d(y, self.w_out, strides=[1, 1, 1, 1], padding="VALID")
        y = tf.tanh(y)

        # Reverse back from BGR to RGB
        y = tf.reverse(y, axis=[-1])

        # Scale from [-1.0, 1.0] to [0.0, 255.0]
        y = (y + 1.0) * 127.5

        return tf.clip_by_value(y, 0.0, 255.0, name="output")

def convert_tf_module_to_fp16_tflite(tf_module: tf.Module, output_path: str):
    """
    Converts tf.Module to uncompressed dynamic FP16 TFLite model.
    """
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    concrete_func = tf_module.__call__.get_concrete_function()
    converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func])
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()

    with open(output_path, "wb") as f:
        f.write(tflite_model)
    size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"Exported FP16 TFLite: {output_path} ({size_mb:.2f} MB)")
