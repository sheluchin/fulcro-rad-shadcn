(ns rad-shadcn.examples.custom-styles
  "Worked examples of extending the RAD renderer (io.github.sheluchin.rad-shadcn.*)
  with your own styles. No report uses them right now; they are installed in
  `client/setup-RAD` anyway so they keep compiling.

  RAD looks a report's renderers up by style key in the control map given to
  `rad-app/install-ui-controls!`:

  | report option            | control map key                     | example here                       |
  |--------------------------|-------------------------------------|------------------------------------|
  | `ro/layout-style`        | `::report/style->layout`            | `::TableReportLayout`              |
  | `::report/control-style` | `::report/control-style->control`   | `::SansPaginationStandardControls` |
  | `ro/row-style`           | `::report/row-style->row-layout`    | `:X`                               |

  To add a style: write a render fn, assoc it into the map under a new key,
  and set the option on the report. The building blocks are the renderer's
  public fns (`rendering.report/render-table`, `render-pagination`, ...) and
  the components in io.github.sheluchin.rad-shadcn.ui.

  These started out as customizations of fulcro-rad-semantic-ui's renderers;
  that version is in git history before the Semantic removal."
  (:require
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.dom :refer [div]]
   [com.fulcrologic.fulcro.dom.events :as evt]
   [com.fulcrologic.rad.attributes :as attr]
   [com.fulcrologic.rad.control :as control]
   [com.fulcrologic.rad.options-util :refer [?!]]
   [com.fulcrologic.rad.report :as report]
   [io.github.sheluchin.rad-shadcn.report :as rendering.report]
   [io.github.sheluchin.rad-shadcn.ui :as ui]))

;;; Row style :X -- a custom row.
;; Greys out rows for which the report's `:custom/disabling-fn` returns true
;; and renders plain values (no links or row actions). The default row covers
;; greying with `rro/row-disabled?`; this shows how to take over a row
;; completely, e.g. to render extra markup per row.

(comp/defsc TableRowLayoutX [_ {:keys [report-instance props]}]
  {}
  (let [{::report/keys [columns on-select-row]
         :custom/keys [disabling-fn]} (comp/component-options report-instance)
        {:keys [highlighted?] ::report/keys [idx]} (comp/get-computed props)]
    (ui/table-row {:selected? highlighted?
                   :disabled? (?! disabling-fn report-instance props)
                   :onClick   (fn [e]
                                (evt/stop-propagation! e)
                                (?! on-select-row report-instance props)
                                (report/select-row! report-instance idx))}
      (map (fn [{::attr/keys [qualified-key] :as column}]
             (ui/table-cell {:key (str "col-" qualified-key)}
               (report/formatted-column-value report-instance props column)))
           columns))))

(let [ui-table-row-layout (comp/factory TableRowLayoutX)]
  (defn render-table-row-X [report-instance row-class row-props]
    (ui-table-row-layout {:report-instance report-instance
                          :row-class       row-class
                          :props           row-props})))

;;; Layout style ::TableReportLayout -- a custom report layout.
;; The default layout puts pagination in the toolbar above the table; this one
;; moves it underneath. It reuses the renderer's table, so it only decides
;; where things go.

(comp/defsc TableReportLayout [this {:keys [report-instance] :as env}]
  {:initLocalState        rendering.report/init-row-factory-state
   :shouldComponentUpdate (fn [_ _ _] true)}
  (let [render-controls (report/control-renderer report-instance)]
    (div {:className "flex flex-col gap-3"}
      (when render-controls
        (render-controls report-instance))
      (div {:className "rounded-md border bg-card"}
        (rendering.report/render-table this env))
      (rendering.report/render-pagination report-instance :label "Pagination (bottom)"))))

(let [ui-table-report-layout (comp/factory TableReportLayout {:keyfn ::report/idx})]
  (defn render-table-report-layout [report-instance]
    (ui-table-report-layout {:report-instance report-instance})))

;;; Control style ::SansPaginationStandardControls -- a custom control bar.
;; Just the input controls, in `ro/control-layout` order: no title, action
;; buttons or pagination. Pair it with a layout that renders pagination
;; itself, like the one above.

(comp/defsc SansPaginationStandardControls [_ {:keys [report-instance]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls report-instance)
        {:keys [input-layout]} (control/standard-control-layout report-instance)
        {:com.fulcrologic.rad.container/keys [controlled?]} (comp/get-computed report-instance)]
    (div {:className "flex flex-col gap-2"}
      (map-indexed
       (fn [idx row]
         (div {:key idx :className "flex flex-wrap items-end gap-3"}
           (keep #(let [c (get controls %)]
                    (when (and c (or (not controlled?) (:local? c)))
                      (control/render-control report-instance % c)))
                 row)))
       input-layout))))

(let [ui-standard-report-controls (comp/factory SansPaginationStandardControls)]
  (defn render-standard-controls [report-instance]
    (ui-standard-report-controls {:report-instance report-instance})))
