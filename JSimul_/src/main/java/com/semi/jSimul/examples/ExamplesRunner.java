package com.semi.jSimul.examples;

import java.util.concurrent.*;
import java.util.logging.Logger;

public final class ExamplesRunner {

    private static final Logger LOG = Logger.getLogger(ExamplesRunner.class.getName());

    public static void main(String[] args) {
        String which = args != null && args.length > 0 ? args[0] : "all";
        
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            switch (which) {
                case "basic" -> runWithTimeout(executor, "BasicUsageExample", () -> BasicUsageExample.main(new String[0]));
                case "condition" -> runWithTimeout(executor, "ConditionExample", () -> ConditionExample.main(new String[0]));
                case "resource" -> runWithTimeout(executor, "ResourceUsageExample", () -> ResourceUsageExample.main(new String[0]));
                case "simutil" -> runWithTimeout(executor, "SimUtilExample", () -> SimUtilExample.main(new String[0]));
                case "flow" -> runWithTimeout(executor, "FlowLineScenario", () -> FlowLineScenario.main(slice(args)));
                default -> {
                    runWithTimeout(executor, "BasicUsageExample", () -> BasicUsageExample.main(new String[0]));
                    runWithTimeout(executor, "ConditionExample", () -> ConditionExample.main(new String[0]));
                    runWithTimeout(executor, "ResourceUsageExample", () -> ResourceUsageExample.main(new String[0]));
                    runWithTimeout(executor, "SimUtilExample", () -> SimUtilExample.main(new String[0]));
                }
            }
        } finally {
            executor.shutdown();
        }
    }

    private static void runWithTimeout(ExecutorService executor, String name, Runnable task) {
        LOG.info(">>> Starting " + name + " <<<");
        long start = System.nanoTime();
        Future<?> future = executor.submit(task);
        try {
            future.get(10, TimeUnit.SECONDS);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            LOG.info(">>> " + name + " COMPLETED in " + elapsed + "ms <<<");
        } catch (TimeoutException e) {
            future.cancel(true);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            LOG.severe(">>> " + name + " FAILED: Timed out after " + elapsed + "ms <<<");
            e.printStackTrace();
        } catch (Exception e) {
            LOG.severe(">>> " + name + " FAILED with exception: " + e.getMessage() + " <<<");
            e.printStackTrace();
        }
        System.out.println(); 
    }

    private static String[] slice(String[] args) {
        if (args == null || args.length <= 1) return new String[0];
        String[] out = new String[args.length - 1];
        System.arraycopy(args, 1, out, 0, out.length);
        return out;
    }
}
