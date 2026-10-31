/**
 * Campus Event Registration System - Theme Switcher
 * Manages White (Light) and Dark theme state, persistence, and transitions.
 */
(function () {
    'use strict';

    var STORAGE_KEY = 'campus_events_theme';

    /**
     * Get preferred theme: stored in localStorage > system preference > default 'light'
     */
    function getPreferredTheme() {
        try {
            var stored = localStorage.getItem(STORAGE_KEY);
            if (stored === 'dark' || stored === 'light') {
                return stored;
            }
        } catch (e) {
            // localStorage might be unavailable or disabled
        }
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            return 'dark';
        }
        return 'light';
    }

    /**
     * Apply theme to <html> tag and update button state
     */
    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        try {
            localStorage.setItem(STORAGE_KEY, theme);
        } catch (e) {
            // Ignore storage write issues
        }

        var toggleBtn = document.getElementById('themeToggle');
        if (toggleBtn) {
            var isDark = theme === 'dark';
            toggleBtn.setAttribute('aria-pressed', isDark ? 'true' : 'false');
            toggleBtn.setAttribute('title', isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme');

            var labelSpan = toggleBtn.querySelector('.theme-toggle-label');
            if (labelSpan) {
                labelSpan.textContent = isDark ? 'Light' : 'Dark';
            }
        }
    }

    /**
     * Toggle between light and dark themes
     */
    function toggleTheme() {
        var current = document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
        var next = current === 'dark' ? 'light' : 'dark';
        applyTheme(next);
    }

    /**
     * Initialize theme toggle button listener
     */
    function setupToggle() {
        var toggleBtn = document.getElementById('themeToggle');
        if (toggleBtn) {
            // Bind click
            toggleBtn.addEventListener('click', function (e) {
                e.preventDefault();
                toggleTheme();
            });

            // Keyboard accessibility
            toggleBtn.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    toggleTheme();
                }
            });

            // Sync current state
            var currentTheme = document.documentElement.getAttribute('data-theme') || getPreferredTheme();
            applyTheme(currentTheme);
        }

        // Listen for OS scheme change if user hasn't chosen manually
        if (window.matchMedia) {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function (e) {
                try {
                    if (!localStorage.getItem(STORAGE_KEY)) {
                        applyTheme(e.matches ? 'dark' : 'light');
                    }
                } catch (err) {
                    // Ignore
                }
            });
        }
    }

    // Run setup when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setupToggle);
    } else {
        setupToggle();
    }
})();

