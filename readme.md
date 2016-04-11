Scalingua
=========

Have you ever wondered that there is no `gettext`-like library for Scala? **Scalingua** is here to fix it! It combines
lightweight runtime library with full-featured macros and SBT plugin that will provide all these advanced features like
extraction of translation strings to a separate file.

Scalingua consists of three modules:

 * `core` — a minimal runtime components for internationalization. It's very lightweight (~ 30 kB) and provides basic
   functions like loading precompiled translations.
 * `scalingua` itself --- library with macros that have these nice features:
    * String iterpolator that makes internationalization of strings as easy as writing one letter before them
    * Macros for strings with contexts (`msgctxt`) and plural strings.
    * Macros leaves no dependency on `scalingua` library --- you can include it in `provided` scope and it will not break anything
    * Macros will extract all your strings to separate `*.pot` file every time you ask it to do so.
    * You can translate more complex formats like HTML --- all placeholders will be escaped
    * You can re-use existing macros from this library to implement custom translation utilities (e.g. create `th` interpolator that will translate HTML), but in this case you will have dependency on `scalingua`
  * `scalingua-sbt` --- SBT plugin that simplifies all above.

Getting started
===============

To use Scalingua in your project you need to include it in following way:

**project/plugins.sbt**:
    
    addSbtPlugin("maven" % "id" % "pending")

**build.sbt**:

    enablePlugin(Scalingua)
    libraryDependencies += "maven" %% "id" % "pending"

**Example.scala**:

    import ru.makkarpov.scalingua.I18n._
    import ru.makkarpov.scalingua.{LanguageId, Messages}
    
    object Example extends App {
      implicit val messages = Messages.compiled()
      implicit val lang = LanguageId("xx", "YY") // e.g. "en", "US"
      
      print(t"Enter your name: ")
      val name = Console.in.readLine()
      
      println(t"Hello, $name!")
      
      print(t"How may ducks do you have? ")
      val ducks = Console.in.readLine().toInt
      
      println(p("Great! You have %(n) duck!", "Great! You have %(n) ducks!", ducks))
    }

When you compile this example, macros will generate the following file at `<target>/messages/main.pot`:

    #  !Generated: yyyy-MM-dd HH:mm:ss
    
    #: src/main/scala/Example.scala:8
    msgid "Enter your name: "
    msgstr ""
    
    #: src/main/scala/Example.scala:11
    msgid "Hello, %(name)!"
    msgstr ""
    
    #: src/main/scala/Example.scala:13
    msgid "How may ducks do you have? "
    msgstr ""
    
    #: src/main/scala/Example.scala:16
    msgid "Great! You have %(n) duck!"
    msgid_plural "Great! You have %(n) ducks!"
    msgstr[0] ""
    msgstr[1] ""

You can place translated files named as `xx_YY.po` in `src/main/locales` directory, and they will be compiled so that `Messages.compiled()` could reach it. After that all messages with implicit language `xx_YY` will be translated. Scalingua could parse `Plural-Forms` header and translate it to Scala expression. There is no reflection used to lookup strings or calculate plural forms, so translation will be as fast as it could be.

Detailed topics
===============

Macro is configured with two `scalacOptions`:
 * `-Xmacro-settings:scalingua:target=....` --- specifies path to file in which all strings should be saved
 * `-Xmacro-settings:scalingua:baseDir=....` --- specifies common file location prefix which will be stripped from context.

SBT plugin sets these properties automatically and can be configured with following SBT settings:
 * `templateTarget` --- overrides target location of `*.pot` file
 * `localePackage` --- specifies a package to which locales should be saved (`"locales"` is the default). When you change this value, you also should adjust argument of `Messages.compiled("...")` appropriately.
 * `sourceDirectories in compileLocales` --- specifies a directories with `*.po` files (default is `src/<conf>/locales`).

