package io.onedonut.re_droid

import rx.Observable
import rx.subjects.PublishSubject

/**
 * Created by pamelactan on 4/24/16.
 */
class RDB<AppState, Action>(val init: AppState, val eventHandler: (Action, AppState) -> AppState) {
    var curAppState: AppState = init

    private val appStateSubject: PublishSubject<AppState> = PublishSubject.create()

    var observers: Map<Any, Observable<out Any?>> = hashMapOf()

    init {
        appStateSubject.onNext(init)

        appStateSubject.subscribe {
            curAppState = it
            //Log.i("Current AppState", "$curAppState")
        }
    }

    fun dispatch(action: Action): Unit {
        // TODO: Add middleware capability. Then add Logger middleware
        //Log.i("Action dispatched", "$action")
        curAppState = eventHandler(action, curAppState)

        appStateSubject.onNext(curAppState)
    }


    fun <R> execute(getter: (AppState) -> R): Observable<R> {
        //
        if (!observers.containsKey(getter)) {
            val o: Observable<R> = appStateSubject.asObservable()
                    // Run the query on the newAppState; push out oldQueryResult
                    .scan (Pair(getter(curAppState), getter(curAppState)),
                            { acc, newAppState ->
                                val (curQueryResult, oldQueryResult) = acc
                                // The curQueryResult becomes the oldQueryResult
                                // We drop the old query result
                                Pair(getter(newAppState), curQueryResult)
                            }
                    )
                    // Only emit if new query result is different from the last query result
                    //  Works fast if the underlying data is immutable: that way, equality checks are
                    //  simply reference checks. This is why the use of Kotlin data classes are ideal.
                    .filter { p -> p.first != p.second }
                    .map { it.first }

            // Make sure this thing is running(updating) even if no observers are subscribed to it
            o.subscribe();
            observers += Pair(getter, o)
        }

        // Make sure that new subscribers get the latest query result upon subscribing
        return Observable.merge(
                observers[getter],
                Observable.just(getter(curAppState))) as Observable<R>
    }
}