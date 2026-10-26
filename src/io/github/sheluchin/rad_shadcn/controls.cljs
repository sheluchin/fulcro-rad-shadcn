(ns io.github.sheluchin.rad-shadcn.controls
  "shadcn renderers for RAD report controls (`ro/controls`): :string, :picker,
  :multi-picker and :button. Ported from fulcro-rad-semantic-ui's controls.*; the control
  options they read are the same, except `:icon` is a lucide-react component
  instead of a Semantic icon class."
  (:require
   ["lucide-react/dist/esm/icons/check.mjs" :default Check]
   ["lucide-react/dist/esm/icons/chevrons-up-down.mjs" :default ChevronsUpDown]
   ["lucide-react/dist/esm/icons/list-filter.mjs" :default ListFilter]
   ["lucide-react/dist/esm/icons/x.mjs" :default X]
   [clojure.string :as str]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.dom.events :as evt]
   [com.fulcrologic.fulcro.raw.components :as rc]
   [com.fulcrologic.rad.control :as control]
   [com.fulcrologic.rad.options-util :refer [?!]]
   [com.fulcrologic.rad.picker-options :as po]
   [io.github.sheluchin.rad-shadcn.fuzzy :as fuzzy]
   [io.github.sheluchin.rad-shadcn.ui :as ui]))

(defn- field
  "Label (if any) stacked over the input."
  [control-key label input]
  (dom/div {:key (str control-key) :className "flex min-w-0 flex-1 flex-col gap-1.5"}
    (when label (ui/label {} label))
    input))

