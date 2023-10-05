package com.example

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.implicits.toTraverseOps
import org.http4s.EntityDecoder
import org.http4s.client.Client

import scala.collection.immutable

package object cart {

  def subtotal(cart: List[Product]): Double = cart.map(_.price).sum

  def add(addition: List[Product], cart: Ref[IO, List[Product]]): IO[Unit] = cart.update(existing => existing ++ addition)

  def taxPayable(sub: Double, tax: Double = 12.5) = sub * tax / 100

  def getProduct(name: String, client: Client[IO])(implicit d: EntityDecoder[IO, Product]): IO[Product] =
    client.expect[Product](s"https://raw.githubusercontent.com/mattjanks16/shopping-cart-test-data/main/$name.json")

  def getProducts(addition: List[String], client: Client[IO])(implicit d: EntityDecoder[IO, Product]): IO[List[Product]] =
    addition.traverse(getProduct(_, client))

  def addedItems(addition: List[Product]): immutable.Iterable[String] = addition.groupBy(_.title).map{
    case (title, products) =>
      val quantity = products.size
      val price = products.head.price
      s"Add $quantity × $title @ $price each"
  }

  def formattedState(added: String, sub: Double, tax: Double, total: Double): String = {
    def formatDecimals(double: Double) = f"$double%1.2f"
    val subtotalFormatted = formatDecimals(sub)
    val totalFormatted = formatDecimals(total)
    val taxFormatted = formatDecimals(tax)
    s"""
      |$added
      |Subtotal = $subtotalFormatted
      |Tax = $taxFormatted
      |Total = $totalFormatted
      |""".stripMargin
  }
}
case class Product(title: String, price: Double)
