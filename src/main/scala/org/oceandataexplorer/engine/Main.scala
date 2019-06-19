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

package org.oceandataexplorer.engine


import java.io.File
import scala.io.Source

import org.apache.spark.sql._
import org.apache.spark.rdd.RDD

import com.github.nscala_time.time.Imports._

import org.oceandataexplorer.engine.workflows._
import org.oceandataexplorer.engine.io.{HadoopWavReader,ConfigHandler}
import org.oceandataexplorer.engine.signalprocessing.SoundCalibration

// scalastyle:off method.length

/**
 * Benchmark workflow main object
 */
object Main {
  /**
   * Function runnning benchmark workflow on SPM dataset
   * @param args The arguments for the job, a single string argument is expected
   * that contains the path to the json job configuration file.
   */
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.getOrCreate()

    val config = ConfigHandler(args(0))

    val wavDirFile = new File(config.wavDir)
    val soundPath = wavDirFile.getCanonicalFile.toURI.toString

    val metadataFile = Source.fromFile(config.metadataFilePath)
    val metadata = metadataFile.mkString.split("\n").toList

    metadataFile.close()

    val soundsNameAndStartDate = metadata
      .map(fileMetadata => {
        val metadataArray = fileMetadata.split(",")

        (metadataArray(0), new DateTime(metadataArray(1), DateTimeZone.UTC))
      })
      .take(config.nFiles)

    val soundNames = soundsNameAndStartDate.map(_._1).reduce((p,n) => p + "," + n)
    val soundsPath = soundPath + "/{" + soundNames + "}"

    val recordSizeInFrame = (config.segmentDuration * config.soundSamplingRate).toInt
    val runId = s"""SPM${config.nFiles}files_${recordSizeInFrame}
      _${config.windowSize}_${config.windowOverlap}_${config.nfft}"""

    val outputBaseDirFile = new File(config.outputBaseDir)
    val resultsDestination = outputBaseDirFile.getCanonicalFile.toURI.toString +
      s"/results/scala_fe/${config.nNodes}/" + runId

    val hadoopWavReader = new HadoopWavReader(
      spark,
      config.segmentDuration
    )

    val records = hadoopWavReader.readWavRecords(
      soundsPath,
      soundsNameAndStartDate,
      config.soundSamplingRate,
      config.soundChannels,
      config.soundSampleSizeInBits
    )

    val calibrationClass = SoundCalibration(config.soundCalibrationFactor)

    val calibratedRecords: RDD[Record] = records
      .mapValues(chan => chan.map(calibrationClass.compute))

    val welchSplTolWorkflow = new WelchSplTolWorkflow(
      spark,
      config.segmentDuration,
      config.windowSize,
      config.windowOverlap,
      config.nfft,
      config.lowFreqTol,
      config.highFreqTol
    )

    val welchsSplsTols = welchSplTolWorkflow(
      calibratedRecords,
      config.soundSamplingRate
    )

    welchsSplsTols
      .write
      .json(resultsDestination)

    spark.close()
  }
}