package io.onedonut.re_droid.middleware

import io.onedonut.re_droid.Action
import io.onedonut.re_droid.RDB
import rx.Observable
import rx.Subscription

/**
 * Created by frenchdonuts on 4/29/16.
 */

open class RxEffect(val asyncComputation: Observable<Action>, val cancellationKey: String = "", origin: String) : Action(origin)

class CancelRxEffect(val cancellationKey: String, origin: String) : Action(origin)


// :: Map<String, Subscription> -> (Dispatcher -> Dispatcher)
fun <AppState> rx(subscriptionMap: MutableMap<String, Subscription>): ((RDB<AppState>, Action) -> Unit) -> ((RDB<AppState>, Action) -> Unit) =
        { dispatcher ->
            { rdb, action ->
                //
                when (action) {
                    is RxEffect ->
                        subscriptionMap.put(action.cancellationKey, action.asyncComputation.subscribe { rdb.dispatch(it) })
                    is CancelRxEffect ->
                        if (subscriptionMap.containsKey(action.cancellationKey))
                            subscriptionMap[action.cancellationKey]?.unsubscribe()
                    // Ignore all other Actions
                    else -> { }
                }

                // Run the next Dispatcher
                dispatcher.invoke(rdb, action)
            }
        }
