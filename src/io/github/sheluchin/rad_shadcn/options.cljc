(ns io.github.sheluchin.rad-shadcn.options
  "Report options read by the shadcn RAD renderer (io.github.sheluchin.rad-shadcn.*).
  They replace fulcro-rad-semantic-ui's `suo/` (semantic-ui-options) keys.
  Put them directly in the report's options:

  ```
  (report/defsc-report Foo [_ _]
    {rro/cell-class (fn [report-instance column-index] ...)})
  ```"
  (:require
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.ui-state-machines :as uism]
   [com.fulcrologic.rad.routing.history :as history]))

(def page-size-choices
  "Rows per page the pager offers."
  [10 25 50 100])

(defn rows-per-page
  "Value for `ro/page-size` that lets the viewer change it: `default`, unless
  the URL picked another (`?size=50`, kept in the route params under the
  report's id as `::rows-per-page`)."
  [default]
  (with-meta
    (fn [env]
      (let [report-id (second (uism/actor->ident env :actor/report))
            chosen    (get-in (history/current-route (::uism/app env)) [:params report-id ::rows-per-page])]
        (if (some #{chosen} page-size-choices) chosen default)))
    {::default default}))

(defn current-page-size
  "The page size `report-instance` loads with: its `ro/page-size` (a number or
  `rows-per-page`), after any choice in the URL."
  [report-instance]
  (let [ps     (comp/component-options report-instance :com.fulcrologic.rad.report/page-size)
        chosen (when (and ps (not (number? ps)))
                 (get-in (history/current-route (comp/any->app report-instance))
                         [:params (second (comp/get-ident report-instance)) ::rows-per-page]))]
    (cond
      (some #{chosen} page-size-choices) chosen
      (number? ps)                       ps
      :else                              (::default (meta ps) 20))))

(def layout-class
  "Class (or `(fn [report-instance])` returning one) for the report's outer div."
  ::layout-class)

(def show-title?
  "Show `ro/title` above the table, as the page's h1. Defaults to false, for
  reports that sit inside a page or tab that already names them."
  ::show-title?)

(def header-class
  "`(fn [report-instance column-index])` returning extra classes for a `th`."
  ::header-class)

(def cell-class
  "`(fn [report-instance column-index])` returning extra classes for a `td`."
  ::cell-class)

(def hide-on-mobile
  "Set of column keys (attribute qualified keys) to hide below the `md`
  breakpoint, so the important columns fit on a phone without scrolling."
  ::hide-on-mobile)

(def row-disabled?
  "`(fn [report-instance row-props])`; truthy greys the row out."
  ::row-disabled?)

(def selectable-rows?
  "Set to false to stop row clicks from calling `ro/on-select-row` and
  highlighting the row. Defaults to true."
  ::selectable-rows?)

(def pinned-row
  "`(fn [report-instance])`: a table row to show above the first page's
  rows, or nil; e.g. a shared item that isn't on the list's first page."
  ::pinned-row)
