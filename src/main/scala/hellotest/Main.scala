package hellotest

import scala.io.Source
import java.util.Scanner
import org.apache.commons.collections4.queue.CircularFifoQueue
import scala.collection.mutable
import scala.language.unsafeNulls
import scala.sys.exit

object Main:
  val CLOUD_SIZE = 10
  val LENGTH_AT_LEAST = 6
  val WINDOW_SIZE = 1000

  @main
  def run(args: String*): Unit = {
    val cloudSize = if (args.length > 0) args(0).toIntOption.getOrElse(CLOUD_SIZE) else CLOUD_SIZE
    val lengthAtLeast = if (args.length > 1) args(1).toIntOption.getOrElse(LENGTH_AT_LEAST) else LENGTH_AT_LEAST
    val windowSize = if (args.length > 2) args(2).toIntOption.getOrElse(WINDOW_SIZE) else WINDOW_SIZE
    println(s"Starting with cloudSize = $cloudSize, lengthAtLeast = $lengthAtLeast, windowSize = $windowSize")

    //SIGPIPE? ?
    sys.addShutdownHook {
      println("Terminating gracefully due to SIGPIPE")
      exit(0)
    }

    val wordProcessor = new StreamFrequencySorter(cloudSize, lengthAtLeast, windowSize)

    val scanner = new Scanner(System.in)
    // takes in user input
    if (args.nonEmpty && args.mkString(" ").startsWith("\"") && args.mkString(" ").endsWith("\"")) {
      val quotedText = args.mkString(" ").stripPrefix("\"").stripSuffix("\"")
      println(s"Processing quoted text: $quotedText")
      
      quotedText.split("\\s+").foreach { word =>
        if (word.length >= lengthAtLeast) wordProcessor.processWord(word)
      }
    } 
    //takes in a file 
    else {
      println("Processing input from stdin (file or terminal)...")
      val input = Source.stdin.getLines()
      input.foreach { word =>
        if (word.length >= lengthAtLeast) wordProcessor.processWord(word)7
      }
    }
  }

class StreamFrequencySorter(var cloudSize: Int, var lengthAtLeast: Int, var windowSize: Int) {
  private val wordFrequency: mutable.Map[String, Int] = mutable.Map()
  private val wordQueue = new CircularFifoQueue[String](windowSize)

  def processWord(word: String): Unit = {
    wordQueue.add(word)
    wordFrequency(word) = wordFrequency.getOrElse(word, 0) + 1

    if (wordQueue.size > windowSize) {
      val oldestWord = wordQueue.remove().nn
      wordFrequency(oldestWord) = wordFrequency(oldestWord) - 1
      if (wordFrequency(oldestWord) == 0) {
        wordFrequency.remove(oldestWord)
      }
    }

    val sortedByFrequency = wordFrequency.toSeq.sortBy { case (word, count) => (-count, word) }.take(cloudSize)

    if (sortedByFrequency.nonEmpty) {
      println(sortedByFrequency.map { case (word, count) => s"$word: $count" }.mkString(" "))
    }
  }
}
