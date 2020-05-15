import cats.effect.{ExitCode, IO, IOApp, Resource}

//Not actually run anything, just example how it can be done
object ServiceApp extends IOApp {

  type Resources = String

  //Main run, very clean and easy to read
  override def run(args: List[String]): IO[ExitCode] = resources.use(program)


  //Your configuration required for service
  def resources: Resource[IO, Resources] = ???

  //Start components and services what you need
  def program(resources: Resources): IO[ExitCode] = ???


}
