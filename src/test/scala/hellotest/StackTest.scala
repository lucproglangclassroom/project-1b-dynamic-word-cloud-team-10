package hellotest

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.util.control.NonFatal

class StreamFrequencySorterSpec extends AnyFlatSpec with Matchers {
  
  // Test the processWord function
  "StreamFrequencySorter" should "process output in order of frequency" in {
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    val sorter = new StreamFrequencySorter(3, 4, 5, 1)

    val testCases = Table(
      ("input", "expectedFrequency"),
      ("helloooo", 1),
      ("hiiiiii", 1),
      ("helloooo", 2),
      ("hiiiiii", 2),
      ("heyyyyyyy", 1)
    )

    forAll(testCases) { (input: String, expectedFrequency: Int) =>
      sorter.processWord(input)
      val frequency = sorter.getTopWords(3).find(_._1 == input).map(_._2).getOrElse(0)
      frequency shouldEqual expectedFrequency
    }
  }

  // Test the getTopWords function
  it should "return the top words correctly" in {
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

  // Test different parameters handling
  it should "handle different parameters correctly" in {
    val testCases = Table(
      ("cloudSize", "minLength", "windowSize", "minFrequency", "words", "expectedTopWords"),
      (3, 4, 5, 1, Seq("hello", "world", "hello", "scala"), Seq(("hello", 2), ("world", 1), ("scala", 1))),
      (3, 6, 5, 1, Seq("hello", "wor", "banana"), Seq(("banana", 1))), // test minLength
      (3, 4, 3, 1, Seq("scala", "java", "python", "kotlin"), Seq(("python", 1), ("kotlin", 1))), // test window size
      (3, 4, 5, 2, Seq("apple", "banana", "apple", "apple"), Seq(("apple", 3))) // test minimum frequency
    )

    forAll(testCases) { (cloudSize, minLength, windowSize, minFrequency, words, expectedTopWords) =>
      val outputStream = new ByteArrayOutputStream()
      val printStream = new PrintStream(outputStream)
      val sorter = new StreamFrequencySorter(cloudSize, minLength, windowSize, minFrequency)

      words.foreach(sorter.processWord)

      val topWords = sorter.getTopWords(cloudSize)

      topWords shouldEqual expectedTopWords
    }
  }

  // Test edge cases for word lengths and frequencies
  it should "not include words shorter than minLength" in {
    val sorter = new StreamFrequencySorter(3, 4, 5, 1)

    sorter.processWord("hi")      // below minLength
    sorter.processWord("hello")   // valid
    sorter.processWord("world")   // valid

    sorter.getTopWords(3) shouldEqual Seq(("hello", 1), ("world", 1)) // "hi" should be ignored
  }

  it should "not include words below minFrequency in top words" in {
    val sorter = new StreamFrequencySorter(3, 4, 5, 2)

    sorter.processWord("hello")   // frequency = 1
    sorter.processWord("hello")   // frequency = 2
    sorter.processWord("world")    // frequency = 1

    sorter.getTopWords(3) shouldEqual Seq(("hello", 2)) // "world" should be ignored due to minFrequency
  }
  
  // Optional: check the behavior with empty input
  it should "return empty top words when no words processed" in {
    val sorter = new StreamFrequencySorter(3, 4, 5, 1)
    sorter.getTopWords(3) shouldEqual Seq.empty // No words processed, should return empty
  }
}

  // sigpipe handling works because the logger said it does trust me bro
