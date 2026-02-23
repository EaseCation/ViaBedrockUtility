package org.oryxel.viabedrockutility.payload.impl.camera;

import nakern.be_camera.camera.*;
import nakern.be_camera.easings.Easings;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles camera payloads by calling BECamera API.
 */
public class CameraPayloadHandler {

    public void handle(CameraPayload payload) {
        if (payload instanceof CameraInstructionPayload p) {
            handleInstruction(p);
        } else if (payload instanceof CameraShakePayload p) {
            handleShake(p);
        } else if (payload instanceof CameraPresetsPayload p) {
            handlePresets(p);
        }
        // CONFIRM type is handled in CameraPayload.STREAM_CODEC
    }

    private void handleInstruction(CameraInstructionPayload p) {
        if (p.isHasSet()) {
            // Resolve position: prefer explicit position, fall back to preset
            Vec3d position = null;
            Vec2f rotation = null;
            Vec3d facing = null;

            if (p.isHasPos()) {
                position = new Vec3d(p.getPosX(), p.getPosY(), p.getPosZ());
            } else {
                // Try to get position from preset
                final CameraPresetManager.Preset preset = CameraPresetManager.INSTANCE.getPreset(p.getPresetRuntimeId());
                if (preset != null && preset.getPosX() != null && preset.getPosY() != null && preset.getPosZ() != null) {
                    position = new Vec3d(preset.getPosX(), preset.getPosY(), preset.getPosZ());
                }
                if (preset != null && preset.getRotX() != null && preset.getRotY() != null) {
                    rotation = new Vec2f(preset.getRotX(), preset.getRotY());
                }
            }

            if (p.isHasRot()) {
                rotation = new Vec2f(p.getRotX(), p.getRotY());
            }

            if (p.isHasFacing()) {
                facing = new Vec3d(p.getFacingX(), p.getFacingY(), p.getFacingZ());
            }

            if (position == null) {
                ViaBedrockUtilityFabric.LOGGER.warn("[BECamera] Camera set instruction has no position (preset {} may not define position)", p.getPresetRuntimeId());
                return;
            }

            EaseOptions easeOptions = null;
            if (p.isHasEase()) {
                easeOptions = new EaseOptions(
                        Easings.byBedrockOrdinal(p.getEasingType()),
                        (long) (p.getEaseDuration() * 1000)
                );
            }

            CameraData data = new CameraData(position, facing, rotation, easeOptions);
            CameraManager.INSTANCE.setCamera(data);
            ViaBedrockUtilityFabric.LOGGER.debug("[BECamera] Camera set: pos={} rot={} facing={} ease={}", position, rotation, facing, p.isHasEase());
        }

        if (p.isHasClear()) {
            CameraManager.INSTANCE.clear();
            ViaBedrockUtilityFabric.LOGGER.debug("[BECamera] Camera cleared");
        }

        if (p.isHasFade()) {
            float fadeIn = p.isHasFadeTime() ? p.getFadeIn() : 0;
            float fadeStay = p.isHasFadeTime() ? p.getFadeStay() : 0;
            float fadeOut = p.isHasFadeTime() ? p.getFadeOut() : 0;

            int r = 0, g = 0, b = 0;
            if (p.isHasFadeColor()) {
                r = Math.round(p.getFadeR() * 255);
                g = Math.round(p.getFadeG() * 255);
                b = Math.round(p.getFadeB() * 255);
            }

            int color = (r << 16) | (g << 8) | b;
            CameraFadeOptions fadeOptions = new CameraFadeOptions(
                    color,
                    (long) (fadeIn * 1000),
                    (long) (fadeStay * 1000),
                    (long) (fadeOut * 1000)
            );
            CameraManager.INSTANCE.fade(fadeOptions);
            ViaBedrockUtilityFabric.LOGGER.debug("[BECamera] Camera fade: in={}s stay={}s out={}s color=#{}", fadeIn, fadeStay, fadeOut, Integer.toHexString(color));
        }
    }

    private void handleShake(CameraShakePayload p) {
        if (p.getAction() == 0) {
            // Add shake
            CameraShakeManager.INSTANCE.addShake(p.getIntensity(), p.getDuration(), p.getShakeType());
            ViaBedrockUtilityFabric.LOGGER.debug("[BECamera] Camera shake added: intensity={} duration={} type={}", p.getIntensity(), p.getDuration(), p.getShakeType());
        } else {
            // Stop all shakes
            CameraShakeManager.INSTANCE.stopAll();
            ViaBedrockUtilityFabric.LOGGER.debug("[BECamera] Camera shake stopped");
        }
    }

    private void handlePresets(CameraPresetsPayload p) {
        List<CameraPresetManager.Preset> presets = new ArrayList<>();
        for (CameraPresetsPayload.PresetEntry entry : p.getPresets()) {
            presets.add(new CameraPresetManager.Preset(
                    entry.getName(),
                    entry.getParent().isEmpty() ? null : entry.getParent(),
                    entry.getPosX(), entry.getPosY(), entry.getPosZ(),
                    entry.getRotX(), entry.getRotY(),
                    null // playerEffects not sent
            ));
        }
        CameraPresetManager.INSTANCE.setPresets(presets);
        ViaBedrockUtilityFabric.LOGGER.debug("[BECamera] Camera presets loaded: {} presets", presets.size());
    }
}
