package io.onedonut.re_droid.utils

/**
 * Created by frenchdonuts on 2/25/16.
 */
data class Three <A, B, C>(val _1: A, val _2: B, val _3: C) {
    override fun equals(other: Any?): Boolean {
        if (other is Three<*, *, *>) {
            return _1 === other._1 &&
                    _2 === other._2 &&
                    _3 === other._3
        }
        return false;
    }
}