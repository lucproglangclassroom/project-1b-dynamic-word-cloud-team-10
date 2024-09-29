package hellotest

import org.apache.commons.collections4.queue.CircularFifoQueue
import scala.collection.mutable
import scala.language.unsafeNulls
import scala.sys.exit
import org.log4s._
import scala.io.Source

object Main {
  private[this] val logger = org.log4s.getLogger
  var PRINT_COUNTER: Int = 0

  def main(args: Array[String]): Unit = {
    // Default values for the arguments
    var cloudSize = 10
    var kSteps = 6
    var minFrequency = 1
    var minLength = 1
    var windowSize = 1000

    // Parse command-line arguments (if provided)
    if (args.length > 0) cloudSize = args(0).toInt
    if (args.length > 1) kSteps = args(1).toInt
    if (args.length > 2) minFrequency = args(2).toInt
    if (args.length > 3) minLength = args(3).toInt
    if (args.length > 4) windowSize = args(4).toInt

    // Log the received arguments
    logger.debug(s"cloudSize: $cloudSize, kSteps: $kSteps, minFrequency: $minFrequency, minLength: $minLength, windowSize: $windowSize")

    // Setup SIGPIPE handling
    sys.addShutdownHook {
      println("Terminating gracefully due to SIGPIPE")
      exit(0)
    }

    // Initialize the word processor with a frequency sorter
    val wordProcessor = new StreamFrequencySorter(cloudSize, minLength, windowSize, minFrequency)

    // Read words from standard input (can be from a continuous stream)
    val lines = Source.stdin.getLines
    val words = lines.flatMap(line => Option(line).map(_.split("(?U)[^\\p{Alpha}0-9']+")).getOrElse(Array.empty[String]))

    // Process each word
    words.filter(_ != null).foreach { word =>
      if (word.length >= minLength) {
        // Convert word to lowercase for case-insensitive comparison
        val lowercasedWord = word.toLowerCase
        wordProcessor.processWord(lowercasedWord)

        // Increment print counter and check if we should print the top words
        PRINT_COUNTER += 1
        if (PRINT_COUNTER % kSteps == 0) {
          printWordCloud(wordProcessor, cloudSize)
        }
      }
    }

    // Print final word cloud after all input has been processed (EOF)
    printWordCloud(wordProcessor, cloudSize)
  }

  // Helper method to print the top words in the word cloud
  // Adjusted printWordCloud method to remove unnecessary colons and ensure correct formatting
  def printWordCloud(wordProcessor: StreamFrequencySorter, cloudSize: Int): Unit = {
    val topWords = wordProcessor.getTopWords(cloudSize)

    // Print the top words in the desired format: "word: frequency"
    if (topWords.nonEmpty) {
      println(topWords.map { case (word, count) => s"$word: $count" }.mkString(" "))
    }

    Console.flush() // Ensure the output is flushed to the console
  }
}

// The StreamFrequencySorter class is complete
class StreamFrequencySorter(
                             var cloudSize: Int,
                             var minLength: Int,
                             var windowSize: Int,
                             var minFrequency: Int
                           ) {
  private val wordFrequency: mutable.Map[String, Int] = mutable.Map()
  private val wordQueue = new CircularFifoQueue[String](windowSize)

  def processWord(word: String): Unit = {
    wordQueue.add(word)
    wordFrequency(word) = wordFrequency.getOrElse(word, 0) + 1

    // Automatically remove oldest word when queue exceeds capacity
    if (wordQueue.size == windowSize) {
      Option(wordQueue.peek()).foreach { oldestWord =>
        wordFrequency(oldestWord) = wordFrequency(oldestWord) - 1
        if (wordFrequency(oldestWord) == 0) {
          wordFrequency.remove(oldestWord)
        }
      }
    }
  }

  def getTopWords(topN: Int): Seq[(String, Int)] = {
    wordFrequency
      .filter(_._2 >= minFrequency) // Filter by minimum frequency
      .toSeq
      .sortBy { case (_, count) => -count } // Sort by frequency in descending order
      .take(topN) // Take the top N most frequent words
  }
}

// Add closing brace for Main object