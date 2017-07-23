package io.onedonut.re_droid.middleware

import io.onedonut.re_droid.Action
import io.onedonut.re_droid.RDB
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

/**
 * Created by frenchdonuts on 4/29/16.
 */

data class RxEffect(val asyncComputation: Observable<Action>, val cancellationKey: String = "", val origin: String = "") : Action

data class CancelRxEffect(val cancellationKey: String, val origin: String = "") : Action


// :: Map<String, Subscription> -> (Dispatcher -> Dispatcher)
fun <AppState> rx(subscriptionMap: MutableMap<String, Disposable>): ((RDB<AppState>, Action) -> Unit) -> ((RDB<AppState>, Action) -> Unit) =
        { dispatcher ->
            { rdb, action ->
                //
                when (action) {
                    is RxEffect -> {
                        subscriptionMap.put(
                                action.cancellationKey,
                                action.asyncComputation.subscribe(
                                        { rdb.dispatch(it) },
                                        { System.out.println("action.origin: ${action.origin}, message: ${it.message}") }
                                )
                        )
                    }
                    is CancelRxEffect ->
                        if (subscriptionMap.containsKey(action.cancellationKey))
                            subscriptionMap[action.cancellationKey]?.dispose()
                    // Ignore all other Actions
                    else -> { }
                }

                // Run the next Dispatcher
                dispatcher.invoke(rdb, action)
            }
        }
