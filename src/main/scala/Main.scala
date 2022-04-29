import cats.data.{Reader, Validated}
import cats.effect._
import cats.implicits._

import com.monovore.decline._
import com.monovore.decline.effect._

import java.time._

import scala.language.postfixOps
import scala.util.Random

sealed trait TimePrecision
case object Millisecond extends TimePrecision
case object Second      extends TimePrecision

implicit val timePrecisionArgument: Argument[TimePrecision] = new Argument[TimePrecision] {

  def read(string: String) = {
    string match {
      case "millis"  => Validated.valid(Millisecond)
      case "seconds" => Validated.valid(Second)
      case _ => Validated.invalidNel("supported time precisions are seconds and milliseconds")
    }
  }

  def defaultMetavar = "millis|seconds"
}

case class Options(prefix: String, timePrecision: TimePrecision, randomLength: Int)

trait HasPrefix[A]:
  extension (a: A) def getPrefix: String

trait HasTimePrecision[A]:
  extension (a: A) def getTimePrecision: TimePrecision

trait HasRandomLength[A]:
  extension (a: A) def getRandomLength: Int

given HasPrefix[Options] with
  extension (o: Options) def getPrefix: String = o.prefix

given HasTimePrecision[Options] with
  extension (o: Options) def getTimePrecision: TimePrecision = o.timePrecision

given HasRandomLength[Options] with
  extension (o: Options) def getRandomLength: Int = o.randomLength

val alpha       = 'A' to 'Z' concat ('a' to 'z')
val numeric     = List('1', '2', '3', '4', '5', '6', '7', '8', '9', '0')
val charset     = numeric concat alpha
val charsetSize = charset size

def encode(i: Long, encoded: String = ""): String =
  if (i == 0) { encoded }
  else { encode(i / charsetSize, charset((i % charsetSize).toInt) + encoded) }

def getPrefix[E: HasPrefix]: Reader[E, String] = Reader(_.getPrefix)

def getTimePart[E: HasTimePrecision]: Reader[E, String] = Reader(e =>
  val now = Instant.now().toEpochMilli

  val timePart = e.getTimePrecision match {
    case Millisecond => now
    case Second      => now / 1000
  }

  encode(timePart)
)

def getRandomPart[E: HasRandomLength]: Reader[E, String] =
  Reader(e => Random.alphanumeric.take(e.getRandomLength).toList.mkString)

def makePrefixedUniqueId = for {
  prefix <- getPrefix
  time   <- getTimePart
  random <- getRandomPart
} yield s"$prefix-$time-$random"

object UIDApp
    extends CommandIOApp(
      name = "sc-uid",
      header = "utility for creating customised UIDs",
      version = "1.0.0"
    ) {

  override def main: Opts[IO[ExitCode]] =
    val prefixOpt        = Opts.option[String]("prefix", short = "p", help = "UID prefix")
    val randomLengthOpt  = Opts
      .option[Int]("random-length", short = "l", help = "number of random characters")
      .withDefault(16)
    val timePrecisionOpt = Opts
      .option[TimePrecision]("time-precision", short = "t", help = "time precision")
      .withDefault(Millisecond)

    (prefixOpt, randomLengthOpt, timePrecisionOpt).mapN { (prefix, randomLength, timePrecision) =>
      val options = Options(prefix, timePrecision, randomLength)
      val uid     = makePrefixedUniqueId.run(options)

      for { _ <- IO.println(uid) } yield ExitCode(0)
    }
}
