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

package org.oceandataexplorer.engine.tmp

import java.io.{File, FileInputStream, InputStream}
import java.net.URL
import javax.sound.sampled.{AudioFileFormat, AudioFormat, AudioInputStream, AudioSystem}

import com.holdenkarau.spark.testing.SharedSparkContext
import org.apache.hadoop.io.{DoubleWritable, LongWritable}
import org.oceandataexplorer.hadoop.io.{TwoDDoubleArrayWritable, WavPcmInputFormat}
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source


/**
 * Class providing examples on read files
 *
 * @author Joseph Allemandou
 */
class TestReadingFiles extends FlatSpec with Matchers with SharedSparkContext {

  private val readmeFilePath = "/README.md"
  private val soundFilePath1 = "/wav/sin_16kHz_2.5s.wav"
  private val soundFilePath2 = "/wav/sin_48kHz_2.5s.wav"
  private val soundFilePathCorrupted = "/wav/sin_16kHz_2.5s_corrupted.wav"

  "Tutorial file" should "be readable" in {
    val tutorialUrl: URL = getClass.getResource(readmeFilePath)
    val tutorialFile: File = new File(tutorialUrl.toURI)
    val tutorialFileInputStream: InputStream = new FileInputStream(tutorialFile)
    val tutorialResourceInputStream: InputStream = getClass.getResourceAsStream(readmeFilePath)
    Source.fromInputStream(tutorialFileInputStream).mkString should be(
      Source.fromInputStream(tutorialResourceInputStream).mkString)
  }

  "Example sound file 1 " should "be readable by javax.sound" in {
    val freq = 16000.0
    val chans = 1
    val bytes = 2.0
    val duration = 2.5
    val dataBytes = freq * duration * chans * bytes

    val inputStream : InputStream = getClass.getResourceAsStream(soundFilePath1)

    val audioFileFormat: AudioFileFormat = AudioSystem.getAudioFileFormat(inputStream)
    audioFileFormat.getByteLength should be(dataBytes + 44)
    audioFileFormat.getFrameLength should be(duration * freq)
    audioFileFormat.getType should be(AudioFileFormat.Type.WAVE)

    val audioFormat: AudioFormat = audioFileFormat.getFormat
    audioFormat.getChannels should be(chans)             // We expect mono, not stereo
    audioFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    audioFormat.getFrameRate should be(freq)           // frameRate defined per file
    audioFormat.getFrameSize should be(bytes)          // 2 bytes of data per frame
    audioFormat.getSampleRate should be(freq)          // Equals frameRate for PCM
    audioFormat.getSampleSizeInBits should be(8 * bytes)
    audioFormat.isBigEndian should be(false)           // Default encoding is little-endian


    val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(inputStream)
    val arraySize = 98
    val byteArray: Array[Byte] = new Array[Byte](arraySize)
    (1 to (dataBytes / arraySize).toInt).foreach(halfSec => {
      audioInputStream.read(byteArray) should be(arraySize)
    })
    audioInputStream.read(byteArray) should be(dataBytes % arraySize)

  }

  "Example sound file 2 " should "be readable by javax.sound" in {
    val freq = 48000.0
    val chans = 1
    val bytes = 2.0
    val duration = 2.5
    val dataBytes = freq * duration * chans * bytes

    val inputStream : InputStream = getClass.getResourceAsStream(soundFilePath2)

    val audioFileFormat: AudioFileFormat = AudioSystem.getAudioFileFormat(inputStream)
    audioFileFormat.getByteLength should be(dataBytes + 44)
    audioFileFormat.getFrameLength should be(duration * freq)
    audioFileFormat.getType should be(AudioFileFormat.Type.WAVE)

    val audioFormat: AudioFormat = audioFileFormat.getFormat
    audioFormat.getChannels should be(chans)             // We expect mono, not stereo
    audioFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    audioFormat.getFrameRate should be(freq)           // frameRate defined per file
    audioFormat.getFrameSize should be(bytes)          // 2 bytes of data per frame
    audioFormat.getSampleRate should be(freq)          // Equals frameRate for PCM
    audioFormat.getSampleSizeInBits should be(8 * bytes)
    audioFormat.isBigEndian should be(false)           // Default encoding is little-endian


    val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(inputStream)
    val arraySize = 98
    val byteArray: Array[Byte] = new Array[Byte](arraySize)
    (1 to (dataBytes / arraySize).toInt).foreach(halfSec => {
      audioInputStream.read(byteArray) should be(arraySize)
    })
    audioInputStream.read(byteArray) should be(dataBytes % arraySize)

  }

  "Example sound file corrupted" should "fail to be read by javax.sound" in {
    val freq = 16000.0
    val chans = 1
    val bytes = 2.0
    val duration = 2.5
    val dataBytes = freq * duration * chans * bytes

    val audioFile = new File(getClass.getResource(soundFilePathCorrupted).toURI)

    val audioFileFormat: AudioFileFormat = AudioSystem.getAudioFileFormat(audioFile)
    audioFileFormat.getByteLength should be(freq * bytes * chans * duration + 44)
    audioFileFormat.getFrameLength should be(duration * freq)
    audioFileFormat.getType should be(AudioFileFormat.Type.WAVE)

    val audioFormat: AudioFormat = audioFileFormat.getFormat
    audioFormat.getChannels should be(chans)             // We expect mono, not stereo
    audioFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    audioFormat.getFrameRate should be(freq)           // frameRate defined per file
    audioFormat.getFrameSize should be(bytes)          // 2 bytes of data per frame
    audioFormat.getSampleRate should be(freq)          // Equals frameRate for PCM
    audioFormat.getSampleSizeInBits should be(8 * bytes)
    audioFormat.isBigEndian should be(false)           // Default encoding is little-endian

    // Corrupted file - real length should not match computed one
    audioFile.length should not be(dataBytes + 44)
  }

  "Example sound file 1 with HadoopReader" should "read WAV data" in {

    val soundUrl: URL = getClass.getResource(soundFilePath1)

    val soundSampleRate = 16000.0f
    val soundChannels = 1
    val soundSmapleSizeInBits = 16
    val soundDurationInSecs = 2.5
    val frameSizeInBytes = (soundSampleRate * soundChannels).toInt

    val slices = 1000

    val conf = sc.hadoopConfiguration

    WavPcmInputFormat.setSampleRate(conf, soundSampleRate)
    WavPcmInputFormat.setChannels(conf, soundChannels)
    WavPcmInputFormat.setSampleSizeInBits(conf, soundSmapleSizeInBits)
    WavPcmInputFormat.setRecordSizeInFrames(conf, (frameSizeInBytes * soundDurationInSecs / slices).toInt)

    val rdd = sc.newAPIHadoopFile[LongWritable, TwoDDoubleArrayWritable, WavPcmInputFormat](
      soundUrl.toURI.toString,
      classOf[WavPcmInputFormat],
      classOf[LongWritable],
      classOf[TwoDDoubleArrayWritable],
      sc.hadoopConfiguration
    ).map{ case (k, v) => (k.get, v.get.map(_.map(_.asInstanceOf[DoubleWritable].get))) }

    rdd.collect().length shouldEqual slices

  }

}
