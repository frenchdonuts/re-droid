package io.onedonut.re_droid.utils

/**
 * Created by frenchdonuts on 2/25/16.
 */
data class Five <A, B, C, D, E>(val _1: A, val _2: B, val _3: C, val _4: D, val _5: E) {
    override fun equals(other: Any?): Boolean {
        if (other is Five<*, *, *, *, *>) {
            return _1 === other._1 &&
                    _2 === other._2 &&
                    _3 === other._3 &&
                    _4 === other._4 &&
                    _5 === other._5
        }
        return false;
    }
}
