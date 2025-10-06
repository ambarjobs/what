(ns what.utils)

(defn prompted-input
  "Presents a propmt and reads a user input."
  [prompt]
  (print prompt)
  (flush)
  (read-line))
