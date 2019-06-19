

package org.oceandataexplorer.engine.io

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.io.Source

/**
 * Class handling configuration for processing jobs
 */
object ConfigHandler {

  case class Configs (
    nNodes: Int,
    nFiles: Int,
    wavDir: String,
    metadataFilePath: String,
    outputBaseDir: String,
    soundChannels: Int,
    soundSampleSizeInBits: Int,
    soundSamplingRate: Float,
    soundCalibrationFactor: Double,
    segmentDuration: Float,
    windowSize: Int,
    windowOverlap: Int,
    nfft: Int,
    lowFreqTol: Option[Double],
    highFreqTol: Option[Double]
  )

  implicit val configsReads: Reads[Configs] = (
    (JsPath \ "n_nodes").read[Int] and
    (JsPath \ "n_files").read[Int] and
    (JsPath \ "wav_directory").read[String] and
    (JsPath \ "metadata_file").read[String] and
    (JsPath \ "output_base_directory").read[String] and
    (JsPath \ "sound_n_channels").read[Int] and
    (JsPath \ "sound_sample_size_in_bits").read[Int] and
    (JsPath \ "sound_sampling_rate").read[Float] and
    (JsPath \ "sound_calibration_factor").read[Double] and
    (JsPath \ "segment_duration").read[Float] and
    (JsPath \ "window_size").read[Int] and
    (JsPath \ "window_overlap").read[Int] and
    (JsPath \ "nfft").read[Int] and
    (JsPath \ "low_freq_tol").readNullable[Double] and
    (JsPath \ "high_freq_tol").readNullable[Double]
  )(Configs.apply _)

  def apply(configFilePath: String): Configs =  {
    val configFile = Source.fromFile(configFilePath)
    val config = Json.parse(configFile.mkString)

    configFile.close()

    config.validate[Configs].get
  }
}
