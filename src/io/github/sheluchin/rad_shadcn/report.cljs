(ns io.github.sheluchin.rad-shadcn.report
  "shadcn table layout for RAD reports: a toolbar (title, input controls,
  action buttons, pagination) over a bordered table. Ported from
  fulcro-rad-semantic-ui's report ns, minus the list and rotated layouts (not
  ported yet). Options: io.github.sheluchin.rad-shadcn.options."
  (:require
   ["lucide-react/dist/esm/icons/arrow-down.mjs" :default ArrowDown]
   ["lucide-react/dist/esm/icons/arrow-up.mjs" :default ArrowUp]
   [clojure.string :as str]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.dom.events :as evt]
   [com.fulcrologic.rad.attributes :as attr]
   [com.fulcrologic.rad.control :as control]
   [com.fulcrologic.rad.form :as form]
   [com.fulcrologic.rad.options-util :refer [?!]]
   [com.fulcrologic.rad.report :as report]
   [com.fulcrologic.rad.report-options :as ro]
   [com.fulcrologic.rad.routing :as rad-routing]
   [io.github.sheluchin.rad-shadcn.controls :as controls]
   [io.github.sheluchin.rad-shadcn.options :as rro]
   [io.github.sheluchin.rad-shadcn.ui :as ui]))

(defn- mobile-class
  "Hides the column on phones if the report lists it in `rro/hide-on-mobile`."
  [report-instance {::attr/keys [qualified-key]}]
  (when (contains? (rro/hide-on-mobile (comp/component-options report-instance)) qualified-key)
    "max-md:hidden"))

(defn- alignment-class [report-instance column]
  (case (?! (get column ro/column-alignment) report-instance column)
    :right "text-right"
    :center "text-center"
    nil))

(defn row-action-buttons [report-instance row-props]
  (let [{::report/keys [row-actions]} (comp/component-options report-instance)]
    (when (seq row-actions)
      (dom/div {:className "flex justify-end gap-1"}
        (map-indexed
         (fn [idx {:keys [label reload? visible? disabled? action]}]
           (when (or (nil? visible?) (?! visible? report-instance row-props))
             (ui/button {:key      idx
                         :variant  :ghost
                         :size     :sm
                         :disabled (boolean (?! disabled? report-instance row-props))
                         :onClick  (fn [e]
                                     (evt/stop-propagation! e)
                                     (when action
                                       (action report-instance row-props)
                                       (when reload? (control/run! report-instance))))}
                        (?! label report-instance row-props))))
         row-actions)))))

;;; Rows

