package com.guizmaii.aecor.study.core.utils
import java.time.Instant

import cats.Foldable

object InstantOps {
  implicit final class InstantOps(private val `this`: Instant) extends AnyVal {
    final def isAfterM[F[_]: Foldable](that: F[Instant]): Boolean = Foldable[F].exists(that)(`this`.isAfter)
  }
}
