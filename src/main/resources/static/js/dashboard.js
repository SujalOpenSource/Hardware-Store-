
document.addEventListener("DOMContentLoaded", () => {

    // Weekly sales chart
    const bars = document.querySelectorAll(".sales-bar");

    const values = Array.from(bars).map(bar =>
        Number(bar.dataset.revenue || 0)
    );

    const maximum = Math.max(...values, 1);

    bars.forEach((bar, index) => {
        const percentage = values[index] === 0
            ? 3
            : Math.max((values[index] / maximum) * 100, 5);

        bar.style.height = `${percentage}%`;
    });

    // Responsive sidebar
    const menuToggle = document.getElementById("menuToggle");
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");

    if (!menuToggle || !sidebar || !overlay) {
        return;
    }

    function openMenu() {
        sidebar.classList.add("nav-open");
        overlay.classList.add("visible");
        document.body.classList.add("menu-open");

        menuToggle.setAttribute("aria-expanded", "true");
        menuToggle.setAttribute("aria-label", "Close navigation menu");
        menuToggle.textContent = "✕";
    }

    function closeMenu() {
        sidebar.classList.remove("nav-open");
        overlay.classList.remove("visible");
        document.body.classList.remove("menu-open");

        menuToggle.setAttribute("aria-expanded", "false");
        menuToggle.setAttribute("aria-label", "Open navigation menu");
        menuToggle.textContent = "☰";
    }

    menuToggle.addEventListener("click", () => {
        const isOpen = sidebar.classList.contains("nav-open");

        if (isOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    });

    overlay.addEventListener("click", closeMenu);

    // Close menu after selecting a navigation link
    sidebar.querySelectorAll(".nav-link").forEach(link => {
        link.addEventListener("click", closeMenu);
    });

    // Close menu with Escape key
    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            closeMenu();
        }
    });

    // Reset mobile menu when returning to desktop width
    window.addEventListener("resize", () => {
        if (window.innerWidth > 900) {
            closeMenu();
        }
    });

});
