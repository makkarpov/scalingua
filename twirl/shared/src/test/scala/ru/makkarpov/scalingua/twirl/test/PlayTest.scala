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

package ru.makkarpov.scalingua.twirl.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.makkarpov.scalingua.{Language, Messages, TaggedLanguage}
import ru.makkarpov.scalingua.twirl.I18n._

class PlayTest extends AnyFlatSpec with Matchers {
  it should "handle HTML translations" in {
    implicit val lang = Language.English
    val x = "\"List<String>\""
    h"A class <code>$x</code> can be used to provide simple list container".body shouldBe
      "A class <code>&quot;List&lt;String&gt;&quot;</code> can be used to provide simple list container"
  }
}
