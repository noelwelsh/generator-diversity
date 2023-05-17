#!/usr/bin/env sh

pandoc -F pandoc-crossref --citeproc -s -o diversity.pdf intro.md formalism.md algorithms.md
