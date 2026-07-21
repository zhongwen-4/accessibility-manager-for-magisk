# Bottom bar frost intensity checklist

SUMMARY: Always interpolate every frost parameter continuously from the solid zero state to the strongest state so the settings slider has no visible jump near zero.
READ WHEN: before changing bottom-bar frost settings, Haze styling, preview behavior, or slider-to-effect mapping.

---

The bottom-bar frost setting is a normalized value from `0f` to `1f`. At `0f`, the bar must be an ordinary opaque surface: blur radius and noise are zero, while the normal and fallback tint alphas are one. At `1f`, the verified strongest state uses a 52 dp blur radius, 0.42 tint alpha, 0.10 noise factor, and 0.68 fallback alpha.

Interpolate all four values from those endpoints with the same normalized setting. Do not add a nonzero minimum blur, noise, or translucency for values above zero: a floor such as `12 + 40 * frost` makes the first slider movement jump abruptly from no effect to a visibly blurred bar.

The Haze effect may still be disabled exactly at zero for performance. Because its enabled-state parameters converge to the opaque zero endpoint, crossing zero remains visually continuous.
