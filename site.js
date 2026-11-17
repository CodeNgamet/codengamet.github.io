(() => {
  "use strict";

  const $ = (selector, parent = document) => parent.querySelector(selector);
  const make = (tag, className, text) => {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined) element.textContent = text;
    return element;
  };

  function setupNavigation() {
    const currentPage = document.body.dataset.page;
    const activePage = currentPage === "item" ? "games" : currentPage;
    document.querySelectorAll("[data-nav]").forEach(link => {
      if (link.dataset.nav === activePage) {
        link.classList.add("active");
        link.setAttribute("aria-current", "page");
      }
    });

    const openers = document.querySelectorAll("[data-community-open]");
    if (!openers.length) return;

    const overlay = make("div", "modal-overlay");
    overlay.id = "communityModal";
    overlay.setAttribute("aria-hidden", "true");
    const dialog = make("div", "community-modal");
    dialog.setAttribute("role", "dialog");
    dialog.setAttribute("aria-modal", "true");
    dialog.setAttribute("aria-labelledby", "communityTitle");
    const close = make("button", "modal-close", "×");
    close.type = "button";
    close.setAttribute("aria-label", "Close");
    const title = make("h2", "", "CodeNGamet Community");
    title.id = "communityTitle";
    const intro = make("p", "", "Find CodeNGamet online.");
    const links = make("div", "community-links");
    [
      ["YouTube", "https://www.youtube.com/@CodeaAndGamet"],
      ["Scratch", "https://scratch.mit.edu/users/CodeNGamet/"],
      ["Discord", "https://discord.com/invite/NGfqunjNM"],
      ["Proton Mail", "mailto:CodeNGamet@proton.me"],
      ["GitHub", "https://github.com/CodeNGamet/codengamet.github.io"]
    ].forEach(([label, href]) => {
      const link = make("a", "community-link", label);
      link.href = href;
      if (href.startsWith("https://")) {
        link.target = "_blank";
        link.rel = "noopener noreferrer";
      }
      links.append(link);
    });
    dialog.append(close, title, intro, links);
    overlay.append(dialog);
    document.body.append(overlay);

    let lastOpener = null;
    const open = event => {
      event.preventDefault();
      lastOpener = event.currentTarget;
      overlay.classList.add("show");
      overlay.setAttribute("aria-hidden", "false");
      close.focus();
    };
    const dismiss = () => {
      overlay.classList.remove("show");
      overlay.setAttribute("aria-hidden", "true");
      if (lastOpener) lastOpener.focus();
    };
    openers.forEach(opener => opener.addEventListener("click", open));
    close.addEventListener("click", dismiss);
    overlay.addEventListener("click", event => {
      if (event.target === overlay) dismiss();
    });
    document.addEventListener("keydown", event => {
      if (event.key === "Escape" && overlay.classList.contains("show")) dismiss();
    });
  }

  function projectCard(project) {
    const card = make("article", "project-card");
    card.dataset.search = [project.name, project.category, project.shortDescription]
      .filter(Boolean).join(" ").toLowerCase();

    const icon = make("div", "project-icon");
    if (project.icon) {
      const image = make("img");
      image.src = project.icon;
      image.alt = `${project.name} icon`;
      image.loading = "lazy";
      icon.append(image);
    }
    const title = make("h3", "", project.name);
    const description = make("p", "", project.shortDescription || project.category || "");
    card.append(icon, title, description);

    if (project.status === "released") {
      const link = make("a", "project-card-link", project.type === "app" ? "View app" : "View game");
      link.href = `item.html?id=${encodeURIComponent(project.id)}`;
      card.append(link);
    } else {
      const button = make("button", "coming-soon", "Coming Soon");
      button.type = "button";
      button.disabled = true;
      card.append(button);
    }
    return card;
  }

  function renderHome(projects) {
    const target = $("#featuredProject");
    if (!target) return;
    const featured = projects.find(project => project.featured && project.status === "released");
    if (!featured) {
      target.replaceChildren(make("p", "", "No featured project yet."));
      return;
    }

    const image = make("img");
    image.src = featured.icon || "";
    image.alt = `${featured.name} icon`;
    const copy = make("div");
    copy.append(
      make("p", "project-status", `${featured.type === "app" ? "Featured app" : "Featured game"} · Released`),
      make("h2", "", featured.name),
      make("p", "", featured.shortDescription || "")
    );
    const link = make("a", "site-button", `View ${featured.name}`);
    link.href = `item.html?id=${encodeURIComponent(featured.id)}`;
    copy.append(link);
    target.replaceChildren(image, copy);
  }

  function renderCatalog(projects, type) {
    const grid = $("#projectGrid");
    if (!grid) return;
    const list = projects.filter(project => project.type === type);
    grid.replaceChildren(...list.map(projectCard));

    const search = $("#projectSearch");
    const noResults = $("#noResults");
    const emptyApps = $("#appsEmpty");
    if (type === "app" && emptyApps) emptyApps.hidden = list.length > 0;
    if (search) {
      const filter = () => {
        const term = search.value.trim().toLowerCase();
        let visible = 0;
        grid.querySelectorAll(".project-card").forEach(card => {
          const matches = !term || card.dataset.search.includes(term);
          card.hidden = !matches;
          if (matches) visible += 1;
        });
        if (noResults) noResults.hidden = visible !== 0;
      };
      search.addEventListener("input", filter);
    }
  }

  function appendParagraphs(parent, paragraphs) {
    (paragraphs || []).forEach(text => parent.append(make("p", "", text)));
  }

  function appendList(parent, items) {
    if (!items || !items.length) return;
    const list = make("ul");
    items.forEach(item => list.append(make("li", "", item)));
    parent.append(list);
  }

  function appendTable(parent, tableData) {
    if (!tableData) return;
    const table = make("table");
    const thead = make("thead");
    const headerRow = make("tr");
    (tableData.headers || []).forEach(label => headerRow.append(make("th", "", label)));
    thead.append(headerRow);
    const tbody = make("tbody");
    (tableData.rows || []).forEach(row => {
      const tr = make("tr");
      row.forEach(value => tr.append(make("td", "", value)));
      tbody.append(tr);
    });
    table.append(thead, tbody);
    parent.append(table);
  }

  function renderSection(section) {
    const container = make("section", "item-section");
    container.append(make("h2", "", section.title || ""));
    appendParagraphs(container, section.paragraphs);
    if (section.table) appendTable(container, section.table);
    if (section.subheading) container.append(make("h3", "", section.subheading));
    appendList(container, section.items);
    (section.subSections || []).forEach(subsection => {
      container.append(make("h3", "", subsection.title || ""));
      appendParagraphs(container, subsection.paragraphs);
      appendList(container, subsection.items);
    });
    return container;
  }

  function renderDownloads(parent, downloads) {
    if (!downloads || !Array.isArray(downloads.platforms) || !downloads.platforms.length) return;
    const controls = make("div", "download-controls");
    const select = make("select", "platform-select");
    select.setAttribute("aria-label", "Choose platform");
    const first = make("option", "", "Choose platform");
    first.value = "";
    select.append(first);
    downloads.platforms.forEach(platform => {
      const option = make("option", "", platform.name);
      option.value = platform.id;
      select.append(option);
    });

    const button = make("a", "download-button", "Download");
    button.href = "#";
    button.setAttribute("aria-disabled", "true");
    const details = make("p", "platform-details", "Choose your platform to see its download and installation information.");

    select.addEventListener("change", () => {
      const chosen = downloads.platforms.find(platform => platform.id === select.value);
      if (!chosen) {
        button.href = "#";
        button.removeAttribute("download");
        button.classList.remove("ready");
        button.setAttribute("aria-disabled", "true");
        button.textContent = "Download";
        details.textContent = "Choose your platform to see its download and installation information.";
        return;
      }
      button.href = downloads.releaseBase + encodeURIComponent(chosen.file);
      button.download = chosen.file;
      button.classList.add("ready");
      button.setAttribute("aria-disabled", "false");
      button.textContent = chosen.button || "Download";
      details.textContent = `${chosen.details} File: ${chosen.file}`;
    });

    controls.append(button, select);
    parent.append(controls, details);
  }

  function renderItem(projects) {
    const target = $("#itemContent");
    if (!target) return;
    const id = new URLSearchParams(window.location.search).get("id");
    const project = projects.find(item => item.id === id && item.status === "released");
    if (!project) {
      const message = make("div", "empty-message");
      message.append(make("h2", "", "Project not found"));
      message.append(make("p", "", "This project may not be released yet, or the link may be incorrect."));
      const back = make("a", "site-button", "Back to projects");
      back.href = "games.html";
      message.append(back);
      target.replaceChildren(message);
      return;
    }

    document.title = `${project.name} — CodeNGamet`;
    const back = make("a", "back-link", `← Back to ${project.type === "app" ? "apps" : "games"}`);
    back.href = project.type === "app" ? "apps.html" : "games.html";

    const hero = make("section", "item-hero");
    const iconWrap = make("div", "item-icon");
    if (project.icon) {
      const image = make("img");
      image.src = project.icon;
      image.alt = `${project.name} icon`;
      iconWrap.append(image);
    }
    const copy = make("div", "item-copy");
    copy.append(
      make("p", "project-status", `Released ${project.type}`),
      make("h2", "", project.name),
      make("p", "", project.description || project.shortDescription || "")
    );

    const actions = make("div", "item-actions");
    if (project.github && project.github.url) {
      const github = make("a", "site-button", project.github.button || "View on GitHub");
      github.href = project.github.url;
      github.target = "_blank";
      github.rel = "noopener noreferrer";
      actions.append(github);
    }
    if (actions.children.length) copy.append(actions);
    renderDownloads(copy, project.downloads);
    hero.append(iconWrap, copy);

    const output = [back, hero];
    if (project.screenshots && project.screenshots.length) {
      const section = make("section", "item-section");
      section.append(make("h2", "", "Screenshots"));
      const grid = make("div", "screenshot-grid");
      project.screenshots.forEach(screenshot => {
        const image = make("img");
        image.src = screenshot.src;
        image.alt = screenshot.alt || project.name;
        image.loading = "lazy";
        grid.append(image);
      });
      section.append(grid);
      output.push(section);
    }
    (project.sections || []).forEach(section => output.push(renderSection(section)));
    target.replaceChildren(...output);
  }

  async function start() {
    setupNavigation();
    try {
      const response = await fetch("projects.json", { cache: "no-cache" });
      if (!response.ok) throw new Error(`Could not load projects.json (${response.status})`);
      const data = await response.json();
      const projects = Array.isArray(data.projects) ? data.projects : [];
      const page = document.body.dataset.page;
      if (page === "home") renderHome(projects);
      if (page === "games") renderCatalog(projects, "game");
      if (page === "apps") renderCatalog(projects, "app");
      if (page === "item") renderItem(projects);
    } catch (error) {
      console.error(error);
      const message = "Projects could not be loaded. Check that projects.json is in the website's main folder.";
      ["#featuredProject", "#projectGrid", "#itemContent"].forEach(selector => {
        const target = $(selector);
        if (target) target.replaceChildren(make("p", "empty-message", message));
      });
    }
  }

  start();
})();
