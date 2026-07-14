const PARALLAX_SELECTOR = ".home-content-bg";
const MAX_OFFSET = 180;
const SPEED = 0.18;

const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

const initHomeBgParallax = () => {
  const target = document.querySelector(PARALLAX_SELECTOR);
  if (!target) return;

  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reducedMotion) return;

  let ticking = false;

  const update = () => {
    const rect = target.getBoundingClientRect();
    const offset = clamp(-rect.top * SPEED, 0, MAX_OFFSET);

    target.style.setProperty("--home-bg-parallax-y", `${Math.round(offset)}px`);
    ticking = false;
  };

  const requestUpdate = () => {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(update);
  };

  update();
  window.addEventListener("scroll", requestUpdate, { passive: true });
  window.addEventListener("resize", requestUpdate);
};

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initHomeBgParallax);
} else {
  initHomeBgParallax();
}
