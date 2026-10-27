# fulcro-rad-shadcn

Tailwind v4 + shadcn/ui-style components for Fulcro, and a RAD renderer built on
them. It replaces `fulcro-rad-semantic-ui` (the RAD renderer) and
`semantic-ui-wrapper` (the React component wrappers).

## What's in it

| Namespace | What |
|---|---|
| `io.github.sheluchin.rad-shadcn.ui` | Components: button, badge, card, input, label, native select, table, avatar, skeleton, spinner, pagination, tabs, and Radix tooltip / popover / dialog / sheet. `cn` merges Tailwind classes. Icons come from lucide-react. |
| `io.github.sheluchin.rad-shadcn.all-controls` | `all-controls`, the map to pass to `rad-app/install-ui-controls!`; the counterpart of `semantic-ui-controls/all-controls`. `merge-controls` adds your own styles. |
| `io.github.sheluchin.rad-shadcn.report` | Table report layout, standard report controls (title, inputs, actions, pagination), table rows. |
| `io.github.sheluchin.rad-shadcn.controls` | Report controls: `:string`, `:picker`, `:multi-picker` (fuzzy search), `:button`, `:boolean` (a switch). |
| `io.github.sheluchin.rad-shadcn.options` | Report options for the renderer (`rro/...`), replacing `suo/` keys. |
| `io.github.sheluchin.rad-shadcn.tailwind` | Writes the CSS that points the app's Tailwind build at this library. |
| `resources/rad_shadcn/theme.css` | Design tokens (shadcn "neutral", WCAG 2.2 AA) and base styles. |

Not yet: RAD form rendering, and list and rotated report layouts.

## Using it

1. **deps.edn**: add it as a git dependency (this repo is private, so the
   machine needs GitHub access), or `:local/root` while developing:

   ```clojure
   io.github.sheluchin/fulcro-rad-shadcn {:git/url "git@github.com:sheluchin/fulcro-rad-shadcn.git"
                                    :git/sha "<sha>"}
   ```

   `bb sha` in this repo prints that line for the current commit.

2. **package.json**: add the npm packages listed in `src/deps.cljs`, plus
   `tailwindcss` and `@tailwindcss/cli`.

3. **Install the renderer** in the client:

   ```clojure
   (rad-app/install-ui-controls! app io.github.sheluchin.rad-shadcn.all-controls/all-controls)
   ```

4. **Tailwind**: before each CSS build, write the import file, then build:

   ```sh
   clojure -M -m io.github.sheluchin.rad-shadcn.tailwind resources/tailwind/rad-shadcn.css
   npx @tailwindcss/cli -i resources/tailwind/app.css -o resources/public/css/app.css --minify
   ```

   The entry file imports Tailwind's layers, then `./rad-shadcn.css`, then
   `@source`s the app's own code. See the comment at the top of `theme.css`.
   The written file holds absolute paths: add it to `.gitignore`.

5. **Theme**: override tokens (`--primary`, `--ring`, `--radius`, ...) in the
   app's own `:root`, after the import.

## Development

Tasks run with [Babashka](https://babashka.org); `bb tasks` lists them.

| Task | What |
|---|---|
| `bb test` | Run the tests (`clojure -M:test`). Extra args pass to the test runner, e.g. `bb test -n io.github.sheluchin.rad-shadcn.fuzzy-test`. |
| `bb lint` | clj-kondo over `src`, `test`, `examples` and `bb`, after importing the Fulcro/RAD kondo configs. |
| `bb css [path]` | Write the Tailwind import file, default `target/tailwind/rad-shadcn.css`. |
| `bb css:check` | Build the theme and components with the Tailwind v4 CLI and check the output. Installs Tailwind under `target/` on first run (needs npm). |
| `bb check` | `test`, `lint` and `css:check`: run before pushing. |
| `bb sha` | Print the `:git/sha` coordinate for HEAD to paste into an app's `deps.edn`. Warns if HEAD isn't pushed or the tree is dirty. |

To try a change in an app before pinning a sha, start the app with its
`:rad-shadcn/local` alias, which points at a local checkout of this repo.
