Scalingua [![Build Status](https://travis-ci.org/makkarpov/scalingua.svg?branch=master)](https://travis-ci.org/makkarpov/scalingua) [![Latest version](https://maven-badges.herokuapp.com/maven-central/ru.makkarpov/scalingua_2.11/badge.svg?subject=version)](http://search.maven.org/#search%7Cga%7C1%7Cscalingua%20AND%20g%3A%22ru.makkarpov%22) [![Join the chat at https://gitter.im/makkarpov/scalingua](https://badges.gitter.im/makkarpov/scalingua.svg)](https://gitter.im/makkarpov/scalingua?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
=========

Have you ever wondered that there is no **gettext**-like library for Scala? **Scalingua** is here to fix it! It comes with
lightweight runtime library and full-featured compile-time macros and SBT plugin that will combine the powers of **gettext** 
and **Scala** in a single library.

Scalingua consists of four modules:

 * `core` — a minimal runtime components for internationalization. It's very lightweight (~ 30 kB) and provides basic
   functions like loading precompiled translations.
 * `scalingua` itself — library with macros that have these features:
    * String interpolator that makes internationalization of strings as easy as writing one letter before them;
    * Plural string interpolator that adds plural suffixes for you;
    * Macros for strings with contexts (`msgctxt`) and plural strings;
    * Macros leaves no dependency on `scalingua` library — you can include it in `provided` scope and it will not break anything;
    * Macros will extract all your strings to separate `*.pot` file every compilation run and keep this file up-to-date with during incremental compilation;
    * You can translate more complex formats like HTML — all placeholders will be escaped, so no XSS attacks are possible;
    * You can re-use existing macros from this library to implement custom translation utilities (e.g. create `th` interpolator that will translate HTML) or move `I18n` method to your `Utils` object, but in this case you will have dependency on `scalingua`
 * `scalingua-sbt` — SBT plugin with small but important task:
    * Automatically sets compiler parameters so you don't have to remember them;
    * Parses `*.po` files and compiles them to efficient binary files and Scala classes.
 * `scalingua-play` — Integration module for Play framework:
    * Dependency injection bindings to summon `Messages` through DI;
    * HTML output format with ready to use functions and interpolators;
    * Extraction of visitor language from request headers.

Getting started
===============

* [Using with Scala](https://github.com/makkarpov/scalingua/wiki/Using-with-Scala) in projects like GUI and console applications
* [Using with Play](https://github.com/makkarpov/scalingua/wiki/Using-with-Play) in projects based on Play Framework
