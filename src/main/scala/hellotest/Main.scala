package hellotest

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

    val wordProcessor = new StreamFrequencySorter(cloudSize, lengthAtLeast, windowSize, System.out)
    val scanner = new Scanner(System.in)

    while (scanner.hasNext()) {
      val word = scanner.next()

      if (word.nn.length >= lengthAtLeast) { 
        wordProcessor.processWord(word.nn)
      }
    }
  }

class StreamFrequencySorter(var cloudSize: Int, var lengthAtLeast: Int, var windowSize: Int, output: java.io.PrintStream) {
  private val wordFrequency: mutable.Map[String, Int] = mutable.Map()
  private val wordQueue = new CircularFifoQueue[String](windowSize)

  def processWord(word: String): Unit = {
    wordQueue.add(word)
    wordFrequency(word) = wordFrequency.getOrElse(word, 0) + 1

    if (wordQueue.size > windowSize) {
      val oldestWord = wordQueue.remove().nn // Use .nn here
      wordFrequency(oldestWord) = wordFrequency(oldestWord) - 1
      if (wordFrequency(oldestWord) == 0) {
        wordFrequency.remove(oldestWord)
      }
    }

    val sortedByFrequency = wordFrequency.toSeq.sortBy { case (word, count) => (-count, word) }.take(cloudSize)

    if (sortedByFrequency.nonEmpty) {
      output.println(sortedByFrequency.map { case (word, count) => s"$word: $count" }.mkString(" "))
        if (output.checkError()) {
          println("error detected. exiting (sigpipe)")
          exit(1)
        }
    }
  }
}
