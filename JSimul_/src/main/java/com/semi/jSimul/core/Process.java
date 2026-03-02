package com.semi.jSimul.core;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process wrapping user logic that awaits events. Modeled after SimPy's Process.
 *
 * <p>This implementation uses virtual threads (Java 21) to run process logic and provides a
 * blocking await(Event) API via {@link ProcessContext}.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Process implements SimEvent {

    /**
     * Functional interface for process logic.
     */
    @FunctionalInterface
    public interface ProcessFunction {
        Object run(ProcessContext ctx) throws Exception;
    }

    /**
     * Context passed into user logic to await events and access environment.
     */
    public final class ProcessContext {

        public Environment env() {
            return env;
        }

        private void completeFromEvent(Event ev, CompletableFuture<Object> fut) {
            if (ev.ok()) {
                fut.complete(ev.value());
            } else {
                Throwable t = (Throwable) ev.value();
                fut.completeExceptionally(Objects.requireNonNullElseGet(t, () -> new RuntimeException("Event failed without cause")));
            }
        }

        /**
         * Await completion of an event; returns its value or throws if failed/interrupted.
         */
        public Object await(Event e) throws Exception {
            // Record the current target event for observability and SimPy parity
            target = e;
            CompletableFuture<Object> fut = new CompletableFuture<>();
            currentWait.set(fut);
            Event.Callback callback = ev -> {
                env.incPending(); // Signal wake up
                completeFromEvent(ev, fut);
            };
            e.addCallback(callback);
            // Handle race where event already processed before callback registration
            if (e.isProcessed() && !fut.isDone()) {
                completeFromEvent(e, fut);
            }
            env.decPending(); // Yield
            
            // Clear active process while blocked
            env.setActiveProcess(null);
            try {
                return fut.join();
            } catch (RuntimeException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof Interrupt) throw (Interrupt) cause;
                if (cause instanceof Exception) throw (Exception) cause;
                throw ex;
            } finally {
                // Restore active process upon resumption
                env.setActiveProcess(Process.this);
                
                // We are resuming (either via callback or immediate completion)
                // ... (comments) ...
                
                currentWait.compareAndSet(fut, null);
                target = null;
                e.removeCallback(callback);
            }
        }

        /**
         * Type-safe overload to await a compositional SimEvent.
         */
        public Object await(SimEvent e) throws Exception {
            return await(e.asEvent());
        }

    }

    private final Environment env;

    private final Event inner;

    private final ProcessFunction function;

    private final ProcessContext ctx = new ProcessContext();

    private final AtomicReference<CompletableFuture<Object>> currentWait = new AtomicReference<>();

    private volatile Event target;

    private static final ExecutorService EXEC = Executors.newVirtualThreadPerTaskExecutor();

    public Process(Environment env, ProcessFunction function) {
        this(env, function, null);
    }

    public Process(Environment env, ProcessFunction function, String name) {
        this.env = env;
        this.inner = new Event(env, name);
        this.function = function;
        // schedule initialization urgently to start before interrupts
        env.schedule(Initialize.make(env, this), Event.URGENT, 0);
    }

    @Override
    public String toString() {
        String n = inner.getName();
        return getClass().getSimpleName() + "(" + (n != null ? n : "") + ")";
    }

    public String getName() {
        return inner.getName();
    }

    public void setName(String name) {
        inner.setName(name);
    }

    public Event target() {
        return target;
    }

    public boolean isAlive() {
        // Alive means completion event not yet triggered
        return !inner.triggered();
    }

    /**
     * Interrupt this process with optional cause.
     */
    public void interrupt(Object cause) {
        if (!isAlive()) {
            throw new IllegalStateException("Process has terminated and cannot be interrupted");
        }
        // In virtual threads, env.activeProcess() might not be reliable if accessed from outside?
        // But if we are in a test, env.activeProcess() is likely null (main thread) or another process.
        // The test calls ctx.env().activeProcess().interrupt("self");
        // So 'this' is the active process.
        // But wait, 'this' is the process object.
        // If we call interrupt() from within the process itself (via ctx.env().activeProcess()),
        // activeProcess() returns 'this'.
        // So env.activeProcess() == this is true.
        // And we throw IllegalStateException.
        
        // But SimPy docs say: "A process can interrupt itself."
        // Wait, SimPy documentation: "A process can be interrupted by another process or by itself."
        // Actually, in SimPy 4:
        // "A process can interrupt itself. This is useful if you want to abort the current step."
        // Let's check Python source or behavior.
        // Python: `process.interrupt()` schedules an Interruption event.
        // If self-interrupted, it just schedules it.
        // Why did I add this check?
        // Probably to prevent recursive chaos or confusion.
        // But if the test expects it to be rejected ("selfInterruptIsRejected"), then my implementation is CORRECT according to the test name.
        // But the test FAILED with IllegalStateException.
        // Wait, the test asserts Throws(IllegalStateException).
        // And it threw IllegalStateException.
        // Why did it fail?
        // [ERROR] ProcessTest.processInterrupt:36->lambda$processInterrupt$2:35 ? IllegalState A process cannot interrupt itself
        
        // Ah! The failure is in `processInterrupt` test (line 24), NOT `selfInterruptIsRejected`.
        // Let's look at `processInterrupt` test code:
        /*
        24:    @Test
        25:    void processInterrupt() {
        26:        Environment env = new Environment();
        27:        Process p = new Process(env, ctx -> {
        28:            try {
        29:                ctx.await(new Timeout(ctx.env(), 10, null));
        30:                return "ok";
        31:            } catch (Interrupt ex) {
        32:                return "interrupted:" + ex.cause();
        33:            }
        34:        });
        35:        // schedule interrupt at time 1
        36:        new Timeout(env, 1).addCallback(ev -> p.interrupt("preempt"));
        37:        Object ret = env.run(p);
        38:        assertEquals("interrupted:preempt", ret);
        39:    }
        */
        
        // Line 36 adds a callback to a Timeout.
        // When Timeout(1) expires, callback runs.
        // Inside callback: `p.interrupt("preempt")`.
        // At this moment (t=1.0), who is the active process?
        // `env.run(p)` started `p`. `p` is running (awaiting Timeout(10)).
        // `Timeout(1)` triggers. `Environment.step()` processes it.
        // `step()` runs callbacks.
        // The callback runs on the main thread (or whoever called `run()`), BUT `env.activeProcess` might still be set to `p` if we are using virtual threads?
        // `Process._start()` sets activeProcess, runs user function, then sets activeProcess=null in finally.
        // But user function is BLOCKED in `ctx.await()`.
        // `ctx.await()` calls `fut.join()`.
        // So `_start()` is stuck in `try` block.
        // So `env.activeProcess` IS `p`.
        
        // When `Timeout(1)` fires, `Environment.run()` loop is processing it.
        // The main thread (running `env.run()`) is executing the callback.
        // So we are NOT inside the virtual thread of `p`.
        // But `env.activeProcess` field refers to `p` because `p`'s virtual thread hasn't finished/yielded in a way that clears it?
        // Wait, `_start()`:
        // env.setActiveProcess(this);
        // ... run ...
        // finally { env.setActiveProcess(null); }
        
        // If `run()` blocks in `await`, the virtual thread is suspended.
        // `activeProcess` remains set to `p`.
        // This is the issue!
        // When we are in `Environment.step()`, we are in the scheduler loop.
        // The "active process" concept in SimPy usually refers to the process currently executing its generator code.
        // In my implementation, `activeProcess` tracks the thread running the process logic.
        // Since the thread is suspended (awaiting), it technically "holds" the active state in my naive implementation.
        
        // Correct fix: `activeProcess` should only be set when the process logic is ACTIVELY running on CPU.
        // When `await()` blocks, we should clear `activeProcess`?
        // `await()`:
        // ...
        // env.decPending(); // Yield logic? No, this just decrements counter.
        // `fut.join()` blocks the virtual thread.
        // The `env.activeProcess` is NOT cleared.
        
        // If I clear it before `fut.join()` and restore it after?
        // `ctx.await()`:
        // env.setActiveProcess(null);
        // fut.join();
        // env.setActiveProcess(process);
        
        // But `ProcessContext` is inner class, can access `Process.this`.
        // Let's verify if this is safe.
        // If multiple processes are running (concurrently in VTs?), only one can be "active" in SimPy terms?
        // SimPy is single-threaded cooperative. Only one process runs at a time.
        // My implementation uses VTs, so multiple COULD run, but `Environment` lock enforces serial step processing.
        // However, VTs allow user code to block.
        // While blocked, they are not "using" the environment.
        
        // So yes, we should clear activeProcess while waiting.
        
        if (env.activeProcess() == this) {
            // If we are truly the active process (self-interrupt), we might want to allow it or handle it.
            // But here we are being interrupted from an external callback (Timeout).
            // The external callback runs in `Environment.run()`.
            // The Environment shouldn't think `p` is active just because `p` is blocked.
            
            // So I will modify `ProcessContext.await` to clear/restore activeProcess.
            // But for `interrupt()`, should I remove the self-check?
            // The test `selfInterruptIsRejected` explicitly asserts that self-interrupt throws IllegalStateException.
            // So the check MUST exist.
            // The problem is that `processInterrupt` test triggers this check falsely.
            
            throw new IllegalStateException("A process cannot interrupt itself");
        }
        env.schedule(Interruption.make(this, cause), Event.URGENT, 0);
    }

    void _start() {
        env.incPending();
        EXEC.submit(() -> {
            env.setActiveProcess(this);
            try {
                Object ret = function.run(ctx);
                if (!inner.triggered()) {
                    inner.markOk(ret);
                }
                env.schedule(inner, Event.NORMAL, 0);
            } catch (ProcessExit exit) {
                inner.markOk(exit.value());
                env.schedule(inner, Event.NORMAL, 0);
            } catch (Throwable t) {
                inner.fail(stripTraceback(t));
                env.schedule(inner, Event.NORMAL, 0);
            } finally {
                env.setActiveProcess(null);
                env.decPending();
            }
        });
    }

    void _resume(Event e) {
        // Initialize: ok==true -> start process; Interruption: ok==false with Interrupt value
        if (e.ok()) {
            _start();
        } else {
            CompletableFuture<Object> wait = currentWait.get();
            if (wait != null && e.value() instanceof Throwable) {
                env.incPending(); // Waking up via interruption
                wait.completeExceptionally(stripTraceback((Throwable) e.value()));
                return;
            }
            // If no wait is registered (e.g., interrupt arrived before await), fail the process
            if (!inner.triggered() && e.value() instanceof Throwable t) {
                if (target != null) {
                    target.removeCallback(this::_resume);
                    target = null;
                }
                inner.fail(stripTraceback(t));
                env.schedule(inner, Event.NORMAL, 0);
            }
        }
    }

    private Throwable stripTraceback(Throwable t) {
        // Drop self-causation and return the underlying cause if present.
        if (t.getCause() != null && t.getCause() != t) {
            return t.getCause();
        }
        return t;
    }

    /**
     * Expose Environment for internal helper events.
     */
    public Environment env() {
        return env;
    }

    /**
     * Underlying completion event for this process.
     */
    @Override
    public Event asEvent() {
        return inner;
    }

}
