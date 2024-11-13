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
import org.http4s.headers.`Content-Disposition`
import org.typelevel.ci.CIString
import sttp.tapir.server.ServerEndpoint

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters._
import java.nio.file.{Paths, StandardOpenOption}

object ServerMain extends IOApp {

  case class InputData(file: Stream[IO, Byte])

  val staticFileEndpoint: ServerEndpoint[Any, IO] =
    staticFilesGetServerEndpoint("static")("/tmp/fileFiller/img")

  val fileRoutes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(staticFileEndpoint)


  val countCharactersEndpoint: Endpoint[Unit, Unit, Unit, String, Any] =
    endpoint.get.in("healthCheck").out(plainBody[String])

  val countCharactersRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(countCharactersEndpoint.serverLogic(_ => {
      IO("Да живой я, блять, живой!".asRight[Unit])
    }))

  val loveEndpoint =
    endpoint.get.in("whatIsLove").out(htmlBodyUtf8)

  val loveRoute = Http4sServerInterpreter[IO]().toRoutes(loveEndpoint.serverLogic{ _ =>
    IO((
      """<!DOCTYPE html>
        |<html lang="en">
        |<head>
        |    <meta charset="UTF-8">
        |    <meta http-equiv="X-UA-Compatible" content="IE=edge">
        |    <meta name="viewport" content="width=device-width, initial-scale=1.0">
        |    <title>Local Image Page</title>
        |</head>
        |<body>
        |    <h1>This is love:</h1>
        |    <img src="static/love.png" alt="Local Image"/>
        |</body>
        |</html>
        |""".stripMargin
    ).asRight[Unit])
  })

  val fileEndpoint =
    endpoint.post
      .in(path[String]("name").example("Макс") / path[String]("animal").example("Обезьяна"))
      .in(streamBinaryBody(Fs2Streams[IO])(CodecFormat.OctetStream()))
      .out(streamBinaryBody(Fs2Streams[IO])(CodecFormat.OctetStream()))

  val fileRout: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(fileEndpoint.serverLogic {
      case (name, animal, file) => {
        val inputFile = "/tmp/fileFiller/template.odt"
        val outputFile = "/tmp/fileFiller/output.odt"
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

        IO(Files[IO].readAll(Path(outputFile)).asRight[Unit])
      }
    }).map(r => Response(
      headers = Headers(`Content-Disposition`("attachment", Map(CIString("filename") -> Some(s"${URLEncoder.encode("aaa", StandardCharsets.UTF_8.toString)}.odt").getOrElse(""))))
    ).withBodyStream(r.body))




  val myEndpoints: List[AnyEndpoint] = List(countCharactersEndpoint, fileEndpoint, loveEndpoint)

  // first interpret as swagger ui endpoints, backend by the appropriate yaml
  val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[IO](myEndpoints, "My App", "1.0")
  val swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)

  override def run(args: List[String]): IO[ExitCode] = {
    val httpApp = (countCharactersRoutes <+> fileRout <+> loveRoute <+> fileRoutes <+> swaggerRoutes).orNotFound

    BlazeServerBuilder[IO]
      .bindHttp(9000, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
