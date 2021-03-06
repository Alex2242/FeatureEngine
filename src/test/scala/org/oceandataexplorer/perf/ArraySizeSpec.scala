/** Copyright (C) 2017-2018 Project-ODE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.oceandataexplorer.perf

import org.scalatest.Matchers

/**
 * Trait facilitating building homogeneous array-oriented test data for performance tests.
 *
 * @author Joseph Allemandou
 */
trait ArraySizeSpec extends Matchers {

  val dataStart = 0.0d
  val dataEnd = 10000.0d
  val dataStep = 0.1d

  val nbRuns = 20
  val loopsInRun = 20

}
