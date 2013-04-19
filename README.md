# The lein-html5-docs Leiningen Plugin

This [Leiningen](https://github.com/technomancy/leiningen) plugin generates
HTML5 API docs for your Clojure project.  It doesn't parse your code, but it
loads it and builds the documentation out of the metadata instead.

## Installation

The installation is pretty easy, simply install `lein-html5-docs` as a
leiningen plugin.  Version 1.1.4 is the last one that supports Leiningen 1.
Starting with 1.2.0, Leiningen version 2 is required.  For leiningen 1, use
`lein plugin install lein-html5-docs 1.1.4`, for leiningen 2 declare the plugin
in your `~/.lein/profiles.clj`.

````
{:user {:plugins [;; ...
                  [lein-html5-docs "2.0.0"]
			      ;; ...
				  ]}}
````

Then leiningen will fetch it automatically from
[Clojars](https://clojars.org/lein-html5-docs).

If you run `lein` now, there should be a `html5-docs` task.

## Usage

In order to make `lein-html5-docs` docs work, there are a few entries you may
want to add to your project's `project.clj`:

```
:html5-docs-docs-dir            ;; Optional: where to put the HTML files.  Defaults to "docs".
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
:html5-docs-repository-url      ;; Required: The URL of your repository, e.g., for github projects it is
                                ;; something like "https://github.com/<group>/<project>/blob/master"
```

After this setup, simply run `lein html5-docs` in your project.

## To be done

I've written this plugin because I was unable to get
[autodoc](http://tomfaulhaber.github.com/autodoc/) or
[marginalia](http://fogus.me/fun/marginalia/) to give me exactly the sort of
output I desired.  So I decided to write my own minimal API docs generation
tool.  The code of this plugin lived as a leiningen task in one of my projects
for several month, and everything was hard-coded to match this very project's
needs.

Then I've invested some time to rip it out of there and make it a stand-alone
leiningen plugin.  It should basically work for any leiningen project, but I
didn't test it extensively.

There's much room for improvements:

  - The code probably needs a cleanup and a bit of restructuring

  - I'm really not a web designer and I'm famous for my bad taste (you might
    want to ask my wife!), so the CSS and layout stuff can surely be improved

  - There might be tons of bugs.  All I can say is that it works fine for my
    project.

I'm happy to integrate patches and pull requests.

## License

Copyright (C) 2012-2013 Tassilo Horn <tsdh@gnu.org>

Distributed under the Eclipse Public License, the same as Clojure.
