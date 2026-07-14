import Lenis from "lenis";

const STORAGE_KEY = "ecomarketSmoothScroll";
let lenisInstance = null;
let rafId = null;

const getStoredPreference = () => localStorage.getItem(STORAGE_KEY) !== "disabled";

const shouldEnableSmoothScroll = () => {
  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  return (
    document.body.classList.contains("home-page") &&
    !document.body.classList.contains("dashboard") &&
    !reducedMotion &&
    getStoredPreference()
  );
};

const scrollToTarget = (target) => {
  if (lenisInstance) {
    lenisInstance.scrollTo(target, {
      offset: -90,
      duration: 1.2,
    });
    return;
  }

  target.scrollIntoView({
    behavior: "smooth",
    block: "start",
  });
};

const setupAnchorLinks = () => {
  document.querySelectorAll('a[href^="#"], a[href^="index.html#"]').forEach((link) => {
    if (link.dataset.smoothScrollBound === "true") return;
    link.dataset.smoothScrollBound = "true";

    link.addEventListener("click", (event) => {
      const href = link.getAttribute("href");
      const targetId = href?.split("#")[1];
      if (!targetId) return;

      const target = document.getElementById(targetId);
      if (!target) return;

      event.preventDefault();
      scrollToTarget(target);
      window.history.pushState(null, "", `#${targetId}`);
    });
  });
};

const updateToggle = () => {
  const toggle = document.getElementById("smooth-scroll-toggle");
  if (!toggle) return;

  const isEnabled = Boolean(lenisInstance);
  toggle.classList.toggle("is-disabled", !isEnabled);
  toggle.setAttribute("aria-pressed", String(isEnabled));
  toggle.textContent = isEnabled ? "Lenis" : "CSS";
  toggle.title = isEnabled ? "Scroll suavizado Lenis activo" : "Scroll normal CSS activo";
};

const startLenis = () => {
  if (lenisInstance || !shouldEnableSmoothScroll()) return;

  lenisInstance = new Lenis({
    duration: 0.45,
    easing: (time) => Math.min(1, 1.001 - Math.pow(2, -10 * time)),
    smoothWheel: true,
    syncTouch: false,
    wheelMultiplier: 0.95,
  });

  const raf = (time) => {
    lenisInstance?.raf(time);
    rafId = requestAnimationFrame(raf);
  };

  rafId = requestAnimationFrame(raf);
  window.lenis = lenisInstance;
  updateToggle();
};

const stopLenis = () => {
  if (rafId) {
    cancelAnimationFrame(rafId);
    rafId = null;
  }

  if (lenisInstance) {
    lenisInstance.destroy();
    lenisInstance = null;
  }

  window.lenis = null;
  updateToggle();
};

const setupToggle = () => {
  const toggle = document.getElementById("smooth-scroll-toggle");
  if (!toggle) return;

  toggle.addEventListener("click", () => {
    const enable = !lenisInstance;
    localStorage.setItem(STORAGE_KEY, enable ? "enabled" : "disabled");

    if (enable) {
      startLenis();
    } else {
      stopLenis();
    }
  });

  updateToggle();
};

const initSmoothScroll = () => {
  setupAnchorLinks();
  setupToggle();
  startLenis();
};

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initSmoothScroll);
} else {
  initSmoothScroll();
}
