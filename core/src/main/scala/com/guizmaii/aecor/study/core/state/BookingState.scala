package com.guizmaii.aecor.study.core.state

import cats.Order
import cats.data.{NonEmptyList => NEL}
import enumeratum._

import scala.collection.immutable

// State itself
final case class BookingState(
    clientId: ClientId,
    concertId: ConcertId,
    seats: NEL[Seat],
    tickets: Option[NEL[Ticket]],
    status: BookingStatus,
    paymentId: Option[PaymentId]
)

// data definitions that are used in BookingState

final case class Money(amount: BigDecimal) extends AnyVal

final case class ClientId(value: String)  extends AnyVal
final case class ConcertId(value: String) extends AnyVal

final case class Row(num: Int)        extends AnyVal
final case class SeatNumber(num: Int) extends AnyVal

final case class Seat(row: Row, number: SeatNumber)

object Seat {
  import cats.implicits._

  implicit val order: Order[Seat] = Order.by((s: Seat) => (s.row.num, s.number.num))
}

final case class Ticket(seat: Seat, price: Money)

final case class PaymentId(value: String) extends AnyVal

sealed trait BookingStatus extends EnumEntry

object BookingStatus extends Enum[BookingStatus] with CirceEnum[BookingStatus] {
  final case object AwaitingConfirmation extends BookingStatus
  final case object Confirmed            extends BookingStatus
  final case object Denied               extends BookingStatus
  final case object Canceled             extends BookingStatus
  final case object Settled              extends BookingStatus

  def values: immutable.IndexedSeq[BookingStatus] = findValues
}