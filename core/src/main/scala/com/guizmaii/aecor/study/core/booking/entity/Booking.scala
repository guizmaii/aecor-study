package com.guizmaii.aecor.study.core.booking.entity

import java.time.Instant

import cats.data.{NonEmptyList => NEL}
import cats.tagless.autoFunctorK
import com.guizmaii.aecor.study.core.booking.state._

@autoFunctorK(autoDerivation = false)
trait Booking[F[_]] {
  def place(client: ClientId, concert: ConcertId, seats: NEL[Seat]): F[Unit]
  def confirm(tickets: NEL[Ticket], expiresAt: Option[Instant]): F[Unit]
  def deny(reason: String): F[Unit]
  def cancel(reason: String): F[Unit]
  def receivePayment(paymentId: PaymentId): F[Unit]
  def status: F[BookingStatus]
  def tickets: F[Option[NEL[Ticket]]]
  def expire: F[Unit]
}
