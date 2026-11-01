package social;
import java.util.List;
import java.util.concurrent.*;

public final class ProducerRunner {
    private final int threads;
    public ProducerRunner(int threads) {
        if (threads < 1) throw new IllegalArgumentException("Thread count must be positive");
        this.threads = threads;
    }
    public void run(List<Runnable> producers) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        boolean interrupted = false;
        try {
            var futures = producers.stream().map(executor::submit).toList();
            ExecutionException failure = null;
            for (Future<?> future : futures) {
                try { future.get(); }
                catch (ExecutionException ex) { if (failure == null) failure = ex; else failure.addSuppressed(ex); }
            }
            if (failure != null) throw failure;
        } finally {
            executor.shutdown();
            // All submitted finite producers and synchronous callbacks drain before return.
            while (!executor.isTerminated()) {
                try { executor.awaitTermination(1, TimeUnit.SECONDS); }
                catch (InterruptedException ex) { interrupted = true; }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
