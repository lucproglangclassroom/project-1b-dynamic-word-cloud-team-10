package hellotest

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfter
import org.scalatest.prop.TableDrivenPropertyChecks._
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.sys.ShutdownHookThread

// TODO: check that functionality works as intended: 
//     - test for the processWord and getTopWords functions individually 
// DONE
class StreamFrequencySorterSpec extends AnyFlatSpec with Matchers{
  //testing out processWords function
  "StreamFrequencySorter" should "process output in order of frequency" in {
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    val sorter = new StreamFrequencySorter(3, 4, 5, 1)

    val testCases = Table(
      ("input", "expectedFrequency"),
      ("hello", 1),
      ("hi", 1),
      ("hello", 2),
      ("hi", 2),
      ("hey", 1)
    )

    forAll(testCases) { (input: String, expectedFrequency: Int) =>
      sorter.processWord(input)
      val frequency = sorter.getTopWords(3).find(_._1 == input).map(_._2).getOrElse(0)
      frequency shouldEqual expectedFrequency
    }
  }
  //testing out getTopWords function
  it should "return the top words correctly" in {
    // initializing a new set of sorters to process words into for topword testing
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    val sorter = new StreamFrequencySorter(3, 4, 10, 1)

    sorter.processWord("apple")
    sorter.processWord("banana")
    sorter.processWord("apple")
    sorter.processWord("orange")
    sorter.processWord("banana")
    sorter.processWord("banana")

    val expectedTopWords = Seq(("banana", 3), ("apple", 2), ("orange", 1))
    val topWords = sorter.getTopWords(3)

    topWords shouldEqual expectedTopWords
  }
// TODO: check for errors with the parameters in StreamFrequencySorter: 
//     - test for min / max parameters in minLength, minFrequency, and windowSize
//     - test for the wordlength minimum critera has been implemented 
//     - OPTIONAL: test to see how program responds with non-numerical inputs
  it should "handle different parameters correctly" in {
    val testCases = Table(
      // first element displays the order of test table
      // tl;dr: parameters | words: fed input | expectedTopWords: expected output
      ("cloudSize", "minLength", "windowSize", "minFrequency", "words", "expectedTopWords"),
      (3, 4, 5, 1, Seq("hello", "world", "hello", "scala"), Seq(("hello", 2), ("world", 1), ("scala", 1))),
      // test minLength
      (3, 6, 5, 1, Seq("hello", "wor", "banana"), Seq(("banana", 1))),
      // test window size
      (3, 4, 3, 1, Seq("scala", "java", "python", "kotlin"), Seq(("python", 1), ("kotlin", 1))),
      // test minimum frequency
      (3, 4, 5, 2, Seq("apple", "banana", "apple", "apple"), Seq(("apple", 3)))
    )

    forAll(testCases) { (cloudSize, minLength, windowSize, minFrequency, words, expectedTopWords) =>
      val outputStream = new ByteArrayOutputStream()
      val printStream = new PrintStream(outputStream)
      val sorter = new StreamFrequencySorter(cloudSize, minLength, windowSize, minFrequency)

      words.foreach { word =>
      sorter.processWord(word)
      } 

      val topWords = sorter.getTopWords(cloudSize)

      topWords shouldEqual expectedTopWords
    }
  }
// TODO: check for error handling
//     - test if sigpipe handling works as intended 

}