/**
 * Student Placement Planner - Frontend Interactions
 * Theme Management, Live Search, and Form Enhancements
 */

// Initialize and manage Light / Dark Theme
function initTheme() {
  const toggleBtn = document.getElementById("themeToggle");
  const currentTheme = document.documentElement.getAttribute("data-theme") || "light";

  updateToggleIcon(toggleBtn, currentTheme);

  if (toggleBtn) {
    toggleBtn.addEventListener("click", () => {
      const activeTheme = document.documentElement.getAttribute("data-theme") || "light";
      const nextTheme = activeTheme === "dark" ? "light" : "dark";

      document.documentElement.setAttribute("data-theme", nextTheme);
      localStorage.setItem("theme", nextTheme);
      updateToggleIcon(toggleBtn, nextTheme);
    });
  }
}

function updateToggleIcon(btn, theme) {
  if (!btn) return;
  if (theme === "dark") {
    btn.innerHTML = "&#9728;"; // Sun icon to switch to light
    btn.setAttribute("title", "Switch to Light Mode");
    btn.setAttribute("aria-label", "Switch to Light Mode");
  } else {
    btn.innerHTML = "&#9790;"; // Moon icon to switch to dark
    btn.setAttribute("title", "Switch to Dark Mode");
    btn.setAttribute("aria-label", "Switch to Dark Mode");
  }
}

// Live Job Filtering & Search
function initJobFilter() {
  const searchInput = document.getElementById("jobSearchInput");
  const locationFilter = document.getElementById("jobLocationFilter");
  const workModeFilter = document.getElementById("jobWorkModeFilter");
  const roleButtons = document.querySelectorAll(".role-filter-btn");
  const jobCards = document.querySelectorAll(".job-card");
  const countDisplay = document.getElementById("jobsCountDisplay");
  const emptyFilterState = document.getElementById("noFilterResults");

  if (!searchInput && !locationFilter && !workModeFilter && !roleButtons.length) return;

  let activeRole = "all";

  function filterJobs() {
    const query = searchInput ? searchInput.value.toLowerCase().trim() : "";
    const loc = locationFilter ? locationFilter.value.toLowerCase().trim() : "";
    const mode = workModeFilter ? workModeFilter.value.toLowerCase().trim() : "";

    let visibleCount = 0;

    jobCards.forEach(card => {
      const title = (card.dataset.title || "").toLowerCase();
      const company = (card.dataset.company || "").toLowerCase();
      const location = (card.dataset.location || "").toLowerCase();
      const workMode = (card.dataset.workmode || "").toLowerCase();
      const category = (card.dataset.category || "").toLowerCase();
      const skills = (card.dataset.skills || "").toLowerCase();
      const description = (card.dataset.description || "").toLowerCase();
      const cardText = card.innerText.toLowerCase();

      const matchesQuery = !query || title.includes(query) || company.includes(query) || skills.includes(query) || description.includes(query) || cardText.includes(query);
      const matchesLoc = !loc || location.includes(loc);
      const matchesMode = !mode || workMode.includes(mode);
      const matchesRole = (activeRole === "all") || category.includes(activeRole) || title.includes(activeRole);

      if (matchesQuery && matchesLoc && matchesMode && matchesRole) {
        card.style.display = "";
        visibleCount++;
      } else {
        card.style.display = "none";
      }
    });

    if (countDisplay) {
      countDisplay.textContent = `Showing ${visibleCount} of ${jobCards.length} opportunities`;
    }

    if (emptyFilterState) {
      emptyFilterState.style.display = (visibleCount === 0 && jobCards.length > 0) ? "block" : "none";
    }
  }

  if (searchInput) {
    searchInput.addEventListener("input", filterJobs);
  }
  if (locationFilter) {
    locationFilter.addEventListener("change", filterJobs);
  }
  if (workModeFilter) {
    workModeFilter.addEventListener("change", filterJobs);
  }
  if (roleButtons.length) {
    roleButtons.forEach(btn => {
      btn.addEventListener("click", () => {
        roleButtons.forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        activeRole = btn.dataset.role || "all";
        filterJobs();
      });
    });
  }
}

// Suggested Skill Tags Quick-Filler
function initSkillTags() {
  const skillInput = document.getElementById("skill");
  document.querySelectorAll(".suggested-tags .tag-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      if (skillInput) {
        skillInput.value = btn.dataset.skill || btn.textContent.trim();
        skillInput.focus();
      }
    });
  });
}

// Client-Side Registration Password Validator
function validateRegistration() {
  const password = document.getElementById("password")?.value || "";
  const confirmPassword = document.getElementById("confirmPassword")?.value || "";
  const passwordError = document.getElementById("passwordError");
  const confirmError = document.getElementById("confirmError");

  let isValid = true;

  if (password.length < 6) {
    if (passwordError) passwordError.style.display = "block";
    isValid = false;
  } else {
    if (passwordError) passwordError.style.display = "none";
  }

  if (password !== confirmPassword) {
    if (confirmError) confirmError.style.display = "block";
    isValid = false;
  } else {
    if (confirmError) confirmError.style.display = "none";
  }

  return isValid;
}

// Task filter tabs
function initTaskFilters() {
  const filterButtons = document.querySelectorAll(".task-filter-btn");
  const taskItems = document.querySelectorAll(".task-item");

  if (!filterButtons.length) return;

  filterButtons.forEach(btn => {
    btn.addEventListener("click", () => {
      filterButtons.forEach(b => b.classList.remove("active"));
      btn.classList.add("active");

      const filter = btn.dataset.filter;

      taskItems.forEach(item => {
        const isCompleted = item.classList.contains("completed");
        const isOverdue = item.classList.contains("overdue");
        if (filter === "all") {
          item.style.display = "";
        } else if (filter === "completed") {
          item.style.display = isCompleted ? "" : "none";
        } else if (filter === "pending") {
          item.style.display = !isCompleted ? "" : "none";
        } else if (filter === "overdue") {
          item.style.display = isOverdue ? "" : "none";
        }
      });
    });
  });
}

// Run on DOM ready
document.addEventListener("DOMContentLoaded", () => {
  initTheme();
  initJobFilter();
  initSkillTags();
  initTaskFilters();

  // Handle generic confirmation forms
  document.querySelectorAll("form[data-confirm]").forEach(form => {
    form.addEventListener("submit", event => {
      if (!window.confirm(form.dataset.confirm || "Are you sure?")) {
        event.preventDefault();
      }
    });
  });
});