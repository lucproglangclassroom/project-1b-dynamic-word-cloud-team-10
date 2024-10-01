package hellotest

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.util.control.NonFatal
import org.mockito.Mockito._
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.log4s.getLogger
import org.slf4j.LoggerFactory
import scala.language.unsafeNulls
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


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
  /*it should "handle SIGPIPE correctly in printWordCloud" in {
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    val sorter = new StreamFrequencySorter(3, 4, 5, 1)
    var exitCalled = false

    sorter.processWord("test")
    sorter.processWord("word")

    printStream.close()

    try {
      Main.printWordCloud(sorter, 10, printStream)
    } 
    catch {
      case NonFatal(e) =>
        exitCalled = true
    }
    exitCalled shouldBe true
  } */
}
 // has the error "Values of types ch.qos.logback.classic.Level and ch.qos.logback.classic.Level | Null cannot be compared with == or !="
 // so im commenting it out for now
class LogCaptureAppender extends AppenderBase[ILoggingEvent] {
  private val events = scala.collection.mutable.ArrayBuffer.empty[ILoggingEvent]

  override def append(eventObject: ILoggingEvent): Unit = {
    events.append(eventObject)
  }

  def getEvents: Seq[ILoggingEvent] = events.toSeq
}

class LoggingTest extends AnyFlatSpec with Matchers {
  "Main" should "log appropriate messages" in {
    // Capture logger output
    val logger = LoggerFactory.getLogger("hellotest.Main").asInstanceOf[Logger]
    val appender = new LogCaptureAppender()
    appender.setContext(logger.getLoggerContext)
    logger.addAppender(appender)
    logger.setLevel(Level.INFO)
    appender.start()

    // Test the logging functionality in Main
    val future = Future {
      Main.main(Array("--cloud-size", "5", "--length-at-least", "3"))
    }

    // Check that logs were created (at least one info log should be there)
    val logs = appender.getEvents
    logs should not be empty

    // Check specific log message content
    val infoLogs = logs.filter(event => Option(event.getLevel).contains(Level.INFO)).map(_.getFormattedMessage)

    // Use contains to check for message existence without null issues
    infoLogs.contains("program start (logging begins here)") should be(true)

    appender.stop()
    Await.result(future, 5.seconds)
  }
}