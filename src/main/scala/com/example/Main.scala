package com.example

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.Ref
import com.example.cart._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.EntityDecoder
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.http4s.circe.CirceEntityDecoder._

object Main extends IOApp {
  def run(addition: List[String]): IO[ExitCode] = {
    implicit val prodCirceDecoder: Decoder[Product] = deriveDecoder
    JavaNetClientBuilder[IO].resource.use { client =>
      Ref[IO].of(List[Product]()).flatMap { cart =>
        resultingState(addition, cart, client)
      }.map(println)
    }.as(ExitCode.Success)
  }

  def resultingState(addition: List[String], cart: Ref[IO, List[Product]], client: Client[IO])
                    (implicit d: EntityDecoder[IO, Product]): IO[String] = for {
    adding <- getProducts(addition, client)
    _ <- add(adding, cart)
    resultingCart <- cart.get
    sub = subtotal(resultingCart)
    tax = taxPayable(sub)
    total = sub + tax
    added = addedItems(adding).mkString("\n")
  } yield formattedState(added, sub, tax, total)
}
