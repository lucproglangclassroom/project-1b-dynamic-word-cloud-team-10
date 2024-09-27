package hellotest

object Main:

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

}

  def main(args: Array[String]) = 
    println("Hello scalatest!")
    println(s"Today's date is ${java.time.LocalDate.now}.")

     // Process each word as it arrives
     //streamOfWords is a val
    //streamOfWords.foreach(processWord)
   

end Main
