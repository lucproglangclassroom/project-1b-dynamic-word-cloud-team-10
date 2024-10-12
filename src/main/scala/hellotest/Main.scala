import scala.collection.immutable.Queue
import scala.language.unsafeNulls
import scala.sys.exit
import org.log4s.getLogger
import scala.io.Source

object Main {
  private[this] val logger = getLogger

  def main(args: Array[String]): Unit = {
    logger.info("Program start (logging begins here)")

    // Default values for the arguments
    val defaultCloudSize = 10
    val defaultKSteps = 6
    val defaultMinFrequency = 1
    val defaultMinLength = 1
    val defaultWindowSize = 1000

    // Parse command-line arguments and use them to initialize configuration
    val (cloudSize, minLength, windowSize) = args.sliding(2, 2).toList.foldLeft((defaultCloudSize, defaultMinLength, defaultWindowSize)) {
      case ((cloudSize, minLength, windowSize), Array("--cloud-size", value: String)) => (value.toInt, minLength, windowSize)
      case ((cloudSize, minLength, windowSize), Array("-c", value: String)) => (value.toInt, minLength, windowSize)
      case ((cloudSize, minLength, windowSize), Array("--length-at-least", value: String)) => (cloudSize, value.toInt, windowSize)
      case ((cloudSize, minLength, windowSize), Array("-l", value: String)) => (cloudSize, value.toInt, windowSize)
      case ((cloudSize, minLength, windowSize), Array("--window-size", value: String)) => (cloudSize, minLength, value.toInt)
      case ((cloudSize, minLength, windowSize), Array("-w", value: String)) => (cloudSize, minLength, value.toInt)
      case (config, _) => config
    }

    logger.debug(s"cloudSize: $cloudSize, minLength: $minLength, windowSize: $windowSize")

    // Initialize the word processor with a frequency sorter
    val wordProcessor = new StreamFrequencySorter(cloudSize, minLength, windowSize, defaultMinFrequency)
    logger.info("Initialized StreamFrequencySorter")

    // Read words from standard input
    val lines = Source.stdin.getLines
    val words = lines.flatMap(line => Option(line).map(_.split("(?U)[^\\p{Alpha}0-9']+")).getOrElse(Array.empty[String]))
    logger.info("Started reading words from standard input")

    // Process each word interactively using scanLeft
    words
      .filter(_ != null)
      .scanLeft((Queue.empty[String], Map.empty[String, Int], 0)) {
        case ((queue, freqMap, printCounter), word) =>
          if (word.length >= minLength) {
            val lowercasedWord = word.toLowerCase
            val (newQueue, newFreqMap) = wordProcessor.processWord(lowercasedWord, queue, freqMap)
            val newPrintCounter = printCounter + 1

            if (newPrintCounter % defaultKSteps == 0) {
              printWordCloud(wordProcessor, cloudSize, newFreqMap, System.out)
            }
            (newQueue, newFreqMap, newPrintCounter)
          } else {
            (queue, freqMap, printCounter)
          }
      }
      .foreach(_ => ()) // To trigger the iterator processing

    // Print final word cloud after all input has been processed (EOF)
    printWordCloud(wordProcessor, cloudSize, Map.empty[String, Int], System.out)
  }

  // Helper method to print the top words in the word cloud
  def printWordCloud(wordProcessor: StreamFrequencySorter, cloudSize: Int, wordFrequency: Map[String, Int], output: java.io.PrintStream): Unit = {
    val topWords = wordProcessor.getTopWords(cloudSize, wordFrequency)

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

// StreamFrequencySorter using immutable Map and Queue
class StreamFrequencySorter(
                             val cloudSize: Int,
                             val minLength: Int,
                             val windowSize: Int,
                             val minFrequency: Int,
                             val output: java.io.PrintStream = System.out
                           ) extends WordProcessor with FrequencySorter {

  private val logger = org.log4s.getLogger

  // Function to process each word with immutable Queue and Map
  def processWord(word: String, wordQueue: Queue[String], wordFrequency: Map[String, Int]): (Queue[String], Map[String, Int]) = {
    if (word.length < minLength) {
      logger.debug(s"Ignoring word due to min length (length ${word.length}): $word")
      (wordQueue, wordFrequency)
    } else {
      val updatedQueue = wordQueue.enqueue(word)
      val updatedFrequency = wordFrequency.updated(word, wordFrequency.getOrElse(word, 0) + 1)
      logger.debug(s"Added word to queue: $word")

      // Automatically remove oldest word when queue exceeds capacity
      if (updatedQueue.size > windowSize) {
        val (oldestWord, remainingQueue) = updatedQueue.dequeue
        val decrementedFrequency = updatedFrequency.updated(oldestWord, updatedFrequency(oldestWord) - 1)
        logger.debug(s"Removed oldest word from queue: $oldestWord")

        // Remove the word from the map if its frequency is 0
        val finalFrequency = if (decrementedFrequency(oldestWord) == 0) {
          decrementedFrequency - oldestWord
        } else {
          decrementedFrequency
        }
        logger.debug(s"Updated word frequency map")

        (remainingQueue, finalFrequency)
      } else {
        (updatedQueue, updatedFrequency)
      }
    }
  }

  // Get the top N words by frequency
  def getTopWords(topN: Int, wordFrequency: Map[String, Int]): Seq[(String, Int)] = {
    wordFrequency
      .filter(_._2 >= minFrequency) // Filter by minimum frequency
      .toSeq
      .sortBy { case (_, count) => -count } // Sort by frequency in descending order
      .take(topN) // Take the top N most frequent words
  }
}

// Trait for processing words
trait WordProcessor {
  def processWord(word: String, wordQueue: Queue[String], wordFrequency: Map[String, Int]): (Queue[String], Map[String, Int])
}

// Trait for sorting word frequencies
trait FrequencySorter {
  val windowSize: Int
  val minFrequency: Int
  val minLength: Int

  def getTopWords(topN: Int, wordFrequency: Map[String, Int]): Seq[(String, Int)]
}