(defn- internal-store-name [control-key]
  (keyword (str 'io.github.sheluchin.rad-shadcn.controls.text-input_ (namespace control-key))
           (name control-key)))

(defn clear!
  "Empty the text control `control-key` of report `instance` and run its
  onChange, as if submitted empty (for the search box's clear button and the
  empty-results message)."
  [instance control-key]
  (let [{:keys [onChange]} (get (control/component-controls instance) control-key)]
    (control/set-parameter! instance control-key "")
    (control/set-parameter! instance (internal-store-name control-key) {:last-sent-value ""})
    (when onChange (onChange instance ""))))

(defsc TextControl [_ {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls instance)
        {:keys [label onChange icon placeholder disabled? visible? user-props] :as control} (get controls control-key)]
    (when control
      (let [label       (?! label instance)
            disabled?   (?! disabled? instance)
            placeholder (?! placeholder)
            visible?    (or (nil? visible?) (?! visible? instance))
            value       (control/current-value instance control-key)
            {:keys [last-sent-value]} (control/current-value instance (internal-store-name control-key))
            chg!        #(control/set-parameter! instance control-key (evt/target-value %))
            ;; only run onChange (which usually reloads/filters) on submit or on
            ;; blur with a changed value, not on every keystroke. Nothing sent
            ;; yet counts as "": tabbing through an empty box mustn't reload
            ;; the report, which re-renders it and drops keyboard focus to
            ;; the top of the page (WCAG 2.4.3).
            run!        (fn [run-if-unchanged? v]
                          (when (and onChange (or run-if-unchanged? (not= (str v) (str last-sent-value))))
                            (control/set-parameter! instance control-key v)
                            (control/set-parameter! instance (internal-store-name control-key) {:last-sent-value v})
                            (onChange instance v)))]
        (when visible?
          (field control-key label
                 ;; a form, so the Go/Enter key of phone keyboards submits it
                 ;; (they don't reliably send an Enter keydown)
                 (dom/form {:className "relative"
                            :onSubmit  (fn [evt]
                                         (evt/prevent-default! evt)
                                         ;; read the input: `value` is from the last render
                                         (run! true (.-value (.querySelector (.-target evt) "input"))))}
                   (when icon
                     (ui/icon icon {:className "pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"}))
                   (ui/input (merge {:enterKeyHint   "search"
                                     :autoCapitalize "off"
                                     :autoCorrect    "off"
                                     :spellCheck     false}
                                    user-props
                                    {:readOnly    (boolean disabled?)
                                     :placeholder (str placeholder)
                                     ;; a placeholder isn't a label (WCAG 3.3.2, 4.1.2)
                                     :aria-label  (when-not label (str placeholder))
                                     :className   (ui/cn (when icon "pl-9") (when-not (str/blank? (str value)) "pr-10"))
                                     :onChange    chg!
                                     :onBlur      #(run! false (evt/target-value %))
                                     :value       (str value)}))
                   (when-not (or disabled? (str/blank? (str value)))
                     (ui/button {:type       "button"
                                 :variant    :ghost
                                 :size       :icon-sm
                                 :aria-label "Clear search"
                                 :className  "absolute top-1/2 right-1 -translate-y-1/2 text-muted-foreground"
                                 ;; the button goes away, so keep focus in the box
                                 :onMouseDown evt/prevent-default!
                                 :onClick    (fn [e]
                                               (some-> (.. e -currentTarget -parentNode) (.querySelector "input") .focus)
                                               (clear! instance control-key))}
                       (ui/icon X))))))))))

(def render-text-control (comp/factory TextControl {:keyfn :control-key}))

(defsc PickerControl [_ {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)
   :componentDidMount     (fn [this]
                            (let [{:keys [instance control-key] :as props} (comp/props this)
                                  {::po/keys [query-key] :as picker-options} (get (control/component-controls instance) control-key)]
                              (when query-key
                                (po/load-picker-options! instance (comp/react-type instance) props picker-options))))}
  (let [controls (control/component-controls instance)
        {:keys [label onChange disabled? visible? action options user-props] :as control} (get controls control-key)]
    (when control
      (let [options   (or (?! options instance) (po/current-picker-options instance control))
            visible?  (or (nil? visible?) (?! visible? instance))]
        (when visible?
          (field control-key (?! label instance)
                 (ui/native-select (merge user-props
                                          {:disabled (boolean (?! disabled? instance))
                                           :options  options
                                           :value    (control/current-value instance control-key)
                                           :onChange (fn [v]
                                                       (control/set-parameter! instance control-key v)
                                                       (binding [rc/*after-render* true]
                                                         (when onChange (onChange instance v))
                                                         (when action (action instance))))}))))))))

(def render-picker-control (comp/factory PickerControl {:keyfn :control-key}))

(def ^:private max-shown
  "Options listed at once; typing narrows the rest."
  50)

(defn- toggle
  "`selected` with `v` added, or removed if it's there."
  [selected v]
  (if (some #{v} selected) (vec (remove #{v} selected)) (conj (vec selected) v)))

(defsc MultiPickerControl
  "Pick any number of options in a dropdown with a fuzzy search box; the
  picked ones show as removable chips. The value is a vector of option
  values (none: no filter). Options load when the dropdown first opens, with
  the control's picker options (po/query-key, po/options-xform giving
  {:text :value :count}, ...). Arrow keys move, Enter toggles."
  [this {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)
   :initLocalState        (fn [_] {:query "" :active 0 :open? false})}
  (let [controls (control/component-controls instance)
        {:keys [label onChange placeholder search-placeholder visible?] :as control} (get controls control-key)]
    (when (and control (or (nil? visible?) (?! visible? instance)))
      (let [{:keys [query active open?]} (comp/get-state this)
            options   (or (po/current-picker-options instance control) [])
            selected  (vec (control/current-value instance control-key))
            matches   (fuzzy/search query :text options)
            shown     (vec (take max-shown matches))
            active    (min active (max 0 (dec (count shown))))
            text-of   (into {} (map (juxt :value :text)) options)
            pick!     (fn [v]
                        (let [new-value (toggle selected v)]
                          (control/set-parameter! instance control-key new-value)
                          (when onChange (onChange instance new-value))))
            set-open! (fn [open?]
                        (comp/set-state! this {:open? open? :query "" :active 0})
                        (when open?
                          (po/load-picker-options! instance (comp/react-type instance) (comp/props instance) control)))
            on-key    (fn [e]
                        (case (.-key e)
                          "ArrowDown" (do (.preventDefault e)
                                          (comp/set-state! this {:active (min (inc active) (dec (count shown)))}))
                          "ArrowUp"   (do (.preventDefault e)
                                          (comp/set-state! this {:active (max 0 (dec active))}))
                          "Enter"     (do (.preventDefault e)
                                          (some-> (get shown active) :value pick!))
                          nil))
            list-id   (str "picker-" (namespace control-key) "-" (name control-key))]
        (field control-key (?! label instance)
               (dom/div {:className "flex flex-wrap items-center gap-2"}
                 (ui/popover
                  {:open?          open?
                   :on-open-change set-open!
                   :className      "flex w-80 max-w-[calc(100vw-2rem)] flex-col p-0"
                   :content
                   (comp/fragment
                    (dom/div {:className "border-b p-2"}
                      (ui/input {:autoFocus          true
                                 :value              query
                                 :placeholder        (str (or search-placeholder "Search"))
                                 :aria-label         (str (or search-placeholder "Search"))
                                 :role               "combobox"
                                 :aria-expanded      true
                                 :aria-controls      list-id
                                 :aria-activedescendant (when (seq shown) (str list-id "-" active))
                                 :autoCapitalize     "off"
                                 :autoCorrect        "off"
                                 :spellCheck         false
                                 :onKeyDown          on-key
                                 :onChange           #(comp/set-state! this {:query (evt/target-value %) :active 0})}))
                    (dom/div {:id                   list-id
                              :role                 "listbox"
                              :aria-multiselectable true
                              :className            "max-h-72 overflow-y-auto p-1"}
                      (cond
                        (empty? options) (dom/div {:className "flex justify-center p-4"} (ui/spinner {}))
                        (empty? shown)   (dom/div {:className "p-3 text-sm text-muted-foreground"} "No matches")
                        :else
                        (map-indexed
                         (fn [i {:keys [text value count]}]
                           (let [picked? (boolean (some #{value} selected))]
                             (dom/div {:key           (str value)
                                       :id            (str list-id "-" i)
                                       :role          "option"
                                       :aria-selected picked?
                                       :onMouseEnter  #(comp/set-state! this {:active i})
                                       :onClick       #(pick! value)
                                       :className     (ui/cn "flex cursor-pointer items-center gap-2 rounded-sm px-2 py-1.5 text-sm"
                                                             (when (= i active) "bg-accent text-accent-foreground"))}
                               (dom/span {:className (ui/cn "flex size-4 shrink-0 items-center justify-center rounded-sm border"
                                                            (if picked? "border-primary bg-primary text-primary-foreground" "border-input"))}
                                 (when picked? (ui/icon Check {:className "size-3"})))
                               (dom/span {:className "min-w-0 flex-1 truncate"} text)
                               (when count
                                 (dom/span {:className "text-xs text-muted-foreground tabular-nums"}
                                   (ui/format-number count))))))
                         shown)))
                    (when (> (clojure.core/count matches) max-shown)
                      (dom/div {:className "border-t px-3 py-2 text-xs text-muted-foreground"}
                        (str "Showing " max-shown " of " (ui/format-number (clojure.core/count matches))
                             ". Type to narrow."))))}
                  (ui/button {:variant       :outline
                              :size          :sm
                              :aria-haspopup "listbox"}
                    (ui/icon ListFilter)
                    (if (seq selected)
                      (str (clojure.core/count selected) (if (= 1 (clojure.core/count selected)) " repo" " repos"))
                      (str (or placeholder "Filter")))
                    (ui/icon ChevronsUpDown {:className "text-muted-foreground"})))
                 (map (fn [v]
                        (ui/badge {:key (str v) :variant :secondary :className "gap-1 pr-1 font-normal"}
                          (get text-of v v)
                          (dom/button {:type       "button"
                                       :aria-label (str "Remove " (get text-of v v))
                                       :onClick    #(pick! v)
                                       :className  (ui/cn "rounded-sm opacity-60 hover:opacity-100" ui/focus-ring)}
                            (ui/icon X {:className "size-3"}))))
                      selected)
                 (when (seq selected)
                   (ui/button {:variant   :ghost
                               :size      :sm
                               :className "text-muted-foreground"
                               :onClick   (fn []
                                            (control/set-parameter! instance control-key [])
                                            (when onChange (onChange instance [])))}
                     "Clear"))))))))

(def render-multi-picker-control (comp/factory MultiPickerControl {:keyfn :control-key}))

(defsc ActionButton [_ {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls instance)
        props    (comp/props instance)
        {:keys [label icon variant action disabled? visible?] :as control} (get controls control-key)]
    (when control
      (let [loading?  (df/loading? (get-in props [df/marker-table (comp/get-ident instance)]))
            visible?  (or (nil? visible?) (?! visible? instance))]
        (when visible?
          (ui/button {:key      (str control-key)
                      :variant  (or variant :outline)
                      :size     :sm
                      :disabled (boolean (or loading? (?! disabled? instance)))
                      :onClick  (fn [] (when action (action instance control-key)))}
                     (cond loading? (ui/spinner {})
                           icon     (ui/icon icon))
                     (?! label instance)))))))

(def render-action-button (comp/factory ActionButton {:keyfn :control-key}))
