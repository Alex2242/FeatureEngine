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

package org.oceandataexplorer.utils

/**
 * Class providing support for N-dimensionnal arrays
 *
 * @param rawArray The n-dimensional array
 */
case class NDimensionalArray (rawArray: Any) {

  if (!rawArray.isInstanceOf[Array[_ <: Any]]) {
    throw new IllegalArgumentException("Whatever was given is not an array")
  }

  /**
   * Function used to access the array
   *
   * @param i The index to slice
   * @return A new N-dimensional array as a NdimensionalArray
   */
  def apply(i: Int): NDimensionalArray = {
    if (!rawArray.isInstanceOf[Array[Array[_ <: Any]]]) {
      throw new IllegalAccessError("No dimensions left to access")
    }

    NDimensionalArray(rawArray.asInstanceOf[Array[Any]](i))
  }

  /**
   * Function returning the given array as an Array[Double]
   *
   * @return The given array as an Array[Double]
   */
  def get: Array[Double] = {
    if (rawArray.isInstanceOf[Array[Array[_ <: Any]]]) {
      throw new IllegalArgumentException("The given array is not one-dimensional")
    }

    rawArray match {
      case arr: Any if arr.isInstanceOf[Array[Double]] => arr.asInstanceOf[Array[Double]]
      case arr: Any if arr.isInstanceOf[Array[Float]] =>
        arr.asInstanceOf[Array[Float]].map(_.toDouble)
      case _ => throw new RuntimeException("Unexpected error")
    }
  }
}
