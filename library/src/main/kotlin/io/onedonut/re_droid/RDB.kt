package io.onedonut.re_droid

import android.util.Log
import io.onedonut.re_droid.utils.Two
import rx.Observable
import rx.subjects.PublishSubject

/**
 * Created by pamelactan on 4/24/16.
 *
 * I wish we could do type aliases...
 */
class RDB<AppState, Action>(val init: AppState,
                            val reducer: (Action, AppState) -> AppState,
                            vararg middlewares: ((RDB<AppState, Action>, Action) -> Unit) -> (RDB<AppState, Action>, Action) -> Unit) {
    //
    var curAppState: AppState = init

    private val appStateSubject: PublishSubject<AppState> = PublishSubject.create()

    private var observers: Map<Any, Observable<out Any?>> = hashMapOf()

    // Our Pure Middleware - simply applies the reducer
    val pure: ((RDB<AppState, Action>, Action) -> Unit) -> ((RDB<AppState, Action>, Action) -> Unit) =
            {
                dispatcher ->
                {
                    rdb, action ->
                    curAppState = reducer(action, rdb.curAppState)
                    appStateSubject.onNext(curAppState)
                    // This Middleware should be applied last, so there should be no Middleware
                    // to dispatch after this

                }
            }

    // Start off w/ a dispatcher that does nothing...
    private var dispatcher: (RDB<AppState, Action>, Action) -> Unit = { rdb, action -> }

    init {
        // ...and end up w/ a dispatcher that applies all our Middleware!
        dispatcher = (listOf(pure) + middlewares.asList())
                .fold(
                        dispatcher,
                        {
                            acc, currentMiddleware ->
                            currentMiddleware.invoke(acc)
                        }
                )
        // ^Note that though Pure is the first Middleware in the list, it is applied LAST
        // In general, the Middleware gets applied right-to-left
    }

    fun dispatch(action: Action): Unit {
        //
        dispatcher.invoke(this, action)
    }

    fun <R> execute(g: (AppState) -> R): Observable<R> {
        //
        if (!observers.containsKey(g)) {
            Log.i("RDB.execute", "Adding a new query to our map...")
            val o: Observable<R> = appStateSubject.asObservable()
                    // Run the query on the newAppState; push out oldQueryResult
                    .scan (Two(g(curAppState), g(curAppState)),
                            { acc, newAppState ->
                                val (curQueryResult, oldQueryResult) = acc
                                // The current query result becomes the old query result
                                // We drop the old query result
                                Two(g(newAppState), curQueryResult)
                            }
                    )
                    // Only emit if new query result is different from the last query result
                    // Two assumptions make this work.
                    //   1) The data is immutable (this is why we use Kotlin's data classes)
                    //   2) The query only grabs data from AppState - it does not process it
                    //  This way, structural equality is the same as reference equality.
                    .filter { p -> p._1 != p._2 }
                    .map { it._1 }

            // Make sure this thing is running even if "no one" is subscribed to it
            observers += Pair(g, o)
        }

        // Make sure that new subscribers get the latest query result upon subscribing
        return Observable.merge(
                observers[g],
                Observable.just(g(curAppState))) as Observable<R>
    }
}