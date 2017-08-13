package io.onedonut.re_droid.middleware

import io.onedonut.re_droid.Action
import io.onedonut.re_droid.RDB
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable

/**
 * Created by frenchdonuts on 4/29/16.
 */

data class Effect(val sideEffect: Single<Action>, val cancellationKey: String = "", val origin: String = "") : Action

data class CancelEffect(val cancellationKey: String, val origin: String = "") : Action


// :: Map<String, Subscription> -> (Dispatcher -> Dispatcher)
fun <Shape> rx(subscriptionMap: MutableMap<String, Disposable>): ((RDB<Shape>, Action) -> Unit) -> ((RDB<Shape>, Action) -> Unit) =
        { dispatcher ->
            { rdb, action ->
                //
                when (action) {
                    is Effect -> {
                        subscriptionMap.put(
                                action.cancellationKey,
                                action.sideEffect.subscribe(
                                        { rdb.dispatch(it) },
                                        { System.out.println("action.origin: ${action.origin}, message: ${it.message}") }
                                )
                        )
                    }
                    is CancelEffect ->
                        if (subscriptionMap.containsKey(action.cancellationKey))
                            subscriptionMap[action.cancellationKey]?.dispose()
                    // Ignore all other Actions
                    else -> { }
                }

                // Run the next Dispatcher
                dispatcher.invoke(rdb, action)
            }
        }
