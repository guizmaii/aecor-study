package com.guizmaii.aecor.study.core.entity

import cats.data.{NonEmptyList => NEL}
import com.guizmaii.aecor.study.core.state._

trait Booking[F[_]] {
  def place(client: ClientId, concert: ConcertId, seats: NEL[Seat]): F[Unit]
  def confirm(tickets: NEL[Ticket]): F[Unit]
  def deny(reason: String): F[Unit]
  def cancel(reason: String): F[Unit]
  def receivePayment(paymentId: PaymentId): F[Unit]
  def status: F[BookingStatus]
  def tickets: F[Option[NEL[Ticket]]]
}
