package io.onedonut.re_droid

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposables
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by frenchdonuts on 4/24/16.
 *
 *
 * @param init The initial State of your Application
 * @param reducer Your implementation of the State Transitions
 * @param scheduler The rx.Scheduler on which you run your Reducer and deliver your query results
 * @param middlewares
 *
 */
class RDB<Shape>(init: Shape,
                    private val reducer: (Action, Shape) -> Shape,
                    private val scheduler: Scheduler,
                    vararg middlewares: Middleware<Shape>) {
    //
    data class Entry<Shape>(val emitter: ObservableEmitter<in Shape>, val state: Shape)

    // We need a thread-safe queue to make sure emitters get every resulting state in response to Actions
    private val queue: ConcurrentLinkedQueue<Entry<Shape>> = ConcurrentLinkedQueue()
    private val emitters: CopyOnWriteArrayList<ObservableEmitter<in Shape>> = CopyOnWriteArrayList()

    @Volatile private var _curShape: Shape = init
    val curShape: Shape
        get() {
            return _curShape
        }

    @Volatile private var emitting = false


    // Our pure middleware - simply applies the reducer
    private val pure: Middleware<Shape> =
            {
                dispatcher ->
                {
                    rdb, action ->
                    //
                    synchronized(this) {
                        _curShape = reducer(action, _curShape)

                        for (emitter in emitters)
                            queue.add(Entry(emitter, _curShape))
                    }

                    emit()
                }
            }

    private val dispatcher: Dispatcher<Shape>

    init {
        dispatcher = (listOf(pure) + middlewares.asList())
                // Start w/ a dispatcher that does nothing...
                .fold({ rdb, action -> }) {
                    acc, currentMiddleware ->
                    currentMiddleware.invoke(acc)
                }
        // ...and end up w/ a dispatcher that applies all our middleware!
        // Note that though pure is the FIRST middleware applied in the list, it is RUN LAST.
        // Middleware gets APPLIED "right-to-left".
        // Middleware is evaluated "right-to-left"("outside-in") then "left-to-right"("inside-out").
    }

    fun dispatch(action: Action): Unit {
        //
        dispatcher.invoke(this, action)
    }

    fun appStates(): Observable<Shape> {
        return Observable.create { emitter ->
            val emit: Shape =
                    synchronized(this) {
                        _curShape
                    }
            // Emit latest state
            emitter.onNext(emit)

            synchronized(this) {
                emitters.add(emitter)
            }

            setDisposable(emitter)
        }
    }

    private fun setDisposable(emitter: ObservableEmitter<in Shape>): Unit {
        emitter.setDisposable(Disposables.fromAction {
            synchronized(this) {
                emitters.remove(emitter)
            }
        })
    }

    private fun emit(): Unit {
        val worker = scheduler.createWorker()
        worker.schedule {
            emitLoop()
            worker.dispose()
        }
    }

    private fun emitLoop() {
        synchronized(this) {
            if (emitting)
                return
            emitting = true
        }

        while (true) {
            val entry =
                    synchronized(this) {
                        if (queue.isEmpty()) {
                            emitting = false
                            return
                        }

                        queue.poll()
                    }

            entry.emitter.onNext(entry.state)
        }
    }
}