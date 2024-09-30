// package hellotest

// import org.scalatest.flatspec.AnyFlatSpec
// import org.scalatest.matchers.must.Matchers._
// import org.scalatest.Ignore

// @Ignore
// class StreamFrequencySorterSpec extends AnyFlatSpec {

//   "StreamFrequencySorter" should "sort and update word frequencies correctly" in {
//     val sorter = new StreamFrequencySorter(10, 1, 10, 1)

//     sorter.processWord("apple")
//     sorter.processWord("banana")
//     sorter.processWord("cherry")
//     sorter.processWord("banana")

//     // "banana" should come first due to higher frequency
//     sorter.getTopWords(3) must contain theSameElementsAs Seq("banana" -> 2, "apple" -> 1, "cherry" -> 1)
//   }

//   it should "remove old words when exceeding window size" in {
//     val sorter = new StreamFrequencySorter(10, 1, 3, 1)

//     sorter.processWord("scala")
//     sorter.processWord("java")
//     sorter.processWord("kotlin")
//     sorter.processWord("python") // "scala" should be removed

//     // "scala" should no longer be in the top words due to window size constraints
//     sorter.getTopWords(3).map(_._1) must not contain ("scala")
//   }
// }