package io.onedonut.re_droid.middleware

import io.onedonut.re_droid.Action
import io.onedonut.re_droid.RDB
import rx.Observable
import rx.Subscription

/**
 * Created by pamelactan on 4/29/16.
 */

open class AsyncEffect(val asyncComputation: Observable<Action>, val cancellationKey: String = "", origin: String) : Action(origin)

class CancelAsyncEffect(val cancellationKey: String, origin: String) : Action(origin)

fun <AppState> async(dispatcher: ((RDB<AppState>, Action) -> Unit)): ((RDB<AppState>, Action) -> Unit) =
        { rdb, action ->
            //
            when (action) {
                is AsyncEffect -> action.asyncComputation.subscribe { rdb.dispatch(it) }
                // Ignore all other Actions
                else -> { }
            }

            // Run the next dispatcher
            dispatcher.invoke(rdb, action)
        }

// :: Map<String, Subscription> -> (Dispatcher -> Dispatcher)
fun <AppState> async2(subscriptionMap: MutableMap<String, Subscription>): ((RDB<AppState>, Action) -> Unit) -> ((RDB<AppState>, Action) -> Unit) =
        { dispatcher ->
            { rdb, action ->
                //
                when (action) {
                    is AsyncEffect ->
                        subscriptionMap.put(action.cancellationKey, action.asyncComputation.subscribe { rdb.dispatch(it) })
                    is CancelAsyncEffect ->
                        if (subscriptionMap.containsKey(action.cancellationKey))
                            subscriptionMap.get(action.cancellationKey)?.unsubscribe()
                    // Ignore all other Actions
                    else -> { }
                }

                // Run the next dispatcher
                dispatcher.invoke(rdb, action)
            }
        }
