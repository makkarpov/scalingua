/******************************************************************************
 * Copyright © 2016 Maxim Karpov                                              *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *     http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package ru.makkarpov.scalingua.play.test

import org.scalatest.{FlatSpec, Matchers}
import ru.makkarpov.scalingua.{Language, Messages, TaggedLanguage}
import ru.makkarpov.scalingua.play.I18n._

class PlayTest extends FlatSpec with Matchers {
  it should "handle HTML translations" in {
    implicit val lang = Language.English
    val x = "\"List<String>\""
    h"A class <code>$x</code> can be used to provide simple list container".body shouldBe
      "A class <code>&quot;List&lt;String&gt;&quot;</code> can be used to provide simple list container"
  }

  it should "handle 'Accept' header" in {
    implicit val messages = new Messages(
      TaggedLanguage.Identity,
      MockEnglishLang("aa-AA"),
      MockEnglishLang("aa-AX"),
      MockEnglishLang("bb-BB"),
      MockEnglishLang("cc-CC")
    )

    import ru.makkarpov.scalingua.play.PlayUtils.{languageFromAccept => f}

    f("") shouldBe Language.English
    f("cc").id.toString shouldBe "cc-CC"
    f("bb").id.toString shouldBe "bb-BB"
    f("aa").id.toString shouldBe "aa-AA"
    f("aa-RR").id.toString shouldBe "aa-AA"
    f("aa-AX").id.toString shouldBe "aa-AX"
    f("bb, cc").id.toString shouldBe "bb-BB"
    f("cc, bb").id.toString shouldBe "cc-CC"
    f("xx, yy, zz") shouldBe Language.English
    f("tt, tt, cc, tt, tt").id.toString shouldBe "cc-CC"
    f("bb, cc; q=0.8").id.toString shouldBe "bb-BB"
    f("cc; q=0.8, bb").id.toString shouldBe "bb-BB"
    f("aa; q=0.2; bb; q=0.4, cc; q=0.8").id.toString shouldBe "cc-CC"
    f("aa; q=0.8, bb; q=0.4, cc; q=0.2").id.toString shouldBe "aa-AA"

    // No exceptions should be thrown on incorrect inputs; English should be returned instead:
    f("111111111") shouldBe Language.English
    f("aa-AA-ww") shouldBe Language.English
    f("aa-AX; q=W") shouldBe Language.English
  }
}
