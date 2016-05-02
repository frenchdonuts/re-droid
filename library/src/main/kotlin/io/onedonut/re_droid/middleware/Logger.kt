package io.onedonut.re_droid.middleware

import android.util.Log
import io.onedonut.re_droid.Action
import io.onedonut.re_droid.RDB

/**
 * Created by pamelactan on 4/24/16.
 *
 * Dispatcher : (RDB<AppState, Action>, Action) -> Unit
 * Middleware :: Dispatcher -> Dispatcher
 */

fun <AppState> logActionsAndState(dispatcher: ((RDB<AppState>, Action) -> Unit)): ((RDB<AppState>, Action) -> Unit) =
        { rdb, action ->
            //
            Log.i("Action dispatched", "$action")

            // Run the next dispatcher
            dispatcher(rdb, action)
        }

