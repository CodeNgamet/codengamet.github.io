# CodeNGamet Website

The website uses one project list: `projects.json`. The Home, Games, and Apps pages read that file, and `item.html` uses a project ID to show the matching project page.

## Main files

- `index.html` — Home page
- `games.html` — Automatically generated game catalogue
- `apps.html` — Automatically generated app catalogue
- `item.html?id=star-hopper` — Shared project information page
- `projects.json` — Names, descriptions, status, images, screenshots, downloads, and project information
- `styles.css` — Shared website styles
- `site.js` — Loads the project data and builds the pages

## Adding a project

Add a new object to the `projects` array in `projects.json`.

Each project needs:

- `id` — Unique lowercase ID used in the item page URL
- `name` — Project name
- `type` — `game` or `app`
- `status` — `released` or `coming-soon`
- `shortDescription` — Text shown on the catalogue card

For a released project, also add its `description`. Add `icon`, `screenshots`, `downloads`, and `sections` when the project uses them. A released project card opens automatically at `item.html?id=YOUR-ID`. Projects marked `coming-soon` show a disabled Coming Soon button.

To feature a released project on the Home page, set `"featured": true` on that project.

## Images and game files

- Put project icons in `icons/`, using names such as `icons/star-hopper.png`.
- Keep screenshots in `screenshots/PROJECT-ID/`, using numbered filenames when helpful.

Use paths relative to the website root in `projects.json` and in page code. Keep filenames lowercase and use hyphens where possible.

## Local preview

Because the pages load `projects.json` with `fetch`, preview the site through a local web server instead of opening the HTML files directly. From this folder, run:

```bash
python3 -m http.server 8000
```

Then open `http://localhost:8000` in a browser.
