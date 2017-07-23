package io.onedonut.re_droid

/**
 * Created by frenchdonuts on 7/23/17.
 */
interface Action
typealias Dispatcher<Shape> = (RDB<Shape>, Action) -> Unit
typealias Middleware<Shape> = (Dispatcher<Shape>) -> Dispatcher<Shape>

