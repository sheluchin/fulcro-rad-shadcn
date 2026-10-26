(ns io.github.sheluchin.rad-shadcn.ui
  "shadcn/ui-style components for Fulcro: Tailwind class recipes from
  https://ui.shadcn.com, plus Radix for the interactive bits.

  Each fn takes a props map (or nil) and children, like Fulcro's DOM fns, and
  merges any `:className` you pass over its own classes (tailwind-merge resolves
  conflicts, so `{:className \"h-8\"}` overrides a default height)."
  (:require
   ["@radix-ui/react-dialog" :as dialog]
   ["@radix-ui/react-popover" :as popover]
   ["@radix-ui/react-tooltip" :as tooltip]
   ["lucide-react/dist/esm/icons/chevron-down.mjs" :default ChevronDown]
   ["lucide-react/dist/esm/icons/chevron-left.mjs" :default ChevronLeft]
   ["lucide-react/dist/esm/icons/chevron-right.mjs" :default ChevronRight]
   ["lucide-react/dist/esm/icons/ellipsis.mjs" :default Ellipsis]
   ["lucide-react/dist/esm/icons/info.mjs" :default Info]
   ["lucide-react/dist/esm/icons/loader-circle.mjs" :default LoaderCircle]
   ["lucide-react/dist/esm/icons/x.mjs" :default X]
   ["tailwind-merge" :refer [extendTailwindMerge]]
   [clojure.string :as str]
   [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
   [com.fulcrologic.fulcro.dom :as dom]))

(def ^:private tw-merge (extendTailwindMerge #js {}))

(defn cn
  "Join class strings (nils and falses are dropped), resolving Tailwind
  conflicts in favor of later classes."
  [& classes]
  (tw-merge (str/join " " (filter string? classes))))

(defn- with-class
  "`props` minus `ks`, with `classes` merged under the caller's :className."
  [props ks & classes]
  (-> (apply dissoc props ks)
      (assoc :className (apply cn (concat classes [(:className props)])))))

;;; Accessibility
;; Targets for WCAG 2.2 AA. Contrast lives in the color tokens
;; (resources/tailwind/app.css); these are the class recipes.

(def focus-ring
  "Focus indicator: the base layer's 2px --ring outline, restated as utilities
  for elements whose own classes would otherwise win."
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring")

(def min-target
  "WCAG 2.5.8's 24px minimum, in px: the root font is 14px, so the rem-based
  size-6 / min-h-6 would only be 21px."
  "min-h-[24px] min-w-[24px]")

(def link-class
  "A text link: underlined at rest, not just on hover, so it isn't identified
  by color alone (WCAG 1.4.1), and at least a 24px target (WCAG 2.5.8)."
  (str min-target " inline-flex items-center rounded-sm underline decoration-current/40"
       " underline-offset-4 hover:decoration-current " focus-ring))

;;; Icons

(def ^:private icon-factory (memoize interop/react-factory))

(defn icon
  "Render a lucide-react icon component, e.g.
  `(icon Search {:className \"size-4\"})`. Require icons one file at a time
  (`[\"lucide-react/dist/esm/icons/search.mjs\" :default Search]`) so the
  bundle only gets the ones we use."
  ([Icon] (icon Icon nil))
  ([Icon props] ((icon-factory Icon) (merge {:aria-hidden true} props))))

;;; Button

(def ^:private button-variants
  {:default     "bg-primary text-primary-foreground shadow-xs hover:bg-primary/90"
   :destructive "bg-destructive text-white shadow-xs hover:bg-destructive/90"
   :outline     "border bg-background text-foreground shadow-xs hover:bg-accent hover:text-accent-foreground"
   :secondary   "bg-secondary text-secondary-foreground shadow-xs hover:bg-secondary/80"
   :ghost       "text-foreground hover:bg-accent hover:text-accent-foreground"
   :link        "text-primary underline-offset-4 hover:underline"})

(def ^:private button-sizes
  {:default "h-9 px-4 py-2 has-[>svg]:px-3"
   :sm      "h-8 gap-1.5 px-3 has-[>svg]:px-2.5"
   :lg      "h-10 px-6 has-[>svg]:px-4"
   :icon    "size-9"
   :icon-sm "size-8"})

(defn button-class
  "Classes for a button; use on an `a` or other element that should look like one."
  [{:keys [variant size] :or {variant :default size :default}}]
  (cn "inline-flex shrink-0 cursor-pointer items-center justify-center gap-2 whitespace-nowrap"
      ;; not transition-all: that would animate the focus outline in from 0
      "rounded-md text-sm font-medium transition-[color,background-color,border-color,box-shadow,opacity]"
      focus-ring
      "disabled:pointer-events-none disabled:opacity-50"
      "[&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4"
      (button-variants variant)
      (button-sizes size)))

(defn button
  "`:variant` one of :default :destructive :outline :secondary :ghost :link;
  `:size` one of :default :sm :lg :icon :icon-sm."
  [props & children]
  (apply dom/button
         (-> (with-class props [:variant :size] (button-class props))
             (update :type #(or % "button")))
         children))

;;; Badge

(def ^:private badge-variants
  {:default     "border-transparent bg-primary text-primary-foreground"
   :secondary   "border-transparent bg-secondary text-secondary-foreground"
   :destructive "border-transparent bg-destructive text-white"
   :outline     "text-foreground"})

(defn badge
  "`:variant` one of :default :secondary :destructive :outline."
  [{:keys [variant] :or {variant :default} :as props} & children]
  (apply dom/span
         (with-class props [:variant]
           "inline-flex w-fit shrink-0 items-center justify-center gap-1 overflow-hidden"
           "rounded-md border px-2 py-0.5 text-xs font-medium whitespace-nowrap"
           "[&>svg]:pointer-events-none [&>svg]:size-3"
           (badge-variants variant))
         children))

;;; Card

(defn card [props & children]
  (apply dom/div
         (with-class props []
           "flex flex-col gap-6 rounded-xl border bg-card py-6 text-card-foreground shadow-sm")
         children))

(defn card-header [props & children]
  (apply dom/div (with-class props [] "grid auto-rows-min items-start gap-1.5 px-6") children))

(defn card-title [props & children]
  (apply dom/div (with-class props [] "leading-none font-semibold") children))

(defn card-description [props & children]
  (apply dom/div (with-class props [] "text-sm text-muted-foreground") children))

(defn card-content [props & children]
  (apply dom/div (with-class props [] "px-6") children))

(defn card-footer [props & children]
  (apply dom/div (with-class props [] "flex items-center px-6") children))

;;; Form inputs

(def ^:private input-class
  (cn "h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1"
      "text-base shadow-xs transition-[color,box-shadow] md:text-sm"
      "placeholder:text-muted-foreground dark:bg-input/30"
      "focus-visible:border-ring" focus-ring
      "disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50"))

(defn format-number
  "1615 -> \"1,615\" (the viewer's locale); anything else as a string."
  [n]
  (if (number? n) (.toLocaleString n) (str n)))

(defn input [props]
  (dom/input (with-class props [] input-class)))

(defn label [props & children]
  (apply dom/label
         (with-class props [] "flex items-center gap-2 text-sm leading-none font-medium select-none")
         children))

(defn native-select
  "A styled `<select>`. `options` is a seq of `{:text :value}`; values are
  matched with `pr-str`, so keywords work."
  [{:keys [options value onChange] :as props}]
  (dom/div {:className "relative w-fit"}
    (dom/select
     (-> (with-class props [:options :value :onChange]
           input-class "appearance-none pr-9")
         (assoc :value (pr-str value)
                :onChange (fn [evt]
                            (let [v (.. evt -target -value)]
                              (when onChange
                                (onChange (:value (first (filter #(= v (pr-str (:value %))) options)))))))))
     (map (fn [{:keys [text value]}]
            (dom/option {:key (pr-str value) :value (pr-str value)} text))
          options))
    (icon ChevronDown {:className "pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 opacity-50"})))

;;; Table

(defn table
  "From md up the table doesn't scroll sideways (it fits), so its header can
  stick under the page header; an overflow container would trap it."
  [props & children]
  (dom/div {:className "relative w-full max-md:overflow-x-auto"}
    (apply dom/table (with-class props [] "w-full caption-bottom text-sm") children)))

(defn table-header [props & children]
  (apply dom/thead (with-class props [] "[&_tr]:border-b") children))

(defn table-body [props & children]
  (apply dom/tbody (with-class props [] "[&_tr:last-child]:border-0") children))

(defn table-row
  "`:selected?` highlights the row; `:disabled?` greys it out (muted text, not
  opacity, which would take the text below 4.5:1)."
  [{:keys [selected? disabled?] :as props} & children]
  (apply dom/tr
         (with-class props [:selected? :disabled?]
           "border-b transition-colors hover:bg-muted/50"
           (when selected? "bg-muted")
           (when disabled? "text-muted-foreground [&_img]:opacity-50"))
         children))

(defn table-head [props & children]
  (apply dom/th
         (with-class props []
           "h-10 px-2 text-left align-middle font-medium whitespace-nowrap text-muted-foreground"
           ;; column names stay in view on long pages, under the h-14 page
           ;; header; the inset shadow is the row border, which doesn't stick
           "md:sticky md:top-[var(--rad-shadcn-sticky-top,3.5rem)] md:z-[5] md:bg-card md:shadow-[inset_0_-1px_0_var(--border)]"
           "md:first:rounded-tl-md md:last:rounded-tr-md")
         children))

(defn table-cell [props & children]
  (apply dom/td (with-class props [] "p-2 align-middle") children))

;;; Avatar

(defn avatar
  "A small square image (repo/owner avatars). `max-w-none` matters in tables:
  preflight's `img {max-width: 100%}` makes an image count as 0px wide when the
  table sizes its columns, so the cell overflows into the next one."
  [props]
  (dom/img (with-class (merge {:alt ""} props) []
             "size-7 max-w-none shrink-0 rounded-md border object-cover")))

;;; Feedback

(defn skeleton [props]
  (dom/div (with-class props [] "animate-pulse rounded-md bg-accent")))

(defn spinner [props]
  (icon LoaderCircle (with-class (merge {:role "status" :aria-label "Loading"} props) []
                       "size-4 animate-spin")))

;;; Tooltip

(def ^:private ui-tooltip-provider (interop/react-factory tooltip/Provider))
(def ^:private ui-tooltip-root (interop/react-factory tooltip/Root))
(def ^:private ui-tooltip-trigger (interop/react-factory tooltip/Trigger))
(def ^:private ui-tooltip-portal (interop/react-factory tooltip/Portal))
(def ^:private ui-tooltip-content (interop/react-factory tooltip/Content))

(declare popover info-tooltip)

(def ^:private tooltip-class
  (cn "z-50 w-fit max-w-xs rounded-md bg-primary px-3 py-1.5"
      "text-xs text-balance text-primary-foreground"))

(defn- touch-screen?
  "True when the primary input can't hover (phones, tablets)."
  []
  (boolean (some-> js/window (.matchMedia "(hover: none)") .-matches)))

(defn tooltip
  "Show `content` on hover/focus of `trigger`, which must be a single DOM
  element (Radix attaches its handlers to it). `:className` goes on the
  content box.

  Radix tooltips never open on touch, so on touch screens this renders a
  tap-to-open popover that looks the same."
  [{:keys [content side className] :or {side "top"}} trigger]
  (if (touch-screen?)
    (popover {:content   content
              :side      side
              :align     "center"
              :className (cn "border-0 p-0 shadow-none" tooltip-class className)}
             trigger)
    (ui-tooltip-provider {:delayDuration 200}
      (ui-tooltip-root {}
        (ui-tooltip-trigger {:asChild true} trigger)
        (ui-tooltip-portal {}
          (ui-tooltip-content
           {:side       side
            :sideOffset 4
            :className  (cn tooltip-class className)}
           content))))))

(defn info-tooltip
  "An (i) icon showing `content` on hover/focus. A real button, so it's in the
  tab order and has a name (`:label`, e.g. \"About Usages\"); 24px square
  for WCAG 2.5.8, with negative margins so it doesn't grow the line."
  [{:keys [label content]}]
  (tooltip {:content content}
    (dom/button {:type       "button"
                 :aria-label (or label "More information")
                 :className  (cn "-my-1 inline-flex size-[24px] shrink-0 cursor-help items-center justify-center"
                                 "rounded-sm text-muted-foreground hover:text-foreground" focus-ring)}
      (icon Info {:className "size-3.5"}))))

;;; Popover

(def ^:private ui-popover-root (interop/react-factory popover/Root))
(def ^:private ui-popover-trigger (interop/react-factory popover/Trigger))
(def ^:private ui-popover-portal (interop/react-factory popover/Portal))
(def ^:private ui-popover-content (interop/react-factory popover/Content))

(defn- stop-propagation [e] (.stopPropagation e))

(defn popover
  "Show `content` in a floating panel when `trigger` (a single DOM element) is
  clicked. Clicks on either don't propagate, so this is safe inside a
  clickable table row (React bubbles events out of portals too).
  `:className` goes on the panel. Pass `:open?` and `:on-open-change` to
  control it (a menu that closes when an item is picked)."
  [{:keys [content side align className open? on-open-change] :or {side "bottom" align "start"}} trigger]
  (ui-popover-root (cond-> {}
                     (some? open?)  (assoc :open open?)
                     on-open-change (assoc :onOpenChange on-open-change))
    (ui-popover-trigger {:asChild true :onClick stop-propagation} trigger)
    (ui-popover-portal {}
      (ui-popover-content
       {:side       side
        :align      align
        :sideOffset 4
        :onClick    stop-propagation
        :className  (cn "z-50 w-72 rounded-md border bg-popover p-4 text-sm"
                        "text-popover-foreground shadow-md outline-hidden"
                        className)}
       content))))

;;; Dialog

(def ^:private ui-dialog-root (interop/react-factory dialog/Root))
(def ^:private ui-dialog-portal (interop/react-factory dialog/Portal))
(def ^:private ui-dialog-overlay (interop/react-factory dialog/Overlay))
(def ^:private ui-dialog-content (interop/react-factory dialog/Content))
(def ^:private ui-dialog-title (interop/react-factory dialog/Title))
(def ^:private ui-dialog-description (interop/react-factory dialog/Description))
(def ^:private ui-dialog-close (interop/react-factory dialog/Close))

(defn dialog
  "A modal. Controlled: pass `:open?` and `:on-open-change` (gets the new
  boolean). `:title` is required by Radix for screen readers; `:description`
  is optional. `:className` goes on the panel (e.g. a wider max-w). Clicks
  inside don't propagate (React bubbles them out of the portal), so this can
  live inside a clickable row."
  [{:keys [open? on-open-change title description className]} & children]
  (ui-dialog-root {:open (boolean open?) :onOpenChange on-open-change}
    (ui-dialog-portal {}
      (ui-dialog-overlay {:className "fixed inset-0 z-50 bg-black/50"
                          :onClick   stop-propagation})
      (ui-dialog-content
       (cond-> {:onClick   stop-propagation
                :className (cn "fixed top-1/2 left-1/2 z-50 flex max-h-[85vh] w-full"
                               "max-w-[calc(100%-2rem)] -translate-x-1/2 -translate-y-1/2 flex-col gap-4"
                               "rounded-lg border bg-background p-6 shadow-lg sm:max-w-lg"
                               className)}
         ;; tells Radix the missing Description is intentional
         (not description) (assoc :aria-describedby js/undefined))
       (dom/div {:className "flex flex-col gap-2 pr-8"}
         (ui-dialog-title {:className "m-0 text-lg leading-none font-semibold break-all"} title)
         (when description
           (ui-dialog-description {:className "text-sm text-muted-foreground"} description)))
       (apply dom/div {:className "min-h-0 flex-1 overflow-auto"} children)
       (ui-dialog-close {:asChild true}
         (button {:variant :ghost :size :icon-sm :aria-label "Close"
                  :className "absolute top-4 right-4"}
                 (icon X)))))))

;;; Sheet

(defn sheet
  "A panel that slides over the page from one edge (shadcn's Sheet): a modal
  Radix Dialog, so it traps focus and closes on Escape or an outside click.
  Controlled like `dialog`: `:open?`, `:on-open-change`. `:side` is :left
  (default) or :right. `:title` names it for screen readers; pass
  `:title-hidden? true` when the content already shows what it is (e.g. a
  menu with a logo)."
  [{:keys [open? on-open-change side title title-hidden? className]} & children]
  (ui-dialog-root {:open (boolean open?) :onOpenChange on-open-change}
    (ui-dialog-portal {}
      (ui-dialog-overlay {:className "fixed inset-0 z-50 bg-black/50"
                          :onClick   stop-propagation})
      (ui-dialog-content
       {:onClick          stop-propagation
        :aria-describedby js/undefined
        :className        (cn "fixed inset-y-0 z-50 flex h-full w-3/4 max-w-xs flex-col gap-4"
                              "bg-background p-4 shadow-lg"
                              (if (= side :right) "right-0 border-l" "left-0 border-r")
                              className)}
       (ui-dialog-title {:className (if title-hidden? "sr-only" "m-0 text-lg font-semibold")} title)
       (apply dom/div {:className "min-h-0 flex-1 overflow-auto"} children)
       (ui-dialog-close {:asChild true}
         (button {:variant :ghost :size :icon-sm :aria-label "Close"
                  :className "absolute top-3 right-3"}
                 (icon X)))))))

;;; Tabs
;; Styled like shadcn's Tabs, but just the trigger bar: our tab content is
;; chosen by routing (a dynamic router + UISM), not by Radix. Each "tab" changes
;; the URL, so semantically this is navigation: a <nav> of buttons, the current
;; one marked with aria-current. (role=tab would promise arrow-key navigation
;; and tabpanels we don't have.)

(defn tabs-list
  "`:aria-label` names the nav (default \"Sections\")."
  [props & children]
  (apply dom/nav
         (with-class (merge {:aria-label "Sections"} props) []
           "inline-flex min-h-9 w-fit max-w-full items-stretch justify-start overflow-x-auto rounded-lg bg-muted p-[3px] text-muted-foreground")
         children))

(defn tab-trigger
  "`:active?` marks the current section. Besides the white fill (barely
  different from --muted), the current one gets an --input border, so the
  state is visible at 3:1 (WCAG 1.4.11)."
  [{:keys [active?] :as props} & children]
  (apply dom/button
         (-> (with-class props [:active?]
               "inline-flex cursor-pointer items-center justify-center gap-1.5 rounded-md"
               "border border-transparent px-3 py-1 text-sm max-sm:px-2 font-medium whitespace-nowrap"
               "text-muted-foreground transition-[color,box-shadow] hover:text-foreground"
               focus-ring "focus-visible:outline-offset-0"
               "disabled:pointer-events-none disabled:opacity-50 [&_svg]:size-4 [&_svg]:shrink-0"
               (when active? "border-input bg-background text-foreground shadow-sm"))
             (assoc :type "button" :aria-current (when active? "page")))
         children))

;;; Pagination

(defn- page-items
  "Page numbers to show around `current` (1-based), with :gap for elided runs."
  [current total]
  (let [shown (into (sorted-set) (filter #(<= 1 % total)) [1 (dec current) current (inc current) total])]
    (reduce (fn [acc p]
              (if (and (seq acc) (> (- p (peek acc)) 1))
                (conj acc (if (= (- p (peek acc)) 2) (dec p) :gap) p)
                (conj acc p)))
            []
            shown)))

(defn pagination
  "Prev / page numbers / next. `on-page-change` gets the new 1-based page.
  `label` names the nav landmark; give each pager on a page its own."
  [{:keys [current-page page-count on-page-change className label]}]
  (let [go! (fn [p] (when (and on-page-change (<= 1 p page-count) (not= p current-page))
                      (on-page-change p)))]
    (dom/nav {:aria-label (or label "Pagination") :className (cn "flex justify-center" className)}
      (dom/ul {:className "flex flex-row items-center gap-1"}
        (dom/li {:key "prev"}
          (button {:variant :ghost :size :icon-sm :aria-label "Previous page"
                   :disabled (<= current-page 1)
                   :onClick #(go! (dec current-page))}
                  (icon ChevronLeft)))
        (map-indexed
         (fn [idx p]
           (dom/li {:key (str idx "-" p)}
             (if (= p :gap)
               (dom/span {:aria-hidden true :className "flex size-8 items-center justify-center"}
                 (icon Ellipsis {:className "size-4"}))
               (button {:variant      (if (= p current-page) :outline :ghost)
                        :size         :icon-sm
                        :aria-label   (str "Page " p)
                        :aria-current (when (= p current-page) "page")
                        :onClick      #(go! p)}
                       (str p)))))
         (page-items current-page page-count))
        (dom/li {:key "next"}
          (button {:variant :ghost :size :icon-sm :aria-label "Next page"
                   :disabled (>= current-page page-count)
                   :onClick #(go! (inc current-page))}
                  (icon ChevronRight)))))))
