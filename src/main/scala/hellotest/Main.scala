package hellotest

import java.util.Scanner
import org.apache.commons.collections4.queue.CircularFifoQueue
import scala.collection.mutable

object Main:
  // Default values for the parameters
  val CLOUD_SIZE = 10
  val LENGTH_AT_LEAST = 6
  val WINDOW_SIZE = 1000

  @main
  def run(
           cloudSize: Int = CLOUD_SIZE,
           lengthAtLeast: Int = LENGTH_AT_LEAST,
           windowSize: Int = WINDOW_SIZE
         ): Unit = {
    println(s"Starting with cloudSize = $cloudSize, lengthAtLeast = $lengthAtLeast, windowSize = $windowSize")

    val wordProcessor = new StreamFrequencySorter(cloudSize, lengthAtLeast, windowSize)

    val scanner = new Scanner(System.in)

    // Continuously read input words and process
    while (scanner.hasNext()) {
      val word = scanner.next()

      // Process the word if it meets the minimum length requirement
      if (word.length >= lengthAtLeast) {
        wordProcessor.processWord(word)
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
      val oldestWord = wordQueue.remove()
      wordFrequency(oldestWord) = wordFrequency(oldestWord) - 1
      if (wordFrequency(oldestWord) == 0) {
        wordFrequency.remove(oldestWord)
      }
    }

    // Sort the map by frequency (descending order)
    val sortedByFrequency = wordFrequency.toSeq.sortBy(-_._2).take(cloudSize)

    // Print the sorted words by frequency
    println(s"Words sorted by frequency: ${sortedByFrequency.mkString(", ")}")
  }
}
