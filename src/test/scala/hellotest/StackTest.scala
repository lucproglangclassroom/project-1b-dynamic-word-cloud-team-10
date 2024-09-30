package hellotest

// example straight from scalatest.org

import scala.collection.mutable.Stack
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.Suite
import org.scalatest.matchers.must.Matchers.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class StackSpec extends AnyFlatSpec with Suite:

  "A Stack" should "pop values in last-in-first-out order" in:
    val stack = Stack.empty[Int]
    stack.push(1)
    stack.push(2)
    stack.pop() must equal(2)
    stack.pop() must equal(1)

  it should "throw NoSuchElementException if an empty stack is popped" in:
    val emptyStack = Stack.empty[Int]
    an[NoSuchElementException] must be thrownBy:
      emptyStack.pop()

end StackSpec

class StreamFrequencySorterSpec extends AnyFlatSpec:

  "StreamFrequencySorter" should "update word frequencies correctly" in:
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    val sorter = new StreamFrequencySorter(3, 4, 5, printStream)

    sorter.processWord("hello")
    sorter.processWord("world")
    sorter.processWord("hello")

    val output = outputStream.toString.trim
    output must include("hello: 2")
    output must include("world: 1")

  it should "remove old words when exceeding window size" in:
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    val sorter = new StreamFrequencySorter(3, 4, 3, printStream)

    sorter.processWord("scala")
    sorter.processWord("java")
    sorter.processWord("kotlin")
    sorter.processWord("python") // This will push "scala" out of the window

    val output = outputStream.toString.trim
    output must not include "scala:"

  it should "sort words by frequency and alphabetically" in:
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    val sorter = new StreamFrequencySorter(2, 4, 5, printStream)

    sorter.processWord("apple")
    sorter.processWord("banana")
    sorter.processWord("apple")

    val output = outputStream.toString.trim
    output must include("apple: 2")
    output must include("banana: 1")

end StreamFrequencySorterSpec