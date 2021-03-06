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

package org.oceandataexplorer.utils.test

import reflect.runtime.universe._
import org.oceandataexplorer.engine.workflows.{SegmentedRecord, AggregatedRecord}

/**
 * Metric error functions
 *
 * @author Joseph Allemandou, Alexandre Degurse
 */
object ErrorMetrics {

  /**
   * Function flattening an aggregated-result into a simple Array
   * to reuse the rmse computation function
   *
   * @param result The aggregated result to be flattened as an array of AggregatedRecord
   * @return The flattened aggregated result as a Array[Double]
   */
  private def aggregatedResultFlattener(result: Array[AggregatedRecord]): Array[Double] = {
    result.flatMap(segRec => segRec._2.flatMap(chans => chans))
  }

  /**
   * Function flattening an segmented-result into a simple Array
   * to reuse the rmse computation function
   *
   * @param result The segmented result to be flattened as an array of SegmentedRecord
   * @return The flattened segmented result as a Array[Double]
   */
  private def segmentedResultFlattener(result: Array[SegmentedRecord]): Array[Double] = {
    result.flatMap(segRec => segRec._2.flatMap(chans => chans.flatMap(chan => chan)))
  }


  /**
   * Wrapper function for Root-mean-square deviation computation.
   * This error metric is symmetric.
   *
   * @tparam T The type of data
   * @param expected The expected value
   * @param actual The actual value
   * @param tag The TypeTag for the type T
   * @return The root-mean-square deviation of the given values
   */
  def rmse[T](expected: T, actual: T)(implicit tag: TypeTag[T]): Double = tag match {
    case tag: TypeTag[T] if tag.tpe =:= typeOf[Double] =>
      rmseDoubles(
        expected.asInstanceOf[Double], actual.asInstanceOf[Double]
      )

    case tag: TypeTag[T] if tag.tpe =:= typeOf[Array[Double]] =>
      rmseArrays(
        expected.asInstanceOf[Array[Double]], actual.asInstanceOf[Array[Double]]
      )

    case tag: TypeTag[T] if tag.tpe =:= typeOf[Array[SegmentedRecord]] =>
      rmseSegmentedResults(
        expected.asInstanceOf[Array[SegmentedRecord]], actual.asInstanceOf[Array[SegmentedRecord]]
      )

    case tag: TypeTag[T] if tag.tpe =:= typeOf[Array[AggregatedRecord]] =>
      rmseAggregatedResults(
        expected.asInstanceOf[Array[AggregatedRecord]], actual.asInstanceOf[Array[AggregatedRecord]]
      )

    case tag: TypeTag[T] => throw new IllegalArgumentException(
      s"Unsupported type (${tag.tpe}) for RMSE "
        + "(must be either Double, Array[Double], Array[SegmentedRecord], Array[AggregatedRecord])"
    )
  }

  /**
   * Root-mean-square deviation of two doubles.
   * This error metric is symmetric.
   *
   * @param expected The expected value
   * @param actual The actual value
   * @return The root-mean-square deviation of the doubles
   */
  def rmseDoubles(expected: Double, actual: Double): Double = {
    math.sqrt(math.pow(expected - actual, 2) / 2)
  }

  /**
   * Root-mean-square deviation of two arrays of doubles.
   * This error metric is symmetric.
   *
   * @param expected The array of expected values
   * @param actual The array of actual values
   * @return The root-mean-square deviation of the arrays
   */
  def rmseArrays(expected: Array[Double], actual: Array[Double]): Double = {
    if (expected.length != actual.length) {
      throw new IllegalArgumentException("The given sequences' sizes don't match")
    }

    val errs = expected.zip(actual).map { case (v1, v2) => math.pow(v1 - v2, 2) }
    val mse = errs.sum / errs.length
    math.sqrt(mse)
  }

  /**
   * Root-mean-square deviation of two result of segmented records.
   * This error metric is symmetric.
   *
   * @param expectedResult The expected result as segmented records
   * @param actualResult The actual result as segmented records
   * @return The root-mean-square deviation of the results
   */
  def rmseSegmentedResults(
    expectedResult: Array[SegmentedRecord],
    actualResult: Array[SegmentedRecord]
  ): Double = {

    val recordNumber = expectedResult.length
    val channelNumber = expectedResult(0)._2.length
    val segmentNumber = expectedResult(0)._2(0).length
    val segmentLength = expectedResult(0)._2(0)(0).length

    if (actualResult.length != recordNumber) {
      throw new IllegalArgumentException("The given sequences' sizes don't match")
    }

    expectedResult.zip(actualResult).foreach{ case (expectedRecord, actualRecord) =>
      // records keys should be equal
      if (expectedRecord._1 != actualRecord._1) {
        throw new IllegalArgumentException("The given records' keys don't match")
      }

      // records should have the same number of channels
      if (actualRecord._2.length != channelNumber) {
        throw new IllegalArgumentException("The given records' number of channels don't match")
      }

      actualRecord._2.foreach(actualChannel => {
        // each channel should have the same number of segments
        if (actualChannel.length != segmentNumber) {
          throw new IllegalArgumentException("The given records' number of segment don't match")
        }

        actualChannel.foreach(actualSegment => {
          // segments should have the same length,
          if (actualSegment.length != segmentLength) {
            throw new IllegalArgumentException("The given sequences' sizes don't match")
          }
        })
      })
    }

    val expectedFlattenResult = segmentedResultFlattener(expectedResult)
    val actualFlattenResult = segmentedResultFlattener(actualResult)

    rmse(expectedFlattenResult, actualFlattenResult)
  }

  /**
   * Root-mean-square deviation of two results of AggregatedRecords.
   * This error metric is symmetric.
   *
   * @param expectedResult The expected results as AggregatedRecords
   * @param actualResult The actual results as AggregatedRecords
   * @return The root-mean-square deviation of the results.
   */
  def rmseAggregatedResults(
    expectedResult: Array[AggregatedRecord],
    actualResult: Array[AggregatedRecord]
  ): Double = {

    val recordNumber = expectedResult.length
    val channelNumber = expectedResult(0)._2.length
    val channelLength = expectedResult(0)._2(0).length

    if (actualResult.length != recordNumber) {
      throw new IllegalArgumentException("The given sequences' sizes don't match")
    }

    expectedResult.zip(actualResult).foreach{ case (expectedRecord, actualRecord) =>
      // records keys should be equal
      if (expectedRecord._1 != actualRecord._1) {
        throw new IllegalArgumentException("The given records' keys don't match")
      }

      // records should have the same number of channels
      if (actualRecord._2.length != channelNumber) {
        throw new IllegalArgumentException("The given records' number of channels don't match")
      }

      actualRecord._2.foreach(actualChannel => {
        // each channel should have the same length
        if (actualChannel.length != channelLength) {
          throw new IllegalArgumentException("The given records' length don't match")
        }
      })
    }

    val expectedFlattenResult = aggregatedResultFlattener(expectedResult)
    val actualFlattenResult = aggregatedResultFlattener(actualResult)

    rmse(expectedFlattenResult, actualFlattenResult)
  }
}
