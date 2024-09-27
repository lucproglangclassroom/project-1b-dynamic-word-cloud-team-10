package hellotest

import java.util.Scanner
import org.apache.commons.collections4.queue.CircularFifoQueue
import scala.collection.mutable
import scala.language.unsafeNulls
import scala.sys.exit

object Main:
  // Default values for the parameters
  val CLOUD_SIZE = 10
  val LENGTH_AT_LEAST = 6
  val WINDOW_SIZE = 1000

  @main
  def run(args: String*): Unit = {
    // Parse arguments if provided, otherwise use defaults
    val cloudSize = if (args.length > 0) args(0).toIntOption.getOrElse(CLOUD_SIZE) else CLOUD_SIZE
    val lengthAtLeast = if (args.length > 1) args(1).toIntOption.getOrElse(LENGTH_AT_LEAST) else LENGTH_AT_LEAST
    val windowSize = if (args.length > 2) args(2).toIntOption.getOrElse(WINDOW_SIZE) else WINDOW_SIZE
    println(s"Starting with cloudSize = $cloudSize, lengthAtLeast = $lengthAtLeast, windowSize = $windowSize")

    // Handle SIGPIPE gracefully
    sys.addShutdownHook {
      println("Terminating gracefully due to SIGPIPE")
      exit(0)
    }

    val wordProcessor = new StreamFrequencySorter(cloudSize, lengthAtLeast, windowSize)
    val scanner = new Scanner(System.in)

    // Continuously read input words and process
    while (scanner.hasNext()) {
      val word = scanner.next()

      // Process the word if it meets the minimum length requirement
      if (word.nn.length >= lengthAtLeast) { // Use .nn here
        wordProcessor.processWord(word.nn) // And here
      }
    }
  }

class StreamFrequencySorter(var cloudSize: Int, var lengthAtLeast: Int, var windowSize: Int) {
  // A mutable Map to store word frequencies
  private val wordFrequency: mutable.Map[String, Int] = mutable.Map()

  // Circular queue to maintain a sliding window of words
  private val wordQueue = new CircularFifoQueue[String](windowSize)

  // Method to process each word, update frequency, and sort by frequency
  def processWord(word: String): Unit = {
    // Add the word to the queue (sliding window)
    wordQueue.add(word)

    // Update the frequency count for the word
    wordFrequency(word) = wordFrequency.getOrElse(word, 0) + 1

    // If the window size exceeds, remove the oldest word from the queue and update its frequency
    if (wordQueue.size > windowSize) {
      val oldestWord = wordQueue.remove().nn // Use .nn here
      wordFrequency(oldestWord) = wordFrequency(oldestWord) - 1
      if (wordFrequency(oldestWord) == 0) {
        wordFrequency.remove(oldestWord)
      }
    }

    // Sort the map by frequency (descending order) and alphabetically in case of ties
    val sortedByFrequency = wordFrequency.toSeq.sortBy { case (word, count) => (-count, word) }.take(cloudSize)

    // Print the sorted words by frequency in the required format
    if (sortedByFrequency.nonEmpty) {
      println(sortedByFrequency.map { case (word, count) => s"$word: $count" }.mkString(" "))
    }
  }
}
