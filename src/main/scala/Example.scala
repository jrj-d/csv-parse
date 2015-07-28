import csv.{CSVParser}

case class Address(
                  street: String,
                  number: Int,
                  postalCode: Int,
                  city: String,
                  country: String,
                  additionalInfo: Option[String]
)

case class Person(
                 firstName: String,
                 lastName: String,
                 age: Int,
                 email: String,
                 address: Address)

object Address {
  implicit val addressIsReadable = CSVParser.register[Address]
}

object Person {
  implicit val personIsReadable = CSVParser.register[Person]
}

object Example {

  def main (args: Array[String]) {

    val nullChar = "\\N"
    val csvParser = CSVParser(nullChar)

    val row = Seq("guy", "george", "55", "guy.george@gmail.com", "rue des lilas", "4", "91000", "Massy", "France", "\\N")
    println(csvParser.parse[Person](row))
    // Success(Person(guy,george,55,guy.george@gmail.com,Address(rue des lilas,4,91000,Massy,France,None)))

    val rowTooShort = Seq("guy", "george", "55", "guy.george@gmail.com", "rue des lilas", "4", "91000", "Massy", "France")
    println(csvParser.parse[Person](rowTooShort))
    // Failure(java.lang.RuntimeException: Expected more cells)

    val rowNotInt = Seq("guy", "george", "55.5", "guy.george@gmail.com", "rue des lilas", "4", "91000", "Massy", "France", "\\N")
    println(csvParser.parse[Person](rowNotInt))
    // Failure(java.lang.NumberFormatException: For input string: "55.5")
  }
}