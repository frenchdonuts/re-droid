package io.onedonut.re_droid.utils

/**
 * Created by frenchdonuts on 2/25/16.
 *
 * A container that uses reference equality for equality checks
 */
data class One <A>(val _1: A) {
    override fun equals(other: Any?): Boolean {
        if (other is One<*>) {
            return _1 === (other as One<*>)._1
        }
        return false
    }
}