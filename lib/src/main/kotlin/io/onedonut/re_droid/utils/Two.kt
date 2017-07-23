package io.onedonut.re_droid.utils

/**
 * Created by frenchdonuts on 2/25/16.
 */
data class Two <A,B> (val _1: A, val _2: B) {
    override fun equals(other: Any?): Boolean {
        if (other is Two<*,*>) {
            return _1 === other._1 &&
                    _2 === other._2
        }
        return false;
    }
}