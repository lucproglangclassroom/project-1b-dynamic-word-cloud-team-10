package hellotest
import java.util.Scanner
import mainargs._
import org.apache.commons.collections4.queue.CircularFifoQueue

object Main:
  //defalt values for values
  val CLOUD_SIZE = 10
  val LENGTH_AT_LEAST = 6
  val WINDOW_SIZE = 1000
}


 object StreamFrequencySorter {
  // A mutable Map to store word frequencies
  var wordFrequency: Map[String, Int] = Map()

  // Method to process each word, update frequency, and sort by frequency
  def processWord(word: String): Unit = {
    // Update the frequency count for the word
    wordFrequency = wordFrequency + (word -> (wordFrequency.getOrElse(word, 0) + 1))

    // Sort the map by frequency (descending order)
    val sortedByFrequency = wordFrequency.toSeq.sortBy(-_._2)  // Sort by frequency (value) in descending order

    // Print the sorted words by frequency
    println(s"Words sorted by frequency: ${sortedByFrequency.mkString(", ")}")
  }

   var cloud_size = CLOUD_SIZE
   var length_at_least = LENGTH_AT_LEAST
   var window_size = WINDOW_SIZE

 }



end Main