(comp/defsc TableRowLayout [_ {:keys [report-instance props]}]
  {}
  (let [{::report/keys [columns link links on-select-row] :as options} (comp/component-options report-instance)
        links          (or links link)
        cell-class     (rro/cell-class options)
        selectable?    (not (false? (rro/selectable-rows? options)))
        action-buttons (row-action-buttons report-instance props)
        {:keys [highlighted?] ::report/keys [idx]} (comp/get-computed props)]
    (ui/table-row (cond-> {:selected? highlighted?
                           :disabled? (?! (rro/row-disabled? options) report-instance props)
                           :onClick   (fn [e]
                                        (evt/stop-propagation! e)
                                        (when selectable?
                                          (?! on-select-row report-instance props)
                                          (report/select-row! report-instance idx)))}
                    ;; Clickable rows are often the only way to open the row's
                    ;; page, so they must work from the keyboard too (WCAG
                    ;; 2.1.1): focusable, Enter/Space act like a click. Keys
                    ;; pressed on a control inside the row are left to it.
                    (and selectable? on-select-row)
                    (assoc :tabIndex  0
                           :className (ui/cn "cursor-pointer" ui/focus-ring "focus-visible:-outline-offset-2")
                           :onKeyDown (fn [e]
                                        (when (and (#{"Enter" " "} (.-key e))
                                                   (identical? (.-target e) (.-currentTarget e)))
                                          (evt/prevent-default! e)
                                          (?! on-select-row report-instance props)
                                          (report/select-row! report-instance idx)))))
      (map-indexed
       (fn [col-idx {::attr/keys [qualified-key] :as column}]
         (ui/table-cell {:key       (str "col-" qualified-key)
                         :className (ui/cn (alignment-class report-instance column)
                                           (mobile-class report-instance column)
                                           (report/column-classes report-instance column)
                                           (?! cell-class report-instance col-idx))}
           (let [{:keys [edit-form entity-id]} (report/form-link report-instance props qualified-key)
                 link-fn (get links qualified-key)
                 value   (get props qualified-key)
                 label   (let [l (report/formatted-column-value report-instance props column)]
                           ;; counts get thousands separators, unless a
                           ;; column formatter already made something else
                           (cond (number? l)                          (ui/format-number l)
                                 (and (number? value) (= l (str value))) (ui/format-number value)
                                 :else                                l))
                 ;; a button: an <a> without href isn't focusable
                 link!   (fn [f] (dom/button {:type      "button"
                                              :className (ui/cn "cursor-pointer text-left font-medium text-foreground" ui/link-class)
                                              :onClick   (fn [e] (evt/stop-propagation! e) (f))}
                                   label))]
             (cond
               edit-form (link! #(form/edit! report-instance edit-form entity-id))
               (fn? link-fn) (link! #(link-fn report-instance props))
               :else label))))
       columns)
      (when action-buttons
        (ui/table-cell {:key "actions" :className "w-px whitespace-nowrap"}
          action-buttons)))))

(let [ui-table-row-layout (comp/factory TableRowLayout)]
  (defn render-table-row [report-instance row-class row-props]
    (ui-table-row-layout {:report-instance report-instance
                          :row-class       row-class
                          :props           row-props})))

;;; Table

(defn- sort-state
  "{:sortable? (fn [column]) :sorting-by qualified-key :ascending? bool}"
  [report-instance]
  (let [{::report/keys [compare-rows]} (comp/component-options report-instance)
        {:keys [sortable-columns sort-by ascending?]} (-> (comp/props report-instance) :ui/parameters ::report/sort)
        sortable? (cond
                    (not compare-rows) (constantly false)
                    sortable-columns #(contains? (set sortable-columns) (::attr/qualified-key %))
                    :else (constantly true))]
    {:sortable? sortable? :sorting-by sort-by :ascending? ascending?}))

(defn- column-head [report-instance {:keys [sortable? sorting-by ascending?]} header-class idx {:keys [label help column]}]
  (let [sorted?   (= sorting-by (::attr/qualified-key column))
        classes   (ui/cn (alignment-class report-instance column)
                         (mobile-class report-instance column)
                         (?! header-class report-instance idx))
        ;; In right-aligned (numeric) columns the info icon and sort arrow go
        ;; before the label, so the label's right edge lines up with the
        ;; numbers under it; after it, the numbers sat under the icon.
        right?    (str/includes? (str classes) "text-right")]
    (ui/table-head {:key       idx
                    :aria-sort (when sorted? (if ascending? "ascending" "descending"))
                    ;; pl-5: room between the leading icon and the previous
                    ;; column's label, so the icon reads as this column's
                    :className (ui/cn classes (when (and right? (or help (sortable? column))) "pl-5"))}
      (dom/span {:className (ui/cn "inline-flex items-center gap-1" (when right? "flex-row-reverse"))}
        (if (sortable? column)
          (ui/button {:variant   :ghost
                      :size      :sm
                      ;; the negative margin cancels the padding on the
                      ;; aligned side, so the label (not the button) aligns
                      :className (ui/cn "h-8 px-3 has-[>svg]:px-3"
                                        (if right? "-mr-3 flex-row-reverse" "-ml-3")
                                        (when sorted? "text-foreground"))
                      :onClick   (fn [e]
                                   (evt/stop-propagation! e)
                                   (report/sort-rows! report-instance column))}
                     label
                     (when sorted? (ui/icon (if ascending? ArrowUp ArrowDown))))
          label)
        (when help
          (ui/info-tooltip {:label (str "About " label) :content help}))))))

(defn- active-search
  "[control-key text] of the report's non-empty search box, if any."
  [report-instance]
  (some (fn [[k {:keys [type]}]]
          (when (= :string type)
            (let [v (str (control/current-value report-instance k))]
              (when-not (str/blank? v) [k v]))))
        (control/component-controls report-instance)))

(defn- empty-message
  "What an empty table says: names the search when one is the reason, with a
  way out."
  [report-instance]
  (if-let [[k text] (active-search report-instance)]
    (dom/div {:className "flex flex-col items-center gap-2"}
      (dom/span {} (str "No matches for “" text "”."))
      (ui/button {:variant :outline :size :sm
                  :onClick (fn [e] (evt/stop-propagation! e) (controls/clear! report-instance k))}
        "Clear search"))
    "No results."))

(defn render-table [this {:keys [report-instance]}]
  (let [{::report/keys [row-actions BodyItem query-inclusions] :as options} (comp/component-options report-instance)
        render-row      ((comp/get-state this :row-factory) BodyItem)
        header-class    (rro/header-class options)
        sorting         (sort-state report-instance)
        rows            (report/current-rows report-instance)
        selected-idx    (report/currently-selected-row report-instance)
        inclusion-props (select-keys (comp/props report-instance) query-inclusions)]
    (ui/table {}
      (ui/table-header {}
        (dom/tr
          (map-indexed (partial column-head report-instance sorting header-class)
                       (report/column-heading-descriptors report-instance options))
          (when (seq row-actions) (ui/table-head {:className "w-px"}))))
      (ui/table-body {}
        (when (= 1 (report/current-page report-instance))
          (when-let [pinned-row (::rro/pinned-row options)]
            (pinned-row report-instance)))
        (if (seq rows)
          (map-indexed
           (fn [idx row]
             (render-row row (merge inclusion-props
                                    {:report-instance report-instance
                                     :row-class       BodyItem
                                     :highlighted?    (= idx selected-idx)
                                     ::report/idx     idx})))
           rows)
          (dom/tr
            (dom/td {:colSpan   (+ (count (ro/columns options)) (if (seq row-actions) 1 0))
                     :className "h-24 text-center text-muted-foreground"}
              (when-not (report/loading? report-instance) (empty-message report-instance)))))))))

;;; Toolbar

(defn- total-results [report-instance]
  ;; the state machine keeps the total in app state, outside the report's query
  (get-in (comp/component->state-map report-instance)
          (conj (comp/get-ident report-instance) :ui/total-results)))

(defn- page-range
  "\"Showing 26–50 of 132\" for the current page, when the total is known."
  [report-instance]
  (let [total (total-results report-instance)
        size  (rro/current-page-size report-instance)
        page  (report/current-page report-instance)]
    (when (and (number? total) (pos? total))
      (dom/p {:className "m-0 text-sm text-muted-foreground tabular-nums"}
        (str "Showing " (.toLocaleString (inc (* (dec page) size)))
             "–" (.toLocaleString (min total (* page size)))
             " of " (.toLocaleString total))))))

(defn- page-size-select
  "Rows-per-page choice, for reports whose `ro/page-size` is
  `rro/rows-per-page`, once there's more than the smallest page to show.
  The choice goes in the URL (?size=), which is where the page size is read
  from; then the report reloads from page 1."
  [report-instance]
  (let [default (::rro/default (meta (comp/component-options report-instance ::report/page-size)))
        total   (total-results report-instance)]
    (when (and default (number? total) (> total (first rro/page-size-choices)))
      (ui/label {:className "font-normal text-muted-foreground"}
        "Rows per page"
        (ui/native-select
         {:value     (rro/current-page-size report-instance)
          :options   (map (fn [n] {:text (str n) :value n}) rro/page-size-choices)
          :className "h-8"
          :onChange  (fn [n]
                       (let [id (second (comp/get-ident report-instance))]
                         (rad-routing/update-route-params!
                          report-instance
                          (fn [p] (update p id #(cond-> (dissoc % ::rro/rows-per-page ::report/current-page)
                                                   (not= n default) (assoc ::rro/rows-per-page n)))))
                         (report/goto-page! report-instance 1)
                         (control/run! report-instance)))})))))

(defn render-pagination
  "Page range and pager for a paginated report, or nil when it fits on one
  page. Rendered above the table (in the toolbar) and below it; `label` tells
  the two nav landmarks apart and `on-page-change` runs after the page
  changes. `sizes?` adds the rows-per-page choice (shown even on one page, so
  a big choice can be undone)."
  [report-instance & {:keys [label on-page-change sizes?]}]
  (let [page-count (report/page-count report-instance)
        sizes      (when sizes? (page-size-select report-instance))]
    (when (or (> page-count 1) sizes)
      (dom/div {:className "flex flex-wrap items-center justify-end gap-x-4 gap-y-2"}
        sizes
        (page-range report-instance)
        (when (> page-count 1)
          (ui/pagination {:current-page   (report/current-page report-instance)
                          :page-count     page-count
                          :label          label
                          :on-page-change (fn [p]
                                            (report/goto-page! report-instance p)
                                            (when on-page-change (on-page-change p)))}))))))

(comp/defsc StandardControls [_ {:keys [report-instance]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [{::report/keys [paginate? title] :as options} (comp/component-options report-instance)
        controls (control/component-controls report-instance)
        {:keys [input-layout action-layout]} (control/standard-control-layout report-instance)
        {:com.fulcrologic.rad.container/keys [controlled?]} (comp/get-computed report-instance)
        shown?   (fn [k] (let [c (get controls k)]
                           (and c (or (not controlled?) (:local? c)))))]
    (dom/div {:className "flex flex-col gap-3"}
      ;; only reports that are the whole page show their title, so it's the
      ;; page's h1
      (when (and (rro/show-title? options) title)
        (dom/h1 {:className "m-0 text-lg font-semibold tracking-tight"}
          (?! title report-instance)))
      (dom/div {:className "flex flex-wrap items-end gap-3"}
        (dom/div {:className "flex min-w-64 flex-1 flex-col gap-2"}
          (map-indexed
           (fn [idx row]
             (dom/div {:key idx :className "flex flex-wrap items-end gap-3"}
               (keep #(when (shown? %) (control/render-control report-instance % (get controls %))) row)))
           input-layout))
        (when (seq action-layout)
          (dom/div {:className "flex items-center gap-2"}
            (keep (fn [k]
                    (when (and (shown? k) (?! (get-in controls [k :visible?] true) report-instance))
                      (control/render-control report-instance k (get controls k))))
                  action-layout)))
        (when paginate?
          (render-pagination report-instance))))))

(let [ui-standard-controls (comp/factory StandardControls)]
  (defn render-standard-controls [report-instance]
    (ui-standard-controls {:report-instance report-instance})))

;;; Layout

(defn init-row-factory-state
  "`:initLocalState` for a layout component that calls `render-table`, which
  reads a memoized row factory from local state."
  [_]
  {:row-factory (memoize
                 (fn [cls]
                   (comp/computed-factory cls
                     {:keyfn (fn [props]
                               (some-> props
                                       (comp/get-computed ::report/idx)
                                       (str "-" (comp/get-ident cls props))))})))})

(comp/defsc TableReportLayout [this {:keys [report-instance] :as env}]
  {:initLocalState        init-row-factory-state
   :shouldComponentUpdate (fn [_ _ _] true)}
  (let [options         (comp/component-options report-instance)
        render-controls (report/control-renderer report-instance)
        busy?           (or (:ui/busy? (comp/props report-instance)) (report/loading? report-instance))]
    (dom/div {:className (ui/cn "flex flex-col gap-3" (?! (rro/layout-class options) report-instance))
              :ref       #(set! (.-layoutEl ^js this) %)}
      (when render-controls
        (render-controls report-instance))
      (dom/div {:className "relative rounded-md border bg-card" :aria-busy (boolean busy?)}
        (when busy?
          (dom/div {:className "absolute inset-0 z-10 flex items-center justify-center rounded-md bg-background/60"}
            (ui/spinner {:className "size-6 text-muted-foreground"})))
        (render-table this env))
      ;; a second pager under the table, so a long page needn't be scrolled
      ;; back up; it takes you to the top of the report, where the new page
      ;; starts (unless that's already in view)
      (when (::report/paginate? options)
        (render-pagination report-instance
                           :label "Pagination (bottom)"
                           :sizes? true
                           :on-page-change (fn [_]
                                             (when-let [el (.-layoutEl ^js this)]
                                               (when (neg? (.-top (.getBoundingClientRect el)))
                                                 (.scrollIntoView el #js {:block "start"})))))))))

(let [ui-table-report-layout (comp/factory TableReportLayout {:keyfn ::report/idx})]
  (defn render-table-report-layout [report-instance]
    (ui-table-report-layout {:report-instance report-instance})))
