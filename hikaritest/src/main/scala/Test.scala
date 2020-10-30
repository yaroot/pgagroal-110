import java.util.concurrent.{ThreadLocalRandom, TimeUnit}

import cats.implicits._
import cats.effect._
import doobie._
import doobie.hikari._
import doobie.implicits._
import scala.concurrent.duration._

object Test extends IOApp {
  val PoolSize: Int     = sys.env.getOrElse("PG_POOLSIZE", "30").toInt
  val MaxLifeTime: Long = sys.env.getOrElse("PG_CONN_LIFETIME", "30").toLong

  val DBHost: String   = sys.env.getOrElse("PG_HOST", "postgres")
  val DBPort: Int      = sys.env.getOrElse("PG_PORT", "5555").toInt
  val DBName: String   = sys.env.getOrElse("PG_DB", "test")
  val DBUser: String   = sys.env.getOrElse("PG_USER", "test")
  val DBPasswd: String = sys.env.getOrElse("PG_PASSWD", "test")

  val Driver     = "org.postgresql.Driver"
  val ConnectUrl = s"jdbc:postgresql://${DBHost}:${DBPort}/${DBName}"

  def newPool(): Resource[IO, Transactor[IO]] = for {
    ce <- ExecutionContexts.cachedThreadPool[IO]
    te <- ExecutionContexts.cachedThreadPool[IO]
    xa <- HikariTransactor.newHikariTransactor[IO](
            driverClassName = Driver,
            url = ConnectUrl,
            user = DBUser,
            pass = DBPasswd,
            connectEC = ce,                            // await connection here
            blocker = Blocker.liftExecutionContext(te) // execute JDBC operations here
          )
    _  <- Resource.liftF {
            xa.configure { ds =>
              IO {
                ds.setMaximumPoolSize(PoolSize)
                ds.setMaxLifetime(TimeUnit.SECONDS.toMillis(MaxLifeTime))
                ds.setConnectionTimeout(1000)
                ds.setInitializationFailTimeout(10)
              }
            }
          }
  } yield xa

  def rand(): Double = {
    ThreadLocalRandom.current().nextInt(100, 1500).toDouble / 1000
  }

  def test(xa: Transactor[IO]): IO[Unit] = {
    val time = rand()
    sql"select pg_sleep(${time})"
      .query[Unit]
      .option
      .transact(xa)
      .timeout(5.seconds)
      .attempt
      .map {
        case Left(ex) => ex.printStackTrace()
        case Right(_) => ()
      } >>
      Timer[IO].sleep(10.millis)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    println(s"$DBUser:$DBPasswd@$DBHost:$DBPort/$DBName (pool_size=$PoolSize, max_lifetime=$MaxLifeTime)")
    timer.sleep(3.seconds)
    val _run = for {
      xa <- fs2.Stream.resource(newPool())
      _  <- fs2.Stream
              .constant(1)
              .covary[IO]
              .parEvalMapUnordered(PoolSize) { _ =>
                test(xa)
              }
    } yield ()

    _run.compile.drain.as(ExitCode.Success)
  }
}
