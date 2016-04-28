package io.onedonut.re_droid.middleware

import android.util.Log
import io.onedonut.re_droid.RDB

/**
 * Created by pamelactan on 4/24/16.
 *
 * middleware :: ((RDB<AppState, Action>, Action) -> Unit) -> (RDB<AppState, Action>, Action) -> Unit)
 */

fun <Action, AppState> logActionsAndState(dispatcher: ((RDB<AppState, Action>, Action) -> Unit)): ((RDB<AppState, Action>, Action) -> Unit) =
        { rdb, action ->
            //
            Log.i("Action dispatched", "$action")
            dispatcher(rdb, action)
            Log.i("State due to dispatch", "${rdb.curAppState}")
        }

