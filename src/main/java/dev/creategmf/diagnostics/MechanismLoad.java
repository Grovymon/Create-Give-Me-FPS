package dev.creategmf.diagnostics;

import dev.creategmf.config.MechanismAnimationGroup;

/** A ranked object-count estimate, not a subsystem timing measurement. */
public record MechanismLoad(MechanismAnimationGroup group, int objects, int estimatedWeight) {
}
