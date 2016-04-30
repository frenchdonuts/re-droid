package io.onedonut.re_droid.middleware

import io.onedonut.re_droid.Action
import io.onedonut.re_droid.RDB
import rx.Observable

/**
 * Created by pamelactan on 4/29/16.
 */

open class AsyncEffect(val asyncComputation: Observable<Action>, origin: String) : Action(origin)

fun <AppState> async(dispatcher: ((RDB<AppState>, Action) -> Unit)): ((RDB<AppState>, Action) -> Unit) =
        { rdb, action ->
            //
            when (action) {
                is AsyncEffect -> action.asyncComputation.subscribe { rdb.dispatch(it) }
                // Ignore all other Actions
                else -> {
                }
            }

            // Run the next dispatcher
            dispatcher.invoke(rdb, action)
        }
