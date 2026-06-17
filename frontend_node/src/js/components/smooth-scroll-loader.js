import Lenis from "lenis";

const shouldEnableSmoothScroll = () => {
  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  return document.body.classList.contains("home-page") && !document.body.classList.contains("dashboard") && !reducedMotion;
};

const setupAnchorLinks = (lenis) => {
  document.querySelectorAll('a[href^="#"], a[href^="index.html#"]').forEach((link) => {
    link.addEventListener("click", (event) => {
      const href = link.getAttribute("href");
      const targetId = href?.split("#")[1];
      if (!targetId) return;

      const target = document.getElementById(targetId);
      if (!target) return;

      event.preventDefault();
      lenis.scrollTo(target, {
        offset: -90,
        duration: 1.2,
      });

      window.history.pushState(null, "", `#${targetId}`);
    });
  });
};

const initSmoothScroll = () => {
  if (!shouldEnableSmoothScroll()) return;

  const lenis = new Lenis({
    duration: 0.45,
    easing: (time) => Math.min(1, 1.001 - Math.pow(2, -10 * time)),
    smoothWheel: true,
    syncTouch: false,
    wheelMultiplier: 0.95,
  });

  const raf = (time) => {
    lenis.raf(time);
    requestAnimationFrame(raf);
  };

  requestAnimationFrame(raf);
  setupAnchorLinks(lenis);

  window.lenis = lenis;
};

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initSmoothScroll);
} else {
  initSmoothScroll();
}
