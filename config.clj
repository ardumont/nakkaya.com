[:site-title "An explorer's log"
 :site-description "Random bits and pieces on stuff that I find interesting."
 :site-url "http://nakkaya.com"
 :in-dir "resources/"
 :out-dir "/tmp/site/"
 :default-template "default.clj"
 :encoding "UTF-8"
 :posts-per-page 2
 :blog-as-index true
 :emacs "/opt/local/bin/emacs"
 :emacs-eval ['(font-lock-mode)
              '(require 'clojure-mode)
              '(set-face-foreground 'font-lock-string-face "#afafff")
              '(set-face-foreground 'font-lock-keyword-face "#ff5f00")
              '(set-face-foreground 'font-lock-function-name-face "#d7af00")
              '(set-face-foreground 'font-lock-builtin-face "#afd700")
              '(set-face-foreground 'font-lock-comment-face "#008787")]]
