package PR3.task1;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class WorkStealing {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);

        int n;
        // Перевірка на коректність введення розміру масиву
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
        // Перевірка на коректність введення початкового та кінцевого значення
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

        // Генерація випадкових значень для масиву
        int[] array = new int[n];
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            array[i] = rand.nextInt(endValue - startValue + 1) + startValue;
        }

        // Виведення згенерованого масиву
        System.out.println("Generated array: " + Arrays.toString(array));

        // Визначення порогу для поділу завдання (десята частина розміру масиву)
        int threshold = n / 10;
        System.out.println("Threshold for splitting tasks: " + threshold);

        // Створення ForkJoinPool для паралельного виконання задач
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        // Створення задачі для паралельного виконання
        long startTime = System.currentTimeMillis();
        int result = pool.invoke(new SumTask(array, 0, n - 1, threshold));
        long endTime = System.currentTimeMillis();

        // Виведення результату
        System.out.println("Total sum: " + result);
        System.out.println("Execution time: " + (endTime - startTime) + " ms");
    }

    // Рекурсивна задача для пошуку попарної суми
    static class SumTask extends RecursiveTask<Integer> {
        private int[] array;
        private int start;
        private int end;
        private int threshold;

        public SumTask(int[] array, int start, int end, int threshold) {
            this.array = array;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }

        @Override
        protected Integer compute() {
//            System.out.println(Thread.currentThread().getName() + " is processing range: " + start + " to " + end);

            // Зберігаємо значення крайніх елементів
            int firstElement = (start == 0) ? array[start] : 0;
            int lastElement = (end == array.length - 1) ? array[end] : 0;

            // Якщо кількість елементів невелика, обробляємо їх в поточному потоці
            if (end - start <= threshold) {
                int sum = 0;
                // Обчислюємо суму, множимо всі елементи на 2, окрім крайніх
                for (int i = start; i <= end; i++) {
                    sum += array[i] * 2; // інші елементи додаються двічі
                }
                // Повертаємо суму з урахуванням збережених крайніх елементів
                return sum - firstElement - lastElement; // віднімаємо ці елементи, оскільки вони вже додані один раз
            } else {
                // Розподіляємо завдання на дві частини
                int mid = (start + end) / 2;
                SumTask leftTask = new SumTask(array, start, mid, threshold);
                SumTask rightTask = new SumTask(array, mid + 1, end, threshold);

                leftTask.fork(); // Запуск лівої підзадачі
                int rightResult = rightTask.compute(); // Обробка правої частини
                int leftResult = leftTask.join(); // Очікування результату лівої частини

                return leftResult + rightResult;
            }
        }
    }
}