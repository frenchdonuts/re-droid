package io.onedonut.re_droid.middleware

import android.util.Log

/**
 * Created by pamelactan on 4/24/16.
 */

fun <Action, AppState> logger(eventHandler: (Action, AppState) -> AppState): (Action, AppState) -> AppState =
        { action, appState ->
            //
            Log.i("Action dispatched", "$action")
            eventHandler(action, appState)
        }
