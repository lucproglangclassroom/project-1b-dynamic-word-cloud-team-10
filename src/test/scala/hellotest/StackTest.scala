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
import java.io.{File, PrintWriter}
import scala.io.{BufferedSource, Source}
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Queue

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

    val initialState = (Queue.empty[String], Map.empty[String, Int])

    testCases.foldLeft(initialState) { case ((wordQueue, wordFrequency), (input, expectedFrequency)) =>
        val (updatedQueue, updatedFrequency) = sorter.processWord(input, wordQueue, wordFrequency)
        
        // Print intermediate states for debugging
        //println(s"Processing word: $input")
        //println(s"Queue: $updatedQueue")
        //println(s"Frequency map: $updatedFrequency")
        
        val frequency = sorter.getTopWords(3, updatedFrequency).find(_._1 == input).map(_._2).getOrElse(0)
        //println(s"Expected frequency: $expectedFrequency, Actual frequency: $frequency")
        frequency shouldEqual expectedFrequency
        
        (updatedQueue, updatedFrequency) // Return updated state
    }
  }



  //testing out getTopWords function
  it should "return the top words correctly" in {
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    val sorter = new StreamFrequencySorter(3, 4, 10, 1)
    
    // Immutable initial state
    val initialState = (Queue.empty[String], Map.empty[String, Int])
    
    // Process words immutably
    val finalState = Seq("apple", "banana", "apple", "orange", "banana", "banana").foldLeft(initialState) { 
        case ((wordQueue, wordFrequency), word) => 
            sorter.processWord(word, wordQueue, wordFrequency)
    }
    
    val expectedTopWords = Seq(("banana", 3), ("apple", 2), ("orange", 1))
    val topWords = sorter.getTopWords(3, finalState._2)
    topWords shouldEqual expectedTopWords
}

// TODO: check for errors with the parameters in StreamFrequencySorter: 
//     - test for min / max parameters in minLength, minFrequency, and windowSize
//     - test for the wordlength minimum critera has been implemented 
//     - OPTIONAL: test to see how program responds with non-numerical inputs
  it should "handle different parameters correctly" in {
    val testCases = Table(
        ("cloudSize", "minLength", "windowSize", "minFrequency", "words", "expectedTopWords"),
        (3, 4, 5, 1, Seq("hello", "world", "hello", "scala"), Seq(("hello", 2), ("world", 1), ("scala", 1))),
        (3, 6, 5, 1, Seq("hello", "wor", "banana"), Seq(("banana", 1))),
        (3, 4, 3, 1, Seq("scala", "java", "python", "kotlin"), Seq(("python", 1), ("kotlin", 1))),
        (3, 4, 5, 2, Seq("apple", "banana", "apple", "apple"), Seq(("apple", 3)))
    )

    forAll(testCases) { (cloudSize, minLength, windowSize, minFrequency, words, expectedTopWords) =>
        val sorter = new StreamFrequencySorter(cloudSize, minLength, windowSize, minFrequency)
        
        val initialState = (Queue.empty[String], Map.empty[String, Int])

        val finalState = words.foldLeft(initialState) {
            case ((wordQueue, wordFrequency), word) =>
                sorter.processWord(word, wordQueue, wordFrequency)
        }

        val topWords = sorter.getTopWords(cloudSize, finalState._2)
        topWords shouldEqual expectedTopWords
    }
  }
  it should "test window size enforcement precisely" in {
    val sorter = new StreamFrequencySorter(3, 4, 3, 1)

    // Define initial state and input sequence
    val initialState = (Queue.empty[String], Map.empty[String, Int])
    val words = Seq("scala", "java", "python", "kotlin")

    // Step through word processing
    val steps = words.foldLeft(Seq.empty[(Queue[String], Map[String, Int])]) { (acc, word) =>
        val currentState = if (acc.isEmpty) initialState else acc.last
        val newState = sorter.processWord(word, currentState._1, currentState._2)
        acc :+ newState
    }

    // Inspect each step
    steps.foreach { case (queue, freqMap) =>
        println(s"Queue: $queue")
        println(s"Frequency map: $freqMap")
    }

    // Expected final state
    val expectedFinalQueue = Queue("python", "kotlin")
    val expectedFinalFrequency = Map("python" -> 1, "kotlin" -> 1)

    steps.last._1 shouldEqual expectedFinalQueue
    steps.last._2 shouldEqual expectedFinalFrequency
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
  val logFilePath = "log/hellotest-scala.log"

  "Main" should "log appropriate messages" in {
    // clear out logging file
    clearLogFile(logFilePath)

    // Test the logging functionality in Main
    val future = Future {
      Main.main(Array("--cloud-size", "5", "--length-at-least", "3"))
    }

    Thread.sleep(1000)
    
    // Check that logs were created (at least one info log should be there)
    val logContent = readLogFile(logFilePath)
    logContent should not be empty
    logContent should include ("Program start (logging begins here)")

    //Await.result(future, 20.seconds)
    
  }
  def clearLogFile(filePath: String): Unit = {
    val writer = new PrintWriter(new File(filePath))
    writer.write("")  // Write an empty string to clear the file
    writer.close()
  }
  def readLogFile(filePath: String): String = {
    val source: BufferedSource = Source.fromFile(filePath)
    try source.getLines().mkString("\n")  // Read all lines and concatenate them into a single string
    finally source.close()
  }
}