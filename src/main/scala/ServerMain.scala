import cats.effect._
import cats.effect.unsafe.implicits.global
import org.graalvm.compiler.virtual.phases.ea.EffectList.Effect
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir._
import sttp.tapir.files._
import fs2.{Stream, text}
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import cats.syntax.all._
import org.odftoolkit.simple.TextDocument
import sttp.capabilities.fs2.Fs2Streams
import fs2.io.file.{Files, Path}

import scala.jdk.CollectionConverters._
import java.nio.file.{Paths, StandardOpenOption}

object ServerMain extends IOApp {

  case class InputData(file: Stream[IO, Byte])

  def countCharacters(s: String): IO[Either[Unit, Int]] = {
    IO.println(s)
    IO.pure(Right[Unit, Int](s.length))
  }

  val countCharactersEndpoint: PublicEndpoint[String, Unit, Int, Any] =
    endpoint.get.in("test").in(path[String]("number")).out(plainBody[Int])

  val countCharactersRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(countCharactersEndpoint.serverLogic(number => {
      countCharacters(number)
    }))



  val fileEndpoint: Endpoint[Unit, (String, String, Stream[IO, Byte]), Unit, String, Any with Fs2Streams[IO]] =
    endpoint.post
      .in(path[String]("name") / path[String]("animal"))
      .in(streamBinaryBody(Fs2Streams[IO])(CodecFormat.OctetStream()))
      .out(plainBody[String])

  val fileRout: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(fileEndpoint.serverLogic {
      case (name, animal, file) => {
        val inputFile = "/home/azdrogov/template.odt"
        val outputFile = "/home/azdrogov/output.odt"
        val variables = Map("{{name}}" -> name, "{{animal}}" -> animal)

        file.through(Files[IO].writeAll(Path(inputFile))).compile.drain.unsafeRunSync

        // Загрузка документа
        val textDocument = TextDocument.loadDocument(inputFile)

        // Получение всего текста документа
        val paragraphs = textDocument.getParagraphIterator.asScala

        for (paragraph <- paragraphs) {
          var textContent = paragraph.getOdfElement.getTextContent
          for ((placeholder, replacement) <- variables) {
            textContent = textContent.replace(placeholder, replacement)
          }
          paragraph.getOdfElement.setTextContent(textContent)
        }

        textDocument.save(outputFile)


        val res: IO[Either[Unit, String]] = IO.pure(Right[Unit, String]("Done"))
        res
      }
    })




  val myEndpoints: List[AnyEndpoint] = List(countCharactersEndpoint, fileEndpoint)

  // first interpret as swagger ui endpoints, backend by the appropriate yaml
  val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[IO](myEndpoints, "My App", "1.0")
  val swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)

  override def run(args: List[String]): IO[ExitCode] = {
    val httpApp = (countCharactersRoutes <+> fileRout <+> swaggerRoutes).orNotFound

    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
