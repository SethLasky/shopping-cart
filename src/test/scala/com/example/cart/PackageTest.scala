package com.example.cart

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.implicits.toTraverseOps
import com.example.Product
import io.circe.Decoder
import munit.CatsEffectSuite
import org.http4s.client.JavaNetClientBuilder

import scala.util.Random
import org.http4s.circe.CirceEntityDecoder._
import io.circe.generic.semiauto._

class PackageTest extends CatsEffectSuite {

  def randomString(length: Int = 10) = Random.alphanumeric.take(length).mkString
  def randomInt(limit: Int = 10) = Random.nextInt(limit)
  def randomDouble(limit: Int = 10) = randomInt(limit) + Random.nextDouble()
  def randomProducts(length: Int = 10) = (1 to length).map(_ => Product(randomString(), randomDouble())).toList

  val validProductNames = List("cheerios", "cornflakes", "frosties", "shreddies", "weetabix")

  implicit val prodCirceDecoder: Decoder[Product] = deriveDecoder

  test("subtotal should add up all products and return the total price before tax") {
    val products = randomProducts()
    val expected = products.map(_.price).sum
    assert(expected == subtotal(products))
  }

  test("add should add a list of products to the existing cart"){
    val initialCart = randomProducts()
    val addition = randomProducts()
    val expected = initialCart ++ addition
    val result = for{
      cart <- Ref[IO].of(initialCart)
      _ <- add(addition, cart)
      finalCart <- cart.get
    } yield finalCart
    assertIO(result, expected)
  }

  test("taxPayable should return the tax needed based on subtotal"){
    val taxPercentage = randomDouble(20)
    val subtotal = randomDouble(100)
    val expected = taxPercentage * subtotal / 100
    assert(expected == taxPayable(subtotal, taxPercentage))
  }

  test("retrieveProduct should retrieve any valid product"){
    val client = JavaNetClientBuilder[IO].create
    val result = validProductNames.traverse(getProduct(_, client)).map(_.size)
    assertIO(result, validProductNames.size)
  }

  test("retrieveProducts should retrieve products when given a list of product names"){
    val client  = JavaNetClientBuilder[IO].create
    val result = getProducts(validProductNames, client).map(_.size)
    assertIO(result, validProductNames.size)
  }

  test("addedItems should return a formatted list of added items"){
    val products = randomProducts()
    val quantity = randomInt()
    val expected = products.map { product =>
      val price = product.price
      s"Add $quantity × ${product.title} @ $price each"
    }.sorted
    val productList = List.fill(quantity)(products).flatten
    val result = addedItems(productList).toList.sorted
    assert(expected == result)
  }

  test("formattedState should take added items, subtotal, tax and total and format them correctly"){
    val added = addedItems(randomProducts()).mkString("\n")
    val sub = randomDouble()
    val subFormatted = f"$sub%1.2f"
    val tax = randomDouble()
    val taxFormatted = f"$tax%1.2f"
    val total = sub + tax
    val totalFormatted = f"$total%1.2f"
    val expected = s"""
       |$added
       |Subtotal = $subFormatted
       |Tax = $taxFormatted
       |Total = $totalFormatted
       |""".stripMargin
    val result = formattedState(added, sub, tax, total)
    assert(expected == result)
  }
}
