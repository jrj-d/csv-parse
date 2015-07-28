package csv

import shapeless._
import scala.util.{Failure, Success, Try}

trait RowToObjectTransformer[A] { def apply(l: Seq[String], nullChar: String): Try[(A, Seq[String])] }

object RowToObjectTransformer {

  implicit object RowToStringTransformer extends RowToObjectTransformer[String] {
    def apply(row: Seq[String], nullChar: String) = Success(row.head, row.tail)
  }

  implicit object RowToIntTransformer extends RowToObjectTransformer[Int] {
    def apply(row: Seq[String], nullChar: String) = Try(row.head.toInt, row.tail)
  }

  implicit object RowToDoubleTransformer extends RowToObjectTransformer[Double] {
    def apply(row: Seq[String], nullChar: String) = Try(row.head.toDouble, row.tail)
  }

  implicit object RowToFloatTransformer extends RowToObjectTransformer[Float] {
    def apply(row: Seq[String], nullChar: String) = Try(row.head.toFloat, row.tail)
  }

  implicit object RowToLongTransformer extends RowToObjectTransformer[Long] {
    def apply(row: Seq[String], nullChar: String) = Try(row.head.toLong, row.tail)
  }

  implicit object RowToBooleanTransformer extends RowToObjectTransformer[Boolean] {
    def apply(row: Seq[String], nullChar: String) = row.head.toLowerCase match {
      case "0" | "false" => Success(false, row.tail)
      case "1" | "true" => Success(true, row.tail)
      case _ => Failure(new RuntimeException(s"cannot cast '${row.head}' to boolean"))
    }
  }

  implicit def RowToCaseClassTransformer[A : Readable, L <: HList](
    implicit generatorOfA: Generic.Aux[A, L], rowConsumer: Lazy[RowConsumer[L]]) = new RowToObjectTransformer[A] {
      def apply(row: Seq[String], nullChar: String) =
        rowConsumer.value(row, nullChar).map{ case (hList, remainingCells) => (generatorOfA.from(hList), remainingCells)}
  }

  implicit def RowToNullableObjectTransformer[A](implicit rowToObjectTransformer: Lazy[RowToObjectTransformer[A]]) = new RowToObjectTransformer[Option[A]] {
    def apply(row: Seq[String], nullChar: String) = row.head match {
      case `nullChar` => Success(None, row.tail)
      case _ => rowToObjectTransformer.value(row, nullChar).map{case (value, remainingCells) => (Some(value), remainingCells)}
    }
  }

}

trait RowConsumer[L <: HList] { def apply(row: Seq[String], nullChar: String): Try[(L, Seq[String])] }

object RowConsumer {

  implicit val hNilConsumer = new RowConsumer[HNil] {
    def apply(row: Seq[String], nullChar: String) = Success((HNil, row))
  }

  implicit def hConsConsumer[H, T <: HList](implicit rowToObjectTransformer: Lazy[RowToObjectTransformer[H]], rowConsumer: Lazy[RowConsumer[T]]) = new RowConsumer[H :: T] {
    def apply(row: Seq[String], nullChar: String) = row match {
      case Nil => Failure(new RuntimeException("Expected more cells"))
      case _ => for {
        (headValue, remainingCellsAfterHead) <- rowToObjectTransformer.value(row, nullChar)
        (tailValues, remainingCells) <- rowConsumer.value(remainingCellsAfterHead, nullChar)
      } yield (headValue :: tailValues, remainingCells)
    }
  }

}

trait Readable[C]

case class CSVParser(nullChar: String) {

  def parse[A](row: Seq[String])(implicit rowToObjectTransformer: Lazy[RowToObjectTransformer[A]]): Try[A] =
    rowToObjectTransformer.value(row, nullChar).map{case (value, remainingCells) => value}

}

object CSVParser {

  def register[A] = new Readable[A] {}

}

