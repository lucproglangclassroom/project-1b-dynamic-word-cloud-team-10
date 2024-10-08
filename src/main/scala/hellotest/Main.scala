package hellotest

import org.apache.commons.collections4.queue.CircularFifoQueue
import scala.collection.mutable
import scala.language.unsafeNulls
import scala.sys.exit
import org.log4s.getLogger
import scala.io.Source

object Main {
  private[this] val logger = getLogger
  var PRINT_COUNTER: Int = 0

  def main(args: Array[String]): Unit = {
    logger.info("program start (logging begins here)")
    // Default values for the arguments
    var cloudSize = 10
    var kSteps = 6
    var minFrequency = 1
    var minLength = 1
    var windowSize = 1000

    // Parse command-line arguments (if provided)
    args.sliding(2, 2).toList.collect {
      case Array("--cloud-size", value: String) => cloudSize = value.toInt
      case Array("-c", value: String) => cloudSize = value.toInt
      case Array("--length-at-least", value: String) => minLength = value.toInt
      case Array("-l", value: String) => minLength = value.toInt
      case Array("--window-size", value: String) => windowSize = value.toInt
      case Array("-w", value: String) => windowSize = value.toInt
    }
    
    // Log the received arguments
    logger.debug(s"cloudSize: $cloudSize, kSteps: $kSteps, minFrequency: $minFrequency, minLength: $minLength, windowSize: $windowSize")

    // Initialize the word processor with a frequency sorter
    val wordProcessor = new StreamFrequencySorter(cloudSize, minLength, windowSize, minFrequency)
    logger.info("Initialized StreamFrequencySorter")

    // Read words from standard input (can be from a continuous stream)
    val lines = Source.stdin.getLines
    val words = lines.flatMap(line => Option(line).map(_.split("(?U)[^\\p{Alpha}0-9']+")).getOrElse(Array.empty[String]))
    logger.info("Started reading words from standard input")

    // Process each word
    words.filter(_ != null).foreach { word =>
      if (word.length >= minLength) {
        // Convert word to lowercase for case-insensitive comparison
        val lowercasedWord = word.toLowerCase
        wordProcessor.processWord(lowercasedWord)
        logger.debug(s"Processed word: $lowercasedWord")

        //println(s"min length req met, adding word: $word")
        // Increment print counter and check if we should print the top words
        PRINT_COUNTER += 1
        if (PRINT_COUNTER % kSteps == 0) {
          printWordCloud(wordProcessor, cloudSize, System.out)
        }
      }
    }
    // Print final word cloud after all input has been processed (EOF)
    printWordCloud(wordProcessor, cloudSize, System.out)
  }

  // Helper method to print the top words in the word cloud
  def printWordCloud(wordProcessor: StreamFrequencySorter, cloudSize: Int, output: java.io.PrintStream): Unit = {
    val topWords = wordProcessor.getTopWords(cloudSize)

    // Print the top words in the desired format: "word: frequency"
    if (topWords.nonEmpty) {
      output.println(topWords.map { case (word, count) => s"$word: $count" }.mkString(" "))
      logger.info("Printed word cloud")
      if (output.checkError()) {
        logger.error("Error detected while printing. Exiting (SIGPIPE)")
        exit(1)
      }
    }
    Console.flush() // Ensure the output is flushed to the console
  }
}

// The StreamFrequencySorter class is complete
class StreamFrequencySorter(
                             var cloudSize: Int,
                             var minLength: Int,
                             var windowSize: Int,
                             var minFrequency: Int,
                             val output: java.io.PrintStream = System.out
                           ) {
  private val logger = org.log4s.getLogger
  var wordFrequency: mutable.Map[String, Int] = mutable.Map()
  private val wordQueue = new CircularFifoQueue[String](windowSize)

  def processWord(word: String): Unit = {
    // ignore word if it doesn't meet the expected minlength
    if (word.length < minLength) {
    logger.debug(s"Ignoring word due to min length (length ${word.length}): $word")
    return 
    }
    wordQueue.add(word)
    wordFrequency(word) = wordFrequency.getOrElse(word, 0) + 1
    logger.debug(s"Added word to queue: $word")

    // automatically remove oldest word when queue exceeds capacity
    if (wordQueue.size == windowSize) {
      Option(wordQueue.peek()).foreach { oldestWord =>
        wordFrequency(oldestWord) = wordFrequency(oldestWord) - 1
        logger.debug(s"Removed oldest word from queue: $oldestWord")
        if (wordFrequency(oldestWord) == 0) {
          wordFrequency.remove(oldestWord)
          logger.debug(s"Removed word from frequency map: $oldestWord")
        }
      }
    }
  }

  def getTopWords(topN: Int): Seq[(String, Int)] = {
    val topWords = wordFrequency
      .filter(_._2 >= minFrequency) // Filter by minimum frequency
      .toSeq
      .sortBy { case (_, count) => -count } // Sort by frequency in descending order
      .take(topN) // Take the top N most frequent words

    logger.debug(s"Top words: ${topWords.mkString(", ")}")
    topWords
  }
}