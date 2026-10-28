(ns io.github.sheluchin.rad-shadcn.all-controls
  "The shadcn renderer's control map, the counterpart of fulcro-rad-semantic-ui's
  `semantic-ui-controls/all-controls`: install it with

  ```
  (rad-app/install-ui-controls! app all-controls)
  ```

  Everything is under the :default style. Add your own styles next to it (see
  examples/custom_styles.cljs in this repo) and opt a report in with
  `ro/layout-style`, `::report/control-style` or `ro/row-style`. Reports and
  their controls only: there's no RAD form rendering yet."
  (:require
   [io.github.sheluchin.rad-shadcn.controls :as controls]
   [io.github.sheluchin.rad-shadcn.report :as report]))

(def all-controls
  {:com.fulcrologic.rad.report/style->layout
   {:default report/render-table-report-layout}

   :com.fulcrologic.rad.report/control-style->control
   {:default report/render-standard-controls}

   :com.fulcrologic.rad.report/row-style->row-layout
   {:default report/render-table-row}

   :com.fulcrologic.rad.control/type->style->control
   {:string {:default controls/render-text-control}
    :picker {:default controls/render-picker-control}
    :multi-picker {:default controls/render-multi-picker-control
                   :toggle-chips controls/render-toggle-chips-control}
    :button {:default controls/render-action-button}
    :boolean {:default controls/render-boolean-control}}})

(defn merge-controls
  "Deep-merge control maps (each level is a map of keyword -> map or render fn)."
  [& maps]
  (apply merge-with (fn [a b] (if (and (map? a) (map? b)) (merge-controls a b) b)) maps))
