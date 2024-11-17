package PR3.task1;

import java.util.*;
import java.util.concurrent.*;

public class WorkDealing {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);

        int n;
        while (true) {
            System.out.print("Enter the size of the array (positive integer): ");
            if (scanner.hasNextInt()) {
                n = scanner.nextInt();
                if (n > 0) {
                    break;
                } else {
                    System.out.println("Size must be a positive integer. Please try again.");
                }
            } else {
                System.out.println("Invalid input! Please enter a valid integer.");
                scanner.next(); // очистка буфера
            }
        }

        int startValue;
        int endValue;
        while (true) {
            System.out.print("Enter the start value: ");
            if (scanner.hasNextInt()) {
                startValue = scanner.nextInt();
                System.out.print("Enter the end value: ");
                if (scanner.hasNextInt()) {
                    endValue = scanner.nextInt();

                    if (startValue <= endValue) {
                        break;
                    } else {
                        System.out.println("Start value must be less than or equal to end value. Please try again.");
                    }
                } else {
                    System.out.println("Invalid input! Please enter a valid integer for the end value.");
                    scanner.next(); // очистка буфера
                }
            } else {
                System.out.println("Invalid input! Please enter a valid integer for the start value.");
                scanner.next(); // очистка буфера
            }
        }

        int[] array = new int[n];
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            array[i] = rand.nextInt(endValue - startValue + 1) + startValue;
        }

        System.out.println("Generated array: " + Arrays.toString(array));

        int threshold = n / 10;
        System.out.println("Threshold for splitting tasks: " + threshold);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        BlockingQueue<SumTask> taskQueue = new LinkedBlockingQueue<>();
        long startTime = System.currentTimeMillis();

        SumTask rootTask = new SumTask(array, 0, n - 1, threshold, taskQueue, executor);
        taskQueue.offer(rootTask);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            futures.add(executor.submit(() -> {
                while (true) {
                    SumTask task = taskQueue.poll();
                    if (task == null) {
                        break; // Вихід, якщо немає завдань
                    }
                    try {
                        task.call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }));
        }

        int result = rootTask.call();

        for (Future<Integer> future : futures) {
            future.get();
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        System.out.println("Total sum: " + result);
        System.out.println("Execution time: " + (endTime - startTime) + " ms");
    }

    static class SumTask implements Callable<Integer> {
        private int[] array;
        private int start;
        private int end;
        private int threshold;
        private BlockingQueue<SumTask> taskQueue;
        private ExecutorService executor;

        public SumTask(int[] array, int start, int end, int threshold, BlockingQueue<SumTask> taskQueue, ExecutorService executor) {
            this.array = array;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
            this.taskQueue = taskQueue;
            this.executor = executor;
        }

        @Override
        public Integer call() throws InterruptedException, ExecutionException {
            long startTime = System.nanoTime(); // Початок вимірювання часу
            if (end - start <= 100000) {
                int sum = 0;
                for (int i = start; i <= end; i++) {
                    sum += array[i] * 2;
                }
                if (start == 0) sum -= array[start];
                if (end == array.length - 1) sum -= array[end];
                long endTime = System.nanoTime();
                System.out.println("Thread " + Thread.currentThread().getName() + " processed range [" + start + ", " + end + "] in " + (endTime - startTime) / 1_000_000 + " ms");

                if ((endTime - startTime) / 1_000_000 > 1) { // Якщо виконання займає більше 1 мс
                    System.out.println("Thread " + Thread.currentThread().getName() + " is overloaded. Delegating tasks.");
                    int mid = (start + end) / 2;
                    SumTask leftTask = new SumTask(array, start, mid, threshold, taskQueue, executor);
                    SumTask rightTask = new SumTask(array, mid + 1, end, threshold, taskQueue, executor);

                    taskQueue.offer(leftTask);
                    taskQueue.offer(rightTask);

                    // Поточний потік повертає нуль, щоб завершити своє виконання
                    return 0;
                }
                return sum;

            } else {
                // Розділяємо задачі на підзадачі без делегації
                int mid = (start + end) / 2;
                SumTask leftTask = new SumTask(array, start, mid, threshold, taskQueue, executor);
                SumTask rightTask = new SumTask(array, mid + 1, end, threshold, taskQueue, executor);

                return leftTask.call() + rightTask.call();
            }

        }
    }
}

