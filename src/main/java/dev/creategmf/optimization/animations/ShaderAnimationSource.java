package dev.creategmf.optimization.animations;
/** Source values change only at Create's setter/setup paths, never from GMF's rendered output. */
public final class ShaderAnimationSource {
    private volatile float speed, offset;
    public void capture(float speed, float offset) { this.speed = speed; this.offset = offset; }
    public void speed(float value) { speed = value; }
    public void offset(float value) { offset = value; }
    public float speed() { return speed; }
    public float offset() { return offset; }
    public float renderedSpeed(boolean paused) { return paused ? 0 : speed; }
}
