package com.guizmaii.aecor.study.core.booking.state

import java.time.Instant

import aecor.data.Folded
import aecor.encoding.{KeyDecoder, KeyEncoder}
import cats.Order
import cats.data.{NonEmptyList => NEL}
import cats.kernel.Monoid
import com.guizmaii.aecor.study.core.booking.event._
import enumeratum._

import scala.collection.immutable

final case class BookingKey(value: String) extends AnyVal

object BookingKey {
  implicit final val keyEncoder: KeyEncoder[BookingKey] = KeyEncoder.encodeKeyString.contramap(_.value)
  implicit final val keyDecoder: KeyDecoder[BookingKey] = KeyDecoder.decodeKeyString.map(BookingKey(_))
}

// State itself
final case class BookingState(
    clientId: ClientId,
    concertId: ConcertId,
    seats: NEL[Seat],
    tickets: Option[NEL[Ticket]],
    status: BookingStatus,
    paymentId: Option[PaymentId],
    expiresAt: Option[Instant]
) {

  import Folded.syntax._

  // This function defines transformations for already existing bookings,
  // that's why we can define it as a method on BookingState
  def handleEvent(e: BookingEvent): Folded[BookingState] =
    e match {
      case _: BookingPlaced => impossible
      case BookingConfirmed(tickets, expiresAt) =>
        copy(
          tickets = Some(tickets),
          status = BookingStatus.Confirmed,
          expiresAt = expiresAt
        ).next
      case _: BookingCancelled    => copy(status = BookingStatus.Canceled).next
      case _: BookingDenied       => copy(status = BookingStatus.Denied).next
      case BookingExpired         => copy(status = BookingStatus.Canceled).next
      case BookingPaid(paymentId) => copy(paymentId = Some(paymentId)).next
      case BookingSettled         => copy(status = BookingStatus.Settled).next
    }
}

object BookingState {
  import Folded.syntax._

  // this is an initialization fold
  final def init(e: BookingEvent): Folded[BookingState] =
    e match {
      case BookingPlaced(clientId, concertId, seats) =>
        BookingState(
          clientId = clientId,
          concertId = concertId,
          seats = seats,
          tickets = None,
          status = BookingStatus.AwaitingConfirmation,
          paymentId = None,
          expiresAt = None
        ).next
      case _ => impossible
    }
}

// data definitions that are used in BookingState

final case class Money(amount: BigDecimal) extends AnyVal

object Money {
  implicit final val monoid: Monoid[Money] =
    new Monoid[Money] {
      def empty: Money                       = Money(0)
      def combine(x: Money, y: Money): Money = Money(x.amount + y.amount)
    }
}

final case class ClientId(value: String)  extends AnyVal
final case class ConcertId(value: String) extends AnyVal

final case class Row(num: Int)        extends AnyVal
final case class SeatNumber(num: Int) extends AnyVal

final case class Seat(row: Row, number: SeatNumber)

object Seat {
  import cats.implicits._

  implicit final val order: Order[Seat] = Order.by(s => (s.row.num, s.number.num))
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
