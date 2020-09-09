package com.bumptech.glide.load.engine.executor

import android.os.Process
import android.os.StrictMode
import android.text.TextUtils
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import com.bumptech.glide.util.Synthetic
import java.util.concurrent.*
import kotlin.math.min

/** A prioritized [ThreadPoolExecutor] for running jobs in Glide.  */
class GlideExecutor @VisibleForTesting internal constructor(private val delegate: ExecutorService) : ExecutorService {
    override fun execute(command: Runnable) {
        delegate.execute(command)
    }

    override fun submit(task: Runnable): Future<*> {
        return delegate.submit(task)
    }

    @Throws(InterruptedException::class)
    override fun <T> invokeAll(tasks: Collection<Callable<T>?>): List<Future<T>> {
        return delegate.invokeAll(tasks)
    }

    @Throws(InterruptedException::class)
    override fun <T> invokeAll(
            tasks: Collection<Callable<T>?>, timeout: Long, unit: TimeUnit): List<Future<T>> {
        return delegate.invokeAll(tasks, timeout, unit)
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun <T> invokeAny(tasks: Collection<Callable<T?>?>): T? {
        return delegate.invokeAny(tasks)
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun <T> invokeAny(
            tasks: Collection<Callable<T>?>, timeout: Long, unit: TimeUnit): T {
        return delegate.invokeAny(tasks, timeout, unit)
    }

    override fun <T> submit(task: Runnable, result: T): Future<T> {
        return delegate.submit(task, result)
    }

    override fun <T> submit(task: Callable<T>): Future<T> {
        return delegate.submit(task)
    }

    override fun shutdown() {
        delegate.shutdown()
    }

    override fun shutdownNow(): List<Runnable> {
        return delegate.shutdownNow()
    }

    override fun isShutdown(): Boolean {
        return delegate.isShutdown
    }

    override fun isTerminated(): Boolean {
        return delegate.isTerminated
    }

    @Throws(InterruptedException::class)
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        return delegate.awaitTermination(timeout, unit)
    }

    override fun toString(): String {
        return delegate.toString()
    }

    /**
     * A strategy for handling unexpected and uncaught [Throwable]s thrown by futures run on the
     * pool.
     */
    interface UncaughtThrowableStrategy {
        fun handle(t: Throwable?)

        companion object {
            /** Silently catches and ignores the uncaught [Throwable]s.  */ // Public API.
            @JvmField
            val IGNORE: UncaughtThrowableStrategy = object : UncaughtThrowableStrategy {
                override fun handle(t: Throwable?) {
                    // ignore
                }
            }

            /** Logs the uncaught [Throwable]s using [.TAG] and [Log].  */
            @JvmField
            val LOG: UncaughtThrowableStrategy = object : UncaughtThrowableStrategy {
                override fun handle(t: Throwable?) {
                    if (t != null && Log.isLoggable(TAG, Log.ERROR)) {
                        Log.e(TAG, "Request threw uncaught throwable", t)
                    }
                }
            }

            /** Rethrows the uncaught [Throwable]s to crash the app.  */ // Public API.
            @JvmField
            val THROW: UncaughtThrowableStrategy = object : UncaughtThrowableStrategy {
                override fun handle(t: Throwable?) {
                    if (t != null) {
                        throw RuntimeException("Request threw uncaught throwable", t)
                    }
                }
            }

            /** The default strategy, currently [.LOG].  */
            @JvmField
            val DEFAULT = LOG
        }
    }

    /**
     * A [java.util.concurrent.ThreadFactory] that builds threads slightly above priority [ ][android.os.Process.THREAD_PRIORITY_BACKGROUND].
     */
    private class DefaultThreadFactory constructor(
            private val name: String?,
            @Synthetic val uncaughtThrowableStrategy: UncaughtThrowableStrategy,
            @Synthetic val preventNetworkOperations: Boolean) : ThreadFactory {
        private var threadNum = 0
        @Synchronized
        override fun newThread(runnable: Runnable): Thread {
            val result: Thread = object : Thread(runnable, "glide-$name-thread-$threadNum") {
                override fun run() {
                    // why PMD suppression is needed: https://github.com/pmd/pmd/issues/808
                    Process.setThreadPriority(
                            DEFAULT_PRIORITY) // NOPMD AccessorMethodGeneration
                    if (preventNetworkOperations) {
                        StrictMode.setThreadPolicy(
                                StrictMode.ThreadPolicy.Builder().detectNetwork().penaltyDeath().build())
                    }
                    try {
                        super.run()
                    } catch (t: Throwable) {
                        uncaughtThrowableStrategy.handle(t)
                    }
                }
            }
            threadNum++
            return result
        }

        companion object {
            private const val DEFAULT_PRIORITY = (Process.THREAD_PRIORITY_BACKGROUND
                    + Process.THREAD_PRIORITY_MORE_FAVORABLE)
        }
    }

    /** A builder for [GlideExecutor]s.  */
    class Builder @Synthetic internal constructor(private val preventNetworkOperations: Boolean) {
        private var corePoolSize = 0
        private var maximumPoolSize = 0
        private var uncaughtThrowableStrategy = UncaughtThrowableStrategy.DEFAULT
        private var name: String? = null
        private var threadTimeoutMillis: Long = 0

        /**
         * Allows both core and non-core threads in the executor to be terminated if no tasks arrive for
         * at least the given timeout milliseconds.
         *
         *
         * Use [.NO_THREAD_TIMEOUT] to remove a previously set timeout.
         */
        fun setThreadTimeoutMillis(threadTimeoutMillis: Long): Builder {
            this.threadTimeoutMillis = threadTimeoutMillis
            return this
        }

        /** Sets the maximum number of threads to use.  */
        fun setThreadCount(@IntRange(from = 1) threadCount: Int): Builder {
            corePoolSize = threadCount
            maximumPoolSize = threadCount
            return this
        }

        /**
         * Sets the [UncaughtThrowableStrategy] to use for unexpected exceptions thrown by tasks
         * on [GlideExecutor]s built by this `Builder`.
         */
        fun setUncaughtThrowableStrategy(strategy: UncaughtThrowableStrategy): Builder {
            uncaughtThrowableStrategy = strategy
            return this
        }

        /**
         * Sets the prefix to use for each thread name created by any [GlideExecutor]s built by
         * this `Builder`.
         */
        fun setName(name: String?): Builder {
            this.name = name
            return this
        }

        /** Builds a new [GlideExecutor] with any previously specified options.  */
        fun build(): GlideExecutor {
            require(!TextUtils.isEmpty(name)) { "Name must be non-null and non-empty, but given: $name" }
            val executor = ThreadPoolExecutor(
                    corePoolSize,
                    maximumPoolSize,  /*keepAliveTime=*/
                    threadTimeoutMillis,
                    TimeUnit.MILLISECONDS,
                    PriorityBlockingQueue(),
                    DefaultThreadFactory(name, uncaughtThrowableStrategy, preventNetworkOperations))
            if (threadTimeoutMillis != NO_THREAD_TIMEOUT) {
                executor.allowCoreThreadTimeOut(true)
            }
            return GlideExecutor(executor)
        }

        companion object {
            /**
             * Prevents core and non-core threads from timing out ever if provided to [ ][.setThreadTimeoutMillis].
             */
            const val NO_THREAD_TIMEOUT = 0L
        }
    }

    companion object {
        /**
         * The default thread name prefix for executors used to load/decode/transform data not found in
         * cache.
         */
        private const val DEFAULT_SOURCE_EXECUTOR_NAME = "source"

        /**
         * The default thread name prefix for executors used to load/decode/transform data found in
         * Glide's cache.
         */
        private const val DEFAULT_DISK_CACHE_EXECUTOR_NAME = "disk-cache"

        /**
         * The default thread count for executors used to load/decode/transform data found in Glide's
         * cache.
         */
        private const val DEFAULT_DISK_CACHE_EXECUTOR_THREADS = 1
        private const val TAG = "GlideExecutor"

        /**
         * The default thread name prefix for executors from unlimited thread pool used to
         * load/decode/transform data not found in cache.
         */
        private const val DEFAULT_SOURCE_UNLIMITED_EXECUTOR_NAME = "source-unlimited"
        private const val DEFAULT_ANIMATION_EXECUTOR_NAME = "animation"

        /** The default keep alive time for threads in our cached thread pools in milliseconds.  */
        private val KEEP_ALIVE_TIME_MS = TimeUnit.SECONDS.toMillis(10)

        // Don't use more than four threads when automatically determining thread count..
        private const val MAXIMUM_AUTOMATIC_THREAD_COUNT = 4

        // May be accessed on other threads, but this is an optimization only so it's ok if we set its
        // value more than once.
        @Volatile
        private var bestThreadCount = 0

        /**
         * Returns a new [Builder] with the [.DEFAULT_DISK_CACHE_EXECUTOR_THREADS] threads,
         * [.DEFAULT_DISK_CACHE_EXECUTOR_NAME] name and [UncaughtThrowableStrategy.DEFAULT]
         * uncaught throwable strategy.
         *
         *
         * Disk cache executors do not allow network operations on their threads.
         */
        fun newDiskCacheBuilder(): Builder {
            return Builder( /*preventNetworkOperations=*/true)
                    .setThreadCount(DEFAULT_DISK_CACHE_EXECUTOR_THREADS)
                    .setName(DEFAULT_DISK_CACHE_EXECUTOR_NAME)
        }

        /** Shortcut for calling [Builder.build] on [.newDiskCacheBuilder].  */
        @JvmStatic
        fun newDiskCacheExecutor(): GlideExecutor {
            return newDiskCacheBuilder().build()
        }

        // Public API.
        @Deprecated("""Use {@link #newDiskCacheBuilder()} and {@link
   *     Builder#setUncaughtThrowableStrategy(UncaughtThrowableStrategy)} instead.""")
        fun newDiskCacheExecutor(
                uncaughtThrowableStrategy: UncaughtThrowableStrategy): GlideExecutor {
            return newDiskCacheBuilder().setUncaughtThrowableStrategy(uncaughtThrowableStrategy).build()
        }

        // Public API.
        @Deprecated("Use {@link #newDiskCacheBuilder()} instead. ")
        fun newDiskCacheExecutor(
                threadCount: Int, name: String?, uncaughtThrowableStrategy: UncaughtThrowableStrategy): GlideExecutor {
            return newDiskCacheBuilder()
                    .setThreadCount(threadCount)
                    .setName(name)
                    .setUncaughtThrowableStrategy(uncaughtThrowableStrategy)
                    .build()
        }

        /**
         * Returns a new [Builder] with the default thread count returned from [ ][.calculateBestThreadCount], the [.DEFAULT_SOURCE_EXECUTOR_NAME] thread name prefix, and
         * the [ ][com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy.DEFAULT]
         * uncaught throwable strategy.
         *
         *
         * Source executors allow network operations on their threads.
         */
        @JvmStatic
        fun newSourceBuilder(): Builder {
            return Builder( /*preventNetworkOperations=*/false)
                    .setThreadCount(calculateBestThreadCount())
                    .setName(DEFAULT_SOURCE_EXECUTOR_NAME)
        }

        /** Shortcut for calling [Builder.build] on [.newSourceBuilder].  */
        @JvmStatic
        fun newSourceExecutor(): GlideExecutor {
            return newSourceBuilder().build()
        }

        // Public API.
        @JvmStatic
        @Deprecated("Use {@link #newSourceBuilder()} instead. ")
        fun newSourceExecutor(
                uncaughtThrowableStrategy: UncaughtThrowableStrategy): GlideExecutor {
            return newSourceBuilder().setUncaughtThrowableStrategy(uncaughtThrowableStrategy).build()
        }

        // Public API.
        @JvmStatic
        @Deprecated("Use {@link #newSourceBuilder()} instead. ")
        fun newSourceExecutor(
                threadCount: Int, name: String?, uncaughtThrowableStrategy: UncaughtThrowableStrategy): GlideExecutor {
            return newSourceBuilder()
                    .setThreadCount(threadCount)
                    .setName(name)
                    .setUncaughtThrowableStrategy(uncaughtThrowableStrategy)
                    .build()
        }

        /**
         * Returns a new unlimited thread pool with zero core thread count to make sure no threads are
         * created by default, [.KEEP_ALIVE_TIME_MS] keep alive time, the [ ][.SOURCE_UNLIMITED_EXECUTOR_NAME] thread name prefix, the [ ][com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy.DEFAULT]
         * uncaught throwable strategy, and the [SynchronousQueue] since using default unbounded
         * blocking queue, for example, [PriorityBlockingQueue] effectively won't create more than
         * `corePoolSize` threads. See [
 * ThreadPoolExecutor documentation](http://developer.android.com/reference/java/util/concurrent/ThreadPoolExecutor.html).
         *
         *
         * Source executors allow network operations on their threads.
         */
        @JvmStatic
        fun newUnlimitedSourceExecutor(): GlideExecutor {
            return GlideExecutor(
                    ThreadPoolExecutor(
                            0, Int.MAX_VALUE,
                            KEEP_ALIVE_TIME_MS,
                            TimeUnit.MILLISECONDS,
                            SynchronousQueue(),
                            DefaultThreadFactory(
                                    DEFAULT_SOURCE_UNLIMITED_EXECUTOR_NAME, UncaughtThrowableStrategy.DEFAULT, false)))
        }

        /**
         * Returns a new fixed thread pool that defaults to either one or two threads depending on the
         * number of available cores to use when loading frames of animations.
         *
         *
         * Animation executors do not allow network operations on their threads.
         */
        @JvmStatic
        fun newAnimationBuilder(): Builder {
            val bestThreadCount = calculateBestThreadCount()
            // We don't want to add a ton of threads running animations in parallel with our source and
            // disk cache executors. Doing so adds unnecessary CPU load and can also dramatically increase
            // our maximum memory usage. Typically one thread is sufficient here, but for higher end devices
            // with more cores, two threads can provide better performance if lots of GIFs are showing at
            // once.
            val maximumPoolSize = if (bestThreadCount >= 4) 2 else 1
            return Builder( /*preventNetworkOperations=*/true)
                    .setThreadCount(maximumPoolSize)
                    .setName(DEFAULT_ANIMATION_EXECUTOR_NAME)
        }

        /** Shortcut for calling [Builder.build] on [.newAnimationBuilder].  */
        @JvmStatic
        fun newAnimationExecutor(): GlideExecutor {
            return newAnimationBuilder().build()
        }

        // Public API.
        @JvmStatic
        @Deprecated("Use {@link #newAnimationBuilder()} instead. ")
        fun newAnimationExecutor(
                threadCount: Int, uncaughtThrowableStrategy: UncaughtThrowableStrategy): GlideExecutor {
            return newAnimationBuilder()
                    .setThreadCount(threadCount)
                    .setUncaughtThrowableStrategy(uncaughtThrowableStrategy)
                    .build()
        }

        /** Determines the number of cores available on the device.  */ // Public API.
        fun calculateBestThreadCount(): Int {
            if (bestThreadCount == 0) {
                bestThreadCount = min(MAXIMUM_AUTOMATIC_THREAD_COUNT, RuntimeCompat.availableProcessors())
            }
            return bestThreadCount
        }
    }
}