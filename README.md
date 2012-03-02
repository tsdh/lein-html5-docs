# The lein-html5-docs Leiningen Plugin

This [Leiningen](https://github.com/technomancy/leiningen) plugin generates
HTML5 API docs for your Clojure project.

## Installation

Well, that's pretty easy, simply `lein-html5-docs` as a leiningen plugin.

```
$ lein plugin install lein-html5-docs 1.0.0
```

That's it.  If you run `lein` now, there should be a `html5-docs` task.

## Usage

In order to make `lein-html5-docs` docs work, there are a few entries you may
need to add to your project's `project.clj`:

```
:html5-docs-name "FooBar"       ;; Optional: if specified, overrides the project :name
:html5-docs-page-title          ;; Optional: defaults to "<ProjectName> API Documentation"
:html5-docs-source-path         ;; Optional: overrides :source-path, handy if you want to document only
                                ;; src/foo, but not src/bar, src/baz, ...
:html5-docs-ns-includes         ;; Required: A regex that is matched against namespace names.  If your
                                ;; all your project's namespaces start with "foo.bar", then it should
								;; be something like #"^foo\.bar.*".
:html5-docs-ns-excludes         ;; Optional: A regex that is matched against namespace names.  For example,
	                            ;; you probably want to set it to #".*\.test\..*" to exclude your test
								;; namespaces.
:html5-docs-repository-src-url  ;; Required: The URL of the source folder in your project's web-UI.  For
                                ;; github this is "https://github.com/<project>/funnyqt/blob/master/src"
```

## TODO



## License

Copyright (C) 2012 Tassilo Horn <tassilo@member.fsf.org>

Distributed under the Eclipse Public License, the same as Clojure.
